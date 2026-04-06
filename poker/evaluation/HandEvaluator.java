import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandEvaluator {
    public void evaluate(Hand hand) {
        Evaluation evaluation = evaluateCards(hand.cards);
        hand.handRank = evaluation.rank;
        hand.kickers.clear();
        hand.kickers.addAll(evaluation.kickers);
        hand.bestFiveCards.clear();
        hand.bestFiveCards.addAll(evaluation.bestFiveCards);
    }

    private Evaluation evaluateCards(List<Card> cards) {
        List<Card> allCards = sortCardsByRankDesc(cards);
        Map<Integer, List<Card>> cardsByRank = groupByRank(allCards);
        Map<String, List<Card>> cardsBySuit = groupBySuit(allCards);

        Evaluation straightFlushEvaluation = evaluateStraightFlush(cardsBySuit);
        if (straightFlushEvaluation != null) {
            return straightFlushEvaluation;
        }

        int fourOfKindRank = findHighestRankWithCount(cardsByRank, 4);
        if (fourOfKindRank != -1) {
            int kickerRank = highestDistinctRankExcluding(allCards, fourOfKindRank);
            List<Card> best = new ArrayList<>();
            best.addAll(takeCardsOfRank(cardsByRank, fourOfKindRank, 4));
            Card kickerCard = findHighestCardByRank(allCards, kickerRank);
            if (kickerCard != null) {
                best.add(kickerCard);
            }
            return new Evaluation(HandRank.FOUR_OF_A_KIND, listOf(fourOfKindRank, kickerRank), best);
        }

        List<Integer> trips = ranksWithMinimumCount(cardsByRank, 3);
        int fullHouseTrip = -1;
        int fullHousePair = -1;
        if (!trips.isEmpty()) {
            fullHouseTrip = trips.get(0);
            fullHousePair = findHighestPairRankForFullHouse(cardsByRank, fullHouseTrip);
        }
        if (fullHouseTrip != -1 && fullHousePair != -1) {
            List<Card> best = new ArrayList<>();
            best.addAll(takeCardsOfRank(cardsByRank, fullHouseTrip, 3));
            best.addAll(takeCardsOfRank(cardsByRank, fullHousePair, 2));
            return new Evaluation(HandRank.FULL_HOUSE, listOf(fullHouseTrip, fullHousePair), best);
        }

        List<Card> flushCards = findBestFlush(cardsBySuit);
        if (flushCards != null) {
            return new Evaluation(HandRank.FLUSH, rankValues(flushCards), flushCards);
        }

        int straightHigh = findStraightHigh(allCards);
        if (straightHigh != -1) {
            List<Card> best = buildStraightCards(allCards, straightHigh);
            return new Evaluation(HandRank.STRAIGHT, listOf(straightHigh), best);
        }

        if (!trips.isEmpty()) {
            int tripRank = trips.get(0);
            List<Integer> kickerRanks = topDistinctRanksExcluding(allCards, listOf(tripRank), 2);
            List<Card> best = new ArrayList<>();
            best.addAll(takeCardsOfRank(cardsByRank, tripRank, 3));
            addCardsByRanks(best, allCards, kickerRanks);
            List<Integer> tiebreakers = new ArrayList<>();
            tiebreakers.add(tripRank);
            tiebreakers.addAll(kickerRanks);
            return new Evaluation(HandRank.THREE_OF_A_KIND, tiebreakers, best);
        }

        List<Integer> pairs = ranksWithMinimumCount(cardsByRank, 2);
        if (pairs.size() >= 2) {
            int highPair = pairs.get(0);
            int lowPair = pairs.get(1);
            int kickerRank = highestDistinctRankExcluding(allCards, highPair, lowPair);
            List<Card> best = new ArrayList<>();
            best.addAll(takeCardsOfRank(cardsByRank, highPair, 2));
            best.addAll(takeCardsOfRank(cardsByRank, lowPair, 2));
            Card kickerCard = findHighestCardByRank(allCards, kickerRank);
            if (kickerCard != null) {
                best.add(kickerCard);
            }
            return new Evaluation(HandRank.TWO_PAIR, listOf(highPair, lowPair, kickerRank), best);
        }

        if (pairs.size() == 1) {
            int pairRank = pairs.get(0);
            List<Integer> kickerRanks = topDistinctRanksExcluding(allCards, listOf(pairRank), 3);
            List<Card> best = new ArrayList<>();
            best.addAll(takeCardsOfRank(cardsByRank, pairRank, 2));
            addCardsByRanks(best, allCards, kickerRanks);
            List<Integer> tiebreakers = new ArrayList<>();
            tiebreakers.add(pairRank);
            tiebreakers.addAll(kickerRanks);
            return new Evaluation(HandRank.ONE_PAIR, tiebreakers, best);
        }

        List<Card> highCards = takeTopCards(allCards, 5);
        return new Evaluation(HandRank.HIGH_CARD, rankValues(highCards), highCards);
    }

    private Evaluation evaluateStraightFlush(Map<String, List<Card>> cardsBySuit) {
        int bestStraightHigh = -1;
        List<Card> bestSuitCards = null;

        for (List<Card> suitCards : cardsBySuit.values()) {
            if (suitCards.size() < 5) {
                continue;
            }
            int straightHigh = findStraightHigh(suitCards);
            if (straightHigh > bestStraightHigh) {
                bestStraightHigh = straightHigh;
                bestSuitCards = suitCards;
            }
        }

        if (bestStraightHigh == -1 || bestSuitCards == null) {
            return null;
        }

        HandRank rank = (bestStraightHigh == 14) ? HandRank.ROYAL_FLUSH : HandRank.STRAIGHT_FLUSH;
        List<Card> best = buildStraightCards(bestSuitCards, bestStraightHigh);
        return new Evaluation(rank, listOf(bestStraightHigh), best);
    }

    private List<Card> sortCardsByRankDesc(List<Card> cards) {
        List<Card> sorted = new ArrayList<>();
        for (Card card : cards) {
            if (card != null) {
                sorted.add(card);
            }
        }
        sorted.sort((a, b) -> Integer.compare(b.getRankValue(), a.getRankValue()));
        return sorted;
    }

    private Map<Integer, List<Card>> groupByRank(List<Card> cards) {
        Map<Integer, List<Card>> grouped = new HashMap<>();
        for (Card card : cards) {
            int rank = card.getRankValue();
            grouped.computeIfAbsent(rank, key -> new ArrayList<>()).add(card);
        }
        return grouped;
    }

    private Map<String, List<Card>> groupBySuit(List<Card> cards) {
        Map<String, List<Card>> grouped = new HashMap<>();
        for (Card card : cards) {
            grouped.computeIfAbsent(card.getSuit(), key -> new ArrayList<>()).add(card);
        }
        return grouped;
    }

    private int findHighestRankWithCount(Map<Integer, List<Card>> cardsByRank, int count) {
        int bestRank = -1;
        for (Map.Entry<Integer, List<Card>> entry : cardsByRank.entrySet()) {
            if (entry.getValue().size() >= count && entry.getKey() > bestRank) {
                bestRank = entry.getKey();
            }
        }
        return bestRank;
    }

    private List<Integer> ranksWithMinimumCount(Map<Integer, List<Card>> cardsByRank, int minCount) {
        List<Integer> ranks = new ArrayList<>();
        for (Map.Entry<Integer, List<Card>> entry : cardsByRank.entrySet()) {
            if (entry.getValue().size() >= minCount) {
                ranks.add(entry.getKey());
            }
        }
        ranks.sort(Collections.reverseOrder());
        return ranks;
    }

    private int findHighestPairRankForFullHouse(Map<Integer, List<Card>> cardsByRank, int excludedTripRank) {
        int bestPairRank = -1;
        for (Map.Entry<Integer, List<Card>> entry : cardsByRank.entrySet()) {
            int rank = entry.getKey();
            int count = entry.getValue().size();
            if (rank == excludedTripRank) {
                continue;
            }
            if (count >= 2 && rank > bestPairRank) {
                bestPairRank = rank;
            }
        }
        return bestPairRank;
    }

    private List<Card> findBestFlush(Map<String, List<Card>> cardsBySuit) {
        List<Card> bestFlush = null;
        List<Integer> bestRanks = null;

        for (List<Card> suitCards : cardsBySuit.values()) {
            if (suitCards.size() < 5) {
                continue;
            }
            List<Card> sortedSuit = sortCardsByRankDesc(suitCards);
            List<Card> candidate = takeTopCards(sortedSuit, 5);
            List<Integer> candidateRanks = rankValues(candidate);
            if (bestFlush == null || compareRankLists(candidateRanks, bestRanks) > 0) {
                bestFlush = candidate;
                bestRanks = candidateRanks;
            }
        }

        return bestFlush;
    }

    private int compareRankLists(List<Integer> left, List<Integer> right) {
        if (right == null) {
            return 1;
        }
        int count = Math.max(left.size(), right.size());
        for (int i = 0; i < count; i++) {
            int l = i < left.size() ? left.get(i) : 0;
            int r = i < right.size() ? right.get(i) : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private int findStraightHigh(List<Card> cards) {
        boolean[] hasRank = new boolean[15];
        for (Card card : cards) {
            int rank = card.getRankValue();
            if (rank < 2 || rank > 14) {
                continue;
            }
            hasRank[rank] = true;
            if (rank == 14) {
                hasRank[1] = true;
            }
        }

        for (int high = 14; high >= 5; high--) {
            boolean straight = true;
            for (int offset = 0; offset < 5; offset++) {
                if (!hasRank[high - offset]) {
                    straight = false;
                    break;
                }
            }
            if (straight) {
                return high;
            }
        }
        return -1;
    }

    private List<Card> buildStraightCards(List<Card> cards, int straightHigh) {
        List<Card> straightCards = new ArrayList<>();
        for (int rank = straightHigh; rank >= straightHigh - 4; rank--) {
            int targetRank = (rank == 1) ? 14 : rank;
            Card card = findHighestCardByRank(cards, targetRank);
            if (card != null) {
                straightCards.add(card);
            }
        }
        return straightCards;
    }

    private Card findHighestCardByRank(List<Card> cards, int rank) {
        for (Card card : cards) {
            if (card.getRankValue() == rank) {
                return card;
            }
        }
        return null;
    }

    private List<Card> takeCardsOfRank(Map<Integer, List<Card>> cardsByRank, int rank, int count) {
        List<Card> source = cardsByRank.get(rank);
        if (source == null) {
            return new ArrayList<>();
        }
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < source.size() && i < count; i++) {
            result.add(source.get(i));
        }
        return result;
    }

    private List<Card> takeTopCards(List<Card> cards, int count) {
        List<Card> top = new ArrayList<>();
        for (int i = 0; i < cards.size() && i < count; i++) {
            top.add(cards.get(i));
        }
        return top;
    }

    private List<Integer> rankValues(List<Card> cards) {
        List<Integer> ranks = new ArrayList<>();
        for (Card card : cards) {
            ranks.add(card.getRankValue());
        }
        return ranks;
    }

    private int highestDistinctRankExcluding(List<Card> cards, int... excludedRanks) {
        List<Integer> excluded = new ArrayList<>();
        for (int rank : excludedRanks) {
            excluded.add(rank);
        }
        List<Integer> top = topDistinctRanksExcluding(cards, excluded, 1);
        return top.isEmpty() ? 0 : top.get(0);
    }

    private List<Integer> topDistinctRanksExcluding(List<Card> cards, List<Integer> excludedRanks, int count) {
        List<Integer> ranks = new ArrayList<>();
        for (Card card : cards) {
            int rank = card.getRankValue();
            if (excludedRanks.contains(rank) || ranks.contains(rank)) {
                continue;
            }
            ranks.add(rank);
            if (ranks.size() == count) {
                break;
            }
        }
        return ranks;
    }

    private void addCardsByRanks(List<Card> destination, List<Card> sourceCards, List<Integer> ranks) {
        for (int rank : ranks) {
            Card card = findHighestCardByRank(sourceCards, rank);
            if (card != null) {
                destination.add(card);
            }
        }
    }

    private List<Integer> listOf(int... values) {
        List<Integer> list = new ArrayList<>();
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    private static class Evaluation {
        final HandRank rank;
        final List<Integer> kickers;
        final List<Card> bestFiveCards;

        Evaluation(HandRank rank, List<Integer> kickers, List<Card> bestFiveCards) {
            this.rank = rank;
            this.kickers = kickers;
            this.bestFiveCards = bestFiveCards;
        }
    }
}
