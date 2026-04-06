// Purpose: Contract for player decision-making (AI or human)
// Methods:
//     • String decideAction(Player player, int currentBet, int minimumRaise, int pot, GamePhase phase) - return action: "fold", "check", "call", "raise", "all-in"
//     • int getRaiseAmount(Player player, int currentBet, int minimumRaise) - if raising, return raise amount


public interface PlayerController {
    PlayerAction decideAction(Player player, int currentBet, int minimumRaise, int pot, GamePhase phase);
    int getRaiseAmount(Player player, int currentBet, int minimumRaise);
}