package com.github.manu156.pinpointintegration.common.runner;

import com.github.manu156.pinpointintegration.common.runner.State;

public interface StateTracker {
    void setState(State state);

    boolean isRunning();
}
