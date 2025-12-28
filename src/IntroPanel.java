// --- File: src/IntroPanel.java (Updated with Multi-Color Typing) ---

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class IntroPanel extends AnimatedPanel {

    private UNOAppManager appManager;
    private Timer displayTimer; // Timer to hold the screen
    private Timer typingTimer;  // Timer for the "one by one" text
    
    private JLabel welcomeLabel;
    private final String welcomeMessage = "Welcome, Player!";
    private StringBuilder currentHtmlText = new StringBuilder(); // Will build the HTML string
    private int charIndex = 0;
    
    // Define the colors to cycle through (using hex codes from UnoTheme)
    private final String[] UNO_COLORS = {
        "#ed1c24", // Red
        "#ffdc00", // Yellow
        "#50b04e"  // Green
    };

    public IntroPanel(UNOAppManager manager) {
        this.appManager = manager;
        setBackground(UnoTheme.BLACK);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // --- 1. Load your Logo ---
        ImageIcon logoIcon = new ImageIcon("images/splash_logo.png");
        JLabel logoLabel = new JLabel(logoIcon);
        gbc.gridx = 0;
        gbc.gridy = 0; // Row 0
        add(logoLabel, gbc);

        // --- 2. Setup the "Typing" Welcome Text ---
        welcomeLabel = new JLabel(""); // Start with empty text
        welcomeLabel.setFont(new Font("Arial Black", Font.BOLD, 48)); 
        welcomeLabel.setForeground(UnoTheme.TEXT_WHITE); // Default color (won't be used)
        gbc.gridy = 1; // Row 1
        gbc.insets = new Insets(20, 0, 0, 0); 
        add(welcomeLabel, gbc);
        
        // --- 3. Setup the Timers ---

        // This timer runs *after* the typing is finished
        displayTimer = new Timer(2000, e -> { // Hold for 2 seconds
            ((Timer)e.getSource()).stop();
            // Tell the main app to fade out and switch to the menu
            appManager.showMenu(); 
        });
        displayTimer.setRepeats(false);

        // This timer types the letters one by one
        typingTimer = new Timer(100, e -> { // 100ms per letter
            if (charIndex < welcomeMessage.length()) {
                // Get the next character
                char c = welcomeMessage.charAt(charIndex);
                
                // Get the next color from our array
                String color = UNO_COLORS[charIndex % UNO_COLORS.length];
                
                // Wrap the character in an HTML span tag with the color
                currentHtmlText.append("<span style='color:" + color + "'>");
                currentHtmlText.append(c);
                currentHtmlText.append("</span>");
                
                // Set the label's text to the new HTML string
                welcomeLabel.setText("<html>" + currentHtmlText.toString() + "</html>");
                
                charIndex++;
            } else {
                // Typing is done
                ((Timer)e.getSource()).stop();
                displayTimer.start(); // Start the 2-second hold
            }
        });
        typingTimer.setRepeats(true);
    }

    /**
     * This is called by the AppManager to start the intro sequence.
     */
    public void startIntro() {
        fadeIn(); // Fade in the panel
        typingTimer.start(); 
    }
}