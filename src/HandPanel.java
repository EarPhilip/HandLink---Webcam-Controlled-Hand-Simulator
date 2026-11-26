import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class HandPanel extends JPanel {

    // Order: thumb, index, middle, ring, pinky
    private final int[] fingerCurls = new int[5];

    public HandPanel() {
        setBackground(new Color(18, 24, 34));
    }

    public void setFingerCurl(int finger, int value) {
        if (finger < 0 || finger >= fingerCurls.length) {
            throw new IllegalArgumentException("Invalid finger index");
        }

        fingerCurls[finger] = Math.max(0, Math.min(100, value));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        double scale = Math.min(
            getWidth() / 600.0,
            getHeight() / 500.0
        );

        g.translate(
            (getWidth() - 600 * scale) / 2,
            (getHeight() - 500 * scale) / 2
        );

        g.scale(scale, scale);
        g.setColor(new Color(130, 224, 190));

        // Wrist and palm
        g.fillRoundRect(260, 350, 85, 95, 25, 25);
        g.fillRoundRect(225, 215, 155, 160, 40, 40);

        // Finger index, base position, direction, width, segment lengths
        drawFinger(g, 0, 240, 315, -135, 34, 35, 30, 22);
        drawFinger(g, 1, 241, 235, -90, 32, 55, 46, 33);
        drawFinger(g, 2, 283, 235, -90, 34, 70, 56, 30);
        drawFinger(g, 3, 325, 235, -90, 32, 55, 46, 29);
        drawFinger(g, 4, 365, 240, -85, 28, 45, 35, 25);

        // Palm details
        g.setColor(new Color(49, 105, 96));
        g.setStroke(new BasicStroke(2));
        g.drawLine(250, 275, 350, 275);
        g.drawLine(260, 335, 345, 335);

        g.dispose();
    }

    private void drawFinger(
            Graphics2D g,
            int finger,
            double x,
            double y,
            double direction,
            float width,
            double... segments) {

        double angle = Math.toRadians(direction);
        double curl = Math.toRadians(fingerCurls[finger]);

        g.setStroke(new BasicStroke(
            width,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));

        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                angle += curl;
            }

            double nextX = x + Math.cos(angle) * segments[i];
            double nextY = y + Math.sin(angle) * segments[i];

            g.drawLine(
                (int) x,
                (int) y,
                (int) nextX,
                (int) nextY
            );

            x = nextX;
            y = nextY;
        }
    }
}