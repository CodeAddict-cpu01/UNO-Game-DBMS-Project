// --- File: src/StatsPanel.java ---

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

// UPDATED to extend AnimatedPanel
public class StatsPanel extends AnimatedPanel {

    private UNOAppManager appManager;
    private GameController gameController;
    private Connection conn;

    // Labels to display the stats
    private JLabel totalGamesLabel = new JLabel("0");
    private JLabel totalTurnsLabel = new JLabel("0");
    private JLabel aiWinsLabel = new JLabel("0");
    private JLabel humanWinsLabel = new JLabel("0");
    private JLabel totalDraw2sLabel = new JLabel("0");
    private JLabel totalWild4sLabel = new JLabel("0");

    public StatsPanel(UNOAppManager manager) {
        this.appManager = manager;
        this.gameController = appManager.getGameController();
        this.conn = appManager.getConnection();

        setLayout(new BorderLayout());
        setBackground(UnoTheme.TABLE_GREEN); // Use the felt green

        // --- Title Panel ---
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(UnoTheme.BLACK);
        titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("GAME STATISTICS");
        title.setFont(UnoTheme.HEADER_FONT);
        title.setForeground(UnoTheme.TEXT_WHITE);
        titlePanel.add(title);
        add(titlePanel, BorderLayout.NORTH);

        // --- Stats Display Panel ---
        JPanel statsGrid = new JPanel(new GridLayout(6, 2, 10, 20)); 
        statsGrid.setOpaque(false); // Make transparent
        statsGrid.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Add labels in Header/Value pairs
        statsGrid.add(createHeaderLabel("Total Games Finished:"));
        statsGrid.add(createStatLabel(totalGamesLabel));
        
        statsGrid.add(createHeaderLabel("Total Turns Played:"));
        statsGrid.add(createStatLabel(totalTurnsLabel));
        
        statsGrid.add(createHeaderLabel("AI Wins:"));
        statsGrid.add(createStatLabel(aiWinsLabel));
        
        statsGrid.add(createHeaderLabel("Human Wins:"));
        statsGrid.add(createStatLabel(humanWinsLabel));
        
        statsGrid.add(createHeaderLabel("Total 'Draw 2's Played:"));
        statsGrid.add(createStatLabel(totalDraw2sLabel));
        
        statsGrid.add(createHeaderLabel("Total 'Wild Draw 4's Played:"));
        statsGrid.add(createStatLabel(totalWild4sLabel));

        add(statsGrid, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(UnoTheme.BLACK);
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        // Use the new StyledButton for a consistent look
        JButton backButton = new UnoUI.StyledButton("BACK TO MENU", UnoTheme.BLUE);
        backButton.setPreferredSize(new Dimension(250, 50));
        backButton.addActionListener(e -> appManager.showMenu());
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Helper method to style the header text
    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UnoTheme.HEADER_FONT);
        label.setForeground(UnoTheme.TEXT_WHITE);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    // Helper method to style the stat value
    private JLabel createStatLabel(JLabel label) {
        label.setFont(UnoTheme.HEADER_FONT);
        label.setForeground(UnoTheme.YELLOW);
        return label;
    }

    /**
     * Called by the AppManager *every time* this panel is shown.
     */
    public void loadStats() {
        try {
            GameStatistics stats = gameController.getGameStatistics(conn); 
            
            totalGamesLabel.setText(String.valueOf(stats.totalGamesFinished));
            totalTurnsLabel.setText(String.valueOf(stats.totalTurnsPlayed));
            aiWinsLabel.setText(String.valueOf(stats.aiWins));
            humanWinsLabel.setText(String.valueOf(stats.humanWins));
            totalDraw2sLabel.setText(String.valueOf(stats.draw2Count));
            totalWild4sLabel.setText(String.valueOf(stats.wild4Count)); 
            
            // Now that stats are loaded, fade the panel in
            fadeIn(); // <-- ADDED
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load stats: " + e.getMessage());
        }
    }
}