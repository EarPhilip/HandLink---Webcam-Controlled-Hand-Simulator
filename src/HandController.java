import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.Timer;

/** Coordinates manual controls and camera input on Swing's event dispatch thread. */
public final class HandController implements AutoCloseable {
    private final HandPanel hand;
    private final JSlider[] sliders;
    private final JCheckBox followCamera;
    private final Timer animation;
    private GestureDetector.Gesture latestGesture = GestureDetector.Gesture.NO_HAND;
    private int targetCurl;
    private boolean updatingSliders;

    public HandController(HandPanel hand, JSlider[] sliders, JCheckBox followCamera) {
        if (sliders.length != 5) throw new IllegalArgumentException("Five sliders required");
        this.hand = hand;
        this.sliders = sliders.clone();
        this.followCamera = followCamera;
        animation = new Timer(16, event -> advanceAnimation());

        for (int i = 0; i < sliders.length; i++) {
            final int finger = i;
            hand.setFingerCurl(finger, sliders[i].getValue());
            sliders[i].addChangeListener(event -> {
                if (!updatingSliders) {
                    animation.stop();
                    followCamera.setSelected(false);
                }
                hand.setFingerCurl(finger, this.sliders[finger].getValue());
            });
        }

        followCamera.addActionListener(event -> {
            animation.stop();
            if (followCamera.isSelected()) applyCameraGesture();
        });
    }

    public void openHand() { animateManually(0); }
    public void closeHand() { animateManually(100); }

    public void onGesture(GestureDetector.Gesture gesture) {
        boolean changed = gesture != latestGesture;
        latestGesture = gesture;
        if (followCamera.isSelected() && changed) applyCameraGesture();
    }

    private void applyCameraGesture() {
        switch (latestGesture) {
            case OPEN_HAND -> animateTo(0);
            case CLOSED_FIST -> animateTo(100);
            case NO_HAND, UNKNOWN -> animation.stop();
        }
    }

    private void animateManually(int target) {
        followCamera.setSelected(false);
        animateTo(target);
    }

    private void animateTo(int target) {
        targetCurl = target;
        for (JSlider slider : sliders) {
            if (slider.getValue() != target) {
                animation.start();
                return;
            }
        }
        animation.stop();
    }

    private void advanceAnimation() {
        boolean finished = true;
        updatingSliders = true;
        try {
            for (JSlider slider : sliders) {
                int current = slider.getValue();
                int next = current < targetCurl
                    ? Math.min(current + 3, targetCurl)
                    : Math.max(current - 3, targetCurl);
                slider.setValue(next);
                if (next != targetCurl) finished = false;
            }
        } finally {
            updatingSliders = false;
        }
        if (finished) animation.stop();
    }

    @Override
    public void close() {
        animation.stop();
        followCamera.setSelected(false);
    }
}
