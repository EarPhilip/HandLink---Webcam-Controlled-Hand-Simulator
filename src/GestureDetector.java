import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/** Experimental silhouette rules for one upright hand inside the tracking box. */
public final class GestureDetector implements AutoCloseable {
    public enum Gesture {
        NO_HAND("No hand detected"),
        OPEN_HAND("Open hand (estimate)"),
        CLOSED_FIST("Closed fist (estimate)"),
        UNKNOWN("Unclear pose");

        private final String label;

        Gesture(String label) { this.label = label; }
        public String label() { return label; }
    }

    private final Mat scratch = new Mat();
    private final Mat hierarchy = new Mat();
    private Gesture candidate = Gesture.NO_HAND;
    private Gesture stable = Gesture.NO_HAND;
    private int consecutiveFrames;

    /** Four matching frames are required before the displayed gesture changes. */
    public Gesture update(Mat mask) {
        Gesture observed = detect(mask);
        if (observed != candidate) {
            candidate = observed;
            consecutiveFrames = 0;
        }
        consecutiveFrames = Math.min(4, consecutiveFrames + 1);
        if (consecutiveFrames == 4) stable = candidate;
        return stable;
    }

    public void reset() {
        candidate = stable = Gesture.NO_HAND;
        consecutiveFrames = 0;
    }

    public Gesture detect(Mat mask) {
        if (mask.empty()) return Gesture.NO_HAND;
        List<MatOfPoint> contours = new ArrayList<>();
        mask.copyTo(scratch);

        try {
            Imgproc.findContours(
                scratch, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
            );

            MatOfPoint largest = null;
            double largestArea = 0;
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > largestArea) {
                    largest = contour;
                    largestArea = area;
                }
            }

            if (largest == null || largestArea < mask.total() * .03) {
                return Gesture.NO_HAND;
            }
            if (largestArea > mask.total() * .75) return Gesture.UNKNOWN;

            Rect bounds = Imgproc.boundingRect(largest);
            // The wrist can exit at the bottom; fingertips need space elsewhere.
            if (bounds.x <= 1 || bounds.y <= 1
                    || bounds.x + bounds.width >= mask.cols() - 1) {
                return Gesture.UNKNOWN;
            }

            return removeWristAndClassify(mask, largest);
        } finally {
            contours.forEach(Mat::release);
        }
    }

    private Gesture removeWristAndClassify(Mat mask, MatOfPoint contour) {
        Mat silhouette = Mat.zeros(mask.size(), CvType.CV_8UC1);
        Mat distances = new Mat();
        Mat contourHierarchy = new Mat();
        List<MatOfPoint> trimmedContours = new ArrayList<>();

        try {
            Imgproc.drawContours(silhouette, List.of(contour), 0, Scalar.all(255), -1);
            Imgproc.distanceTransform(silhouette, distances, Imgproc.DIST_L2, 5);
            Core.MinMaxLocResult palm = Core.minMaxLoc(distances);
            if (palm.maxVal < 14) return Gesture.UNKNOWN;

            // The largest inscribed circle approximates the palm. Trim below it
            // so a long wrist does not make a compact fist look like a thin shape.
            int cut = Math.min(mask.rows(), (int) Math.ceil(palm.maxLoc.y + palm.maxVal * 1.15));
            if (cut < mask.rows()) {
                Mat wrist = silhouette.submat(new Rect(0, cut, mask.cols(), mask.rows() - cut));
                try {
                    wrist.setTo(Scalar.all(0));
                } finally {
                    wrist.release();
                }
            }

            Imgproc.findContours(silhouette, trimmedContours, contourHierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            MatOfPoint hand = null;
            double largestArea = 0;
            for (MatOfPoint candidate : trimmedContours) {
                double area = Imgproc.contourArea(candidate);
                if (area > largestArea) {
                    hand = candidate;
                    largestArea = area;
                }
            }

            return hand == null ? Gesture.UNKNOWN : classify(hand, Imgproc.boundingRect(hand));
        } finally {
            trimmedContours.forEach(Mat::release);
            silhouette.release();
            distances.release();
            contourHierarchy.release();
        }
    }

    private Gesture classify(MatOfPoint contour, Rect bounds) {
        MatOfPoint2f curve = new MatOfPoint2f(contour.toArray());
        MatOfPoint2f simplified = new MatOfPoint2f();
        MatOfPoint polygon = new MatOfPoint();
        MatOfPoint hullShape = new MatOfPoint();
        MatOfInt hull = new MatOfInt();
        MatOfInt4 defects = new MatOfInt4();

        try {
            Imgproc.approxPolyDP(
                curve, simplified, Imgproc.arcLength(curve, true) * .003, true
            );
            polygon.fromArray(simplified.toArray());
            Point[] points = polygon.toArray();
            if (points.length < 3) return Gesture.UNKNOWN;

            Imgproc.convexHull(polygon, hull);
            int[] indexes = hull.toArray();
            if (indexes.length < 3) return Gesture.UNKNOWN;

            Point[] hullPoints = new Point[indexes.length];
            for (int i = 0; i < indexes.length; i++) hullPoints[i] = points[indexes[i]];
            hullShape.fromArray(hullPoints);

            double hullArea = Imgproc.contourArea(hullShape);
            if (hullArea <= 0) return Gesture.UNKNOWN;
            double solidity = Imgproc.contourArea(polygon) / hullArea;
            int gaps = 0;

            if (indexes.length > 3 && points.length > 3) {
                try {
                    Imgproc.convexityDefects(polygon, hull, defects);
                } catch (CvException exception) {
                    return Gesture.UNKNOWN;
                }

                int[] values = defects.empty() ? new int[0] : defects.toArray();
                for (int i = 0; i < values.length; i += 4) {
                    Point start = points[values[i]];
                    Point end = points[values[i + 1]];
                    Point valley = points[values[i + 2]];
                    double a = distance(start, valley);
                    double b = distance(end, valley);
                    double c = distance(start, end);
                    if (a < 1 || b < 1) continue;

                    double cosine = (a*a + b*b - c*c) / (2*a*b);
                    double angle = Math.acos(Math.max(-1, Math.min(1, cosine)));
                    double depth = values[i + 3] / 256.0;

                    if (depth > Math.max(10, bounds.height * .055)
                            && angle < Math.toRadians(100)
                            && valley.y < bounds.y + bounds.height * .72) {
                        gaps++;
                    }
                }
            }

            if (gaps >= 2 && solidity < .90) return Gesture.OPEN_HAND;

            double aspect = bounds.height / (double) bounds.width;
            if (gaps == 0 && solidity >= .88 && aspect >= .65 && aspect <= 1.65) {
                return Gesture.CLOSED_FIST;
            }
            return Gesture.UNKNOWN;
        } finally {
            curve.release();
            simplified.release();
            polygon.release();
            hullShape.release();
            hull.release();
            defects.release();
        }
    }

    private static double distance(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    @Override
    public void close() {
        scratch.release();
        hierarchy.release();
    }
}
