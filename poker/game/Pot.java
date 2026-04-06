/*
game/Pot.java
Purpose: Manage all chips in play, handle side pots
Fields:
    • int mainPot - chips available to all players
    • List<SidePot> sidePots - list of side pots (for all-ins)
    • (Inner class) SidePot { int amount; List<Player> eligiblePlayers; }
Methods:
    • void addToPot(Player player, int amount) - collect bet from player
    • void collectBets(List<Player> players) - collect all currentBets
    • void createSidePot(List<Player> allInPlayers, List<Player> activePlayers) - split pot when player goes all-in
    • void distributeWinnings(List<Player> winners, HandEvaluator evaluator) - award chips to winner(s)
    • int getTotalPot() - return main + all side pots
    • void reset() - clear all pots for new hand
*/

import java.util.List;
import java.util.ArrayList;

public class Pot {

    class SidePot {
        int amount;
        List<Player> eligiblePlayers;

        SidePot(List<Player> eligiblePlayers) {
            this.amount = 0;
            this.eligiblePlayers = eligiblePlayers;
        }
    }

    int mainPot;
    List<SidePot> sidePots = new ArrayList<>();

    public void addToPot(int amount) {
        mainPot += amount;
    }

    public void collectBets(List<Player> players) {
        for (Player player : players) {
            mainPot += player.currentBet;
        }
    }

    public void createSidePot(List<Player> allInPlayers, List<Player> activePlayers) {
        int allInAmount = Integer.MAX_VALUE;
        for (Player p: allInPlayers) {
            if (p.totalBet < allInAmount) {
                allInAmount = p.totalBet;
            }
        }
        
        SidePot sp = new SidePot(activePlayers);
        for (Player p: activePlayers) {
            int excess = p.totalBet - allInAmount;
            if (excess > 0) {
                sp.amount += excess;
                mainPot -= excess;
            }
        }
        sidePots.add(sp);


    }
    public void distributeWinnings(List<Player> winners, HandEvaluator evaluator) {
        // Split main pot equally among winners
        int share = mainPot / winners.size();
        for (Player winner : winners) {
            winner.addChips(share);
        }
        
        // Distribute each side pot among eligible winners
        for (SidePot sp : sidePots) {
            List<Player> eligibleWinners = new ArrayList<>();
            for (Player winner : winners) {
                if (sp.eligiblePlayers.contains(winner)) {
                    eligibleWinners.add(winner);
                }
            }
            if (!eligibleWinners.isEmpty()) {
                int sideShare = sp.amount / eligibleWinners.size();
                for (Player winner : eligibleWinners) {
                    winner.addChips(sideShare);
                }
            }
        }
    }

    public int getTotalPot() {
        int total = mainPot;
        for (SidePot sp : sidePots) {
            total += sp.amount;
        }
        return total;
    }

    public void reset() {
        mainPot = 0;
        sidePots.clear();
    }
}

