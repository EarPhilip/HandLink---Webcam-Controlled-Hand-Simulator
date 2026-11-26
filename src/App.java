import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Color background = new Color(18, 24, 34);

            JFrame window = new JFrame("HandLink - Hand Simulator");
            window.setSize(900, 800);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setLocationRelativeTo(null);
            window.getContentPane().setBackground(background);

            JLabel title = new JLabel(
                "HandLink",
                SwingConstants.CENTER
            );

            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(Color.WHITE);

            HandPanel handPanel = new HandPanel();

            JPanel controls = new JPanel(new GridLayout(5, 1, 0, 5));
            controls.setBackground(background);
            controls.setBorder(
                BorderFactory.createEmptyBorder(10, 25, 20, 25)
            );

            String[] names = {
                "Thumb", "Index", "Middle", "Ring", "Pinky"
            };

            for (int i = 0; i < names.length; i++) {
                // Each listener keeps its own finger index.
                final int finger = i;

                JLabel label = new JLabel(names[i]);
                label.setForeground(Color.WHITE);
                label.setPreferredSize(new Dimension(70, 30));

                JSlider slider = new JSlider(0, 100, 0);
                slider.setBackground(background);
                slider.setToolTipText("0 = straight, 100 = curled");

                slider.addChangeListener(event -> {
                    handPanel.setFingerCurl(finger, slider.getValue());
                });

                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBackground(background);
                row.add(label, BorderLayout.WEST);
                row.add(slider, BorderLayout.CENTER);

                controls.add(row);
            }

            window.add(title, BorderLayout.NORTH);
            window.add(handPanel, BorderLayout.CENTER);
            window.add(controls, BorderLayout.SOUTH);

            window.setVisible(true);
        });
    }
}