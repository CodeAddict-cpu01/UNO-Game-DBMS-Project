import java.util.List;
import java.util.ArrayList;

/**
 * Player: A simple Model/Entity class to represent an UNO player in the Java application.
 * This holds the in-memory data for the currently active game.
 */
public class Player {
    private int playerId;
    private String name;
    private String type; // "human" or "AI"
    private int score;
    
    // Stores the list of Card IDs the player currently holds in their hand
    private List<Integer> currentHand; 

    // Constructor to create a player object from database data
    public Player(int playerId, String name, String type, int score) {
        this.playerId = playerId;
        this.name = name;
        this.type = type;
        this.score = score;
        this.currentHand = new ArrayList<>();
    }

    // --- Getters ---
    public int getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
    
    public int getScore() {
        return score;
    }

    public List<Integer> getCurrentHand() {
        return currentHand;
    }
    
    // --- Setters (for dynamic gameplay updates) ---
    public void setScore(int score) {
        this.score = score;
    }

    // Helper to manage the in-memory hand (add a drawn card)
    public void addCardToHand(int cardId) {
        this.currentHand.add(cardId);
    }
    
    // Helper to manage the in-memory hand (remove a played card)
    public boolean removeCardFromHand(int cardId) {
        return this.currentHand.remove((Integer) cardId);
    }
    
    @Override
    public String toString() {
        return String.format("%s (ID: %d, Type: %s, Cards: %d)", 
                             name, playerId, type, currentHand.size());
    }
}
