package land.plainfunctional.monad;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.util.RandomUtils;

import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static land.plainfunctional.monad.Maybe.just;
import static land.plainfunctional.monad.Maybe.nothing;
import static land.plainfunctional.util.InstrumentationUtils.sleep;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class TestData {

    ///////////////////////////////////////////////////////////////////////////
    // Random test values
    ///////////////////////////////////////////////////////////////////////////

    static RandomUtils random = new RandomUtils();


    ///////////////////////////////////////////////////////////////////////////
    // Test suppliers
    // 'Supplier' instances are "nullary" functions (kind of a deferred value)
    // Here they will NOT be "deferred" constants, as the functions will neither be pure nor total, but partial.
    // For this implementation, as the function takes no input parameter the Reader monad's "environment" is implicit,
    // it will just be the 'Supplier' instance.
    //
    ///////////////////////////////////////////////////////////////////////////

    static Supplier<?> runtimeExceptionSupplier = RuntimeException::new;
    static Supplier<?> throwRuntimeExceptionSupplier =
        () -> {
            throw new RuntimeException();
        };
    static Supplier<?> nullSupplier = () -> null;


    static Supplier<Integer> zeroSupplier = () -> 0;
    static Supplier<Integer> deferredZero = zeroSupplier;


    static Supplier<Integer> oneSupplier = () -> 1;
    static Supplier<Integer> deferredOne = () -> 1;
    static Supplier<Integer> delayedOneSupplier =
        () -> {
            sleep(200, MILLISECONDS);
            return oneSupplier.get();
        };
    static Supplier<Maybe<Integer>> delayedJustOneSupplier =
        () -> {
            sleep(random.pickRandomInteger(200, 5000), MILLISECONDS);
            return just(oneSupplier.get());
        };
    static Supplier<Maybe<Integer>> delayedMaybeOneSupplier =
        () -> {
            sleep(random.pickRandomInteger(200, 5000), MILLISECONDS);
            return random.pickRandomBoolean()
                ? just(oneSupplier.get())
                : nothing();
        };


    static Supplier<Integer> randomIntegerSupplier = () -> random.pickRandomIntegerBetweenOneAnd(1000);

    /*
    // Emulating a typical remote operation, which includes a time-out mechanism
    static Integer getRandomlyDelayedOrTimedOutRandomInteger() {
        Instant start = now();
        int randomInteger = random.pickRandomInteger(200, 3000);
        sleep(randomInteger, MILLISECONDS);
        boolean successfulEvaluation = randomInteger < 2000; // ms
        if (successfulEvaluation) {
            int evaluatedValue = randomNumberSupplier.get();
            System.out.printf(
                "'getRandomlyDelayedOrTimedOutRandomInteger' -> %s, took %d ms%n",
                evaluatedValue,
                between(start, now()).toMillis()
            );
            return evaluatedValue;
        }
        System.err.printf(
            "'getRandomlyDelayedOrTimedOutRandomInteger' -> %s, took %d ms%n",
            "runtime exception",
            between(start, now()).toMillis()
        );
        throw new RuntimeException(new TimeoutException(format("This took way too long!%n")));
    }
    */
    static Integer getRandomlyDelayedOrTimedOutRandomInteger() {
        return getRandomlyDelayedOrTimedOutInteger(
            // random.generateRandomString(2, 6)
            randomIntegerSupplier.get()
        );
    }

    // Emulating a typical remote operation (which includes a time-out mechanism)
    static Integer getRandomlyDelayedOrTimedOutInteger(Integer integer) {
        return getDelayedOrTimedOutInteger(
            2000, // => ~ 1/3 chance of timeout
            random.pickRandomInteger(200, 3000),
            integer
        );
    }


    public static Integer getDelayedInteger(Integer integer) {
        return getDelayedInteger(
            1000,
            integer
        );
    }

    public static Integer getDelayedInteger(Integer delayInMilliseconds, Integer integer) {
        return getDelayedOrTimedOutInteger(
            null,
            delayInMilliseconds,
            integer
        );
    }

    // Emulating a typical remote operation (which includes a time-out mechanism)
    static Integer getDelayedOrTimedOutInteger(Integer timeOutThresholdInMilliseconds, Integer delayInMilliseconds, Integer integer) {
        Instant start = now();

        if (timeOutThresholdInMilliseconds == null) {
            timeOutThresholdInMilliseconds = Integer.MAX_VALUE;
        }
        if (delayInMilliseconds == null) {
            timeOutThresholdInMilliseconds = Integer.MAX_VALUE;
        }
        sleep(delayInMilliseconds, MILLISECONDS);
        boolean successfulEvaluation = delayInMilliseconds < timeOutThresholdInMilliseconds;
        if (successfulEvaluation) {
            System.out.printf(
                "'getDelayedOrTimedOutInteger' -> %d, took %d ms%n",
                integer,
                between(start, now()).toMillis()
            );
            return integer;
        }
        System.out.printf(
            "'getDelayedOrTimedOutInteger' -> %s, took %d ms%n",
            "timeout",
            between(start, now()).toMillis()
        );
        throw new RuntimeException(new TimeoutException(format("This took way too long!%n")));
    }

    static Supplier<Integer> delayedRandomIntegerOrBottomSupplier = TestData::getRandomlyDelayedOrTimedOutRandomInteger;
    static Supplier<Maybe<Integer>> delayedMaybeRandomIntegerSupplier =
        () -> {
            try {
                return just(delayedRandomIntegerOrBottomSupplier.get());

            } catch (Exception ex) {
                return nothing();
            }
        };
    //private static Reader<Integer> delayedRandomIntegerOrBottomReader = Reader.of(delayedRandomIntegerOrBottomSupplier);


    static Supplier<String> helloWorldSupplier = () -> "Hello World!";
    static Supplier<String> delayedHelloWorldSupplier =
        () -> {
            sleep(1000, MILLISECONDS);
            return helloWorldSupplier.get();
        };


    //static Supplier<String> randomStringSupplier = () -> random.generateRandomString(2, 6);

    // Emulating a typical remote operation (which includes a time-out mechanism)
    static String getRandomlyDelayedStringOrTimeoutException() {
        return getRandomlyDelayedStringOrTimeoutException(
            random.generateRandomString(2, 6)
        );
    }

    //public static Function<String, String> delayedStringOrBottomFunction = (string) -> GET_RANDOMLY_DELAYED_STRING_OR_TIMEOUT(string, random);

    //public static Supplier<String> delayedRandomStringOrBottomSupplier = ReaderSpecs::GET_RANDOMLY_DELAYED_OR_TIMED_OUT_RANDOM_STRING;
    //private static Reader<String> delayedRandomStringOrBottomReader = Reader.of(delayedRandomStringOrBottomSupplier);

    // Emulating a typical remote operation (which includes a time-out mechanism)
    // Partiality handles by 'Maybe'
    public static Maybe<String> getMaybeRandomlyDelayedString(String string) {
        try {
            // Partiality/bottom values/undefined values: Handling of nulls
            return Maybe.of(getRandomlyDelayedStringOrTimeoutException(string));

        } catch (RuntimeException exception) {
            // Partiality/bottom values/undefined values: Handling of runtime exceptions
            return nothing();
        }
    }

    // Emulating a typical remote operation (which includes a time-out mechanism)
    public static Function<String, Supplier<String>> GET_RANDOMLY_DELAYED_STRING_OR_TIMEOUT_EXCEPTIONS =
        (string) ->
            () ->
                getDelayedStringOrTimeoutException(
                    2000, // => ~ 1/3 chance of timeout
                    random.pickRandomInteger(200, 3000),
                    string
                );

    // Emulating a typical remote operation (which includes a time-out mechanism)
    public static String getRandomlyDelayedStringOrTimeoutException(String string) {
        return getDelayedStringOrTimeoutException(
            2000, // => ~ 1/3 chance of timeout
            random.pickRandomInteger(200, 3000),
            string
        );
    }

    public static String getDelayedString(String string) {
        return getDelayedString(
            1000,
            string
        );
    }

    public static String getDelayedString(Integer delayInMillisecond, String string) {
        return getDelayedStringOrTimeoutException(
            null,
            delayInMillisecond,
            string
        );
    }

    //public static Function<String, String> delayedStringOrBottomFunction = ReaderSpecs::GET_RANDOMLY_DELAYED_OR_TIMED_OUT_STRING;

    // Emulating a typical remote operation (which includes a time-out mechanism)
    static String getDelayedStringOrTimeoutException(Integer timeOutThresholdInMilliseconds, Integer delayInMilliseconds, String string) {
        Instant start = now();

        if (timeOutThresholdInMilliseconds == null) {
            timeOutThresholdInMilliseconds = Integer.MAX_VALUE;
        }
        if (delayInMilliseconds == null) {
            timeOutThresholdInMilliseconds = Integer.MAX_VALUE;
        }
        sleep(delayInMilliseconds, MILLISECONDS);
        boolean successfulEvaluation = delayInMilliseconds < timeOutThresholdInMilliseconds;
        if (successfulEvaluation) {
            System.out.printf(
                "'getDelayedOrTimedOutString' -> \"%s\", took %d ms%n",
                string,
                between(start, now()).toMillis()
            );
            return string;
        }
        System.out.printf(
            "'getDelayedOrTimedOutString' -> %s, took %d ms%n",
            "timeout",
            between(start, now()).toMillis()
        );
        throw new RuntimeException(new TimeoutException(format("This took way too long!%n")));
    }


    static Supplier<LocalDate> todaySupplier = LocalDate::now;
    static Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;


    ///////////////////////////////////////////////////////////////////////////
    // Test monoids
    ///////////////////////////////////////////////////////////////////////////

    public static final FreeMonoid<Integer> INTEGERS_UNDER_ADDITION_MONOID =
        new FreeMonoid<>(
            Integer::sum,
            0
        );

    public static final FreeMonoid<String> STRING_APPENDING_MONOID =
        new FreeMonoid<>(
            (string1, string2) -> string1 + string2,
            ""
        );

    // See: https://en.wikipedia.org/wiki/Comma-separated_values
    public static final Function<String, FreeMonoid<String>> CSV_MONOID_FUNCTION =
        (separator) ->
            new FreeMonoid<>(
                (string1, string2) -> isBlank(string2) ? string1 : string1 + separator + string2,
                ""
            );

    // See: https://en.wikipedia.org/wiki/Comma-separated_values
    public static final FreeMonoid<String> CSV_MONOID =
        CSV_MONOID_FUNCTION.apply(", ");

    // NB! Null-valued identity elements not allowed for monoids
    //public static final FreeMonoid<Void> SYSTEM_OUT_MONOID_VOID =
    //    new FreeMonoid<>(
    //        (string1, string2) -> {
    //            System.out.println(string2);
    //            return null;
    //        }
    //        , null
    //    );

    public static final FreeMonoid<String> SYSTEM_OUT_MONOID_STRING =
        new FreeMonoid<>(
            (string1, string2) -> {
                System.out.println(string2);
                return "";
            }
            , ""
        );

    // TODO: Include 'Unit' concept?
    /*
    public static final FreeMonoid<Unit> SYSTEM_OUT_MONOID_UNIT =
        new FreeMonoid<>(
            (string1, string2) -> {
                System.out.println(string2);
                return UNIT;
            }
            , UNIT
        );
    */
}
