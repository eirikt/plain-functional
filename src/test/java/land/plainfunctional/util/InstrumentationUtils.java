package land.plainfunctional.util;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static org.apache.commons.lang3.StringUtils.repeat;

public class InstrumentationUtils {

    public static final String DOTS = "...";
    public static final String NOT_YET_IMPLEMENTED = "Not yet implemented";
    public static final String NOT_SUPPORTED = "Not supported";
    public static final String NOT_YET_SUPPORTED = "Not yet supported";
    public static final String REDEFINED_ELSEWHERE = "Redefined elsewhere";
    public static final String REDEFINED = "Redefined";
    public static final String NOT_APPLICABLE = "N/A";
    public static final String SKIPPED = "Skipped";
    public static final String TEMPORARY = "Temporary";
    public static final String TODO = "TODO";

    public static final String DEBUG_INDENTATION = repeat(" ", 2);
    public static final String STANDARD_INDENTATION = repeat(DEBUG_INDENTATION, 2);
    public static final String CONTINUATION_INDENTATION = repeat(STANDARD_INDENTATION, 2);


    public static <T> T notImplemented() {
        return notYetImplemented();
    }

    public static <T> T notYetImplemented() {
        throw new UnsupportedOperationException(NOT_YET_IMPLEMENTED);
    }

    /**
     * "Not supported", meaning "not applicable" (N/A).
     */
    public static <T> T notSupported() {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

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

        if (counter == null) {
            System.out.printf(
                "Step (unknown): %s (at %d ms) (thread \"%s\")%n",
                result,
                between(start, now()).toMillis(),
                currentThread.getName()
            );
            return;
        }

        System.out.printf(
            "Step #%d: %s (at %d ms) (thread \"%s\")%n",
            counter.incrementAndGet(),
            result,
            between(start, now()).toMillis(),
            currentThread.getName()
        );
    }
}
