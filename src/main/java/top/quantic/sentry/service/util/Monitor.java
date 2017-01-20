package top.quantic.sentry.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Monitor<T> {

    private final String name;
    private final int failuresToTrigger;
    private final int successesToRecover;
    private final ArrayBlockingQueue<T> states;
    private final List<Listener> listenerList = new ArrayList<>();

    private volatile Function<T, Boolean> healthCheck;
    private volatile State state = State.GOOD;
    private volatile double nominalRatio = 0.95;

    public Monitor(String name, int failuresToTrigger, int successesToRecover, Function<T, Boolean> healthCheck) {
        this.name = name;
        this.failuresToTrigger = failuresToTrigger;
        this.successesToRecover = successesToRecover;
        this.healthCheck = healthCheck;
        int capacity = 2 * Math.max(failuresToTrigger, successesToRecover);
        this.states = new ArrayBlockingQueue<>(capacity, true);
    }

    public synchronized State check(T value) {
        if (!states.offer(value)) {
            states.poll();
            states.offer(value);
        }

        List<T> snapshot = states.stream().collect(Collectors.toList());

        if (state == State.GOOD) {
            int consecutive = 0;
            int index = snapshot.size() - 1;
            while (consecutive < failuresToTrigger && index >= 0) {
                if (!healthCheck.apply(snapshot.get(index))) {
                    consecutive++;
                    index--;
                } else {
                    break;
                }
            }

            if (consecutive == failuresToTrigger) {
                toBadState();
            }
        } else if (state == State.BAD) {
            int consecutive = 0;
            int index = snapshot.size() - 1;
            while (consecutive < successesToRecover || index >= 0) {
                if (healthCheck.apply(snapshot.get(index))) {
                    consecutive++;
                    index--;
                } else {
                    break;
                }
            }

            if (consecutive == successesToRecover) {
                toRecoveryState();
            }
        } else {
            if (determineRatio() > nominalRatio) {
                toGoodState();
            }
        }

        return state;
    }

    private void toGoodState() {
        State previous = state;
        state = State.GOOD;
        notifyChange(previous);
    }

    private void toRecoveryState() {
        State previous = state;
        state = State.RECOVERY;
        notifyChange(previous);
    }

    private void toBadState() {
        State previous = state;
        state = State.BAD;
        notifyChange(previous);
    }

    public String getName() {
        return name;
    }

    public State getState() {
        return state;
    }

    public int getFailuresToTrigger() {
        return failuresToTrigger;
    }

    public int getSuccessesToRecover() {
        return successesToRecover;
    }

    public Function<T, Boolean> getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(Function<T, Boolean> healthCheck) {
        this.healthCheck = healthCheck;
    }

    public double getNominalRatio() {
        return nominalRatio;
    }

    public void setNominalRatio(double nominalRatio) {
        this.nominalRatio = nominalRatio;
    }

    public void addListener(Listener listener) {
        listenerList.add(listener);
    }

    public void removeListener(Listener listener) {
        listenerList.remove(listener);
    }

    public void removeAllListeners() {
        listenerList.clear();
    }

    private double determineRatio() {
        return ((double) states.stream().mapToInt(s -> healthCheck.apply(s) ? 1 : 0).sum()) / states.size();
    }

    private void notifyChange(State previousState) {
        Snapshot snapshot = new Snapshot(name, previousState, state, determineRatio());
        listenerList.forEach(listener -> listener.onStateChange(snapshot));
    }

    @Override
    public String toString() {
        return "Monitor{" +
            "name='" + name + '\'' +
            ", failuresToTrigger=" + failuresToTrigger +
            ", successesToRecover=" + successesToRecover +
            ", states=" + states +
            ", state=" + state +
            '}';
    }

    public interface Listener {
        void onStateChange(Snapshot snapshot);
    }

    public enum State {
        GOOD, BAD, RECOVERY
    }

    public static class Snapshot {
        private final String name;
        private final State lastState;
        private final State currentState;
        private final double lastRatio;

        public Snapshot(String name, State lastState, State currentState, double lastRatio) {
            this.name = name;
            this.lastState = lastState;
            this.currentState = currentState;
            this.lastRatio = lastRatio;
        }

        public String getName() {
            return name;
        }

        public State getLastState() {
            return lastState;
        }

        public State getCurrentState() {
            return currentState;
        }

        public double getLastRatio() {
            return lastRatio;
        }

        @Override
        public String toString() {
            return "Snapshot{" +
                "name=" + name +
                ", lastState=" + lastState +
                ", currentState=" + currentState +
                ", lastRatio=" + lastRatio +
                '}';
        }
    }

}
