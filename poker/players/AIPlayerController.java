import java.util.Random;

public class AIPlayerController implements PlayerController {
    private final Random random = new Random();

    @Override
    public PlayerAction decideAction(Player player, int currentBet, int minimumRaise, int pot, GamePhase phase) {
        int toCall = Math.max(0, currentBet - player.currentBet);

        if (!player.isActive || player.isAllIn) {
            return PlayerAction.CHECK;
        }
        if (player.chipStack <= toCall) {
            return PlayerAction.ALL_IN;
        }

        int strength = estimatePreflopStrength(player.holeCards);

        if (toCall == 0) {
            if (strength >= 8 && random.nextDouble() < 0.45) {
                return PlayerAction.RAISE;
            }
            return PlayerAction.CHECK;
        }

        if (strength >= 9 && player.chipStack > toCall + minimumRaise) {
            return random.nextDouble() < 0.6 ? PlayerAction.RAISE : PlayerAction.CALL;
        }

        if (strength >= 6) {
            return PlayerAction.CALL;
        }

        return (toCall <= player.chipStack * 0.08) ? PlayerAction.CALL : PlayerAction.FOLD;
    }

    @Override
    public int getRaiseAmount(Player player, int currentBet, int minimumRaise) {
        int toCall = Math.max(0, currentBet - player.currentBet);
        int stackAfterCall = player.chipStack - toCall;

        if (stackAfterCall <= minimumRaise) {
            return Math.max(0, stackAfterCall);
        }

        int desired = minimumRaise * (1 + random.nextInt(3));
        return Math.min(desired, stackAfterCall);
    }

    private int estimatePreflopStrength(Card[] holeCards) {
        Card first = holeCards[0];
        Card second = holeCards[1];
        if (first == null || second == null) {
            return 0;
        }

        int firstValue = first.getRankValue();
        int secondValue = second.getRankValue();
        int high = Math.max(firstValue, secondValue);
        int low = Math.min(firstValue, secondValue);
        boolean pair = firstValue == secondValue;
        boolean suited = first.getSuit().equals(second.getSuit());
        boolean connected = (high - low) == 1;

        int score = 0;
        if (pair) {
            score += 6 + (high >= 10 ? 2 : 0);
        }
        if (high >= 13) {
            score += 2;
        }
        if (high >= 11 && low >= 10) {
            score += 2;
        }
        if (suited) {
            score += 1;
        }
        if (connected && high >= 9) {
            score += 1;
        }

        return Math.min(score, 10);
    }
}