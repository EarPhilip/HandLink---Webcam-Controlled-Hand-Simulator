import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/** Compares the tracking area against a saved, empty background. */
public final class BackgroundDetector implements AutoCloseable {
    private final Mat gray = new Mat();
    private final Mat background = new Mat();
    private final Mat difference = new Mat();
    private final Mat mask = new Mat();
    private final Mat kernel = Imgproc.getStructuringElement(
        Imgproc.MORPH_ELLIPSE, new Size(5, 5)
    );

    public void calibrate(Mat image) {
        prepareGray(image);
        gray.copyTo(background);
    }

    public Mat createMask(Mat image) {
        prepareGray(image);

        if (background.empty()) {
            mask.create(gray.size(), CvType.CV_8UC1);
            mask.setTo(Scalar.all(0));
            return mask;
        }

        Core.absdiff(gray, background, difference);
        Imgproc.threshold(difference, mask, 30, 255, Imgproc.THRESH_BINARY);

        // Remove small specks and fill small gaps in the foreground.
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

        return mask;
    }

    private void prepareGray(Mat image) {
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(7, 7), 0);
    }

    @Override
    public void close() {
        gray.release();
        background.release();
        difference.release();
        mask.release();
        kernel.release();
    }
}
