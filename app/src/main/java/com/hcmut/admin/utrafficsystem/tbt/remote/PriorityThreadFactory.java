package com.hcmut.admin.utrafficsystem.tbt.remote;

import android.os.Process;

import java.util.concurrent.ThreadFactory;

public class PriorityThreadFactory implements ThreadFactory {
    private int threadPriority;
    private final String prefix;
    private int threadCount = 1;

    public PriorityThreadFactory(int threadPriority, String prefix) {
        this.threadPriority = threadPriority;
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        Runnable wrapperRunnable = () -> {
            try {
                Process.setThreadPriority(threadPriority);
            } catch (Throwable t) {
                // just to be safe
            }
            runnable.run();
        };
        String name = prefix + " - " + threadCount;
        threadCount++;
        return new Thread(wrapperRunnable, name);
    }
}
