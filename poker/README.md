Texas Hold'em Poker - Complete Project Structure
1. Folder/Package Structure
poker/
    models/
        Card.java
        Deck.java
        Player.java
    
    game/
        TexasHoldemGame.java
        Pot.java
        BettingRound.java
        GamePhase.java
    
    evaluation/
        Hand.java
        HandEvaluator.java
        HandRank.java
    
    players/
        PlayerController.java
        AIPlayerController.java
        HumanPlayerController.java
    
    ui/
        ConsoleUI.java
        GameDisplay.java
    
    MainClass.java


Breakdown by Folder:
poker/models/
    • Card.java - Single playing card
    • Deck.java - 52-card deck management
    • Player.java - Player data (chips, cards, status)
poker/game/
    • TexasHoldemGame.java - Main game controller
    • Pot.java - Chip/money management
    • BettingRound.java - Single betting round logic
    • GamePhase.java - Enum (PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN)
poker/evaluation/
    • Hand.java - Represents 5-7 card hand
    • HandEvaluator.java - Logic to rank hands
    • HandRank.java - Enum (HIGH_CARD, ONE_PAIR, FLUSH, etc.)
poker/players/
    • PlayerController.java - Interface for player decisions
    • AIPlayerController.java - AI implementation
    • HumanPlayerController.java - Human console input implementation
poker/ui/
    • ConsoleUI.java - All console input/output
    • GameDisplay.java - Format game state for display
poker/
    • MainClass.java - Entry point


2. Class Breakdown
models/Card.java ✅
Purpose: Represent a single playing card
Fields:
    • String rank - card rank (2-10, Jack, Queen, King, Ace)
    • String suit - card suit (Hearts, Diamonds, Clubs, Spades)
Methods:
    • String getRank() - return card rank
    • String getSuit() - return card suit
    • int getRankValue() - return numeric value for comparison (2=2, Jack=11, Ace=14)
    • String toString() - display card as "Rank of Suit"

models/Deck.java 🔧
Purpose: Manage 52-card deck and deal cards
Fields:
    • Card[] cards - array of 52 cards
    • int topCard - index of next card to deal
Methods:
    • Deck() - constructor: create all 52 cards
    • void shuffle() - randomize card order, reset topCard
    • Card deal() - return next card and increment topCard
    • void reset() - reset topCard to 0
    • int cardsRemaining() - return how many cards left

models/Player.java
Purpose: Store player data (chips, cards, bet status)
Fields:
    • String name - player name
    • int chipStack - total chips player has
    • Card[] holeCards - player's 2 private cards (size 2)
    • int currentBet - amount bet in current betting round
    • int totalBet - total amount bet in current hand
    • boolean isActive - true if still in hand (not folded)
    • boolean isAllIn - true if player is all-in
    • PlayerController controller - decision-making strategy (AI or human)
Methods:
    • Player(String name, int startingChips, PlayerController controller) - constructor
    • void receiveCard(Card card) - add card to holeCards
    • void addChips(int amount) - increase chip stack
    • void removeChips(int amount) - decrease chip stack
    • void placeBet(int amount) - update currentBet and remove chips
    • void clearBet() - reset currentBet to 0 (after round ends)
    • void fold() - set isActive to false
    • void resetForNewHand() - clear cards, bets, flags
    • boolean canAfford(int amount) - check if player has enough chips
    • Card[] getHoleCards() - return hole cards
    • String toString() - display player info

game/GamePhase.java (enum)
Purpose: Track current stage of the game
Values:
    • PRE_FLOP - after hole cards dealt, before flop
    • FLOP - 3 community cards on table
    • TURN - 4 community cards on table
    • RIVER - 5 community cards on table
    • SHOWDOWN - reveal and compare hands

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

game/BettingRound.java
Purpose: Handle one round of betting (pre-flop, flop, turn, or river)
Fields:
    • int currentBet - highest bet in this round
    • Player lastRaiser - who raised last (to detect when round ends)
    • int minimumRaise - minimum raise amount
Methods:
    • BettingRound(int bigBlindAmount) - constructor, sets minimum raise
    • void processBet(Player player, String action, int amount, ConsoleUI ui) - execute player's decision
    • boolean isRoundComplete(List<Player> activePlayers) - check if all players acted and matched bet
    • void reset() - reset for next betting round

