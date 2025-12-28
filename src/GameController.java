import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * GameController: Manages all UNO game logic and state manipulation via JDBC.
 * This class holds the business logic, transactional control, and rule validation.
 */
public class GameController {

    // --- Player Management Methods ---

    /**
     * Inserts a new player into the Players table.
     */
    public void addPlayer(Connection conn, String name, String type) throws SQLException {
        String sql = "INSERT INTO Players (name, type) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, type); 
            pstmt.executeUpdate();
            System.out.println("Player '" + name + "' added successfully.");
        }
    }
    
    /**
     * Retrieves all players from the database and returns them as a list of Player objects.
     */
    public List<Player> listAllPlayers(Connection conn) throws SQLException {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT player_id, name, type, score FROM Players ORDER BY player_id";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.println("\n--- Current Registered Players ---");
            while (rs.next()) {
                Player player = new Player(rs.getInt("player_id"), rs.getString("name"), rs.getString("type"), rs.getInt("score"));
                players.add(player);
                System.out.println(player.toString());
            }
        }
        return players;
    }

    /**
     * Resets the Players table and sets up a fresh session with 1 Human and N AI bots.
     */
    public void setupSessionPlayers(Connection conn, int aiOpponentCount) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.executeUpdate("TRUNCATE TABLE Hands");
            stmt.executeUpdate("TRUNCATE TABLE Moves");
            stmt.executeUpdate("TRUNCATE TABLE Deck");
            stmt.executeUpdate("TRUNCATE TABLE Game");
            stmt.executeUpdate("TRUNCATE TABLE Players");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        // Add Human Player
        addPlayer(conn, "You", "human");

        // Add AI Players
        for (int i = 1; i <= aiOpponentCount; i++) {
            addPlayer(conn, "AI Bot " + i, "AI");
        }
        
        System.out.println("Session setup with 1 Human and " + aiOpponentCount + " AI.");
    }


    // --- State Retrieval Methods ---

    /**
     * Retrieves the data for a single card based on its ID.
     */
    public Card getCardDetails(Connection conn, int cardId) throws SQLException {
        String sql = "SELECT card_id, color, value, points FROM Cards WHERE card_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cardId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Card(rs.getInt("card_id"), rs.getString("color"), rs.getString("value"), rs.getInt("points"));
                }
                return null; // Card not found
            }
        }
    }

    /**
     * Retrieves the hand (list of Card objects) for a specific player in a specific game.
     */
    public List<Card> getPlayerHand(Connection conn, int gameId, int playerId) throws SQLException {
        List<Card> hand = new ArrayList<>();
        String sql = "SELECT C.card_id, C.color, C.value, C.points " +
                     "FROM Hands H JOIN Cards C ON H.card_id = C.card_id " +
                     "WHERE H.game_id = ? AND H.player_id = ? " +
                     "ORDER BY C.color, C.value";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, playerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    hand.add(new Card(rs.getInt("card_id"), rs.getString("color"), rs.getString("value"), rs.getInt("points")));
                }
            }
        }
        return hand;
    }
    
    /**
     * Retrieves the current turn, direction, top card, AND pending draw stack.
     */
    public GameStatus getGameStatus(Connection conn, int gameId) throws SQLException {
        String sql = "SELECT current_turn, direction, current_card_id, active_color, pending_draw_stack FROM Game WHERE game_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int currentTurn = rs.getInt("current_turn");
                    String direction = rs.getString("direction");
                    int currentCardId = rs.getInt("current_card_id");
                    String activeColor = rs.getString("active_color"); 
                    int pendingDraws = rs.getInt("pending_draw_stack"); 

                    Card topCard = getCardDetails(conn, currentCardId);
                    
                    if (topCard.isWild() && activeColor != null) {
                        topCard.setColor(activeColor); 
                    }
                    
                    return new GameStatus(currentTurn, direction, topCard, pendingDraws);
                }
                return null;
            }
        }
    }
    
    /**
     * Retrieves a map of all players in the game and their current hand size.
     */
    public Map<Integer, Integer> getHandCounts(Connection conn, int gameId) throws SQLException {
        Map<Integer, Integer> handCounts = new HashMap<>();
        String sql = "SELECT player_id, COUNT(*) as card_count " +
                     "FROM Hands " +
                     "WHERE game_id = ? " +
                     "GROUP BY player_id";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    handCounts.put(rs.getInt("player_id"), rs.getInt("card_count"));
                }
            }
        }
        return handCounts;
    }
    
    /**
     * Queries the database and aggregates statistics from all completed games.
     */
    public GameStatistics getGameStatistics(Connection conn) throws SQLException {
        GameStatistics stats = new GameStatistics();
        String sql;
        
        try (Statement stmt = conn.createStatement()) {
            
            // 1. Get total games finished
            sql = "SELECT COUNT(*) FROM Game WHERE status = 'finished'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) stats.totalGamesFinished = rs.getInt(1);
            }

            // 2. Get total turns played (sum of all moves)
            sql = "SELECT COUNT(*) FROM Moves";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) stats.totalTurnsPlayed = rs.getInt(1);
            }
            
            // 3. Get AI vs Human wins
            sql = "SELECT P.type, COUNT(G.game_id) as wins " +
                  "FROM Game G JOIN Players P ON G.winner_id = P.player_id " +
                  "WHERE G.status = 'finished' " +
                  "GROUP BY P.type";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    if (rs.getString("type").equalsIgnoreCase("AI")) {
                        stats.aiWins = rs.getInt("wins");
                    } else {
                        stats.humanWins = rs.getInt("wins");
                    }
                }
            }
            
            // 4. Get Draw 2 count (only 'played' actions)
            sql = "SELECT COUNT(M.move_id) FROM Moves M " +
                  "JOIN Cards C ON M.card_id = C.card_id " +
                  "WHERE C.value = 'draw2' AND M.action = 'played'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) stats.draw2Count = rs.getInt(1);
            }

            // 5. Get Wild Draw 4 count (only 'played' actions)
            sql = "SELECT COUNT(M.move_id) FROM Moves M " +
                  "JOIN Cards C ON M.card_id = C.card_id " +
                  "WHERE C.value = 'wild4' AND M.action = 'played'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) stats.wild4Count = rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching game statistics: " + e.getMessage());
            throw e;
        }
        
        return stats;
    }


    // --- Core Rule and Transactional Methods ---

    /**
     * Selects a single random, available card_id from the Deck for the current game.
     */
    private int selectAndLockCard(Connection conn, int gameId) throws SQLException {
        String sql = "SELECT card_id FROM Deck WHERE game_id = ? AND status = 'in_deck' ORDER BY RAND() LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("card_id");
                }
                throw new SQLException("Deck ran out of cards during dealing!"); 
            }
        }
    }

    /**
     * Recycles the discard pile back into the draw deck.
     */
    private void refillDeck(Connection conn, int gameId, int currentTopCardId) throws SQLException {
        String sql = "UPDATE Deck SET status = 'in_deck' WHERE game_id = ? AND status = 'in_discard' AND card_id != ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, currentTopCardId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Deck refilled with " + rowsAffected + " cards from the discard pile.");
        }
    }

    /**
     * Primary method for a player to draw a card from the deck.
     * Assumes it is being called *within* an existing transaction.
     */
    public Card drawCard(Connection conn, int gameId, int playerId) throws SQLException {
        int cardId = -1;
        Card drawnCard = null;
        
        GameStatus status = getGameStatus(conn, gameId); 
        int currentTopCardId = status.getTopCard().getCardId();

        try {
            cardId = selectAndLockCard(conn, gameId);
        } catch (SQLException e) {
            if (e.getMessage().contains("Deck ran out")) {
                System.out.println("Deck is empty! Initiating refill from discard pile...");
                refillDeck(conn, gameId, currentTopCardId);
                cardId = selectAndLockCard(conn, gameId);
            } else {
                throw e; 
            }
        }

        if (cardId != -1) {
            String sqlUpdateDeck = "UPDATE Deck SET status = 'in_hand' WHERE game_id = ? AND card_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateDeck)) {
                pstmt.setInt(1, gameId);
                pstmt.setInt(2, cardId);
                pstmt.executeUpdate();
            }

            String sqlInsertHand = "INSERT INTO Hands (game_id, player_id, card_id) VALUES (?, ?, ?)";
            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertHand)) {
                pstmtInsert.setInt(1, gameId);
                pstmtInsert.setInt(2, playerId);
                pstmtInsert.setInt(3, cardId);
                pstmtInsert.executeUpdate();
            }
            drawnCard = getCardDetails(conn, cardId);
        }
        return drawnCard;
    }

    /**
     * Deals 7 cards to each player and flips the first non-Wild card onto the discard pile.
     */
    private void dealInitialCards(Connection conn, int gameId, List<Player> players) throws SQLException {
        String sqlUpdateDeckStatus = "UPDATE Deck SET status = ? WHERE game_id = ? AND card_id = ?";
        String sqlInsertHand = "INSERT INTO Hands (game_id, player_id, card_id) VALUES (?, ?, ?)";
        for (Player player : players) {
            for (int i = 0; i < 7; i++) {
                int cardId = selectAndLockCard(conn, gameId); 
                try(PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateDeckStatus)) {
                    pstmtUpdate.setString(1, "in_hand");
                    pstmtUpdate.setInt(2, gameId);
                    pstmtUpdate.setInt(3, cardId);
                    pstmtUpdate.executeUpdate();
                }
                try(PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertHand)) {
                    pstmtInsert.setInt(1, gameId);
                    pstmtInsert.setInt(2, player.getPlayerId());
                    pstmtInsert.setInt(3, cardId);
                    pstmtInsert.executeUpdate();
                }
            }
            System.out.println("Dealt 7 cards to Player ID: " + player.getPlayerId());
        }
        
        // --- FLIP FIRST DISCARD CARD (Logic to avoid Wilds) ---
        Card firstCard;
        int firstCardId;

        while (true) {
            firstCardId = selectAndLockCard(conn, gameId);
            firstCard = getCardDetails(conn, firstCardId);
            
            if (!firstCard.isWild()) {
                break; // Found a valid starting card
            }
            System.out.println("Flipped a Wild card, re-drawing for a valid start card...");
        }

        String activeColor = firstCard.getColor();

        try(PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateDeckStatus)) {
            pstmtUpdate.setString(1, "in_discard");
            pstmtUpdate.setInt(2, gameId);
            pstmtUpdate.setInt(3, firstCardId);
            pstmtUpdate.executeUpdate();
        }
        
        String sqlUpdateInitialCard = "UPDATE Game SET current_card_id = ?, active_color = ? WHERE game_id = ?";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateInitialCard)) {
            pstmtUpdate.setInt(1, firstCardId);
            pstmtUpdate.setString(2, activeColor); // Set the active color
            pstmtUpdate.setInt(3, gameId);
            pstmtUpdate.executeUpdate();
        }
        System.out.println("Flipped initial card (" + firstCard.toString() + ") to discard pile.");
    }

    /**
     * Initializes a new UNO game, populates the deck, deals cards, and sets the initial state.
     */
    public int startGame(Connection conn, List<Player> players) throws SQLException {
        int gameId = -1;
        int firstPlayerId = players.get(0).getPlayerId();
        conn.setAutoCommit(false); 
        try {
            String sqlInsertGame = "INSERT INTO Game (status, current_turn) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertGame, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, "setup");
                pstmt.setInt(2, firstPlayerId);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        gameId = rs.getInt(1);
                    }
                }
            }
            if (gameId == -1) throw new SQLException("Failed to create new game entry.");

            String sqlPopulateDeck = "INSERT INTO Deck (game_id, card_id, status) " +
                                     "SELECT ?, card_id, 'in_deck' FROM Cards";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlPopulateDeck)) {
                pstmt.setInt(1, gameId);
                pstmt.executeUpdate();
            }
            System.out.println("Deck populated with 108 cards for Game ID: " + gameId);
            
            dealInitialCards(conn, gameId, players);
            
            String sqlUpdateStatus = "UPDATE Game SET status = 'ongoing' WHERE game_id = ?";
            try(PreparedStatement pstmt = conn.prepareStatement(sqlUpdateStatus)){
                pstmt.setInt(1, gameId);
                pstmt.executeUpdate();
            }
            conn.commit();
            System.out.println("Game ID " + gameId + " started! It is Player ID " + firstPlayerId + "'s turn.");
            return gameId;
        } catch (SQLException e) {
            System.err.println("Transaction failed. Rolling back changes.");
            conn.rollback(); 
            throw e; 
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    /**
     * Checks if a player's proposed card is a valid move, including STACKING logic.
     */
    public boolean validateMove(Card playedCard, Card topCard, int pendingDrawStack) {
        if (playedCard == null || topCard == null) return false;

        // Rule 1: Stacking logic
        if (pendingDrawStack > 0) {
            if (playedCard.getValue().equalsIgnoreCase("draw2") && topCard.getValue().equalsIgnoreCase("draw2")) {
                return true; 
            }
            if (playedCard.getValue().equalsIgnoreCase("wild4") && topCard.getValue().equalsIgnoreCase("wild4")) {
                return true; 
            }
            return false; // Not a valid stack
        }

        // Rule 2: Wild cards can always be played (if not stacking)
        if (playedCard.isWild()) {
            return true;
        }

        // Rule 3: Must match color OR number/action value (if not stacking)
        boolean matchesColor = playedCard.getColor().equalsIgnoreCase(topCard.getColor());
        boolean matchesValue = playedCard.getValue().equalsIgnoreCase(topCard.getValue());
        
        return (matchesColor || matchesValue);
    }
    
    /**
     * Calculates the ID of the next player, now including skipCount.
     */
    private int getNextPlayerId(Connection conn, int gameId, int currentPlayerId, int skipCount) throws SQLException {
        List<Integer> playerOrder = new ArrayList<>();
        String direction = "clockwise";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_id FROM Players ORDER BY player_id")) {
            while (rs.next()) {
                playerOrder.add(rs.getInt("player_id"));
            }
        }
        
        String sqlDirection = "SELECT direction FROM Game WHERE game_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlDirection)) {
            pstmt.setInt(1, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    direction = rs.getString("direction");
                }
            }
        }
        
        if (playerOrder.isEmpty()) throw new SQLException("Cannot determine next player: No players found.");

        int currentIndex = playerOrder.indexOf(currentPlayerId);
        int numPlayers = playerOrder.size();
        int steps = 1 + skipCount; // 1 for normal turn, +1 for each skip

        int nextIndex;
        if (direction.equalsIgnoreCase("anticlockwise")) {
            nextIndex = (currentIndex - steps + (numPlayers * steps)) % numPlayers; // Ensure positive index
        } else { 
            nextIndex = (currentIndex + steps) % numPlayers;
        }

        return playerOrder.get(nextIndex);
    }


    /**
     * Executes a player's move, including handling special card logic AND STACKING.
     */
    public boolean processMove(Connection conn, int gameId, int playerId, Card cardToPlay, String action, String nextColor) throws SQLException {
        conn.setAutoCommit(false); 
        
        int skipCount = 0;
        String cardValue = cardToPlay.getValue();
        
        GameStatus status = getGameStatus(conn, gameId);
        int currentPendingDraws = status.getPendingDrawStack(); 

        try {
            // 1. LOG THE MOVE
            String sqlLogMove = "INSERT INTO Moves (game_id, player_id, card_id, action, turn_number) " +
                                "VALUES (?, ?, ?, ?, (SELECT COUNT(*)+1 FROM Moves AS M WHERE M.game_id = ?))"; 
            try (PreparedStatement pstmt = conn.prepareStatement(sqlLogMove)) {
                pstmt.setInt(1, gameId);
                pstmt.setInt(2, playerId);
                pstmt.setInt(3, cardToPlay.getCardId());
                pstmt.setString(4, action);
                pstmt.setInt(5, gameId);
                pstmt.executeUpdate();
            }
            System.out.printf("Move Logged: Player %d performed action '%s' with card %s.%n", playerId, action, cardToPlay.toString());
            
            
            // 2. UPDATE HANDS, DECK, AND GAME STATE
            if (action.equalsIgnoreCase("played")) {
                
                // A. Remove card from player's hand
                String sqlRemoveHand = "DELETE FROM Hands WHERE game_id = ? AND player_id = ? AND card_id = ? LIMIT 1";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlRemoveHand)) {
                    pstmt.setInt(1, gameId);
                    pstmt.setInt(2, playerId);
                    pstmt.setInt(3, cardToPlay.getCardId());
                    pstmt.executeUpdate();
                }
                
                // B. Update card status in Deck to 'in_discard'
                String sqlUpdateDeck = "UPDATE Deck SET status = 'in_discard' WHERE game_id = ? AND card_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateDeck)) {
                    pstmt.setInt(1, gameId);
                    pstmt.setInt(2, cardToPlay.getCardId());
                    pstmt.executeUpdate();
                }

                // --- C. APPLY SPECIAL CARD LOGIC (UPDATED FOR STACKING) ---
                int newPendingDraws = 0;
                
                if (cardValue.equalsIgnoreCase("reverse")) {
                    String newDirection = status.getDirection().equalsIgnoreCase("clockwise") ? "anticlockwise" : "clockwise";
                    String sqlReverse = "UPDATE Game SET direction = ? WHERE game_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlReverse)) {
                        pstmt.setString(1, newDirection);
                        pstmt.setInt(2, gameId);
                        pstmt.executeUpdate();
                    }
                    System.out.println("!!! Game direction REVERSED to " + newDirection);
                
                } else if (cardValue.equalsIgnoreCase("skip")) {
                    skipCount = 1;
                    System.out.println("!!! Next player will be SKIPPED.");
                
                } else if (cardValue.equalsIgnoreCase("draw2")) {
                    newPendingDraws = currentPendingDraws + 2; // ADD to the stack
                    skipCount = 1; 
                    System.out.println("!!! Draw stack is now " + newPendingDraws);
                
                } else if (cardValue.equalsIgnoreCase("wild4")) {
                    newPendingDraws = currentPendingDraws + 4; // ADD to the stack
                    skipCount = 1; 
                    System.out.println("!!! Draw stack is now " + newPendingDraws);
                }
                
                // --- D. GET FINAL NEXT PLAYER (accounts for skips)
                int finalNextPlayerId = getNextPlayerId(conn, gameId, playerId, skipCount);

                // E. Update the central Game State (Top Card, Next Turn, Active Color, Pending Draws)
                String sqlUpdateGame = "UPDATE Game SET current_card_id = ?, current_turn = ?, active_color = ?, pending_draw_stack = ? WHERE game_id = ?";
                
                String colorToSet = cardToPlay.isWild() ? nextColor : cardToPlay.getColor();
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateGame)) {
                    pstmt.setInt(1, cardToPlay.getCardId());
                    pstmt.setInt(2, finalNextPlayerId);
                    pstmt.setString(3, colorToSet);
                    pstmt.setInt(4, newPendingDraws); 
                    pstmt.setInt(5, gameId);
                    pstmt.executeUpdate();
                }
                System.out.printf("Turn successfully passed to Player ID %d.%n", finalNextPlayerId);

            } else if (action.equalsIgnoreCase("drawn_and_passed")) {
                // Player is drawing/passing. This means they must PAY the draw stack penalty.
                int penalty = currentPendingDraws;
                if (penalty > 0) {
                    System.out.printf("!!! Player %d must draw the STACK of %d cards!%n", playerId, penalty);
                    for (int i = 0; i < penalty; i++) {
                        drawCard(conn, gameId, playerId); 
                    }
                }
                
                int finalNextPlayerId = getNextPlayerId(conn, gameId, playerId, 0); // No skip
                
                // Update the turn AND RESET the draw stack
                String sqlUpdateGameTurn = "UPDATE Game SET current_turn = ?, pending_draw_stack = 0 WHERE game_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateGameTurn)) {
                    pstmt.setInt(1, finalNextPlayerId);
                    pstmt.setInt(2, gameId);
                    pstmt.executeUpdate();
                }
                System.out.printf("Turn successfully passed to Player ID %d.%n", finalNextPlayerId);
            }
            
            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Move processing failed. Rolling back transaction.");
            conn.rollback(); 
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // --- AI Strategy Method ---

    /**
     * Implements the Advanced AI Strategy.
     * It scores all valid moves and picks the best one.
     */
    public AIDecision getAIDecision(List<Card> validMoves, List<Card> fullHand) {
        
        Card bestCard = null;
        int bestScore = -1;
        String chosenColor = null;

        // 1. Score and find the highest-scoring card
        for (Card card : validMoves) {
            int score = 0;
            String value = card.getValue();

            switch (value) {
                case "wild4":   score = 100; break;
                case "draw2":   score = 80;  break;
                case "skip":
                case "reverse": score = 70;  break;
                case "wild":    score = 60;  break;
                default:
                    try {
                        score = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        score = 0; 
                    }
                    break;
            }

            if (score > bestScore) {
                bestScore = score;
                bestCard = card;
            }
        }

        // 2. If a Wild card was chosen, determine the best color
        if (bestCard != null && bestCard.isWild()) {
            Map<String, Long> colorCounts = fullHand.stream()
                .filter(c -> !c.isWild()) 
                .collect(Collectors.groupingBy(Card::getColor, Collectors.counting()));

            chosenColor = colorCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("red"); 
        }

        return new AIDecision(bestCard, chosenColor);
    }
    
    // --- NEW: Method to end the game and record the winner ---
    /**
     * Updates the database to mark a game as finished and record the winner.
     * This is essential for the Stats page to work.
     */
    public void endGame(Connection conn, int gameId, int winnerId) throws SQLException {
        // This query sets the game status and winner ID
        String sql = "UPDATE Game SET status = 'finished', winner_id = ? WHERE game_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, winnerId);
            pstmt.setInt(2, gameId);
            pstmt.executeUpdate();
            
            System.out.println("Game ID " + gameId + " marked as 'finished'. Winner: " + winnerId);
        }
    }
}