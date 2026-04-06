import java.util.ArrayList;
import java.util.List;

public class MainClass {
    private static final int DEFAULT_PLAYER_COUNT = 4;
    private static final int MIN_PLAYER_COUNT = 2;
    private static final int MAX_PLAYER_COUNT = 22;

    public static void main(String[] args) {
        int totalPlayers = resolvePlayerCount(args);
        List<Player> players = new ArrayList<>();
        players.add(new Player("You", 1000, new HumanPlayerController()));
        for (int i = 1; i < totalPlayers; i++) {
            players.add(new Player("AI-" + i, 1000, new AIPlayerController()));
        }

        System.out.println("Texas Hold'em: You vs " + (totalPlayers - 1) + " AI players");
        TexasHoldemGame game = new TexasHoldemGame(players, 10, 20, false);
        game.startGame(5);
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
}