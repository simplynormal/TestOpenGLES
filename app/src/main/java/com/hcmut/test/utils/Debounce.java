package com.hcmut.test.utils;

import java.util.Timer;
import java.util.TimerTask;

public class Debounce {
    private Timer debounceTimer;
    private long delayMillis;

    public Debounce(long delayMillis) {
        this.delayMillis = delayMillis;
        debounceTimer = new Timer();
    }

    public void debounce(final Runnable function) {
        try {
            debounceTimer.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        debounceTimer = new Timer(); // Create a new Timer instance
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                function.run();
            }
        }, delayMillis);
    }
}
