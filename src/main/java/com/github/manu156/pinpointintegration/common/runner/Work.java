package com.github.manu156.pinpointintegration.common.runner;


public interface Work {
    void run();
    void cancel();
    boolean isRunning();
}
