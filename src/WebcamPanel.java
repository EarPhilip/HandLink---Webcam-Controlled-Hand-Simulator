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
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class WebcamPanel extends JPanel {
    private final JLabel preview =
        new JLabel("Camera off", SwingConstants.CENTER);

    private final JButton cameraButton =
        new JButton("Start Camera");

    private final JButton calibrateButton = new JButton("Calibrate Background");
    private final JCheckBox maskBox = new JCheckBox("Show Mask");
    private final JLabel status = new JLabel("Start the camera to begin", SwingConstants.CENTER);
    private final AtomicBoolean calibrationRequested = new AtomicBoolean(false);
    private final AtomicBoolean calibrated = new AtomicBoolean(false);
    private final AtomicBoolean showMask = new AtomicBoolean(false);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private SwingWorker<Void, BufferedImage> worker;

    public WebcamPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(18, 24, 34));

        preview.setForeground(Color.WHITE);
        preview.setPreferredSize(new Dimension(640, 480));

        add(preview, BorderLayout.CENTER);
        status.setForeground(Color.WHITE);
        add(status, BorderLayout.NORTH);

        JPanel cameraControls = new JPanel();
        cameraControls.setBackground(getBackground());
        maskBox.setBackground(getBackground());
        maskBox.setForeground(Color.WHITE);
        calibrateButton.setEnabled(false);
        cameraControls.add(cameraButton);
        cameraControls.add(calibrateButton);
        cameraControls.add(maskBox);
        add(cameraControls, BorderLayout.SOUTH);

        calibrateButton.addActionListener(event -> {
            calibrationRequested.set(true);
            status.setText("Saving the empty background...");
        });
        maskBox.addActionListener(event -> showMask.set(maskBox.isSelected()));

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

        calibrationRequested.set(false);
        calibrated.set(false);
        status.setText("Empty the green box, then calibrate");
        running.set(true);
        preview.setText("Opening camera...");
        cameraButton.setText("Stop Camera");

        worker = new SwingWorker<Void, BufferedImage>() {
            @Override
            protected Void doInBackground() throws Exception {
                OpenCV.loadLocally();

                VideoCapture camera = new VideoCapture();
                Mat frame = new Mat();

                try (BackgroundDetector detector = new BackgroundDetector()) {
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

                        // Analyze only the box, keeping your face/body outside it.
                        Rect box = new Rect(160, 40, 320, 400);
                        Mat region = frame.submat(box);
                        try {
                            if (calibrationRequested.getAndSet(false)) {
                                detector.calibrate(region);
                                calibrated.set(true);
                            }

                            Mat mask = detector.createMask(region);
                            if (showMask.get()) {
                                publish(toImage(mask));
                            } else {
                                Imgproc.rectangle(
                                    frame, box.tl(), box.br(),
                                    new Scalar(190, 224, 130), 2
                                );
                                publish(toImage(frame));
                            }
                        } finally {
                            region.release();
                        }
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

                calibrateButton.setEnabled(true);
                status.setText(calibrated.get()
                    ? "Background saved - place your hand in the box"
                    : "Empty the green box, then click Calibrate Background");

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
                    calibrateButton.setEnabled(false);
                    calibrated.set(false);
                    status.setText("Start the camera to begin");
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
        calibrateButton.setEnabled(false);
    }

    private static BufferedImage toImage(Mat frame) {
        BufferedImage image = new BufferedImage(
            frame.cols(),
            frame.rows(),
            frame.channels() == 1
                ? BufferedImage.TYPE_BYTE_GRAY
                : BufferedImage.TYPE_3BYTE_BGR
        );

        byte[] pixels =
            ((DataBufferByte) image.getRaster().getDataBuffer())
                .getData();

        frame.get(0, 0, pixels);

        return image;
    }
}
