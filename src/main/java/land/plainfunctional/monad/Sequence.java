package land.plainfunctional.monad;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;

/**
 * <p>
 * <i>Functor context:</i>
 * <b>
 * Contains one or more enumerated (possible duplicated) values
 * </b>
 * </p>
 *
 * <p>
 * Sequences is also known as <i>lists</i>.
 * ({@link Sequence} delegates to an {@link ArrayList} instance.)
 * </p>
 *
 * <p>
 * The contained values are also known as <i>elements</i> (of the sequence/list).
 * </p>
 *
 * @param <T> The type of the contained values
 * @see <a href="https://en.wikipedia.org/wiki/Sequence">Mathematical sequences</a>
 * @see <a href="https://en.wikipedia.org/wiki/List_(abstract_data_type)">List (abstract data type)</a>
 */
public class Sequence<T> implements Monad<T> {

    ///////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Just for having a {@link Sequence} instance to reach the member methods, e.g. <code>pure</code>.
     */
    public static <T> Sequence<T> withSequence() {
        return withSequence(null);
    }

    /**
     * Just for having a typed {@link Sequence} instance to reach the member methods, e.g. <code>pure</code>.
     */
    public static <T> Sequence<T> withSequence(Class<T> type) {
        return empty();
    }

    /**
     * @return an empty {@link Sequence}
     */
    public static <T> Sequence<T> empty() {
        return new Sequence<>();
    }

    // TODO: Possible heap pollution from parameterized vararg type (=> @SafeVarargs)
    // => https://www.baeldung.com/java-safevarargs

    /**
     * @return a {@link Sequence} containing the given vales
     */
    public static <T> Sequence<T> of(T... values) {
        return new Sequence<>(values);
    }

    /**
     * @return a {@link Sequence} containing the given vales
     */
    public static <T> Sequence<T> of(Iterable<? extends T> values) {
        return new Sequence<>(values);
    }


    ///////////////////////////////////////////////////////
    // State & Constructors
    ///////////////////////////////////////////////////////

    protected final List<T> values;

    protected Sequence() {
        this.values = new ArrayList<>();
    }

    // TODO: Possible heap pollution from parameterized vararg type (=> @SafeVarargs)
    // => https://www.baeldung.com/java-safevarargs
    protected Sequence(T... values) {
        this();
        for (T value : values) {
            Arguments.requireNotNull(value, "'Sequence' cannot contain 'null' values");
            this.values.add(value);
        }
    }

    protected Sequence(Iterable<? extends T> iterable) {
        this();
        for (T value : iterable) {
            Arguments.requireNotNull(value, "'Sequence' cannot contain 'null' values");
            this.values.add(value);
        }
    }


    ///////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////

    /**
     * @return 'true' if this sequence (of values) contains no elements, otherwise 'false'
     */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    /**
     * @return the number of elements in this sequence (of values)
     */
    public long size() {
        return this.values.size();
    }

    /**
     * @return a shallow copy of this sequence (of values)
     */
    public List<T> asJavaList() {
        return new ArrayList<>(this.values);
    }

    List<T> _unsafe() {
        return this.values;
    }


    ///////////////////////////////////////////////////////
    // Functor properties
    ///////////////////////////////////////////////////////

    @Override
    public <U> Sequence<U> map(Function<? super T, ? extends U> function) {
        List<U> mappedValues = new ArrayList<>(this.values.size());
        for (T value : this.values) {
            U mappedValue = function.apply(value);

            // Partial function handling:
            // NB! Skipping inclusion as mapped value has no representation in the codomain
            if (mappedValue == null) {
                System.err.printf("NB! 'Sequence::map': Skipping mapping as value has no representation in the codomain, %s%n", value);
                continue;
            }

            mappedValues.add(mappedValue);
        }
        return new Sequence<>(mappedValues);
    }


    ///////////////////////////////////////////////////////
    // Applicative properties
    ///////////////////////////////////////////////////////

    @Override
    public Sequence<T> pure(T value) {
        return new Sequence<>(value);
    }

    @Override
    public <U> Sequence<U> apply(Applicative<Function<? super T, ? extends U>> functionInContext) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    ///////////////////////////////////////////////////////
    // Monad properties
    ///////////////////////////////////////////////////////

    @Override
    public Sequence<T> join() {
        List<T> flattenedValues = new ArrayList<>(this.values.size());
        for (T element : this.values) {
            if (element instanceof Sequence) {
                Sequence<?> wrappedElement = ((Sequence<?>) element);
                List<?> _unsafeWrappedElement = wrappedElement._unsafe();
                if (_unsafeWrappedElement.isEmpty()) {
                    // TODO: Figure out validity and what to do here...
                    throw new IllegalStateException("TODO: \"Wrapped\" 'Sequence' element is empty");
                }
                if (_unsafeWrappedElement.size() > 1) {
                    // TODO: Figure out validity and what to do here...
                    throw new IllegalStateException("TODO: \"Wrapped\" 'Sequence' element contains more than one elements");
                }
                // TODO: Validate with tests
                // TODO: You can break this one, can't you? Which functions (the types of it) can be mapped...?
                element = (T) _unsafeWrappedElement.get(0);
            }
            flattenedValues.add(element);
        }
        return new Sequence<>(flattenedValues);
    }


    ///////////////////////////////////////////////////////
    // java.lang.Object
    ///////////////////////////////////////////////////////

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.values)
            .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Sequence<?> otherSequence = (Sequence<?>) other;

        return new EqualsBuilder()
            .append(this.values, otherSequence.values)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
