package com.hcmut.test.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Debounce {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Future<?> futureTask = null;

    private final long delay;

    public Debounce(long delay) {
        this.delay = delay;
    }

    public void debounce(final Runnable runnable) {
        // If a task is already scheduled, cancel it
        if (futureTask != null && !futureTask.isDone()) {
            futureTask.cancel(true);
        }

        // Schedule the new task
        futureTask = scheduler.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }
}
