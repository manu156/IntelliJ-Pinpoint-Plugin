package com.github.manu156.pinpointintegration.common.runner;

import java.util.function.Consumer;

public interface Runner {

    void onFinish(Consumer<Void> onFinish);

    void run();
    void cancel();

}
