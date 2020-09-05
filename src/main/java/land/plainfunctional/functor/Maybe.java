package land.plainfunctional.functor;

import java.util.function.Function;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import land.plainfunctional.typeclass.Functor;

/**
 * <p>
 * <i>Functor context:</i> <b>The value/values may or may not be present</b>
 * </p>
 * <p>
 * Haskell type definition:<br><br>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;data Maybe a = Nothing | Just a</code>
 * </p>
 * <p>
 * Here {@link Maybe} is the <i>type constructor</i>,
 * while <code>Nothing</code> and <code>Just</code> are <i>data constructors</i> (also known as <i>value constructors</i>).
 * We may regard <code>Nothing</code> as a constant as it is a <i>nullary</i> data constructor.
 * <code>Just</code> on the other hand, has a parametric type variable <code>a</code>,
 * making the {@link Maybe} functor a <code>polymorphic</code> type.
 * Instances of {@link Maybe} will either be a <code>Nothing</code> or a <code>Just</code> value,
 * so {@link Maybe} is an <i>algebraic data type (ADT)</i>.
 * </p>
 * <p>
 * As <code>Nothing</code> is a constant, it is a singleton i this library.
 * </p>
 * <p>
 * The Maybe functor is also known as <code>Option</code>, and <code>Optional</code>.
 * </p>
 *
 * @param <T> The type of the value which is present or not.
 *            It is the same as the parametric type 'a' in the Haskell definition.
 * @see <a href="https://en.wikipedia.org/wiki/Option_type">Option type (Wikipedia)</a>
 * @see <a href="https://wiki.haskell.org/Constructor">Haskell constructors</a>
 */
public class Maybe<T> implements Functor<T> {

    ///////////////////////////////////////////////////////////////////////////
    // Constants and unit values
    ///////////////////////////////////////////////////////////////////////////

    private static final Maybe<? extends Object> NOTHING = new Maybe<>(null);


    ///////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////

    public static <T> Maybe<T> of(T value) {
        return value == null ? nothing() : just(value);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked") // 'NOTHING' is covariant
    public static <T> Maybe<T> nothing() {
        return (Maybe<T>) NOTHING;
    }

    public static <T> Maybe<T> just(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create a 'Maybe.Just' from a 'null' value");
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
        return this.value == null;
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
        Maybe<? extends Object> otherMaybe = (Maybe<? extends Object>) other;

        return new EqualsBuilder()
            .append(this.value, otherMaybe.value)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
