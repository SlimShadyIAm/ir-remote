package com.slimshady.irremote;

import java.util.function.Consumer;

/** All methods and transmission completions run on the UI thread. */
final class RepeatSender<T> {
    interface Scheduler {
        void postDelayed(Runnable task, long delayMs);
        void removeCallbacks(Runnable task);
    }
    interface Sender<T> { void send(T signal, Consumer<RuntimeException> completion); }
    private final Scheduler scheduler;
    private final Sender<T> sender;
    private final Consumer<RuntimeException> onError;
    private final Runnable repeat = this::tick;
    private T signal;
    private boolean active;
    private boolean busy;
    private int generation;
    static final long REPEAT_GAP_MS = 100;

    RepeatSender(Scheduler scheduler, Sender<T> sender, Consumer<RuntimeException> onError) {
        this.scheduler = scheduler; this.sender = sender; this.onError = onError;
    }
    void start(T signal) {
        stop(); this.signal = signal; active = true; tick();
    }
    void stop() {
        active = false; signal = null; generation++;
        scheduler.removeCallbacks(repeat);
    }
    boolean sendOnce(T signal) {
        if (busy || active) return false;
        transmit(signal, false); return true;
    }
    private void tick() {
        if (!active) return;
        if (busy) scheduler.postDelayed(repeat, REPEAT_GAP_MS);
        else transmit(signal, true);
    }
    private void transmit(T value, boolean repeating) {
        busy = true;
        int startedGeneration = generation;
        sender.send(value, error -> {
            busy = false;
            if (error != null) { stop(); onError.accept(error); return; }
            if (repeating && active && startedGeneration == generation)
                scheduler.postDelayed(repeat, REPEAT_GAP_MS);
        });
    }
}
