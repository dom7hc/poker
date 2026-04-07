import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameDisplay {
    private static final int SEAT_BLOCK_LINES = 2;
    private static final int HORIZONTAL_SEAT_GAP = 3;
    private static final int SIDE_TO_TABLE_GAP = 3;
    private static final int SIDE_VERTICAL_GAP = 1;

    public String renderTable(
        List<Player> players,
        Card[] communityCards,
        int communityCardCount,
        int pot,
        GamePhase phase,
        int dealerPosition,
        String title,
        String turnHint,
        Map<Player, String> playerLastActions
    ) {
        if (players == null || players.isEmpty()) {
            return "(no players)";
        }

        int playerCount = players.size();
        int seatWidth = seatWidthFor(playerCount);
        int centerWidth = centerWidthFor(playerCount);

        List<Player> top = new ArrayList<>();
        List<Player> right = new ArrayList<>();
        List<Player> bottom = new ArrayList<>();
        List<Player> left = new ArrayList<>();
        distributeSeats(players, top, right, bottom, left);

        List<List<String>> topBlocks = buildSeatBlocks(top, playerLastActions);
        List<List<String>> rightBlocks = buildSeatBlocks(right, playerLastActions);
        List<List<String>> bottomBlocks = reverseBlocks(buildSeatBlocks(bottom, playerLastActions));
        List<List<String>> leftBlocks = reverseBlocks(buildSeatBlocks(left, playerLastActions));

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("ASCII Poker Table (Seated View)");

        if (!topBlocks.isEmpty()) {
            for (int lineIndex = 0; lineIndex < SEAT_BLOCK_LINES; lineIndex++) {
                String row = renderHorizontalLine(topBlocks, lineIndex, seatWidth);
                lines.add(centerLine(row, finalTableWidth(topBlocks, bottomBlocks, seatWidth, centerWidth)));
            }
        }

        List<String> centerLines = centerBlock(
            title,
            phase,
            pot,
            dealerPosition,
            communityCards,
            communityCardCount,
            turnHint,
            centerWidth
        );

        int sideRows = Math.max(sideRowsNeeded(leftBlocks.size()), sideRowsNeeded(rightBlocks.size()));
        int middleRows = Math.max(centerLines.size(), sideRows);
        List<String> leftColumn = renderSideColumn(leftBlocks, seatWidth, middleRows);
        List<String> rightColumn = renderSideColumn(rightBlocks, seatWidth, middleRows);

        int tableWidth = finalTableWidth(topBlocks, bottomBlocks, seatWidth, centerWidth);
        for (int rowIndex = 0; rowIndex < middleRows; rowIndex++) {
            String leftText = rowIndex < leftColumn.size() ? leftColumn.get(rowIndex) : repeat(" ", seatWidth);
            String centerText = rowIndex < centerLines.size() ? centerLines.get(rowIndex) : repeat(" ", centerWidth + 2);
            String rightText = rowIndex < rightColumn.size() ? rightColumn.get(rowIndex) : repeat(" ", seatWidth);

            String row = leftText
                + repeat(" ", SIDE_TO_TABLE_GAP)
                + centerText
                + repeat(" ", SIDE_TO_TABLE_GAP)
                + rightText;
            lines.add(centerLine(row, tableWidth));
        }

        if (!bottomBlocks.isEmpty()) {
            for (int lineIndex = 0; lineIndex < SEAT_BLOCK_LINES; lineIndex++) {
                String row = renderHorizontalLine(bottomBlocks, lineIndex, seatWidth);
                lines.add(centerLine(row, tableWidth));
            }
        }

        return String.join(System.lineSeparator(), lines);
    }

    private void distributeSeats(
        List<Player> players,
        List<Player> top,
        List<Player> right,
        List<Player> bottom,
        List<Player> left
    ) {
        int total = players.size();
        int base = total / 4;
        int remainder = total % 4;

        int topCount = base + (remainder > 0 ? 1 : 0);
        int rightCount = base + (remainder > 1 ? 1 : 0);
        int bottomCount = base + (remainder > 2 ? 1 : 0);
        int leftCount = total - topCount - rightCount - bottomCount;

        int index = 0;
        for (int i = 0; i < topCount && index < total; i++, index++) {
            top.add(players.get(index));
        }
        for (int i = 0; i < rightCount && index < total; i++, index++) {
            right.add(players.get(index));
        }
        for (int i = 0; i < bottomCount && index < total; i++, index++) {
            bottom.add(players.get(index));
        }
        for (int i = 0; i < leftCount && index < total; i++, index++) {
            left.add(players.get(index));
        }
    }

    private List<String> centerBlock(
        String title,
        GamePhase phase,
        int pot,
        int dealerPosition,
        Card[] communityCards,
        int communityCardCount,
        String turnHint,
        int centerWidth
    ) {
        List<String> lines = new ArrayList<>();
        String border = "+" + repeat("-", centerWidth) + "+";

        String safeTurnHint = (turnHint == null || turnHint.isBlank()) ? "-" : turnHint;

        lines.add(border);
        lines.add("|" + pad(truncate(" " + title, centerWidth), centerWidth) + "|");
        lines.add(
            "|" + pad(
                truncate(" Phase: " + phase + "   Pot: " + pot + "   Dealer Seat: S" + twoDigits(dealerPosition + 1), centerWidth),
                centerWidth
            ) + "|"
        );
        lines.add("|" + pad(truncate(" Turn: " + safeTurnHint, centerWidth), centerWidth) + "|");
        for (String boardLine : boardLines(communityCards, communityCardCount, centerWidth)) {
            lines.add("|" + pad(boardLine, centerWidth) + "|");
        }
        lines.add(border);
        return lines;
    }

    private List<String> boardLines(Card[] communityCards, int communityCardCount, int centerWidth) {
        List<String> lines = new ArrayList<>();
        String firstPrefix = " Board: ";
        String continuationPrefix = repeat(" ", firstPrefix.length());

        if (communityCardCount <= 0) {
            lines.add(firstPrefix + "-");
            return lines;
        }

        String current = firstPrefix;
        boolean hasCardInCurrentLine = false;
        for (int i = 0; i < communityCardCount; i++) {
            String token = "[" + fullCard(communityCards[i]) + "]";
            String candidate = hasCardInCurrentLine ? current + " " + token : current + token;

            if (candidate.length() <= centerWidth) {
                current = candidate;
                hasCardInCurrentLine = true;
                continue;
            }

            lines.add(current);
            current = continuationPrefix + token;
            hasCardInCurrentLine = true;
        }

        lines.add(current);
        return lines;
    }

    private List<List<String>> buildSeatBlocks(List<Player> seatPlayers, Map<Player, String> playerLastActions) {
        List<List<String>> blocks = new ArrayList<>();
        for (Player player : seatPlayers) {
            blocks.add(seatBlock(player, playerLastActions));
        }
        return blocks;
    }

    private List<List<String>> reverseBlocks(List<List<String>> source) {
        List<List<String>> reversed = new ArrayList<>();
        for (int i = source.size() - 1; i >= 0; i--) {
            reversed.add(source.get(i));
        }
        return reversed;
    }

    private List<String> renderSideColumn(List<List<String>> seatBlocks, int seatWidth, int totalRows) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < totalRows; i++) {
            lines.add(repeat(" ", seatWidth));
        }
        if (seatBlocks.isEmpty()) {
            return lines;
        }

        if (seatBlocks.size() == 1) {
            int start = Math.max(0, (totalRows - SEAT_BLOCK_LINES) / 2);
            placeSeatBlock(lines, seatBlocks.get(0), start, seatWidth);
            return lines;
        }

        float step = (float) (totalRows - SEAT_BLOCK_LINES) / (float) (seatBlocks.size() - 1);
        for (int i = 0; i < seatBlocks.size(); i++) {
            int start = Math.round(i * step);
            placeSeatBlock(lines, seatBlocks.get(i), start, seatWidth);
        }
        return lines;
    }

    private void placeSeatBlock(List<String> columnLines, List<String> block, int startRow, int seatWidth) {
        for (int i = 0; i < SEAT_BLOCK_LINES; i++) {
            int row = startRow + i;
            if (row < 0 || row >= columnLines.size()) {
                continue;
            }
            columnLines.set(row, pad(truncate(block.get(i), seatWidth), seatWidth));
        }
    }

    private String renderHorizontalLine(List<List<String>> seatBlocks, int lineIndex, int seatWidth) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < seatBlocks.size(); i++) {
            if (i > 0) {
                builder.append(repeat(" ", HORIZONTAL_SEAT_GAP));
            }
            builder.append(pad(truncate(seatBlocks.get(i).get(lineIndex), seatWidth), seatWidth));
        }
        return builder.toString();
    }

    private List<String> seatBlock(Player player, Map<Player, String> playerLastActions) {
        List<String> block = new ArrayList<>();

        String line1 = player.name + " [" + fullCard(player.holeCards, 0) + ", " + fullCard(player.holeCards, 1) + "]";
        String line2 = "Bet: " + player.currentBet + ", Chips: " + player.chipStack;
        String playerAction = playerLastActions == null ? null : playerLastActions.get(player);
        if (playerAction != null && !playerAction.isBlank()) {
            line2 += ", " + playerAction;
        }

        block.add(line1);
        block.add(line2);
        return block;
    }

    private String fullCard(Card[] cards, int index) {
        if (cards == null || index < 0 || index >= cards.length || cards[index] == null) {
            return "?";
        }
        return cards[index].getRank() + " " + cards[index].getSuit();
    }

    private String fullCard(Card card) {
        if (card == null) {
            return "?";
        }
        return card.getRank() + " " + card.getSuit();
    }

    private int sideRowsNeeded(int seatCount) {
        if (seatCount <= 0) {
            return 0;
        }
        return seatCount * SEAT_BLOCK_LINES + (seatCount - 1) * SIDE_VERTICAL_GAP;
    }

    private int finalTableWidth(List<List<String>> topBlocks, List<List<String>> bottomBlocks, int seatWidth, int centerWidth) {
        int middleWidth = seatWidth + SIDE_TO_TABLE_GAP + (centerWidth + 2) + SIDE_TO_TABLE_GAP + seatWidth;
        int topWidth = topBlocks.isEmpty() ? 0 : renderHorizontalLine(topBlocks, 0, seatWidth).length();
        int bottomWidth = bottomBlocks.isEmpty() ? 0 : renderHorizontalLine(bottomBlocks, 0, seatWidth).length();
        return Math.max(middleWidth, Math.max(topWidth, bottomWidth));
    }

    private int seatWidthFor(int playerCount) {
        if (playerCount <= 6) {
            return 34;
        }
        if (playerCount <= 10) {
            return 30;
        }
        if (playerCount <= 14) {
            return 27;
        }
        if (playerCount <= 18) {
            return 24;
        }
        return 22;
    }

    private int centerWidthFor(int playerCount) {
        if (playerCount <= 6) {
            return 62;
        }
        if (playerCount <= 10) {
            return 58;
        }
        if (playerCount <= 14) {
            return 54;
        }
        if (playerCount <= 18) {
            return 50;
        }
        return 46;
    }

    private String centerLine(String line, int totalWidth) {
        if (line.length() >= totalWidth) {
            return line;
        }
        int leftPadding = (totalWidth - line.length()) / 2;
        return repeat(" ", leftPadding) + line;
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

    private String truncate(String text, int maxWidth) {
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
}
