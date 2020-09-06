package land.plainfunctional.functor;

import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import land.plainfunctional.typeclass.Applicative;
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
 * As <code>Nothing</code> is a constant, it is implemented as a singleton.
 * </p>
 * <p>
 * The Maybe functor is also known as <code>Option</code>, and <code>Optional</code>.
 * </p>
 *
 * @param <T> The type of the value which is present or not.
 *            It is the same as the parametric type 'a' in the Haskell definition.
 * @see <a href="https://en.wikipedia.org/wiki/Option_type">Option type (Wikipedia)</a>
 * @see <a href="https://wiki.haskell.org/Constructor">Haskell constructors</a>
 * @see <a href="https://en.wikipedia.org/wiki/Algebraic_data_type">Algebraic data types</a>
 */
public class Maybe<T> implements Applicative<T> {

    ///////////////////////////////////////////////////////////////////////////
    // Constants and unit values
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Singleton {@link Maybe} 'Nothing' value, acting as a unit of {@link Maybe}.
     */
    private static final Maybe<? extends Object> NOTHING = new Maybe<>(null);


    ///////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Just for having a {@link Maybe} instance to reach the member methods, e.g. <code>pure</code>.
     */
    public static <T> Maybe<T> withMaybe() {
        return withMaybe(null);
    }

    /**
     * Just for having a typed {@link Maybe} instance to reach the member methods, e.g. <code>pure</code>.
     */
    public static <T> Maybe<T> withMaybe(Class<T> clazz) {
        return nothing();
    }

    /**
     * Trusted factory method.
     * (As no {@link Maybe} yet exists, we are free to use either of the stat constructors.)
     *
     * @param value The value to be put into this {@link Maybe} functor
     * @return A 'Nothing' if the given value is 'null', otherwise 'Just'
     */
    public static <T> Maybe<T> of(T value) {
        return value == null ? nothing() : just(value);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <code>Nothing</code> data constructor.
     */
    @SuppressWarnings("unchecked") // 'NOTHING' is covariant
    public static <T> Maybe<T> nothing() {
        return (Maybe<T>) NOTHING;
    }

    /**
     * <code>Just</code> data constructor.
     */
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

    /**
     * @return 'true' if and only if the 'nothing' data constructor is used, otherwise 'true'
     */
    public boolean isNothing() {
        return this.value == null;
    }

    /**
     * <p>
     * Retrieve this {@link Maybe} functor's value if this is a 'Just',
     * otherwise the given default value will be returned.
     * </p>
     * <p>
     * This is a simple application of <code>fold</code>.
     * </p>
     *
     * @param defaultValue The default value in case this is 'Nothing'
     * @return this functor's value in case this is a 'Just'
     */
    public T getOrDefault(T defaultValue) {
        return fold(
            () -> defaultValue,
            // The 'ignored' bound parameter should obviously have been named '_' ("unit value"), but the Java compiler won't allow that
            (ignored) -> this.value
        );
    }

    /**
     * <p>
     * Retrieve this {@link Maybe} functor's value if this is a 'Just',
     * otherwise return (the bottom value) <code>null</code>.
     * </p>
     * <p>
     * This is an even simpler (and somewhat reckless) application of <code>fold</code>.
     * </p>
     *
     * @return this functor's value in case this is a 'Just'
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T getOrNull() {
        return fold(
            () -> null,
            // The 'ignored' bound parameter should obviously have been named '_' ("unit value"), but the Java compiler won't allow that
            (ignored) -> this.value
        );
    }

    /**
     * <p>
     * To <i>fold</i> a data structure means creating a new representation of this value.
     * This will most often result in leaving the {@link Maybe} functor behind.
     * </p>
     * <p>
     * In abstract algebra, this is known as a "catamorphism".
     * A catamorphism deconstructs (destroys) a data structure,
     * in contrast to the homomorphic preservation of data structures.
     * </p>
     *
     * @param onNothing Supplier ("nullary" function/deferred constant) of the default value in case of 'Nothing'
     * @param onJust    Function (unary) (the "catamorphism") to be applied to this functor's value in case it is a 'Just'
     * @param <U>       The type of the folded/returning value
     * @return the folded value
     */
    public <U> U fold(Supplier<U> onNothing, Function<? super T, ? extends U> onJust) {
        return isNothing()
            ? onNothing.get()
            : onJust.apply(this.value);
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
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Trusted factory method.
     * (As no {@link Maybe} yet exists, we are free to use either of the data constructors.)
     *
     * {@inheritDoc}
     */
    @Override
    public Maybe<T> pure(T value) {
        return of(value);
    }

    @Override
    public <U> Maybe<U> apply(Functor<Function<T, U>> appliedFunctionInContext) {
        return just(
            ((Maybe<Function<T, U>>) appliedFunctionInContext)
                .getOrNull()
                .apply(this.value)
        );
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
