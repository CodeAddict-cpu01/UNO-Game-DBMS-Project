// --- File: src/AIDecision.java ---

/**
 * A simple helper class to return the AI's complete decision:
 * which card to play, and what color to choose if it's a Wild.
 */
public class AIDecision {
    
    public Card card;
    public String nextColor; // null if not a wild

    public AIDecision(Card card, String nextColor) {
        this.card = card;
        this.nextColor = nextColor;
    }
}