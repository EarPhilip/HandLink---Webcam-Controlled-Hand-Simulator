import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Color background = new Color(18, 24, 34);

            JFrame window = new JFrame("HandLink - Hand Simulator");
            window.setSize(1200, 850);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setLocationRelativeTo(null);
            window.getContentPane().setBackground(background);

            JLabel title = new JLabel(
                    "HandLink",
                    SwingConstants.CENTER);

            title.setFont(new Font("SansSerif", Font.BOLD, 32));
            title.setForeground(Color.WHITE);

            HandPanel handPanel = new HandPanel();
            WebcamPanel webcamPanel = new WebcamPanel();
            String[] names = {
                    "Thumb", "Index", "Middle", "Ring", "Pinky"
            };

            JSlider[] sliders = new JSlider[names.length];

            // Arrays let the listeners share mutable values.
            int[] targetCurl = { 0 };
            boolean[] updatingSliders = { false };

            Timer animation = new Timer(16, null);

            JPanel sliderPanel = new JPanel(
                    new GridLayout(5, 1, 0, 5));
            sliderPanel.setBackground(background);

            for (int i = 0; i < names.length; i++) {
                final int finger = i;

                JLabel label = new JLabel(names[i]);
                label.setForeground(Color.WHITE);
                label.setPreferredSize(new Dimension(70, 30));

                JSlider slider = new JSlider(0, 100, 0);
                slider.setBackground(background);
                slider.setToolTipText("0 = straight, 100 = curled");

                slider.addChangeListener(event -> {
                    // Manual input interrupts the automatic animation.
                    if (!updatingSliders[0]) {
                        animation.stop();
                    }

                    handPanel.setFingerCurl(
                            finger,
                            slider.getValue());
                });

                sliders[i] = slider;

                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBackground(background);
                row.add(label, BorderLayout.WEST);
                row.add(slider, BorderLayout.CENTER);

                sliderPanel.add(row);
            }

            animation.addActionListener(event -> {
                boolean finished = true;
                updatingSliders[0] = true;

                for (JSlider slider : sliders) {
                    int current = slider.getValue();
                    int target = targetCurl[0];

                    // Move toward the target without overshooting.
                    int next;

                    if (current < target) {
                        next = Math.min(current + 3, target);
                    } else {
                        next = Math.max(current - 3, target);
                    }

                    slider.setValue(next);

                    if (next != target) {
                        finished = false;
                    }
                }

                updatingSliders[0] = false;

                if (finished) {
                    animation.stop();
                }
            });

            JButton openButton = new JButton("Open Hand");
            openButton.addActionListener(event -> {
                targetCurl[0] = 0;
                animation.restart();
            });

            JButton closeButton = new JButton("Close Hand");
            closeButton.addActionListener(event -> {
                targetCurl[0] = 100;
                animation.restart();
            });

            JPanel buttons = new JPanel();
            buttons.setBackground(background);
            buttons.add(openButton);
            buttons.add(closeButton);

            JPanel controls = new JPanel(new BorderLayout(0, 10));
            controls.setBackground(background);
            controls.setBorder(
                    BorderFactory.createEmptyBorder(10, 25, 20, 25));
            controls.add(sliderPanel, BorderLayout.CENTER);
            controls.add(buttons, BorderLayout.SOUTH);

            window.add(title, BorderLayout.NORTH);
            JPanel views = new JPanel(new GridLayout(1, 2, 15, 0));
            views.setBackground(background);
            views.setBorder(
                    BorderFactory.createEmptyBorder(15, 15, 15, 15));

            views.add(webcamPanel);
            views.add(handPanel);

            window.add(views, BorderLayout.CENTER);
            window.add(controls, BorderLayout.SOUTH);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    animation.stop();
                    webcamPanel.stopCamera();
                }
            });
            window.setVisible(true);
        });
    }
}