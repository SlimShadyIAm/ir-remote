package com.slimshady.irremote;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.Assert.*;

public class RepeatSenderTest {
    private final List<Runnable> pending = new ArrayList<>();
    private final List<String> sent = new ArrayList<>();
    private final List<RuntimeException> errors = new ArrayList<>();
    private Consumer<RuntimeException> completion;
    private final RepeatSender<String> sender = new RepeatSender<>(new RepeatSender.Scheduler() {
        public void postDelayed(Runnable task, long delay) {
            assertEquals(100, delay); pending.add(task);
        }
        public void removeCallbacks(Runnable task) { pending.removeIf(r -> r == task); }
    }, (signal, done) -> {
        assertNull("Transmissions must never overlap", completion);
        sent.add(signal); completion = done;
    }, errors::add);
    private void complete(RuntimeException error) {
        Consumer<RuntimeException> done = completion; completion = null; done.accept(error);
    }
    private void tick() { pending.remove(0).run(); }

    @Test public void repeatsOnlyAfterTransmissionCompletes() {
        sender.start("volume"); assertEquals(1, sent.size()); assertTrue(pending.isEmpty());
        complete(null); assertEquals(1, pending.size()); tick();
        assertEquals(List.of("volume", "volume"), sent); assertTrue(pending.isEmpty());
    }
    @Test public void releaseDuringTransmissionPreventsAnotherSend() {
        sender.start("volume"); sender.stop(); complete(null);
        assertTrue(pending.isEmpty()); assertEquals(1, sent.size());
    }
    @Test public void releaseDuringGapRemovesScheduledRepeat() {
        sender.start("volume"); complete(null); sender.stop();
        assertTrue(pending.isEmpty()); assertEquals(1, sent.size());
    }
    @Test public void rapidRepressWaitsForPreviousSendWithoutDuplicatingRepeats() {
        sender.start("old"); sender.stop(); sender.start("new");
        assertEquals(1, sent.size()); assertEquals(1, pending.size());
        tick(); assertEquals(1, pending.size()); assertEquals(1, sent.size());
        complete(null); assertEquals(1, pending.size()); tick();
        assertEquals(List.of("old", "new"), sent);
        complete(null); assertEquals(1, pending.size());
    }
    @Test public void cancelWhileWaitingForPreviousSendStaysStopped() {
        sender.start("old"); sender.stop(); sender.start("new"); sender.stop(); complete(null);
        assertTrue(pending.isEmpty()); assertEquals(List.of("old"), sent);
    }
    @Test public void failureStopsRepeatingAndReportsOnce() {
        RuntimeException error = new IllegalArgumentException("Unsupported frequency");
        sender.start("bad"); complete(error);
        assertEquals(List.of(error), errors); assertTrue(pending.isEmpty());
        assertTrue(sender.sendOnce("valid")); complete(null); assertTrue(pending.isEmpty());
    }
    @Test public void accessibilityClickSendsOnceAndCannotQueueWhileBusy() {
        assertTrue(sender.sendOnce("click")); assertFalse(sender.sendOnce("second"));
        complete(null); assertTrue(pending.isEmpty()); assertEquals(List.of("click"), sent);
    }
}
