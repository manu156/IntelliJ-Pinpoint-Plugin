package com.github.manu156.pinpointintegration.common.runner;

import com.intellij.openapi.application.ApplicationManager;

import java.util.function.Consumer;

public class PooledThreadRun implements Runner {
    private CancellationToken cancellationToken;
    private final Consumer<CancellationToken> doRun;
    private Consumer<Void> onFinish;

    public PooledThreadRun(Consumer<CancellationToken> doRun) {
        cancellationToken = new CancellationToken();
        this.doRun = doRun;
        onFinish = ignored -> {};
    }

    @Override
    public void onFinish(Consumer<Void> onFinish) {
        this.onFinish = onFinish;
    }

    @Override
    public void run() {
        ApplicationManager.getApplication().executeOnPooledThread(
                () -> {
                    doRun.accept(cancellationToken);
                    onFinish.accept(null);
                }
        );
    }

    @Override
    public void cancel() {
        cancellationToken.cancel();
        cancellationToken = new CancellationToken();
    }
}
