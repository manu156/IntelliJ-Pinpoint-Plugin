package com.github.manu156.pinpointintegration.common.runner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CancellationToken {
    private final CompletableFuture<Boolean> cancelled = new CompletableFuture<>();

    public void onCancellation(Runnable callback) {
        cancelled.thenAccept(
                isCancelled -> {
                    if (Boolean.TRUE.equals(isCancelled)) {
                        try {
                            callback.run();
                        } catch (Exception ignored) {
                            // ignore callback exceptions
                        }
                    }
                }
        );
    }

    public boolean isCancelled() {
        try {
            return cancelled.isDone() && cancelled.get();
        } catch (ExecutionException ex) {
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return cancelled.isDone();
        }
    }

    public void cancel() {
        cancelled.complete(true);
    }
}
