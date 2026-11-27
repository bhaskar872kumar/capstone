package com.healthdekho.recommendation;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/** Runs an AI call on a virtual thread and gives up after a timeout, so a slow model never blocks the request. */
@Component
public class AiCallExecutor {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public <T> T call(Supplier<T> task, Duration timeout)
            throws TimeoutException, ExecutionException, InterruptedException {
        Future<T> future = executor.submit(task::get);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw ex;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
