import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class HandPanel extends JPanel {

    public HandPanel() {
        setBackground(new Color(18, 24, 34));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        // Keep the drawing centered and proportional when resized.
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

        // Fingers: index, middle, ring, pinky
        g.fillRoundRect(225, 95, 32, 155, 30, 30);
        g.fillRoundRect(266, 65, 34, 185, 30, 30);
        g.fillRoundRect(309, 90, 32, 160, 30, 30);
        g.fillRoundRect(351, 130, 29, 125, 28, 28);

        // Thumb
        g.setStroke(new BasicStroke(
            36,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));

        g.drawLine(240, 315, 180, 255);

        // Palm details
        g.setColor(new Color(49, 105, 96));
        g.setStroke(new BasicStroke(2));
        g.drawLine(250, 275, 350, 275);
        g.drawLine(260, 335, 345, 335);

        g.dispose();
    }
}