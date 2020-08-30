package land.plainfunctional.functor;

import java.util.function.Function;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import land.plainfunctional.typeclass.Functor;

/**
 * <p>
 * Maybe a value/values.
 * </p>
 * <p>
 * Haskell definition:<br><br>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;data Maybe a = Nothing | Just a</code>
 * </p>
 * <p>
 * The Maybe functor is also known as <code>Option</code>, and <code>Optional</code>.
 * </p>
 *
 * @param <T> The type of the value which is present or not
 * @see <a href="https://en.wikipedia.org/wiki/Option_type">Option type (Wikipedia)</a>
 */
public class Maybe<T> implements Functor<T> {

    ///////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////

    public static <T> Maybe<T> of(T value) {
        return value == null ? nothing() : just(value);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////

    public static <T> Maybe<T> nothing() {
        return new Maybe<>(null);
    }

    public static <T> Maybe<T> just(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create a 'Maybe.Just' from a null value");
        }
        return new Maybe<>(value);
    }


    ///////////////////////////////////////////////////////////////////////////
    // State
    ///////////////////////////////////////////////////////////////////////////

    private final T value;


    ///////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////

    private Maybe(T value) {
        this.value = value;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Maybe methods
    ///////////////////////////////////////////////////////////////////////////

    public boolean isNothing() {
        return value == null;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public <U> Maybe<U> map(Function<? super T, ? extends U> function) {
        if (this.isNothing()) {
            return nothing();
        }
        return just(function.apply(this.value));
    }


    ///////////////////////////////////////////////////////////////////////////
    // java.lang.Object
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.value)
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
        Maybe<?> otherMaybe = (Maybe<?>) other;

        return new EqualsBuilder()
            .append(this.value, otherMaybe.value)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
