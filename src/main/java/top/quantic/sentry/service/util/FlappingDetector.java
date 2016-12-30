package top.quantic.sentry.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;

/**
 * Simple implementation of a flapping detection algorithm.
 * Ported from https://github.com/dclowd9901/FlappingDetection/blob/master/FlappingDetection.js
 */
public class FlappingDetector<T> {

    private static final Logger log = LoggerFactory.getLogger(FlappingDetector.class);
    private static final int DEFAULT_CAPACITY = 100;

    private final ArrayBlockingQueue<T> states;
    private final int capacity;
    private final Function<T, Boolean> healthCheck;
    private final List<FlappingListener> listenerList = new ArrayList<>();

    private volatile State state = State.GOOD;
    private volatile double lastRatio = 0;
    private volatile double averageRatio = 1;
    private volatile double windowMargin = 0.02;
    private volatile double nominalRatio = 0.98;
    private volatile boolean settled = false;
    private volatile double recoveryThreshold;

    public FlappingDetector(Function<T, Boolean> healthCheck) {
        this(healthCheck, DEFAULT_CAPACITY);
    }

    public FlappingDetector(Function<T, Boolean> healthCheck, int maxStates) {
        this.healthCheck = healthCheck;
        this.capacity = maxStates;
        this.states = new ArrayBlockingQueue<>(capacity, true);
    }

    private double determineRatio() {
        return ((double) states.stream().mapToInt(s -> healthCheck.apply(s) ? 1 : 0).sum()) / states.size();
    }

    public void check(T value) {
        states.offer(value);
        double ratio = determineRatio();
        double ratioLow = lastRatio - windowMargin;
        double ratioHigh = lastRatio + windowMargin;

        if (state == State.GOOD) {
            // Services is in good state
            if (ratio < nominalRatio) {
                // Service dipped below viability
                toBadState();
            } else {
                if (states.size() == capacity) {
                    // Two shifts for cleanup after a recovery state
                    tryPoll();
                    tryPoll();
                }
            }
        } else if (state == State.RECOVERY) {
            // Service is not 100%, but it appears to be recovering
            if (ratio > lastRatio) {
                tryPoll();
            }

            // Service appears to have reached nominal status
            if (ratio >= nominalRatio) {
                averageRatio = nominalRatio;
                toGoodState();
            }
        } else {
            // Service is in bad mode
            // test if the ratios have settled
            settled = ratio == lastRatio;

            if (settled) {
                averageRatio = ratio;
            }

            // test if ratio is upward bound
            if (ratio > lastRatio) {
                // A recovery threshold is the average between the average bad ratio and
                // the nominal ratio. If the service's ratio surpasses this amount, it's deemed
                // "in recovery"
                recoveryThreshold = (nominalRatio - averageRatio) / 2 + averageRatio;

                if (ratio > recoveryThreshold) {
                    toRecoveryState();
                }
            }

            if (ratio >= ratioLow && ratio <= ratioHigh) {
                // maintain window length if ratio doesn't change drastically
                tryPoll();
            }
        }

        lastRatio = ratio;
        log.debug("{}", toString());
    }

    private void tryPoll() {
        if (!states.isEmpty()) {
            states.poll();
        }
    }

    private void toBadState() {
        state = State.BAD;
        notifyChange();
    }

    private void toRecoveryState() {
        state = State.RECOVERY;
        notifyChange();
    }

    private void toGoodState() {
        state = State.GOOD;
        notifyChange();
    }

    public void addListener(FlappingListener listener) {
        listenerList.add(listener);
    }

    public void removeListener(FlappingListener listener) {
        listenerList.remove(listener);
    }

    public void removeAllListeners() {
        listenerList.clear();
    }

    private void notifyChange() {
        listenerList.forEach(listener -> listener.onStateChange(new Snapshot(state, settled, lastRatio, averageRatio)));
    }

    public State getState() {
        return state;
    }

    public double getAverageRatio() {
        return averageRatio;
    }

    public double getLastRatio() {
        return lastRatio;
    }

    public double getRecoveryThreshold() {
        return recoveryThreshold;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getWindowMargin() {
        return windowMargin;
    }

    public void setWindowMargin(double windowMargin) {
        this.windowMargin = windowMargin;
    }

    public double getNominalRatio() {
        return nominalRatio;
    }

    public void setNominalRatio(double nominalRatio) {
        this.nominalRatio = nominalRatio;
    }

    @Override
    public String toString() {
        return "FlappingDetector{" +
            "state=" + state +
            ", lastRatio=" + lastRatio +
            ", settled=" + settled +
            ", recoveryThreshold=" + recoveryThreshold +
            ", averageRatio=" + averageRatio +
            '}';
    }

    public enum State {
        GOOD, BAD, RECOVERY
    }

    public interface FlappingListener {
        void onStateChange(Snapshot snapshot);
    }

    public static final class Snapshot {
        private final State state;
        private final boolean settled;
        private final double lastRatio;
        private final double averageRatio;

        private Snapshot(State state, boolean settled, double lastRatio, double averageRatio) {
            this.state = state;
            this.settled = settled;
            this.lastRatio = lastRatio;
            this.averageRatio = averageRatio;
        }

        public State getState() {
            return state;
        }

        public boolean isSettled() {
            return settled;
        }

        public double getLastRatio() {
            return lastRatio;
        }

        public double getAverageRatio() {
            return averageRatio;
        }

        @Override
        public String toString() {
            return "Result{" +
                "state=" + state +
                ", settled=" + settled +
                ", lastRatio=" + lastRatio +
                ", averageRatio=" + averageRatio +
                '}';
        }
    }
}
