

class Card {
    String rank;
    String suit;

    Card(String rank, String suit) {
        this.rank = rank;
        this.suit = suit;
    }

    //Getter methods
    public String getRank() {
        return rank;
    }

    public String getSuit() {
        return suit;
    }

    public int getRankValue() {
        return switch (this.rank) {
            case "Ace"   -> 14;
            case "2"     -> 2;
            case "3"     -> 3;
            case "4"     -> 4;
            case "5"     -> 5;
            case "6"     -> 6;
            case "7"     -> 7;
            case "8"     -> 8;
            case "9"     -> 9;
            case "10"    -> 10;
            case "Jack"  -> 11;
            case "Queen" -> 12;
            case "King"  -> 13;
            default      -> 0;
        };
    }

    @Override
    public String toString() {
        return rank + " " + suit;
    }
}

