package land.plainfunctional.util;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class Arguments {

    /**
     * Checks that the specified object reference is not {@code null}.
     * This method is designed primarily for doing parameter validation in methods and constructors.
     *
     * @throws IllegalArgumentException if given {@code arg} is {@code null}
     */
    public static <T> void requireNotNull(T arg, String errorMessage) {
        if (arg == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks that the specified object reference is not a blank string.
     * This method is designed primarily for doing parameter validation in methods and constructors.
     *
     * @throws IllegalArgumentException if given string {@code arg} is blank
     */
    public static void requireNotBlank(String arg, String errorMessage) {
        if (isBlank(arg)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
