package land.plainfunctional.util;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;

public class InstrumentationUtils {

    public static void sleep(long value, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(value));

        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public static void printThreadInfo() {
        Thread currentThread = Thread.currentThread();
        System.out.printf(
            "Thread id=%s (name=%s, priority=%s, state=%s)%n",
            currentThread.getId(),
            currentThread.getName(),
            currentThread.getPriority(),
            currentThread.getState().name()
        );
    }

    public static <T> void printExecutionStepInfo(Instant start, AtomicInteger counter, T value) {
        Thread currentThread = Thread.currentThread();
        String result = (value == null)
            ? "(not yet evaluated)"
            : format("'%s'", value);

        System.out.printf(
            "Step #%d: %s (at %d ms) (thread \"%s\")%n",
            counter.incrementAndGet(),
            result,
            between(start, now()).toMillis(),
            currentThread.getName()
        );
    }
}
