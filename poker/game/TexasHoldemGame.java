import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TexasHoldemGame {
    private static final int RECENT_HAND_LIMIT = 4;
    private static final int MIN_PLAYER_COUNT = 2;
    private static final int MAX_PLAYER_COUNT = 22;

    private final List<Player> players;
    private final Deck deck;
    private final Card[] communityCards;
    private int communityCardCount;

    private final Pot pot;
    private final BettingRound bettingRound;
    private final HandEvaluator handEvaluator;
    private final GameDisplay gameDisplay;

    private GamePhase currentPhase;
    private int dealerPosition;

    private final int smallBlindAmount;
    private final int bigBlindAmount;
    private final boolean debugEnabled;
    private final int initialTotalChips;
    private final List<HandSummary> recentHandSummaries = new ArrayList<>();
    private int currentHandNumber;

    public TexasHoldemGame(List<Player> players, int smallBlindAmount, int bigBlindAmount) {
        this(players, smallBlindAmount, bigBlindAmount, false);
    }

    public TexasHoldemGame(List<Player> players, int smallBlindAmount, int bigBlindAmount, boolean debugEnabled) {
        validatePlayerCount(players);
        this.players = new ArrayList<>(players);
        this.smallBlindAmount = smallBlindAmount;
        this.bigBlindAmount = bigBlindAmount;
        this.debugEnabled = debugEnabled;

        this.deck = new Deck();
        this.communityCards = new Card[5];
        this.pot = new Pot();
        this.bettingRound = new BettingRound(bigBlindAmount);
        this.handEvaluator = new HandEvaluator();
        this.gameDisplay = new GameDisplay();

        this.currentPhase = GamePhase.PRE_FLOP;
        this.dealerPosition = 0;
        this.initialTotalChips = calculateCurrentChipTotal();

        debugLog("Debug mode enabled. Initial table chips: " + initialTotalChips);
    }

    public void startGame() {
        startGame(20);
    }

    public void startGame(int maxHands) {
        int handNumber = 1;
        while (countPlayersWithChips() > 1 && handNumber <= maxHands) {
            currentHandNumber = handNumber;
            playHand();
            runPostHandDiagnostics(handNumber);
            moveDealerButton();
            handNumber++;
        }
        System.out.println("\nGame ended.");
    }

    public void playHand() {
        prepareNextHand();
        if (countPlayersWithChips() < 2) {
            return;
        }
        debugValidateState("after prepare");

        String lastAction = postBlinds();
        debugValidateState("after blinds");
        dealHoleCards();
        refreshTable("Hand " + currentHandNumber + " - Hole Cards", null, lastAction);
        debugValidateState("after hole cards");

        currentPhase = GamePhase.PRE_FLOP;
        runBettingRound(false, getNextActingSeat(getBigBlindPosition()), "Hand " + currentHandNumber + " - PRE_FLOP");
        debugValidateState("after pre-flop betting");
        if (hasSingleActivePlayer()) {
            String outcome = payoutSingleRemainingPlayer();
            refreshTable("Hand " + currentHandNumber + " - PRE_FLOP", null, outcome);
            debugValidateState("after early payout (pre-flop)");
            return;
        }

        dealFlop();
        refreshTable("Hand " + currentHandNumber + " - FLOP", null, "Flop dealt.");
        debugValidateState("after flop deal");
        currentPhase = GamePhase.FLOP;
        runBettingRound(true, getNextActingSeat(dealerPosition), "Hand " + currentHandNumber + " - FLOP");
        debugValidateState("after flop betting");
        if (hasSingleActivePlayer()) {
            String outcome = payoutSingleRemainingPlayer();
            refreshTable("Hand " + currentHandNumber + " - FLOP", null, outcome);
            debugValidateState("after early payout (flop)");
            return;
        }

        dealTurn();
        refreshTable("Hand " + currentHandNumber + " - TURN", null, "Turn dealt.");
        debugValidateState("after turn deal");
        currentPhase = GamePhase.TURN;
        runBettingRound(true, getNextActingSeat(dealerPosition), "Hand " + currentHandNumber + " - TURN");
        debugValidateState("after turn betting");
        if (hasSingleActivePlayer()) {
            String outcome = payoutSingleRemainingPlayer();
            refreshTable("Hand " + currentHandNumber + " - TURN", null, outcome);
            debugValidateState("after early payout (turn)");
            return;
        }

        dealRiver();
        refreshTable("Hand " + currentHandNumber + " - RIVER", null, "River dealt.");
        debugValidateState("after river deal");
        currentPhase = GamePhase.RIVER;
        runBettingRound(true, getNextActingSeat(dealerPosition), "Hand " + currentHandNumber + " - RIVER");
        debugValidateState("after river betting");
        if (hasSingleActivePlayer()) {
            String outcome = payoutSingleRemainingPlayer();
            refreshTable("Hand " + currentHandNumber + " - RIVER", null, outcome);
            debugValidateState("after early payout (river)");
            return;
        }

        currentPhase = GamePhase.SHOWDOWN;
        List<Player> winners = determineWinners();
        if (!winners.isEmpty()) {
            String outcome = distributePot(winners);
            refreshTable("Hand " + currentHandNumber + " - SHOWDOWN", null, outcome);
        } else {
            pot.reset();
            refreshTable("Hand " + currentHandNumber + " - SHOWDOWN", null, "No winners detected.");
        }
        debugValidateState("after showdown payout");
    }

    private void prepareNextHand() {
        deck.reset();
        deck.shuffle();
        currentPhase = GamePhase.PRE_FLOP;

        pot.reset();
        bettingRound.reset();

        communityCardCount = 0;
        Arrays.fill(communityCards, null);

        for (Player player : players) {
            if (player.chipStack > 0) {
                player.resetForNewHand();
            } else {
                player.isActive = false;
                player.isAllIn = true;
                player.currentBet = 0;
                player.totalBet = 0;
            }
        }
    }

    private String postBlinds() {
        int smallBlindPosition = getSmallBlindPosition();
        int bigBlindPosition = getBigBlindPosition();

        Player smallBlindPlayer = players.get(smallBlindPosition);
        Player bigBlindPlayer = players.get(bigBlindPosition);

        int smallBlind = Math.min(smallBlindAmount, smallBlindPlayer.chipStack);
        int bigBlind = Math.min(bigBlindAmount, bigBlindPlayer.chipStack);

        smallBlindPlayer.placeBet(smallBlind);
        bigBlindPlayer.placeBet(bigBlind);

        if (smallBlindPlayer.chipStack == 0) {
            smallBlindPlayer.isAllIn = true;
        }
        if (bigBlindPlayer.chipStack == 0) {
            bigBlindPlayer.isAllIn = true;
        }

        pot.addToPot(smallBlind);
        pot.addToPot(bigBlind);

        bettingRound.currentBet = bigBlindPlayer.currentBet;
        bettingRound.lastRaiser = bigBlindPlayer;

        return smallBlindPlayer.name + " posted small blind " + smallBlind
            + ", " + bigBlindPlayer.name + " posted big blind " + bigBlind + ".";
    }

    private void dealHoleCards() {
        for (int i = 0; i < 2; i++) {
            for (Player player : players) {
                if (player.isActive) {
                    player.receiveCard(deck.deal());
                }
            }
        }
    }

    private void dealFlop() {
        communityCards[communityCardCount++] = deck.deal();
        communityCards[communityCardCount++] = deck.deal();
        communityCards[communityCardCount++] = deck.deal();
    }

    private void dealTurn() {
        communityCards[communityCardCount++] = deck.deal();
    }

    private void dealRiver() {
        communityCards[communityCardCount++] = deck.deal();
    }

    private void runBettingRound(boolean resetRoundState, int startSeat, String tableTitle) {
        if (resetRoundState) {
            bettingRound.reset();
            for (Player player : players) {
                player.clearBet();
            }
        }

        String lastAction = "Betting round started.";
        // Post-flop rounds start with currentBet=0, so force one action cycle.
        boolean forceActionCycle = resetRoundState;
        int safetyCounter = 0;
        while ((forceActionCycle || !bettingRound.isRoundComplete(getActivePlayers())) && safetyCounter < 20) {
            forceActionCycle = false;
            safetyCounter++;
            boolean anyActionTaken = false;

            for (int i = 0; i < players.size(); i++) {
                int seat = (startSeat + i) % players.size();
                Player player = players.get(seat);

                if (!player.isActive || player.isAllIn || player.chipStack <= 0) {
                    continue;
                }
                anyActionTaken = true;
                refreshTable(tableTitle, player, lastAction);

                PlayerAction action = chooseLegalAction(player);
                int raiseAmount = 0;
                if (action == PlayerAction.RAISE) {
                    raiseAmount = getLegalRaiseAmount(player);
                    if (raiseAmount <= 0) {
                        action = PlayerAction.CALL;
                    }
                }

                bettingRound.processBet(player, action, raiseAmount, pot);

                if (player.chipStack == 0) {
                    player.isAllIn = true;
                }

                String actionDetails = action == PlayerAction.RAISE ? " by " + raiseAmount : "";
                lastAction = player.name + " -> " + action + actionDetails + " | pot=" + pot.getTotalPot();
                refreshTable(tableTitle, null, lastAction);

                if (hasSingleActivePlayer()) {
                    break;
                }
            }

            if (hasSingleActivePlayer()) {
                break;
            }
            if (!anyActionTaken) {
                break;
            }
        }

        refreshTable(tableTitle, null, "Betting round complete.");
        for (Player player : players) {
            player.clearBet();
        }
        bettingRound.reset();
    }

    private PlayerAction chooseLegalAction(Player player) {
        int toCall = Math.max(0, bettingRound.currentBet - player.currentBet);

        PlayerAction desired = player.controller.decideAction(
            player,
            bettingRound.currentBet,
            bettingRound.minimumRaise,
            pot.getTotalPot(),
            currentPhase
        );

        if (toCall == 0) {
            if (desired == PlayerAction.CALL) {
                return PlayerAction.CHECK;
            }
            return desired;
        }

        if (desired == PlayerAction.CHECK) {
            return PlayerAction.CALL;
        }
        if ((desired == PlayerAction.CALL || desired == PlayerAction.RAISE) && player.chipStack <= toCall) {
            return PlayerAction.ALL_IN;
        }
        return desired;
    }

    private int getLegalRaiseAmount(Player player) {
        int toCall = Math.max(0, bettingRound.currentBet - player.currentBet);
        int stackAfterCall = player.chipStack - toCall;
        if (stackAfterCall <= 0) {
            return 0;
        }

        int desired = player.controller.getRaiseAmount(player, bettingRound.currentBet, bettingRound.minimumRaise);
        if (desired < bettingRound.minimumRaise) {
            desired = bettingRound.minimumRaise;
        }
        return Math.min(desired, stackAfterCall);
    }

    private List<Player> determineWinners() {
        List<Player> contenders = getActivePlayers();
        List<Player> winners = new ArrayList<>();
        if (contenders.isEmpty()) {
            return winners;
        }
        if (contenders.size() == 1) {
            winners.add(contenders.get(0));
            return winners;
        }

        Hand bestHand = null;

        for (Player challenger : contenders) {
            Hand challengerHand = buildHand(challenger);
            challengerHand.evaluate(handEvaluator);

            if (bestHand == null) {
                bestHand = challengerHand;
                winners.add(challenger);
                continue;
            }

            int comparison = challengerHand.compareTo(bestHand);
            if (comparison > 0) {
                winners.clear();
                winners.add(challenger);
                bestHand = challengerHand;
            } else if (comparison == 0) {
                winners.add(challenger);
            }
        }

        return winners;
    }

    private Hand buildHand(Player player) {
        Card[] board = Arrays.copyOf(communityCards, communityCardCount);
        return new Hand(player.holeCards, board);
    }

    private String distributePot(List<Player> winners) {
        if (winners.isEmpty()) {
            pot.reset();
            return "No winners. Pot reset.";
        }

        int total = pot.getTotalPot();
        int baseShare = total / winners.size();
        int remainder = total % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            Player winner = winners.get(i);
            int payout = baseShare + (i < remainder ? 1 : 0);
            winner.addChips(payout);
        }

        String resultMessage;
        if (winners.size() == 1) {
            resultMessage = "Winner: " + winners.get(0).name + " wins " + total + " chips.";
        } else {
            String winnerNames = formatPlayerNames(winners);
            resultMessage = "Split pot: " + winnerNames + " share " + total + " chips (" + baseShare + " each"
                + (remainder > 0 ? ", remainder to first " + remainder + " winner(s)" : "")
                + ").";
        }
        recordHandSummary(winners, total);
        pot.reset();
        return resultMessage;
    }

    private String payoutSingleRemainingPlayer() {
        List<Player> activePlayers = getActivePlayers();
        if (activePlayers.isEmpty()) {
            pot.reset();
            return "No active players left. Pot reset.";
        }
        return distributePot(activePlayers);
    }

    private boolean hasSingleActivePlayer() {
        return getActivePlayers().size() == 1;
    }

    private List<Player> getActivePlayers() {
        List<Player> active = new ArrayList<>();
        for (Player player : players) {
            if (player.isActive) {
                active.add(player);
            }
        }
        return active;
    }

    private int countPlayersWithChips() {
        int count = 0;
        for (Player player : players) {
            if (player.chipStack > 0) {
                count++;
            }
        }
        return count;
    }

    private void moveDealerButton() {
        dealerPosition = getNextSeatWithChips(dealerPosition);
    }

    private int getSmallBlindPosition() {
        return getNextSeatWithChips(dealerPosition);
    }

    private int getBigBlindPosition() {
        return getNextSeatWithChips(getSmallBlindPosition());
    }

    private int getNextSeatWithChips(int fromSeat) {
        for (int i = 1; i <= players.size(); i++) {
            int seat = (fromSeat + i) % players.size();
            if (players.get(seat).chipStack > 0) {
                return seat;
            }
        }
        return fromSeat;
    }

    private int getNextActingSeat(int fromSeat) {
        for (int i = 1; i <= players.size(); i++) {
            int seat = (fromSeat + i) % players.size();
            Player player = players.get(seat);
            if (player.isActive && !player.isAllIn && player.chipStack > 0) {
                return seat;
            }
        }
        return fromSeat;
    }

    private String formatCommunityCards() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < communityCardCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(communityCards[i]);
        }
        return builder.toString();
    }

    private void refreshTable(String title, Player currentTurnPlayer, String lastAction) {
        clearConsole();
        String turnHint = currentTurnPlayer == null ? "-" : formatTurnHint(currentTurnPlayer);
        System.out.println(
            gameDisplay.renderTable(
                players,
                communityCards,
                communityCardCount,
                pot.getTotalPot(),
                currentPhase,
                dealerPosition,
                title,
                turnHint,
                lastAction
            )
        );
    }

    private String formatTurnHint(Player player) {
        int seat = players.indexOf(player);
        int toCall = Math.max(0, bettingRound.currentBet - player.currentBet);
        return "S" + twoDigits(seat + 1) + " " + player.name + " to act (to call " + toCall + ")";
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void validatePlayerCount(List<Player> playersToValidate) {
        if (playersToValidate == null) {
            throw new IllegalArgumentException("Players list cannot be null.");
        }

        int count = playersToValidate.size();
        if (count < MIN_PLAYER_COUNT || count > MAX_PLAYER_COUNT) {
            throw new IllegalArgumentException(
                "Player count must be between " + MIN_PLAYER_COUNT + " and " + MAX_PLAYER_COUNT + ". Got: " + count
            );
        }
    }

    private String formatPlayerNames(List<Player> playersToFormat) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < playersToFormat.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(playersToFormat.get(i).name);
        }
        return builder.toString();
    }

    private void recordHandSummary(List<Player> winners, int potSize) {
        String boardSnapshot = communityCardCount == 0 ? "-" : formatCommunityCards();
        String stacksSnapshot = formatStackSnapshot();

        HandSummary summary = new HandSummary(
            currentHandNumber,
            formatPlayerNames(winners),
            potSize,
            boardSnapshot,
            stacksSnapshot
        );

        recentHandSummaries.add(summary);
        if (recentHandSummaries.size() > RECENT_HAND_LIMIT) {
            recentHandSummaries.remove(0);
        }
    }

    private String formatStackSnapshot() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            Player player = players.get(i);
            builder.append(player.name).append(":").append(player.chipStack);
        }
        return builder.toString();
    }

    private void printRecentHandsTable() {
        final int handWidth = 4;
        final int winnersWidth = 18;
        final int potWidth = 6;
        final int boardWidth = 36;
        final int stacksWidth = 36;

        String separator = "+"
            + repeat("-", handWidth + 2) + "+"
            + repeat("-", winnersWidth + 2) + "+"
            + repeat("-", potWidth + 2) + "+"
            + repeat("-", boardWidth + 2) + "+"
            + repeat("-", stacksWidth + 2) + "+";

        System.out.println();
        System.out.println("Recent 4-Hand Summary");
        System.out.println(separator);
        System.out.println(
            "| " + pad("Hand", handWidth)
                + " | " + pad("Winner(s)", winnersWidth)
                + " | " + pad("Pot", potWidth)
                + " | " + pad("Board", boardWidth)
                + " | " + pad("Stacks", stacksWidth)
                + " |"
        );
        System.out.println(separator);

        for (HandSummary summary : recentHandSummaries) {
            System.out.println(
                "| " + pad(String.valueOf(summary.handNumber), handWidth)
                    + " | " + pad(summary.winners, winnersWidth)
                    + " | " + pad(String.valueOf(summary.potSize), potWidth)
                    + " | " + pad(summary.board, boardWidth)
                    + " | " + pad(summary.stacks, stacksWidth)
                    + " |"
            );
        }

        System.out.println(separator);
    }

    private String pad(String value, int width) {
        if (value == null) {
            value = "";
        }
        if (value.length() > width) {
            if (width <= 1) {
                return value.substring(0, width);
            }
            return value.substring(0, width - 1) + "~";
        }
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private void runPostHandDiagnostics(int handNumber) {
        if (!debugEnabled) {
            return;
        }
        debugAssert(pot.getTotalPot() == 0, "Pot should be 0 after hand " + handNumber + " but was " + pot.getTotalPot());
        int currentTotalChips = calculateCurrentChipTotal();
        debugAssert(
            currentTotalChips == initialTotalChips,
            "Chip conservation failed after hand " + handNumber + ". Expected " + initialTotalChips + ", got " + currentTotalChips
        );
        debugLog("Hand " + handNumber + " checks passed. Total chips: " + currentTotalChips + ".");
        debugPrintPlayerState();
    }

    private void debugValidateState(String checkpoint) {
        if (!debugEnabled) {
            return;
        }
        debugAssert(bettingRound.currentBet >= 0, "Negative bettingRound.currentBet at " + checkpoint);
        debugAssert(communityCardCount >= 0 && communityCardCount <= 5, "Invalid community card count at " + checkpoint);

        for (Player player : players) {
            debugAssert(player.chipStack >= 0, player.name + " has negative chip stack at " + checkpoint);
            debugAssert(player.currentBet >= 0, player.name + " has negative current bet at " + checkpoint);
            debugAssert(player.totalBet >= 0, player.name + " has negative total bet at " + checkpoint);
            if (player.isAllIn) {
                debugAssert(player.chipStack == 0, player.name + " is all-in but has non-zero stack at " + checkpoint);
            }
        }

        validateNoDuplicateVisibleCards(checkpoint);

        int currentTotalChips = calculateCurrentChipTotal();
        debugAssert(
            currentTotalChips == initialTotalChips,
            "Chip conservation failed at " + checkpoint + ". Expected " + initialTotalChips + ", got " + currentTotalChips
        );
    }

    private void validateNoDuplicateVisibleCards(String checkpoint) {
        Set<String> seenCards = new HashSet<>();

        for (Player player : players) {
            if (!player.isActive) {
                continue;
            }
            for (Card holeCard : player.holeCards) {
                if (holeCard == null) {
                    continue;
                }
                String key = holeCard.getRank() + "|" + holeCard.getSuit();
                debugAssert(seenCards.add(key), "Duplicate hole card " + key + " at " + checkpoint);
            }
        }

        for (int i = 0; i < communityCardCount; i++) {
            Card boardCard = communityCards[i];
            debugAssert(boardCard != null, "Community card slot " + i + " is null at " + checkpoint);
            String key = boardCard.getRank() + "|" + boardCard.getSuit();
            debugAssert(seenCards.add(key), "Duplicate community card " + key + " at " + checkpoint);
        }
    }

    private int calculateCurrentChipTotal() {
        int total = pot.getTotalPot();
        for (Player player : players) {
            total += player.chipStack;
        }
        return total;
    }

    private void debugPrintPlayerState() {
        if (!debugEnabled) {
            return;
        }
        debugLog("Player state snapshot:");
        for (Player player : players) {
            debugLog(
                "  " + player.name
                    + " stack=" + player.chipStack
                    + ", active=" + player.isActive
                    + ", allIn=" + player.isAllIn
                    + ", currentBet=" + player.currentBet
                    + ", totalBet=" + player.totalBet
            );
        }
    }

    private void debugAssert(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("[DEBUG] " + message);
        }
    }

    private void debugLog(String message) {
        if (debugEnabled) {
            System.out.println("[DEBUG] " + message);
        }
    }

    private static class HandSummary {
        final int handNumber;
        final String winners;
        final int potSize;
        final String board;
        final String stacks;

        HandSummary(int handNumber, String winners, int potSize, String board, String stacks) {
            this.handNumber = handNumber;
            this.winners = winners;
            this.potSize = potSize;
            this.board = board;
            this.stacks = stacks;
        }
    }
}
