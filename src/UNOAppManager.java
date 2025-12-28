// --- File: src/UNOAppManager.java ---

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class UNOAppManager extends JFrame {
    
    private CardLayout cardLayout;
    private JPanel mainPanel; 
    
    // The "screens"
    private IntroPanel introPanel; // <-- NEW
    private MainMenuPanel mainMenuPanel;
    private GamePanel gamePanel;
    private StatsPanel statsPanel; 

    // Core logic
    private Connection conn;
    private GameController gameController;

    public UNOAppManager() {
        // 1. Setup DB Connection
        try {
            conn = DBConnector.getConnection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Fatal: Failed to connect to DB: " + e.getMessage());
            System.exit(1);
        }
        gameController = new GameController();

        // 2. Setup the main window
        setTitle("UNO DBMS Project");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        
        // 3. Setup the CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 4. Create and add all panels
        introPanel = new IntroPanel(this); // <-- NEW
        mainPanel.add(introPanel, "INTRO"); // <-- NEW
        
        mainMenuPanel = new MainMenuPanel(this);
        mainPanel.add(mainMenuPanel, "MENU");
        
        statsPanel = new StatsPanel(this); 
        mainPanel.add(statsPanel, "STATS"); 
        
        add(mainPanel);
        
        // 5. Start by showing the INTRO
        cardLayout.show(mainPanel, "INTRO");
        introPanel.startIntro(); // Tell the intro to start its animation
    }
    
    // --- Navigation Methods (UPDATED WITH ANIMATION) ---
    
    public void showMenu() {
        // Determine which panel is currently showing and fade it out
        AnimatedPanel currentPanel = introPanel; // Assume it's the intro
        if (gamePanel != null && gamePanel.isShowing()) {
            currentPanel = gamePanel;
        } else if (statsPanel.isShowing()) {
            currentPanel = statsPanel;
        }
        
        currentPanel.fadeOut(() -> {
            cardLayout.show(mainPanel, "MENU");
            mainMenuPanel.fadeIn();
        });
    }
    
    public void showStats() {
        mainMenuPanel.fadeOut(() -> {
            statsPanel.loadStats(); 
            cardLayout.show(mainPanel, "STATS");
            // fadeIn() is called inside loadStats()
        });
    }

    public void startNewGame(int aiCount) {
        mainMenuPanel.fadeOut(() -> {
            try {
                gameController.setupSessionPlayers(conn, aiCount);
                
                if (gamePanel != null) {
                    mainPanel.remove(gamePanel); // Remove old game
                }
                gamePanel = new GamePanel(this); 
                mainPanel.add(gamePanel, "GAME");
                
                cardLayout.show(mainPanel, "GAME");
                // fadeIn() is called from GamePanel's constructor
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error starting game session: " + e.getMessage());
                showMenu(); 
            }
        });
    }
    
    // --- Pass-through methods ---
    public Connection getConnection() { return conn; }
    public GameController getGameController() { return gameController; }

    /**
     * This is your main entry point.
     * Run this file.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UNOAppManager().setVisible(true);
        });
    }
}