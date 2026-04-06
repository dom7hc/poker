import java.util.ArrayList;
import java.util.List;

public class GameDisplay {
    private static final int SIDE_WIDTH = 44;
    private static final int CENTER_WIDTH = 62;
    private static final String EMPTY_SIDE = " ";

    public String renderTable(
        List<Player> players,
        Card[] communityCards,
        int communityCardCount,
        int pot,
        GamePhase phase,
        int dealerPosition,
        String title,
        String turnHint,
        String lastAction
    ) {
        if (players == null || players.isEmpty()) {
            return "(no players)";
        }

        int total = players.size();
        int base = total / 4;
        int remainder = total % 4;

        int topCount = base + (remainder > 0 ? 1 : 0);
        int rightCount = base + (remainder > 1 ? 1 : 0);
        int bottomCount = base + (remainder > 2 ? 1 : 0);
        int leftCount = total - topCount - rightCount - bottomCount;

        List<SeatInfo> top = new ArrayList<>();
        List<SeatInfo> right = new ArrayList<>();
        List<SeatInfo> bottom = new ArrayList<>();
        List<SeatInfo> left = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < topCount && index < total; i++, index++) {
            top.add(new SeatInfo(index, players.get(index)));
        }
        for (int i = 0; i < rightCount && index < total; i++, index++) {
            right.add(new SeatInfo(index, players.get(index)));
        }
        for (int i = 0; i < bottomCount && index < total; i++, index++) {
            bottom.add(new SeatInfo(index, players.get(index)));
        }
        for (int i = 0; i < leftCount && index < total; i++, index++) {
            left.add(new SeatInfo(index, players.get(index)));
        }

        List<SeatInfo> leftDisplay = reverse(left);
        List<SeatInfo> bottomDisplay = reverse(bottom);

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("ASCII Poker Table (Seated View)");

        if (!top.isEmpty()) {
            lines.add("  " + joinSeatLine(top, dealerPosition));
        }

        List<String> center = centerBlock(title, phase, pot, dealerPosition, communityCards, communityCardCount, turnHint, lastAction);
        int rowCount = Math.max(center.size(), Math.max(leftDisplay.size(), right.size()));
        for (int i = 0; i < rowCount; i++) {
            String leftText = i < leftDisplay.size() ? seatText(leftDisplay.get(i), dealerPosition) : EMPTY_SIDE;
            String rightText = i < right.size() ? seatText(right.get(i), dealerPosition) : EMPTY_SIDE;
            String centerText = i < center.size() ? center.get(i) : repeat(" ", CENTER_WIDTH + 2);

            lines.add(
                pad(rightTrim(leftText, SIDE_WIDTH), SIDE_WIDTH)
                    + "  "
                    + centerText
                    + "  "
                    + pad(rightTrim(rightText, SIDE_WIDTH), SIDE_WIDTH)
            );
        }

        if (!bottomDisplay.isEmpty()) {
            lines.add("  " + joinSeatLine(bottomDisplay, dealerPosition));
        }

        return String.join(System.lineSeparator(), lines);
    }

    private List<String> centerBlock(
        String title,
        GamePhase phase,
        int pot,
        int dealerPosition,
        Card[] communityCards,
        int communityCardCount,
        String turnHint,
        String lastAction
    ) {
        List<String> lines = new ArrayList<>();
        String border = "+" + repeat("-", CENTER_WIDTH) + "+";

        String safeTurnHint = (turnHint == null || turnHint.isBlank()) ? "-" : turnHint;
        String safeLastAction = (lastAction == null || lastAction.isBlank()) ? "-" : lastAction;

        lines.add(border);
        lines.add("|" + pad(rightTrim(" " + title, CENTER_WIDTH), CENTER_WIDTH) + "|");
        lines.add(
            "|" + pad(
                rightTrim(" Phase: " + phase + "   Pot: " + pot + "   Dealer Seat: S" + twoDigits(dealerPosition + 1), CENTER_WIDTH),
                CENTER_WIDTH
            ) + "|"
        );
        lines.add("|" + pad(rightTrim(" Turn: " + safeTurnHint, CENTER_WIDTH), CENTER_WIDTH) + "|");
        lines.add("|" + pad(rightTrim(" Last: " + safeLastAction, CENTER_WIDTH), CENTER_WIDTH) + "|");
        lines.add("|" + pad(rightTrim(" Board: " + boardText(communityCards, communityCardCount), CENTER_WIDTH), CENTER_WIDTH) + "|");
        lines.add(border);
        return lines;
    }

    private String boardText(Card[] communityCards, int communityCardCount) {
        if (communityCardCount <= 0) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < communityCardCount; i++) {
            if (i > 0) {
                builder.append(" ");
            }
            builder.append(shortCard(communityCards[i]));
        }
        return builder.toString();
    }

    private String joinSeatLine(List<SeatInfo> seats, int dealerPosition) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < seats.size(); i++) {
            if (i > 0) {
                builder.append("   ");
            }
            builder.append(rightTrim(seatText(seats.get(i), dealerPosition), 36));
        }
        return builder.toString();
    }

    private String seatText(SeatInfo seatInfo, int dealerPosition) {
        Player player = seatInfo.player;
        String dealerMarker = seatInfo.seatIndex == dealerPosition ? "D" : "-";
        String stateMarker = !player.isActive ? "F" : (player.isAllIn ? "AI" : "A");
        return "S" + twoDigits(seatInfo.seatIndex + 1)
            + "[" + dealerMarker + "/" + stateMarker + "] "
            + player.name
            + " " + holeCards(player.holeCards)
            + " b" + player.currentBet
            + " c" + player.chipStack;
    }

    private String holeCards(Card[] cards) {
        if (cards == null || cards.length == 0) {
            return "[?? ??]";
        }
        String first = cards.length > 0 ? shortCard(cards[0]) : "??";
        String second = cards.length > 1 ? shortCard(cards[1]) : "??";
        return "[" + first + " " + second + "]";
    }

    private String shortCard(Card card) {
        if (card == null) {
            return "??";
        }
        String rank = shortRank(card.getRank());
        String suit = shortSuit(card.getSuit());
        return rank + suit;
    }

    private String shortRank(String rank) {
        if (rank == null) {
            return "?";
        }
        return switch (rank) {
            case "Ace" -> "A";
            case "King" -> "K";
            case "Queen" -> "Q";
            case "Jack" -> "J";
            default -> rank;
        };
    }

    private String shortSuit(String suit) {
        if (suit == null || suit.isEmpty()) {
            return "?";
        }
        return switch (suit) {
            case "Hearts" -> "H";
            case "Diamonds" -> "D";
            case "Clubs" -> "C";
            case "Spades" -> "S";
            default -> suit.substring(0, 1).toUpperCase();
        };
    }

    private List<SeatInfo> reverse(List<SeatInfo> source) {
        List<SeatInfo> reversed = new ArrayList<>();
        for (int i = source.size() - 1; i >= 0; i--) {
            reversed.add(source.get(i));
        }
        return reversed;
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private String pad(String text, int width) {
        StringBuilder builder = new StringBuilder(text);
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private String rightTrim(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxWidth) {
            return text;
        }
        if (maxWidth <= 1) {
            return text.substring(0, maxWidth);
        }
        return text.substring(0, maxWidth - 1) + "~";
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private static class SeatInfo {
        final int seatIndex;
        final Player player;

        SeatInfo(int seatIndex, Player player) {
            this.seatIndex = seatIndex;
            this.player = player;
        }
    }
}
