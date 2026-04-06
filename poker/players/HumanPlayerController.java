import java.util.Locale;
import java.util.Scanner;

public class HumanPlayerController implements PlayerController {
    private final Scanner scanner;

    public HumanPlayerController() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public PlayerAction decideAction(Player player, int currentBet, int minimumRaise, int pot, GamePhase phase) {
        int toCall = Math.max(0, currentBet - player.currentBet);

        while (true) {
            if (toCall == 0) {
                System.out.print("Choose action [check, raise, fold, all-in]: ");
            } else {
                System.out.print("Choose action [call, raise, fold, all-in]: ");
            }

            String input = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (input.equals("f") || input.equals("fold")) {
                return PlayerAction.FOLD;
            }
            if (input.equals("a") || input.equals("allin") || input.equals("all-in") || input.equals("all in")) {
                return PlayerAction.ALL_IN;
            }

            if (toCall == 0) {
                if (input.equals("k") || input.equals("check")) {
                    return PlayerAction.CHECK;
                }
                if (input.equals("r") || input.equals("raise")) {
                    if (player.chipStack < minimumRaise) {
                        System.out.println("Not enough chips to make a legal raise. Use check or all-in.");
                        continue;
                    }
                    return PlayerAction.RAISE;
                }
            } else {
                if (input.equals("c") || input.equals("call")) {
                    return PlayerAction.CALL;
                }
                if (input.equals("r") || input.equals("raise")) {
                    int maxRaise = player.chipStack - toCall;
                    if (maxRaise < minimumRaise) {
                        System.out.println("Not enough chips to raise after calling. Use call/fold/all-in.");
                        continue;
                    }
                    return PlayerAction.RAISE;
                }
                if (input.equals("k") || input.equals("check")) {
                    System.out.println("You cannot check when there is a bet to call.");
                    continue;
                }
            }

            System.out.println("Invalid action. Try again.");
        }
    }

    @Override
    public int getRaiseAmount(Player player, int currentBet, int minimumRaise) {
        int toCall = Math.max(0, currentBet - player.currentBet);
        int maxRaise = player.chipStack - toCall;
        if (maxRaise <= 0) {
            return 0;
        }

        int minRaise = Math.min(minimumRaise, maxRaise);
        while (true) {
            System.out.print("Enter raise amount (" + minRaise + " - " + maxRaise + ") or 'all': ");
            String input = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

            if (input.equals("all") || input.equals("allin") || input.equals("all-in") || input.equals("all in")) {
                return maxRaise;
            }

            try {
                int amount = Integer.parseInt(input);
                if (amount >= minRaise && amount <= maxRaise) {
                    return amount;
                }
                System.out.println("Raise must be between " + minRaise + " and " + maxRaise + ".");
            } catch (NumberFormatException ignored) {
                System.out.println("Please enter a number or 'all'.");
            }
        }
    }

}
