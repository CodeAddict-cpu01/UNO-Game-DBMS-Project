import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnimatedSplashScreen extends JWindow {

    private AnimationPanel panel;
    private Timer animationTimer;
    
    // Animation States
    private enum State { DROP, EXPLODE, SHOW_TEXT, WAIT }
    private State currentState = State.DROP;

    // Animation Variables
    private int bombY = -100; 
    private int explosionRadius = 0;
    private float textAlpha = 0.0f;
    private int waitCounter = 0;
    private boolean whiteFlash = false; 

    // Configuration
    private final int WINDOW_WIDTH = 600;
    private final int WINDOW_HEIGHT = 400;
    private final int BOMB_SPEED = 14;      // Speed of falling bomb
    private final int EXPLOSION_SPEED = 25; // Speed of expanding blast

    public AnimatedSplashScreen() {
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null); // Centers window on screen
        setBackground(new Color(0, 0, 0, 0)); // Transparent window background

        panel = new AnimationPanel();
        setContentPane(panel);
        
        // Start animation at 60 Frames Per Second
        animationTimer = new Timer(16, e -> updateAnimation());
        animationTimer.start();
        
        setVisible(true);
    }

    private void updateAnimation() {
        int centerY = getHeight() / 2;

        switch (currentState) {
            case DROP:
                bombY += BOMB_SPEED;
                if (bombY >= centerY - 25) { 
                    bombY = centerY - 25;
                    currentState = State.EXPLODE;
                    whiteFlash = true; // Trigger the "lighting" flash
                }
                break;

            case EXPLODE:
                whiteFlash = false; 
                explosionRadius += EXPLOSION_SPEED;
                if (explosionRadius > getWidth() * 1.5) {
                    currentState = State.SHOW_TEXT;
                }
                break;

            case SHOW_TEXT:
                textAlpha += 0.05f; // Fast fade in
                if (textAlpha >= 1.0f) {
                    textAlpha = 1.0f;
                    currentState = State.WAIT;
                }
                break;

            case WAIT:
                waitCounter++;
                if (waitCounter > 80) { // Fast Switch after 1.3 seconds
                    animationTimer.stop();
                    dispose(); // Close intro
                    // Launch the Game Menu
                    SwingUtilities.invokeLater(() -> new UNOAppManager().setVisible(true));
                }
                break;
        }
        panel.repaint();
    }

    private class AnimationPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;

            if (whiteFlash) {
                g2.setColor(Color.WHITE); 
                g2.fillRect(0, 0, w, h);
                return; 
            } 
            
            if (currentState == State.SHOW_TEXT || currentState == State.WAIT) {
                g2.setColor(new Color(20, 20, 20)); 
                g2.fillRect(0, 0, w, h);
            }

            if (currentState == State.DROP) {
                drawBomb(g2, cx - 25, bombY);
            } 
            else if (currentState == State.EXPLODE) {
                drawExplosion(g2, cx, cy, explosionRadius);
            } 
            else if (currentState == State.SHOW_TEXT || currentState == State.WAIT) {
                drawExplosion(g2, cx, cy, w * 2); 
                drawLogoText(g2, cx, cy);
            }
        }

        private void drawBomb(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(100, 50, 0)); // Fuse
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(x + 25, y, x + 35, y - 15);
            g2.setColor(Color.ORANGE); // Spark
            g2.fillOval(x + 32, y - 20, 8, 8);
            g2.setColor(Color.BLACK); // Bomb
            g2.fillOval(x, y, 50, 50);
        }

        private void drawExplosion(Graphics2D g2, int cx, int cy, int radius) {
            g2.setColor(new Color(237, 28, 36)); // Outer Red
            g2.fillOval(cx - radius/2, cy - radius/2, radius, radius);
            g2.setColor(new Color(255, 140, 0)); // Orange
            int r2 = (int)(radius * 0.7);
            g2.fillOval(cx - r2/2, cy - r2/2, r2, r2);
            g2.setColor(new Color(255, 220, 0)); // Inner Yellow
            int r3 = (int)(radius * 0.4);
            g2.fillOval(cx - r3/2, cy - r3/2, r3, r3);
        }

        private void drawLogoText(Graphics2D g2, int cx, int cy) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
            Font font = new Font("Arial Black", Font.BOLD, 70);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            String text = "UNO GAME";
            int x = cx - (fm.stringWidth(text) / 2);
            int y = cy + (fm.getAscent() / 4);

            g2.setColor(Color.BLACK); // Shadow
            g2.drawString(text, x + 5, y + 5);

            int curX = x;
            Color[] colors = {new Color(237, 28, 36), new Color(255, 220, 0), new Color(80, 176, 78), new Color(0, 114, 188)};
            for (int i = 0; i < text.length(); i++) {
                g2.setColor(colors[i % 4]);
                String letter = String.valueOf(text.charAt(i));
                g2.drawString(letter, curX, y);
                curX += fm.stringWidth(letter);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AnimatedSplashScreen());
    }
}