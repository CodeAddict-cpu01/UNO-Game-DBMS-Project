// --- File: src/AnimatedPanel.java ---

import javax.swing.*;
import java.awt.*;

/**
 * An abstract JPanel that provides built-in fade-in and fade-out
 * animation capabilities. All other "screens" in the app will extend this.
 */
public abstract class AnimatedPanel extends JPanel {

    private Timer animationTimer;
    private float alpha = 0.0f; // 0.0 = invisible, 1.0 = fully visible
    private int fadeSpeed = 30; // Milliseconds per frame
    private float fadeStep = 0.05f; // How much to fade per frame

    public AnimatedPanel() {
        // Start completely transparent
        setOpaque(false);
    }

    /**
     * Overrides the default painting method to apply an alpha (transparency)
     * level to the entire panel.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Apply transparency
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        
        // Draw the panel's actual content (background, buttons, etc.)
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Call the original paint for all child components (buttons, labels)
        super.paintChildren(g2d); 
        
        g2d.dispose();
    }

    /**
     * Starts the fade-in animation.
     */
    public void fadeIn() {
        // Stop any previous animation
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        animationTimer = new Timer(fadeSpeed, e -> {
            alpha += fadeStep;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                ((Timer)e.getSource()).stop();
            }
            repaint();
        });
        animationTimer.start();
    }

    /**
     * Starts the fade-out animation. When finished, it runs the 'onFinished' task.
     * @param onFinished A piece of code (a Runnable) to execute *after* fading out.
     */
    public void fadeOut(Runnable onFinished) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        animationTimer = new Timer(fadeSpeed, e -> {
            alpha -= fadeStep;
            if (alpha <= 0.0f) {
                alpha = 0.0f;
                ((Timer)e.getSource()).stop();
                // Run the "what next?" code
                if (onFinished != null) {
                    onFinished.run();
                }
            }
            repaint();
        });
        animationTimer.start();
    }
}