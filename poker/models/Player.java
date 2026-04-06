    // • String name - player name
    // • int chipStack - total chips player has
    // • Card[] holeCards - player's 2 private cards (size 2)
    // • int currentBet - amount bet in current betting round
    // • int totalBet - total amount bet in current hand
    // • boolean isActive - true if still in hand (not folded)
    // • boolean isAllIn - true if player is all-in
    // • PlayerController controller - decision-making strategy (AI or human)

public class Player {
    String name;
    int chipStack;
    Card[] holeCards = new Card[2];
    int currentBet;
    int totalBet;
    boolean isActive;
    boolean isAllIn;
    PlayerController controller;

    Player(String name, int startingChips, PlayerController controller) {
        this.name = name;
        this.chipStack = startingChips;
        this.controller = controller;
    }

    public void receiveCard(Card card) {
        if (holeCards[0] == null) {
            holeCards[0] = card;
            return;
        } 
        holeCards[1] = card;
    }

    public void addChips(int amount) {
        chipStack += amount;
    }

    public void removeChips(int amount) {
        chipStack -= amount;
    }

    public void placeBet(int amount) {
        currentBet += amount;
        totalBet += amount;
        removeChips(amount);
    }

    public void clearBet() {
        currentBet = 0;
    }

    public void fold() {
        isActive = false;
    }

    public void resetForNewHand() {
        holeCards = new Card[2];
        currentBet = 0;
        totalBet = 0;
        isActive = true;
        isAllIn = false;
    }
}

