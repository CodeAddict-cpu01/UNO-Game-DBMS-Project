// --- File: src/UnoTheme.java ---

import java.awt.Color;
import java.awt.Font;

/**
 * A helper class to store all the standard colors and fonts
 * for the UNO GUI theme, making it easy to keep a consistent style.
 */
public class UnoTheme {
    
    // --- Official UNO Colors ---
    public static final Color RED = new Color(237, 28, 36);
    public static final Color YELLOW = new Color(255, 220, 0); 
    public static final Color GREEN = new Color(80, 176, 78);
    public static final Color BLUE = new Color(0, 114, 188);
    public static final Color BLACK = new Color(30, 30, 30); // A softer black for UI

    // --- Theme Colors ---
    // A deep red, like the back of a card, for the main menu
    public static final Color BACKGROUND = new Color(138, 0, 0); 
    // A felt green for the "table" area
    public static final Color TABLE_GREEN = new Color(0, 100, 0); // <-- This is the missing variable
    // Standard white text
    public static final Color TEXT_WHITE = new Color(255, 255, 255);

    // --- Standard Fonts ---
    public static final Font TITLE_FONT = new Font("Arial Black", Font.BOLD, 60);
    public static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 24);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 18);
    public static final Font NORMAL_FONT = new Font("Arial", Font.PLAIN, 14);
}