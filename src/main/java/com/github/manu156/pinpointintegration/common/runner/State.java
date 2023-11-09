package com.github.manu156.pinpointintegration.common.runner;

public enum State {
    RUNNING,
    IDLE;
    public boolean isRunning() {
        return this == State.RUNNING;
    }
}
