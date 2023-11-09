package com.github.manu156.pinpointintegration.common.runner;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SimpleStateTracker implements StateTracker {

    private final AtomicReference<State> state;
    private final Consumer<AtomicReference<State>> onStateChange;

    public SimpleStateTracker(State state, Consumer<AtomicReference<State>> onStateChange) {
        this.state = new AtomicReference<>(state);
        this.onStateChange = onStateChange;
    }

    @Override
    public void setState(State state) {
        this.state.set(state);
        onStateChange.accept(this.state);
    }

    @Override
    public boolean isRunning() {
        return state.get().isRunning();
    }
}