game/TexasHoldemGame.java
Purpose: Main game controller - orchestrates entire game flow
Fields:
    • List<Player> players - all players at table
    • Deck deck - the card deck
    • Card[] communityCards - up to 5 community cards (size 5)
    • int communityCardCount - how many community cards dealt
    • Pot pot - manages all chips
    • BettingRound bettingRound - current betting round
    • GamePhase currentPhase - current game stage
    • int dealerPosition - index of dealer button
    • int smallBlindAmount - small blind size
    • int bigBlindAmount - big blind size
    • ConsoleUI ui - console interface
    • GameDisplay display - display formatter
Methods:
    • TexasHoldemGame(List<Player> players, int smallBlind, int bigBlind, ConsoleUI ui) - constructor
    • void startGame() - main game loop
    • void playHand() - play one complete hand
    • void postBlinds() - collect small and big blinds
    • void dealHoleCards() - deal 2 cards to each player
    • void dealFlop() - deal 3 community cards
    • void dealTurn() - deal 1 community card
    • void dealRiver() - deal 1 community card
    • void runBettingRound() - execute one betting round
    • Player determineWinner() - evaluate hands and find winner
    • void distributePot(Player winner) - give chips to winner
    • void prepareNextHand() - reset for new hand, move dealer button
    • void moveDealerButton() - increment dealerPosition
    • List<Player> getActivePlayers() - return players still in hand
    • void displayGameState() - show current table state

evaluation/HandRank.java (enum)
Purpose: Define poker hand types and their rank order
Values (in order from weakest to strongest):
    • HIGH_CARD(1)
    • ONE_PAIR(2)
    • TWO_PAIR(3)
    • THREE_OF_A_KIND(4)
    • STRAIGHT(5)
    • FLUSH(6)
    • FULL_HOUSE(7)
    • FOUR_OF_A_KIND(8)
    • STRAIGHT_FLUSH(9)
    • ROYAL_FLUSH(10)
Methods:
    • int getValue() - return numeric rank for comparison

evaluation/Hand.java
Purpose: Represent and evaluate 5-7 cards (hole + community)
Fields:
    • List<Card> cards - up to 7 cards
    • HandRank handRank - evaluated hand type
    • List<Card> bestFiveCards - best 5-card combination
    • List<Integer> kickers - tiebreaker values
Methods:
    • Hand(Card[] holeCards, Card[] communityCards) - constructor
    • void evaluate(HandEvaluator evaluator) - determine hand rank and best 5 cards
    • int compareTo(Hand other) - compare two hands (returns -1, 0, or 1)
    • HandRank getHandRank() - return hand type
    • String toString() - display hand description

evaluation/HandEvaluator.java
Purpose: Logic to identify and rank poker hands
Methods:
    • void evaluateHand(Hand hand) - determine hand's rank and best cards
    • boolean isRoyalFlush(List<Card> cards) - check for royal flush
    • boolean isStraightFlush(List<Card> cards) - check for straight flush
    • boolean isFourOfAKind(List<Card> cards) - check for four of a kind
    • boolean isFullHouse(List<Card> cards) - check for full house
    • boolean isFlush(List<Card> cards) - check for flush
    • boolean isStraight(List<Card> cards) - check for straight
    • boolean isThreeOfAKind(List<Card> cards) - check for three of a kind
    • boolean isTwoPair(List<Card> cards) - check for two pair
    • boolean isOnePair(List<Card> cards) - check for one pair
    • List<Card> findBestFiveCards(List<Card> cards, HandRank rank) - extract best 5 cards
    • int compareHands(Hand hand1, Hand hand2) - determine winner with tiebreakers
    • Map<String, Integer> getRankCounts(List<Card> cards) - helper: count each rank

players/PlayerController.java (interface)
Purpose: Contract for player decision-making (AI or human)
Methods:
    • String decideAction(Player player, int currentBet, int minimumRaise, int pot, GamePhase phase) - return action: "fold", "check", "call", "raise", "all-in"
    • int getRaiseAmount(Player player, int currentBet, int minimumRaise) - if raising, return raise amount

players/AIPlayerController.java
Purpose: Implement AI decision logic (simple strategy)
Methods:
    • String decideAction(Player player, int currentBet, int minimumRaise, int pot, GamePhase phase) - AI logic (basic)
    • int getRaiseAmount(Player player, int currentBet, int minimumRaise) - AI raise calculation
