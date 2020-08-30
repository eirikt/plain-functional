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

    public static <T> Maybe<T> just(T value) {
        //throw new UnsupportedOperationException("Not yet implemented");
        return new Maybe<>(value);
    }

    public static <T> Maybe<T> nothing() {
        //throw new UnsupportedOperationException("Not yet implemented");
        return new Maybe<>(null);
    }

    private final T value;

    private Maybe(T value) {
        this.value = value;
    }

    public boolean isNothing() {
        //throw new UnsupportedOperationException("Not yet implemented");
        return value == null;
    }

    //@Override
    //public <U> Maybe<U> map(java.util.function.Function<? super T, ? extends U> function) {
    //    throw new UnsupportedOperationException("Not yet implemented");
    //}

    /*
    // WORKS!
    @Override
    public <U> Maybe<U> map(Function<? super T, ? extends U> function) {
        if (this.isNothing()) {
            // Partial function, returning nothing/bottom
            function = x -> null;
        }
        U result = function.apply(this.value);
        return (result == null) ? nothing() : just(result);
    }
    */
    /*
    // WORKS!
    @Override
    public <U> Maybe<U> map(Function<? super T, ? extends U> function) {
        if (this.isNothing()) {
            // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
            return (Maybe<U>) this;
        }
        return just(function.apply(this.value));
    }
    */
    /*
    // WORKS!
    @Override
    public <U> Maybe<U> map(Function<? super T, ? extends U> function) {
        if (this.isNothing()) {
            // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
            return (Maybe<U>) NOTHING;
        }
        return just(function.apply(this.value));
    }
    */
    @Override
    public <U> Maybe<U> map(Function<? super T, ? extends U> function) {
        if (this.isNothing()) {
            return nothing();
        }
        return just(function.apply(this.value));
    }

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
