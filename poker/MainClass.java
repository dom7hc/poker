import java.util.ArrayList;
import java.util.List;

public class MainClass {
    private static final int DEFAULT_PLAYER_COUNT = 4;
    private static final int MIN_PLAYER_COUNT = 2;
    private static final int MAX_PLAYER_COUNT = 22;
    private static final int DEFAULT_HANDS_PER_MATCH = 5;
    private static final int MIN_HANDS_PER_MATCH = 1;

    public static void main(String[] args) {
        int totalPlayers = resolvePlayerCount(args);
        int handsPerMatch = resolveHandsPerMatch(args);
        System.out.println("Texas Hold'em: You vs " + (totalPlayers - 1) + " AI players");
        System.out.println("Match length: " + handsPerMatch + " hand(s)");

        boolean playAgain = true;
        while (playAgain) {
            List<Player> players = createPlayers(totalPlayers);
            TexasHoldemGame game = new TexasHoldemGame(players, 10, 20, false);
            game.startGame(handsPerMatch);
            playAgain = HumanPlayerController.askYesNo("Play again? [y/n]: ");
        }

        System.out.println("Thanks for playing.");
    }

    private static int resolvePlayerCount(String[] args) {
        if (args == null || args.length == 0) {
            return DEFAULT_PLAYER_COUNT;
        }

        try {
            int requestedCount = Integer.parseInt(args[0]);
            if (requestedCount < MIN_PLAYER_COUNT || requestedCount > MAX_PLAYER_COUNT) {
                System.out.println(
                    "Player count must be between " + MIN_PLAYER_COUNT + " and " + MAX_PLAYER_COUNT + ". Using default " + DEFAULT_PLAYER_COUNT + "."
                );
                return DEFAULT_PLAYER_COUNT;
            }
            return requestedCount;
        } catch (NumberFormatException ex) {
            System.out.println("Invalid player count '" + args[0] + "'. Using default " + DEFAULT_PLAYER_COUNT + ".");
            return DEFAULT_PLAYER_COUNT;
        }
    }

    private static int resolveHandsPerMatch(String[] args) {
        if (args == null || args.length < 2) {
            return DEFAULT_HANDS_PER_MATCH;
        }

        try {
            int requestedHands = Integer.parseInt(args[1]);
            if (requestedHands < MIN_HANDS_PER_MATCH) {
                System.out.println(
                    "Hands per match must be >= " + MIN_HANDS_PER_MATCH + ". Using default " + DEFAULT_HANDS_PER_MATCH + "."
                );
                return DEFAULT_HANDS_PER_MATCH;
            }
            return requestedHands;
        } catch (NumberFormatException ex) {
            System.out.println("Invalid hands per match '" + args[1] + "'. Using default " + DEFAULT_HANDS_PER_MATCH + ".");
            return DEFAULT_HANDS_PER_MATCH;
        }
    }

    private static List<Player> createPlayers(int totalPlayers) {
        List<Player> players = new ArrayList<>();
        players.add(new Player("You", 1000, new HumanPlayerController()));
        for (int i = 1; i < totalPlayers; i++) {
            players.add(new Player("AI-" + i, 1000, new AIPlayerController()));
        }
        return players;
    }
}