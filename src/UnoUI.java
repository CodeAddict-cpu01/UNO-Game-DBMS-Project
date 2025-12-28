// --- File: src/UnoUI.java ---

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A helper class containing custom, styled, and animated
 * Swing components for the UNO GUI.
 */
public class UnoUI {

    /**
     * A modern, rounded button that smoothly changes color on hover.
     */
    public static class StyledButton extends JButton {
        private Color normalColor;
        private Color hoverColor;
        private Color pressedColor;
        private Timer hoverTimer;
        private float currentAlpha = 0f; // For smooth fade

        public StyledButton(String text, Color baseColor) {
            super(text);
            this.normalColor = baseColor;
            this.hoverColor = baseColor.brighter(); // Color when hovered
            this.pressedColor = baseColor.darker();  // Color when clicked

            setFont(UnoTheme.BUTTON_FONT);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(300, 60));

            // Timer to animate the hover effect
            hoverTimer = new Timer(20, e -> {
                if (getModel().isRollover()) {
                    if (currentAlpha < 1.0f) currentAlpha += 0.1f;
                } else {
                    if (currentAlpha > 0.0f) currentAlpha -= 0.1f;
                }
                if (currentAlpha < 0) currentAlpha = 0;
                if (currentAlpha > 1) currentAlpha = 1;
                
                // Stop timer if it's done fading in or out
                if (currentAlpha == 0.0f || currentAlpha == 1.0f) {
                    ((Timer)e.getSource()).stop();
                }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hoverTimer.start();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    hoverTimer.start();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Calculate current color based on hover state
            Color base = getModel().isPressed() ? pressedColor : normalColor;
            Color blended = blend(base, hoverColor, currentAlpha);

            // Draw rounded background
            g2.setColor(blended);
            g2.fill(new RoundRectangle2D.Float(0, 0, width, height, 20, 20));

            // Draw Text manually to center it
            FontMetrics fm = g2.getFontMetrics();
            Rectangle stringBounds = fm.getStringBounds(getText(), g2).getBounds();
            int textX = (width - stringBounds.width) / 2;
            int textY = (height - stringBounds.height) / 2 + fm.getAscent();

            g2.setColor(getForeground());
            g2.setFont(getFont());
            g2.drawString(getText(), textX, textY);
            g2.dispose();
        }

        // Helper function to blend two colors
        private Color blend(Color c1, Color c2, float ratio) {
            if (ratio > 1f) ratio = 1f;
            else if (ratio < 0f) ratio = 0f;
            float iRatio = 1.0f - ratio;

            int i1 = c1.getRGB();
            int i2 = c2.getRGB();

            int a1 = (i1 >> 24 & 0xff);
            int r1 = ((i1 & 0xff0000) >> 16);
            int g1 = ((i1 & 0xff00) >> 8);
            int b1 = (i1 & 0xff);

            int a2 = (i2 >> 24 & 0xff);
            int r2 = ((i2 & 0xff0000) >> 16);
            int g2 = ((i2 & 0xff00) >> 8);
            int b2 = (i2 & 0xff);

            int a = (int) ((a1 * iRatio) + (a2 * ratio));
            int r = (int) ((r1 * iRatio) + (r2 * ratio));
            int g = (int) ((g1 * iRatio) + (g2 * ratio));
            int b = (int) ((b1 * iRatio) + (b2 * ratio));

            return new Color(a << 24 | r << 16 | g << 8 | b);
        }
    }

    /**
     * A Card button that "pops up" when hovered.
     */
    public static class PopUpCardButton extends JButton {
        private final int popHeight = 20; // How high it pops up
        private boolean isHovered = false;

        public PopUpCardButton(ImageIcon icon) {
            super(icon);
            setBorder(null);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            // Add padding to the top so it doesn't get cut off when it moves up
            setBorder(new EmptyBorder(popHeight, 0, 0, 0));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        isHovered = true;
                        // Shift padding to bottom to make it look like it moved up
                        setBorder(new EmptyBorder(0, 0, popHeight, 0)); 
                        revalidate();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                     if (isEnabled()) {
                        isHovered = false;
                        // Reset padding to top
                        setBorder(new EmptyBorder(popHeight, 0, 0, 0));
                        revalidate();
                     }
                }
            });
        }
        
        @Override
        public void setEnabled(boolean b) {
             super.setEnabled(b);
             if (!b) {
                 // Reset position if disabled while hovering
                 setBorder(new EmptyBorder(popHeight, 0, 0, 0));
             }
        }
    }
}