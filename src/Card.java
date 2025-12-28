/**
 * Card: Model class representing a single UNO card, holding its database attributes.
 * This version includes a setter for color to handle Wild card active_color.
 */
public class Card {
    private int cardId;
    private String color; // red, green, blue, yellow, wild
    private String value; // 0-9, skip, reverse, draw2, wild, wild4
    private int points;   // For scoring

    public Card(int cardId, String color, String value, int points) {
        this.cardId = cardId;
        this.color = color;
        this.value = value;
        this.points = points;
    }

    // --- Getters ---
    public int getCardId() { return cardId; }
    public String getColor() { return color; }
    public String getValue() { return value; }
    public int getPoints() { return points; }
    
    // --- Setter (This is the missing method) ---
    /**
     * Used by getGameStatus to dynamically set the active color of a Wild card.
     */
    public void setColor(String color) {
        // Only allow setting the color if this is a Wild card
        if (this.color.equalsIgnoreCase("wild")) {
            this.color = color;
        }
    }

    // --- Helper for rules ---
    public boolean isWild() {
        // Check the original value, as the color might change
        return this.value.equalsIgnoreCase("wild") || this.value.equalsIgnoreCase("wild4");
    }

    @Override
    public String toString() {
        // Use the VALUE to check for wild, as color may be (e.g.) "red"
        if (isWild()) { 
            // If the color hasn't been set yet (e.g., in hand), just show the value
            if (this.color.equalsIgnoreCase("wild")) {
                 return String.format("[%s]", value.toUpperCase());
            }
            return String.format("[%s] (Set to %s)", value.toUpperCase(), color.toUpperCase());
        }
        return String.format("%s %s", color.toUpperCase(), value.toUpperCase());
    }
}