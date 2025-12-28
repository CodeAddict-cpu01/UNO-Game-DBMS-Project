/**
 * GameStatus: Model class to return the essential real-time state of the game.
 */
public class GameStatus {
    private int currentTurnPlayerId;
    private String direction;
    private Card topCard;
    private int pendingDrawStack; // <-- ADDED

    public GameStatus(int currentTurnPlayerId, String direction, Card topCard, int pendingDrawStack) {
        this.currentTurnPlayerId = currentTurnPlayerId;
        this.direction = direction;
        this.topCard = topCard;
        this.pendingDrawStack = pendingDrawStack; // <-- ADDED
    }

    // --- Getters ---
    public int getCurrentTurnPlayerId() { return currentTurnPlayerId; }
    public String getDirection() { return direction; }
    public Card getTopCard() { return topCard; }
    public int getPendingDrawStack() { return pendingDrawStack; } // <-- ADDED
}