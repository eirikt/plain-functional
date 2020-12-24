package land.plainfunctional.util;

import java.util.Random;

public class RandomUtils {

    private static final Random PSEUDO_RANDOM_GENERATOR = new Random();

    /**
     * @return a pseudo-random boolean value
     */
    public boolean pickRandomBoolean() {
        return pickRandomIntegerBetweenOneAnd(2) > 1;
    }

    /**
     * @param upperBound the upper bound (inclusive) of random 1-based integer
     * @return a pseudo-random 1-based integer
     */
    public int pickRandomIntegerBetweenOneAnd(int upperBound) {
        return PSEUDO_RANDOM_GENERATOR.nextInt(upperBound) + 1;
    }

    /**
     * @param lowerBound the lower bound (inclusive) of random 1-based integer
     * @param upperBound the upper bound (inclusive) of random 1-based integer
     * @return a pseudo-random 1-based integer
     */
    public int pickRandomInteger(int lowerBound, int upperBound) {
        return generateRandomIntegers(1, lowerBound, upperBound + 1)[0];
    }

    /**
     * @param numberOfDigits the number of digits to generate
     * @return An int array with pseudo-random digits
     */
    public int[] generateRandomDigits(int numberOfDigits) {
        return generateRandomIntegers(numberOfDigits, 1, 9);
    }

    /**
     * @param numberOfDigits the number of digits to generate
     * @param min            the lower bound (inclusive) of each random digit
     * @param max            the upper bound (inclusive) of each random digit
     * @return An int array with pseudo-random digits within the given range
     */
    public int[] generateRandomIntegers(int numberOfDigits, int min, int max) {
        return PSEUDO_RANDOM_GENERATOR.ints(numberOfDigits, min, max + 1).toArray();
    }

    /**
     * @param maximumNumberOfCharacters the maximum length of the generated string
     * @return A pseudo-random string with given maximum length
     */
    public String generateRandomString(int minimumNumberOfCharacters, int maximumNumberOfCharacters) {
        int leftCodePointLimit = 97;   // Letter 'a'
        int rightCodePointLimit = 122; // Letter 'z'

        return PSEUDO_RANDOM_GENERATOR
            .ints(leftCodePointLimit, rightCodePointLimit + 1)
            .limit(pickRandomInteger(minimumNumberOfCharacters, maximumNumberOfCharacters))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }
}
