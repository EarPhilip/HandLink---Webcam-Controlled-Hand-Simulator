
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
                    "Welcome to HandLink",
                    SwingConstants.CENTER
            );

            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(Color.WHITE);

            window.add(title, BorderLayout.NORTH);
            window.add(new HandPanel(), BorderLayout.CENTER);
            window.setVisible(true);
        });
    }
}
