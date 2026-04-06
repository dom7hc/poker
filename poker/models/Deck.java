public class Deck {
    Card[] cards = new Card[52];
    int topCard = 0;

    Deck() {
        String[] ranks = {"Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King"};
        String[] suits = {"Hearts", "Spades", "Clubs", "Diamonds"};

        int index = 0;
        for (String suit: suits) { 
            for (String rank : ranks) {
                cards[index] = new Card(rank, suit);
                index++;
            }
        }
    }

    public void shuffle() {
        for (int i = cards.length - 1; i > 0; i--) {
            int randomIndex = (int) (Math.random() * (i +1));
            Card temp = cards[i];
            cards[i] = cards[randomIndex];
            cards[randomIndex] = temp;
        }
    }

    public Card deal() {
        return cards[topCard++];
    }

    public int cardsRemaining() {
        return 52 - topCard;
    }

    public void reset() {
        topCard = 0;
    }
}

