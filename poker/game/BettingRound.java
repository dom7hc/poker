/*
Purpose: Handle one round of betting (pre-flop, flop, turn, or river)
Fields:
    • int currentBet - highest bet in this round
    • Player lastRaiser - who raised last (to detect when round ends)
    • int minimumRaise - minimum raise amount
Methods:
    • BettingRound(int bigBlindAmount) - constructor, sets minimum raise
    • void processBet(Player player, String action, int amount, ConsoleUI ui) - execute player's decision
    • boolean isRoundComplete(List<Player> activePlayers) - check if all players acted and matched bet
*/

import java.util.List;

public class BettingRound {
    int currentBet;
    Player lastRaiser;
    int minimumRaise;

    BettingRound(int bigBlindAmount) {
        this.minimumRaise = bigBlindAmount;
    }

    public void processBet(Player player, PlayerAction action, int amount, Pot pot) {
        switch (action) {
            case FOLD  -> player.fold();
            case CHECK -> {} // nothing changes
            case CALL  -> {
                int callAmount = currentBet - player.currentBet;
                player.placeBet(callAmount);
                pot.addToPot(callAmount);
            }
            case RAISE -> {
                int raiseAmount = currentBet - player.currentBet + amount;
                player.placeBet(raiseAmount);
                pot.addToPot(raiseAmount);
                currentBet = player.currentBet;
                lastRaiser = player;
            }
            case ALL_IN -> {
                int allIn = player.chipStack;
                player.placeBet(allIn);
                pot.addToPot(allIn);
                player.isAllIn = true;
                if (player.currentBet > currentBet) {
                    currentBet = player.currentBet;
                    lastRaiser = player;
                }
            }
        }
    }

    public boolean isRoundComplete(List<Player> activePlayers) {
        for (Player p : activePlayers) {
            if (!p.isAllIn && p.currentBet != currentBet) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        currentBet = 0;
        lastRaiser = null;
    }
}