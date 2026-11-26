import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class WebcamPanel extends JPanel {
    private final JLabel preview =
        new JLabel("Camera off", SwingConstants.CENTER);

    private final JButton cameraButton =
        new JButton("Start Camera");

    private final AtomicBoolean running = new AtomicBoolean(false);
    private SwingWorker<Void, BufferedImage> worker;

    public WebcamPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(18, 24, 34));

        preview.setForeground(Color.WHITE);
        preview.setPreferredSize(new Dimension(640, 480));

        add(preview, BorderLayout.CENTER);
        add(cameraButton, BorderLayout.SOUTH);

        cameraButton.addActionListener(event -> {
            if (worker == null) {
                startCamera();
            } else {
                stopCamera();
            }
        });
    }

    public void startCamera() {
        if (worker != null) return;

        running.set(true);
        preview.setText("Opening camera...");
        cameraButton.setText("Stop Camera");

        worker = new SwingWorker<Void, BufferedImage>() {
            @Override
            protected Void doInBackground() throws Exception {
                OpenCV.loadLocally();

                VideoCapture camera = new VideoCapture();
                Mat frame = new Mat();

                try {
                    if (!running.get()) return null;

                    if (!camera.open(0)) {
                        throw new IllegalStateException(
                            "Cannot open camera."
                        );
                    }

                    while (running.get()) {
                        if (!camera.read(frame) || frame.empty()) {
                            throw new IllegalStateException(
                                "No camera frame received."
                            );
                        }

                        Core.flip(frame, frame, 1);
                        Imgproc.resize(frame, frame, new Size(640, 480));

                        publish(toImage(frame));
                        Thread.sleep(33);
                    }
                } finally {
                    camera.release();
                    frame.release();
                }

                return null;
            }

            @Override
            protected void process(List<BufferedImage> frames) {
                if (worker != this || !running.get() || !isDisplayable()) {
                    return;
                }

                BufferedImage latest = frames.get(frames.size() - 1);

                // Fit the image inside the panel without stretching it.
                double scale = Math.min(
                    Math.max(1, preview.getWidth())
                        / (double) latest.getWidth(),
                    Math.max(1, preview.getHeight())
                        / (double) latest.getHeight()
                );

                Image scaled = latest.getScaledInstance(
                    Math.max(1, (int) (latest.getWidth() * scale)),
                    Math.max(1, (int) (latest.getHeight() * scale)),
                    Image.SCALE_FAST
                );

                preview.setText(null);
                preview.setIcon(new ImageIcon(scaled));
            }

            @Override
            protected void done() {
                boolean stopped = !running.get();

                try {
                    get();
                    preview.setText("Camera stopped");
                } catch (Exception exception) {
                    preview.setText(
                        stopped
                            ? "Camera stopped"
                            : "Camera error - check terminal"
                    );

                    if (!stopped) {
                        exception.printStackTrace();
                    }
                } finally {
                    preview.setIcon(null);
                    running.set(false);
                    worker = null;
                    cameraButton.setText("Start Camera");
                    cameraButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void stopCamera() {
        if (worker == null) return;

        running.set(false);
        cameraButton.setText("Stopping camera...");
        cameraButton.setEnabled(false);
    }

    private static BufferedImage toImage(Mat frame) {
        BufferedImage image = new BufferedImage(
            frame.cols(),
            frame.rows(),
            BufferedImage.TYPE_3BYTE_BGR
        );

        byte[] pixels =
            ((DataBufferByte) image.getRaster().getDataBuffer())
                .getData();

        frame.get(0, 0, pixels);

        return image;
    }
}