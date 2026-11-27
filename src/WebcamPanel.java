import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;
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
    private record CameraFrame(
        BufferedImage image, GestureDetector.Gesture gesture, int calibrationEpoch
    ) { }
    private GestureDetector.Gesture latestGesture = GestureDetector.Gesture.NO_HAND;
    private Consumer<GestureDetector.Gesture> gestureListener = gesture -> { };
    private final JLabel gestureLabel = new JLabel("Gesture: camera off", SwingConstants.CENTER);
    private final JLabel preview =
        new JLabel("Camera off", SwingConstants.CENTER);

    private final JButton cameraButton =
        new JButton("Start Camera");

    private final JButton calibrateButton = new JButton("Calibrate Background");
    private final JCheckBox maskBox = new JCheckBox("Show Mask");
    private final JLabel status = new JLabel("Start the camera to begin", SwingConstants.CENTER);
    // A new epoch invalidates any frames queued before recalibration.
    private final AtomicInteger calibrationEpoch = new AtomicInteger();
    private final AtomicBoolean showMask = new AtomicBoolean(false);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private SwingWorker<Void, CameraFrame> worker;

    public WebcamPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(18, 24, 34));

        preview.setForeground(Color.WHITE);
        preview.setPreferredSize(new Dimension(640, 480));

        add(preview, BorderLayout.CENTER);
        status.setForeground(Color.WHITE);
        gestureLabel.setForeground(new Color(130, 224, 190));
        JPanel information = new JPanel(new GridLayout(2, 1, 0, 4));
        information.setBackground(getBackground());
        information.add(status);
        information.add(gestureLabel);
        add(information, BorderLayout.NORTH);

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
            calibrationEpoch.incrementAndGet();
            reportGesture(GestureDetector.Gesture.NO_HAND);
            gestureLabel.setText("Gesture: recalibrating background");
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

        calibrationEpoch.set(0);
        reportGesture(GestureDetector.Gesture.NO_HAND);
        gestureLabel.setText("Gesture: calibrate background first");
        status.setText("Empty the green box, then calibrate");
        running.set(true);
        preview.setText("Opening camera...");
        cameraButton.setText("Stop Camera");

        worker = new SwingWorker<Void, CameraFrame>() {
            @Override
            protected Void doInBackground() throws Exception {
                OpenCV.loadLocally();

                VideoCapture camera = new VideoCapture();
                Mat frame = new Mat();

                try (BackgroundDetector detector = new BackgroundDetector();
                     GestureDetector gestures = new GestureDetector()) {
                    if (!running.get()) return null;

                    if (!camera.open(0)) {
                        throw new IllegalStateException(
                            "Cannot open camera."
                        );
                    }

                    int appliedEpoch = 0;
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
                            int requestedEpoch = calibrationEpoch.get();
                            if (requestedEpoch != appliedEpoch) {
                                detector.calibrate(region);
                                gestures.reset();
                                appliedEpoch = requestedEpoch;
                            }

                            Mat mask = detector.createMask(region);
                            GestureDetector.Gesture gesture = appliedEpoch > 0
                                ? gestures.update(mask)
                                : GestureDetector.Gesture.NO_HAND;
                            if (showMask.get()) {
                                publish(new CameraFrame(toImage(mask), gesture, appliedEpoch));
                            } else {
                                Imgproc.rectangle(
                                    frame, box.tl(), box.br(),
                                    new Scalar(190, 224, 130), 2
                                );
                                publish(new CameraFrame(toImage(frame), gesture, appliedEpoch));
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
            protected void process(List<CameraFrame> frames) {
                if (worker != this || !running.get() || !isDisplayable()) {
                    return;
                }

                CameraFrame result = frames.get(frames.size() - 1);
                if (result.calibrationEpoch() != calibrationEpoch.get()) return;
                boolean calibrated = result.calibrationEpoch() > 0;
                calibrateButton.setEnabled(true);
                status.setText(calibrated
                    ? "Background saved - place your hand in the box"
                    : "Empty the green box, then click Calibrate Background");

                reportGesture(result.gesture());
                gestureLabel.setText(calibrated
                    ? "Gesture: " + result.gesture().label()
                    : "Gesture: calibrate background first");
                BufferedImage latest = result.image();

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
                    reportGesture(GestureDetector.Gesture.NO_HAND);
                    status.setText("Start the camera to begin");
                    gestureLabel.setText("Gesture: camera off");
                }
            }
        };

        worker.execute();
    }

    public void stopCamera() {
        if (worker == null) return;

        running.set(false);
        reportGesture(GestureDetector.Gesture.NO_HAND);
        cameraButton.setText("Stopping camera...");
        cameraButton.setEnabled(false);
        calibrateButton.setEnabled(false);
    }

    // Register and invoke listeners on Swing's event dispatch thread.
    public void setGestureListener(Consumer<GestureDetector.Gesture> listener) {
        gestureListener = Objects.requireNonNull(listener);
        gestureListener.accept(latestGesture);
    }

    private void reportGesture(GestureDetector.Gesture gesture) {
        if (gesture == latestGesture) return;
        latestGesture = gesture;
        gestureListener.accept(gesture);
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
