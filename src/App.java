import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
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
            JFrame window = new JFrame("HandLink - Hand Simulator");

            window.setSize(900, 650);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setLocationRelativeTo(null);
            window.getContentPane().setBackground(new Color(18, 24, 34));

            JLabel title = new JLabel(
                "HandLink",
                SwingConstants.CENTER
            );

            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(Color.WHITE);

            HandPanel handPanel = new HandPanel();

            JSlider indexSlider = new JSlider(0, 100, 0);
            indexSlider.setMajorTickSpacing(25);
            indexSlider.setPaintTicks(true);
            indexSlider.setPaintLabels(true);

            indexSlider.addChangeListener(event -> {
                handPanel.setIndexCurl(indexSlider.getValue());
            });

            JPanel controls = new JPanel(new BorderLayout());
            controls.setBorder(
                BorderFactory.createTitledBorder("Index finger curl")
            );
            controls.add(indexSlider, BorderLayout.CENTER);

            window.add(title, BorderLayout.NORTH);
            window.add(handPanel, BorderLayout.CENTER);
            window.add(controls, BorderLayout.SOUTH);

            window.setVisible(true);
        });
    }
}