package com.github.manu156.pinpointintegration.common.runner;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


public class SimpleWork<R extends Runner> implements Work {
    private final R runner;
    private final StateTracker stateTracker;

    public SimpleWork(R runner, Consumer<AtomicReference<State>> onChangeToState) {
        this.stateTracker = new SimpleStateTracker(State.IDLE, onChangeToState);
        this.runner = runner;
        runner.onFinish(ignored -> stateTracker.setState(State.IDLE));
    }

    @Override
    public void cancel() {
        stateTracker.setState(State.IDLE);
        runner.cancel();
    }

    @Override
    public void run() {
        if (!stateTracker.isRunning()) {
            stateTracker.setState(State.RUNNING);
            runner.run();
        } else {
            cancel();
        }
    }

    @Override
    public boolean isRunning() {
        return stateTracker.isRunning();
    }

    public void flip() {
        run();
    }
}
