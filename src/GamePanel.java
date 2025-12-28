import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.awt.image.BufferedImage;
import java.util.stream.Collectors;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.border.Border; // Import for Border

// This class is a JPanel, not a JFrame. It's a "screen" managed by UNOAppManager.
public class GamePanel extends AnimatedPanel { // Extends AnimatedPanel for fading

    // --- Core Game Components ---
    private UNOAppManager appManager; 
    private GameController gameController;
    private Connection conn;
    private List<Player> activePlayers;
    private int gameId;
    private int humanPlayerId = 1; 
    private boolean isMyTurn = false;

    // --- GUI Components ---
    private JPanel opponentPanel;
    private JPanel discardPilePanel;
    private JPanel playerHandPanel; 
    private JScrollPane playerHandScrollPane;
    private JButton drawButton; 
    private JLabel topCardLabel;
    private JLabel statusLabel;
    private Map<Integer, JLabel> opponentLabels;
    private JTextArea gameLogArea;
    private JScrollPane logScrollPane;

    // --- Animation Components ---
    private Timer turnIndicatorTimer;
    private boolean isGlowing = false;
    private final Border handBorderDefault = BorderFactory.createMatteBorder(5, 0, 0, 0, Color.WHITE);
    private final Border handBorderGlow = BorderFactory.createMatteBorder(5, 0, 0, 0, UnoTheme.YELLOW);
    // --- End New Components ---

    // --- Constructor ---
    public GamePanel(UNOAppManager manager) {
        this.appManager = manager;
        this.gameController = appManager.getGameController();
        this.conn = appManager.getConnection();
        
        setLayout(new BorderLayout(10, 10)); 
        
        opponentLabels = new HashMap<>();
        try {
            activePlayers = gameController.listAllPlayers(conn);
            humanPlayerId = activePlayers.stream()
                                  .filter(p -> p.getType().equalsIgnoreCase("human"))
                                  .findFirst().map(Player::getPlayerId).orElse(1); 
            
            gameId = gameController.startGame(conn, activePlayers);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to start game: " + e.getMessage());
        }

        createOpponentPanel();
        createDiscardPilePanel();
        createPlayerHandPanel();
        createLogPanel(); 

        add(opponentPanel, BorderLayout.NORTH);
        add(discardPilePanel, BorderLayout.CENTER);
        add(playerHandScrollPane, BorderLayout.SOUTH);
        add(logScrollPane, BorderLayout.EAST); 

        // Initialize the animation timer
        turnIndicatorTimer = new Timer(500, e -> {
            isGlowing = !isGlowing; // Flip the boolean
            playerHandScrollPane.setBorder(isGlowing ? handBorderGlow : handBorderDefault);
        });
        turnIndicatorTimer.setRepeats(true); // Keep looping

        log("--- New Game Started (ID: " + gameId + ") ---");
        refreshGameState();
        
        fadeIn(); // Fade in the game panel
    }
    
    // --- GUI Creation Methods (Styled) ---

