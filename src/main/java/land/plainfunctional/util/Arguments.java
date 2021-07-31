package land.plainfunctional.util;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class Arguments {

    /**
     * Checks that the given parameter is not {@code null}.
     * This method is designed primarily for doing parameter validation in methods and constructors.
     *
     * @throws IllegalArgumentException if given parameter is {@code null}
     */
    public static <T> void requireNotNull(T arg, String errorMessage) {
        if (arg == null) {
            throw (isBlank(errorMessage))
                ? new IllegalArgumentException("Argument cannot be null")
                : new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks that the given string parameter is not a blank string.
     * This method is designed primarily for doing parameter validation in methods and constructors.
     *
     * @throws IllegalArgumentException if given string parameter is blank
     */
    public static void requireNotBlank(String arg, String errorMessage) {
        if (isBlank(arg)) {
            throw (isBlank(errorMessage))
                ? new IllegalArgumentException("Argument cannot be a blank string")
                : new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks that the given integer parameter is greater than or equal to the given threshold.
     *
     * @throws IllegalArgumentException given integer parameter is greater than or equal to the given threshold.
     */
    public static void requireGreaterThanOrEqualTo(Integer threshold, Integer integer, String errorMessage) {
        requireNotNull(threshold, "'threshold' argument cannot be null");
        requireNotNull(integer, "'integer' argument cannot be null");

        if (threshold > integer) {
            throw (isBlank(errorMessage))
                ? new IllegalArgumentException(format("%d must be greater than %d", integer, threshold))
                : new IllegalArgumentException(errorMessage);
        }
    }
}
