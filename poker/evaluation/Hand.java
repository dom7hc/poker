// evaluation/Hand.java
// Purpose: Represent and evaluate 5-7 cards (hole + community)
// Fields:
//     • List<Card> cards - up to 7 cards
//     • HandRank handRank - evaluated hand type
//     • List<Card> bestFiveCards - best 5-card combination
//     • List<Integer> kickers - tiebreaker values
// Methods:
//     • Hand(Card[] holeCards, Card[] communityCards) - constructor
//     • void evaluate(HandEvaluator evaluator) - determine hand rank and best 5 cards
//     • int compareTo(Hand other) - compare two hands (returns -1, 0, or 1)
//     • HandRank getHandRank() - return hand type
//     • String toString() - display hand description

import java.util.List;
import java.util.ArrayList;

public class Hand {
    List<Card> cards = new ArrayList<>();
    HandRank handRank;
    List<Card> bestFiveCards = new ArrayList<>();
    List<Integer> kickers = new ArrayList<>();

    Hand(Card[] holeCards, Card[] communityCards) {
        for (Card card : holeCards) {
            cards.add(card);
        }
        for (Card card : communityCards) {
            cards.add(card);
        }
    }

    public void evaluate(HandEvaluator evaluator) {
        evaluator.evaluate(this);
    }

    public int compareTo(Hand other) {
        return Integer.compare(this.handRank.ordinal(), other.handRank.ordinal());
    }

    public HandRank getHandRank() {
        return handRank;
    }

    @Override
    public String toString() {
        return handRank + " " + bestFiveCards.toString();
    }
}