    private void createOpponentPanel() {
        opponentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        opponentPanel.setBackground(UnoTheme.RED); 
        opponentPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, Color.WHITE));
        
        statusLabel = new JLabel("Loading game, please wait..."); 
        statusLabel.setForeground(UnoTheme.TEXT_WHITE);
        statusLabel.setFont(UnoTheme.HEADER_FONT);
        opponentPanel.add(statusLabel); 
        
        for (Player p : activePlayers) {
            if (p.getPlayerId() != humanPlayerId) {
                JLabel oppLabel = new JLabel(p.getName() + ": 7 Cards");
                oppLabel.setForeground(UnoTheme.TEXT_WHITE);
                oppLabel.setFont(UnoTheme.NORMAL_FONT.deriveFont(Font.BOLD, 16f));
                opponentLabels.put(p.getPlayerId(), oppLabel);
                opponentPanel.add(oppLabel);
            }
        }
    }

    private void createDiscardPilePanel() {
        discardPilePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        discardPilePanel.setBackground(UnoTheme.TABLE_GREEN); 
        
        topCardLabel = new JLabel();
        topCardLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2),
            BorderFactory.createLineBorder(Color.WHITE, 5)
        ));
        
        drawButton = new UnoUI.StyledButton("DRAW CARD", UnoTheme.YELLOW);
        drawButton.setPreferredSize(new Dimension(150, 50)); 
        drawButton.setForeground(UnoTheme.BLACK);
        drawButton.addActionListener(e -> handleDrawAction()); 

        discardPilePanel.add(topCardLabel);
        discardPilePanel.add(drawButton);
    }
    
    private void createPlayerHandPanel() {
        playerHandPanel = new JPanel();
        playerHandPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        playerHandPanel.setBackground(UnoTheme.BLUE);
        
        playerHandScrollPane = new JScrollPane(playerHandPanel);
        playerHandScrollPane.setPreferredSize(new Dimension(1200, 180));
        playerHandScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        playerHandScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        // Set the default border
        playerHandScrollPane.setBorder(handBorderDefault); 
    }

    private void createLogPanel() {
        gameLogArea = new JTextArea();
        gameLogArea.setEditable(false); 
        gameLogArea.setBackground(UnoTheme.BLACK);
        gameLogArea.setForeground(UnoTheme.TEXT_WHITE);
        gameLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        gameLogArea.setMargin(new Insets(10, 10, 10, 10));
        gameLogArea.setLineWrap(true); 
        gameLogArea.setWrapStyleWord(true); 

        logScrollPane = new JScrollPane(gameLogArea);
        logScrollPane.setPreferredSize(new Dimension(300, 0)); 
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, Color.WHITE));
    }

    private void log(String message) {
        gameLogArea.append("• " + message + "\n");
        gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength());
    }

    // --- All Helper Methods ---

    /**
     * REFRESH METHOD - UPDATED with animation logic.
     */
    private void refreshGameState() {
        try {
            GameStatus status = gameController.getGameStatus(conn, gameId);
            if (status == null) return; 
            
            int currentPlayerId = status.getCurrentTurnPlayerId();
            int pendingDraws = status.getPendingDrawStack();
            Card topCard = status.getTopCard();
            
            Player currentPlayer = activePlayers.stream()
                    .filter(p -> p.getPlayerId() == currentPlayerId)
                    .findFirst().orElse(null);

            // 1. LOG THE NEW TURN
            log(String.format("--- Turn for %s ---", currentPlayer.getName()));
            log("Top Card: " + topCard.toString());
            if (pendingDraws > 0) {
                log("!! PENDING DRAW STACK: " + pendingDraws + " !!");
            }
            log("Current Player: " + currentPlayer.getName());

            // 2. Update Top Card Image
            String topCardImagePath;
            String cardValue = topCard.getValue().toLowerCase();
            String cardColor = topCard.getColor().toLowerCase();
            if (topCard.isWild()) {
                topCardImagePath = "images/power_cards/" + cardValue + ".png";
            } else {
                if (cardValue.equals("skip") || cardValue.equals("reverse") || cardValue.equals("draw2")) {
                    topCardImagePath = "images/power_cards/" + cardColor + "_" + cardValue + ".png";
                } else {
                    topCardImagePath = "images/" + cardColor + "/" + cardValue + ".png";
                }
            }
            topCardLabel.setIcon(getScaledIcon(topCardImagePath, 100, 150));
            topCardLabel.setText(null);


            // 3. Update Player Hand
            playerHandPanel.removeAll(); 
            List<Card> humanHand = gameController.getPlayerHand(conn, gameId, humanPlayerId);
            for (Card card : humanHand) {
                playerHandPanel.add(createCardButton(card)); // Will now create a PopUpCardButton
            }
            
            // 4. Update Opponent Card Counts (FIXED)
            Map<Integer, Integer> counts = gameController.getHandCounts(conn, gameId);
            for (Map.Entry<Integer, JLabel> entry : opponentLabels.entrySet()) {
                int count = counts.getOrDefault(entry.getKey(), 0); 
                entry.getValue().setText(activePlayers.stream()
                        .filter(p -> p.getPlayerId() == entry.getKey()) 
                        .findFirst().get().getName() + ": " + count + " Cards");
            }
            
            // --- 5. UPDATE TURN STATUS (ANIMATION LOGIC) ---
            if (currentPlayerId == humanPlayerId) {
                isMyTurn = true;
                setHandEnabled(true);
                drawButton.setEnabled(true);
                statusLabel.setText("Your Turn!" + (pendingDraws > 0 ? " (Must draw " + pendingDraws + " or stack)" : ""));
                
                // START THE PULSING ANIMATION
                if (!turnIndicatorTimer.isRunning()) {
                    turnIndicatorTimer.start();
                }
            } else {
                isMyTurn = false;
                setHandEnabled(false);
                drawButton.setEnabled(false);
                statusLabel.setText("AI (Player " + currentPlayerId + ") is thinking...");
                
                // STOP THE PULSING ANIMATION
                if (turnIndicatorTimer.isRunning()) {
                    turnIndicatorTimer.stop();
                }
                playerHandScrollPane.setBorder(handBorderDefault); // Reset to normal
                
                handleAITurn(currentPlayerId, pendingDraws);
            }
            
            playerHandPanel.revalidate();
            playerHandPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error refreshing game: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void setHandEnabled(boolean enabled) {
        for (Component comp : playerHandPanel.getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(enabled);
            }
        }
    }
    
    private ImageIcon getScaledIcon(String imagePath, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(imagePath);
            if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                throw new Exception("Image not found");
            }
            return new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            System.err.println("Image not found: " + imagePath);
            String errorText = imagePath.replace("images/", "").replace(".png", "").replace("/", " ");
            JLabel errorLabel = new JLabel("<html><center>" + errorText.toUpperCase() + "</center></html>");
            errorLabel.setOpaque(true);
            errorLabel.setBackground(Color.BLACK);
            errorLabel.setForeground(Color.WHITE);
            errorLabel.setPreferredSize(new Dimension(width, height));
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            errorLabel.paint(g);
            g.dispose();
            return new ImageIcon(img);
        }
    }

    private JButton createCardButton(Card card) {
        String imagePath;
        String cardValue = card.getValue().toLowerCase();
        String cardColor = card.getColor().toLowerCase();

        if (card.isWild()) {
            imagePath = "images/power_cards/" + cardValue + ".png"; 
        } else {
            if (cardValue.equals("skip") || cardValue.equals("reverse") || cardValue.equals("draw2")) {
                imagePath = "images/power_cards/" + cardColor + "_" + cardValue + ".png";
            } else {
                imagePath = "images/" + cardColor + "/" + cardValue + ".png";
            }
        }

        ImageIcon icon = getScaledIcon(imagePath, 80, 120);
        
        // Use the PopUpCardButton
        JButton cardButton = new UnoUI.PopUpCardButton(icon);
        cardButton.setActionCommand(String.valueOf(card.getCardId()));
        
        cardButton.addActionListener(e -> {
            if (isMyTurn) {
                handleCardPlayAction(card);
            }
        });
        
        return cardButton;
    }

    private void handleCardPlayAction(Card cardToPlay) {
        try {
            GameStatus status = gameController.getGameStatus(conn, gameId);
            int pendingDraws = status.getPendingDrawStack();

            if (!gameController.validateMove(cardToPlay, status.getTopCard(), pendingDraws)) {
                JOptionPane.showMessageDialog(this, "Invalid Move! You cannot play this card.", "Rule Violation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String nextColor = cardToPlay.getColor();
            if (cardToPlay.isWild()) {
                String[] colors = {"red", "green", "blue", "yellow"};
                String chosen = (String) JOptionPane.showInputDialog(
                        this, "Choose the next color:", "Wild Card Played",
                        JOptionPane.QUESTION_MESSAGE, null, colors, colors[0]);
                if (chosen != null) {
                    nextColor = chosen;
                }
            }
            
            log("You played: " + cardToPlay.toString());
            if(cardToPlay.isWild()) log("You changed the color to " + nextColor.toUpperCase());

            gameController.processMove(conn, gameId, humanPlayerId, cardToPlay, "played", nextColor);
            
            // --- ADDED WIN LOGIC ---
            if (gameController.getPlayerHand(conn, gameId, humanPlayerId).isEmpty()) {
                log("!!! YOU WIN THE GAME !!!");
                gameController.endGame(conn, gameId, humanPlayerId); // <-- Notify DB
                JOptionPane.showMessageDialog(this, "Congratulations, YOU WIN!");
                appManager.showMenu();
                return; 
            }

            refreshGameState(); 

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error playing card: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDrawAction() {
        if (!isMyTurn) return;
        
        try {
            GameStatus status = gameController.getGameStatus(conn, gameId);
            int pendingDraws = status.getPendingDrawStack();
            
            String action = "drawn_and_passed";
            Card cardForLog; 

            if (pendingDraws == 0) {
                cardForLog = gameController.drawCard(conn, gameId, humanPlayerId);
                log("You drew: " + cardForLog.toString());
                JOptionPane.showMessageDialog(this, "You drew: " + cardForLog.toString());
            } else {
                log("You must draw the stack of " + pendingDraws + " cards!");
                JOptionPane.showMessageDialog(this, "You must draw the stack of " + pendingDraws + " cards!");
                cardForLog = gameController.getPlayerHand(conn, gameId, humanPlayerId).get(0); 
            }
            
            gameController.processMove(conn, gameId, humanPlayerId, cardForLog, action, null);
            refreshGameState();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error drawing card: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void handleAITurn(int aiPlayerId, int pendingDraws) {
        String aiName = activePlayers.stream()
                        .filter(p -> p.getPlayerId() == aiPlayerId)
                        .findFirst().get().getName();

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                Thread.sleep(1500); 

                List<Card> aiHand = gameController.getPlayerHand(conn, gameId, aiPlayerId);
                GameStatus status = gameController.getGameStatus(conn, gameId);
                
                List<Card> validMoves = aiHand.stream()
                    .filter(card -> gameController.validateMove(card, status.getTopCard(), pendingDraws))
                    .collect(Collectors.toList());
                
                String logMessage; 

                if (validMoves.isEmpty()) {
                    String action = "drawn_and_passed";
                    Card cardForLog;
                    if (pendingDraws == 0) {
                        cardForLog = gameController.drawCard(conn, gameId, aiPlayerId);
                        logMessage = aiName + " draws 1 card and passes.";
                    } else {
                        logMessage = aiName + " must draw the stack of " + pendingDraws + " cards!";
                        cardForLog = aiHand.get(0); // Dummy card
                    }
                    gameController.processMove(conn, gameId, aiPlayerId, cardForLog, action, null);

                } else {
                    AIDecision decision = gameController.getAIDecision(validMoves, aiHand);
                    Card cardToPlay = decision.card;
                    String nextColor = decision.nextColor;
                    
                    logMessage = aiName + " plays: " + cardToPlay.toString();
                    if (cardToPlay.isWild()) {
                        logMessage += " (Color changed to " + nextColor.toUpperCase() + ")";
                    }
                    
                    gameController.processMove(conn, gameId, aiPlayerId, cardToPlay, "played", nextColor);
                    
                    // --- ADDED WIN LOGIC ---
                    if (gameController.getPlayerHand(conn, gameId, aiPlayerId).isEmpty()) {
                        log("!!! " + aiName + " WINS THE GAME !!!");
                        gameController.endGame(conn, gameId, aiPlayerId); // <-- Notify DB
                        
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(GamePanel.this, aiName + " has won!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                            appManager.showMenu();
                        });
                    }
                }
                return logMessage; 
            }

            @Override
            protected void done() {
                try {
                    String logMessage = get(); 
                    if (logMessage != null) {
                        log(logMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log("!! AI Error: " + e.getMessage());
                }
                refreshGameState(); 
            }
        };
        
        worker.execute();
    }
}