import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class WebcamPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WebcamPreview::openWindow);
    }

    private static void openWindow() {
        JFrame window = new JFrame("HandLink - Webcam Preview");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel preview = new JLabel(
            "Opening camera...",
            SwingConstants.CENTER
        );

        preview.setPreferredSize(new Dimension(640, 480));

        window.add(preview);
        window.pack();
        window.setLocationRelativeTo(null);

        AtomicBoolean running = new AtomicBoolean(true);

        SwingWorker<Void, BufferedImage> worker =
            new SwingWorker<Void, BufferedImage>() {

                @Override
                protected Void doInBackground() throws Exception {
                    // Load OpenCV on the background thread.
                    OpenCV.loadLocally();

                    VideoCapture camera = new VideoCapture();
                    Mat frame = new Mat();

                    try {
                        if (!camera.open(0)) {
                            throw new IllegalStateException(
                                "Cannot open camera. Check camera permissions."
                            );
                        }

                        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
                        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);

                        while (running.get() && !isCancelled()) {
                            if (!camera.read(frame) || frame.empty()) {
                                throw new IllegalStateException(
                                    "Camera stopped returning images."
                                );
                            }

                            // Mirror the preview.
                            Core.flip(frame, frame, 1);

                            Imgproc.resize(
                                frame,
                                frame,
                                new Size(640, 480)
                            );

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
                protected void process(List<BufferedImage> images) {
                    if (!window.isDisplayable()) {
                        return;
                    }

                    // Display the newest available image.
                    BufferedImage latest = images.get(images.size() - 1);

                    preview.setText(null);
                    preview.setIcon(new ImageIcon(latest));
                }

                @Override
                protected void done() {
                    if (!window.isDisplayable() || isCancelled()) {
                        return;
                    }

                    try {
                        get();
                    } catch (Exception exception) {
                        preview.setIcon(null);
                        preview.setText("Camera error - check the terminal.");

                        Throwable cause = exception.getCause();

                        if (cause != null) {
                            cause.printStackTrace();
                        } else {
                            exception.printStackTrace();
                        }
                    }
                }
            };

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                running.set(false);
                worker.cancel(true);
            }
        });

        window.setVisible(true);
        worker.execute();
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