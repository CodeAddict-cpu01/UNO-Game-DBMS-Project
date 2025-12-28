// --- File: src/MainMenuPanel.java ---

import javax.swing.*;
import java.awt.*;

// UPDATED to extend AnimatedPanel
public class MainMenuPanel extends AnimatedPanel {
    
    private UNOAppManager appManager;
    private JComboBox<Integer> aiCountBox;

    public MainMenuPanel(UNOAppManager manager) {
        this.appManager = manager;
        
        setLayout(new GridBagLayout());
        setBackground(UnoTheme.BACKGROUND); // Use theme color
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- 1. Colorful Title ---
        JLabel titleLabel = new JLabel("<html><span style='color:#ed1c24'>U</span><span style='color:#ffdc00'>N</span><span style='color:#50b04e'>O</span> <span style='color:white'>GAME</span></html>");
        titleLabel.setFont(UnoTheme.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        add(titleLabel, gbc);

        // --- 2. AI Selection Panel ---
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        selectionPanel.setOpaque(false); // Transparent background
        
        JLabel selectLabel = new JLabel("Select AI Opponents: ");
        selectLabel.setFont(UnoTheme.HEADER_FONT);
        selectLabel.setForeground(UnoTheme.TEXT_WHITE);
        
        Integer[] options = {1, 2, 3, 4, 5}; 
        aiCountBox = new JComboBox<>(options);
        aiCountBox.setFont(UnoTheme.BUTTON_FONT);
        aiCountBox.setSelectedIndex(2); 
        
        selectionPanel.add(selectLabel);
        selectionPanel.add(aiCountBox);
        
        gbc.gridy = 1;
        add(selectionPanel, gbc);

        // --- 3. Start Button (Uses new StyledButton) ---
        JButton startButton = new UnoUI.StyledButton("START GAME", UnoTheme.GREEN);
        startButton.addActionListener(e -> {
            int aiCount = (Integer) aiCountBox.getSelectedItem();
            appManager.startNewGame(aiCount);
        });
        gbc.gridy = 2;
        add(startButton, gbc);
        
        // --- 4. Stats Button (Uses new StyledButton) ---
        JButton statsButton = new UnoUI.StyledButton("VIEW STATS", UnoTheme.BLUE);
        statsButton.addActionListener(e -> appManager.showStats());
        gbc.gridy = 3;
        add(statsButton, gbc);

        // --- 5. Exit Button (Uses new StyledButton) ---
        JButton exitButton = new UnoUI.StyledButton("EXIT", UnoTheme.RED);
        exitButton.addActionListener(e -> System.exit(0));
        gbc.gridy = 4;
        add(exitButton, gbc);
    }
}