AI Strategy (simple):
    • Random chance to fold, call, or raise based on pot odds
    • Can be enhanced later with hand strength analysis

players/HumanPlayerController.java
Purpose: Implement human decision via console input
Fields:
    • ConsoleUI ui - console interface for input
Methods:
    • HumanPlayerController(ConsoleUI ui) - constructor
    • String decideAction(Player player, int currentBet, int minimumRaise, int pot, GamePhase phase) - prompt user for action
    • int getRaiseAmount(Player player, int currentBet, int minimumRaise) - prompt user for raise amount

ui/ConsoleUI.java
Purpose: Handle all console input/output
Methods:
    • void displayMessage(String message) - print message
    • void displayDivider() - print visual separator
    • String getPlayerAction(Player player, List<String> validActions) - prompt for action
    • int getPlayerRaiseAmount(Player player, int minimum, int maximum) - prompt for raise amount
    • void displayCards(Card[] cards) - show cards
    • void displayPlayerInfo(Player player, boolean showHoleCards) - show player details
    • void waitForEnter() - pause for user to press Enter
    • void clearScreen() - clear console (optional)

ui/GameDisplay.java
Purpose: Format game state for display
Methods:
    • String formatTableState(List<Player> players, Card[] communityCards, int pot, int dealerPosition) - create table view
    • String formatPlayer(Player player, boolean isDealer, boolean showHoleCards) - format player info
    • String formatCommunityCards(Card[] cards, int count) - format flop/turn/river
    • String formatPot(int amount) - format pot display
    • String formatWinner(Player winner, Hand hand) - format winner announcement

MainClass.java
Purpose: Entry point - initialize and start game
Methods:
    • public static void main(String[] args) - set up game and run
        ○ Create ConsoleUI
        ○ Create 4 PlayerControllers (1 human, 3 AI)
        ○ Create 4 Players with starting chips (e.g., 1000 each)
        ○ Create TexasHoldemGame
        ○ Start game loop

3. Interfaces & Polymorphism
PlayerController Interface
    • Why: Allows swapping AI for human players without changing TexasHoldemGame logic
    • Implementation: TexasHoldemGame calls player.controller.decideAction() without knowing if it's AI or human
    • Benefit: Can add new player types (e.g., AdvancedAI, RemotePlayer) without modifying existing code

4. Data Flow
Game Initialization
    1. MainClass creates 4 Player objects (each with a PlayerController)
    2. Creates TexasHoldemGame with players, blinds, and UI
    3. Calls game.startGame()
Single Hand Flow
    1. Setup: prepareNextHand() - reset players, shuffle deck, move dealer button
    2. Blinds: postBlinds() - charge small/big blind to players left of dealer
    3. Hole Cards: dealHoleCards() - give 2 cards to each player
    4. Pre-Flop Betting: runBettingRound() → collects bets
    5. Flop: dealFlop() - 3 community cards
    6. Flop Betting: runBettingRound() → collects bets
    7. Turn: dealTurn() - 1 community card
    8. Turn Betting: runBettingRound() → collects bets
    9. River: dealRiver() - 1 community card
    10. River Betting: runBettingRound() → collects bets
    11. Showdown: determineWinner() - evaluate hands, find winner
    12. Payout: pot.distributeWinnings() - award chips
    13. Loop: Repeat from step 1
Betting Round Flow
    1. BettingRound.processBet() is called for each active player
    2. Player's controller.decideAction() is called → returns action
    3. Action is validated and executed:
        ○ Fold: player.fold(), remove from active list
        ○ Check/Call: player.placeBet(amount), chips → pot
        ○ Raise: player.placeBet(amount), update currentBet
    4. BettingRound.isRoundComplete() checks if all players matched bet
    5. pot.collectBets() - transfer all bets to pot
Chip Movement
AI/Human Decision Integration

5. Extensibility Points
✅ Swapping AI for human: Just create player with HumanPlayerController instead of AIPlayerController
✅ Testing hand evaluation independently: HandEvaluator doesn't depend on TexasHoldemGame
✅ Debugging chip logic: Pot class is isolated, can be tested separately
✅ Console I/O separation: All display logic in ConsoleUI and GameDisplay, easy to replace with GUI later
