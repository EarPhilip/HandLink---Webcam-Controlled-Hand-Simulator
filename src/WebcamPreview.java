import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class WebcamPreview {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("HandLink - Webcam Preview");
            WebcamPanel webcamPanel = new WebcamPanel();

            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.add(webcamPanel);
            window.pack();
            window.setLocationRelativeTo(null);

            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    webcamPanel.stopCamera();
                }
            });

            window.setVisible(true);
        });
    }
}