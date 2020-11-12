package land.plainfunctional.monad;

import java.util.function.Function;
import java.util.function.Supplier;

import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;
import land.plainfunctional.value.AbstractProtectedValue;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * <p>
 * <i>Functor context:</i>
 * <b>
 * The value may or may not be present.
 * </b>
 * </p>
 *
 * <p>
 * Haskell type definition:<br><br>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;data Maybe a = Nothing | Just a</code>
 * </p>
 *
 * <p>
 * Here {@link Maybe} is the <i>type constructor</i>,
 * while <code>Nothing</code> and <code>Just</code> are <i>data constructors</i> (also known as <i>value constructors</i>).
 * We may regard <code>Nothing</code> as a constant as it is a <i>"nullary"</i> data constructor.
 * <code>Just</code> on the other hand, has a parametric type variable <code>a</code>,
 * making the {@link Maybe} monad a <code>polymorphic</code> type.
 * Instances of {@link Maybe} will either be a <code>Nothing</code> or a <code>Just</code> value,
 * so {@link Maybe} is a <i>sum type</i> (also known as <i>tagged union</i>, <i>disjoint union</i>, <i>variant</i>, <i>coproduct</i>).
 * </p>
 *
 * <p>
 * The semantics of the "nothing" type value is that it will ignore any function parameters (morphisms) and just pass the current state along.
 * The "nothing" type will always represent a terminal value, there is no escape from it.
 * </p>
 *
 * <p>
 * {@link Maybe} may be seen as a specialization of the {@link Either} monad,
 * and is implemented accordingly in this library.
 * ({@link Maybe} "inherits" (via subclass polymorphism) an {@link Either} instance.)
 * </p>
 *
 * <p>
 * As <code>Nothing</code> is a constant, it is implemented as a singleton in this library.
 * </p>
 *
 * <p>
 * The Maybe monad is also known as <code>Option</code>, and <code>Optional</code>.
 * </p>
 *
 * @param <T> The type of the value which is present or not.
 *            It is the same as the parametric type 'a' in the Haskell definition.
 * @see <a href="https://en.wikipedia.org/wiki/Option_type">Option type (Wikipedia)</a>
 * @see <a href="https://wiki.haskell.org/Constructor">Haskell constructors</a>
 * @see <a href="https://en.wikipedia.org/wiki/Tagged_union">Sum types</a>
 */
public class Maybe<T> extends AbstractProtectedValue<Either<?, T>> implements Monad<T> {

    ///////////////////////////////////////////////////////////////////////////
    // Constants and unit values
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Singleton {@link Maybe} 'Nothing' value, acting as unit of {@link Maybe} Nothing.
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
     * Just for having a (typed) {@link Maybe} instance to reach the member methods, e.g. <code>pure</code>.
     */
    public static <T> Maybe<T> withMaybe(Class<T> type) {
        return nothing();
    }

    /**
     * Trusted factory method.
     * (As no {@link Maybe} yet exists, we are free to use either of the data constructors.)
     *
     * @param value The value to be put into this {@link Maybe} functor
     * @return A 'Nothing' if the given value is 'null', otherwise 'Just'
     */
    public static <T> Maybe<T> of(T value) {
        return value == null
            ? nothing()
            : just(value);
    }

    public static Maybe<String> ofNonBlankString(String stringValue) {
        return isBlank(stringValue)
            ? nothing()
            : just(stringValue);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <code>Nothing</code> data constructor.
     */
    @SuppressWarnings("unchecked") // 'NOTHING' is covariant to all objects
    public static <T> Maybe<T> nothing() {
        return (Maybe<T>) NOTHING;
    }

    /**
     * Typed <code>Nothing</code> data constructor.
     */
    @SuppressWarnings("unchecked") // 'NOTHING' is covariant to all objects
    public static <T> Maybe<T> nothing(Class<T> type) {
        return (Maybe<T>) NOTHING;
    }

    /**
     * <code>Just</code> data constructor.
     */
    public static <T> Maybe<T> just(T value) {
        // TODO: Is this too strict? It deviates from Vavr's way of doing it, which allows 'null' for its 'Option.some' data constructor
        // => So far, it works as a fail-fast policy...
        Arguments.requireNotNull(value, "Cannot create a 'Maybe.Just' from a 'null'/non-existing (/\"bottom\"/) value");

        return new Maybe<>(value);
    }


    ///////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////

    protected Maybe(T value) {
        super(value == null
            ? Either.left(null)
            : Either.right(value)
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Predicate used for pattern matching.
     *
     * @return 'true' if and only if the 'nothing' data constructor is used, otherwise 'true'
     */
    public boolean isNothing() {
        return this.value.isLeft();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public <U> Maybe<U> map(Function<? super T, ? extends U> function) {
        Arguments.requireNotNull(function, "'function' argument cannot be null");
        //return isNothing()
        //    ? nothing()
        //    : just(function.apply(this.value.tryGet()));

        // It is also possible to implement homomorphism using a catamorphism.
        // Because here, the "catamorphism" is accidentally equal to a regular homomorphism...
        return fold(
            Maybe::nothing,
            (ignored) -> just(function.apply(this.value.tryGet()))
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     *
     * As no {@link Maybe} context yet exists, we are free to use either of the data constructors.
     */
    @Override
    public Maybe<T> pure(T value) {
        return of(value);
    }

    @Override
    public <V> Maybe<V> apply(Applicative<Function<? super T, ? extends V>> functionInContext) {
        Arguments.requireNotNull(functionInContext, "'functionInContext' argument cannot be null");

        // TODO: May throw 'ClassCastException'! (See inherited JavaDoc) Any chance of mitigating this - with Java's type system? (Lacking higher kinded types)
        Maybe<Function<? super T, ? extends V>> maybeFunction =
            (Maybe<Function<? super T, ? extends V>>) functionInContext;

        return maybeFunction.isNothing()
            ? (Maybe<V>) functionInContext
            : just(maybeFunction.tryGet().apply(this.value.tryGet())
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Monad
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Maybe<T> join() {
        if (this.value instanceof Either.Right<?, ?>) {
            if (this.value.tryGet() instanceof Maybe<?>) {
                // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
                // TODO: Well, also argue that this must be the case...
                return (Maybe<T>) this.value.tryGet();
            }
            // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
            // TODO: Well, also argue that this must be the case...
            return (Maybe<T>) this.value;
        }
        return nothing();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fold
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return a simple string representation of this {@link Maybe}
     */
    public String toStringMaybe() {
        return fold(
            () -> "Nothing",
            // The 'ignored' bound parameter should obviously have been named '_' ("unit value"), but the Java compiler won't allow that
            (ignored) -> format("Just(%s)", this.value.tryGet())
        );
    }

    /**
     * <p>
     * Retrieve this {@link Maybe} functor's value if this is a 'Just',
     * otherwise throw a {@link NullPointerException} (a bottom value).
     * </p>
     *
     * <p>
     * This is a very simple (and somewhat reckless and unforgiving) application of <code>fold</code>.
     * </p>
     *
     * @return this functor's value in case this is a 'Just'
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T tryGet() {
        return fold(
            () -> { throw new NullPointerException(); },
            // The 'ignored' bound parameter should obviously have been named '_' ("unit value"), but the Java compiler won't allow that
            (ignored) -> this.value.tryGet()
        );
    }

    /**
     * <p>
     * Retrieve this {@link Maybe} functor's value if this is a 'Just',
     * otherwise return (the bottom value) <code>null</code>.
     * </p>
     *
     * <p>
     * This is a very simple (and somewhat reckless) application of <code>fold</code>.
     * </p>
     *
     * @return this functor's value in case this is a 'Just'
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T getOrNull() {
        return getOrDefault(null);
    }

    /**
     * <p>
     * Retrieve this {@link Maybe} functor's value if this is a 'Just',
     * otherwise the given default value will be returned.
     * </p>
     *
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
            (ignored) -> this.value.tryGet()
        );
    }

    /**
     * <p>
     * To <i>fold</i> a value means creating a new representation of it.
     * </p>
     *
     * <p>
     * In abstract algebra, this is known as a <i>catamorphism</i>.
     * A catamorphism deconstructs (destroys) data structures
     * in contrast to the <i>homomorphic</i> <i>preservation</i> of data structures,
     * and <i>isomorphisms</i> where one can <i>resurrect</i> the originating data structure.
     * </p>
     *
     * "Plain functionally" (Haskell-style), "foldleft" (<code>foldl</code>) is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;foldl :: (b -&gt; a -&gt; b) -&gt; b -&gt; f a -&gt; b
     * </code>
     * </p>
     *
     * <p>
     * <i>This means</i>: A binary function <code>b -&gt; a -&gt; b</code>,
     * together with an initial value of type <code>b</code>,
     * is applied to a functor <code>f</code> of type <code>a</code>,
     * returning a new value of type<code>b</code>.
     * </p>
     *
     * <p>
     * As {@link Maybe} is a single-value functor, there is no need for a <i>binary</i> function;
     * It is replaced by an unary function, which transforms the single <code>Just</code> value of this {@link Maybe}.
     * Also, the need for an initial value is redundant;
     * It is replaced by a special "nullary" function in case this {@link Maybe} is a <code>Nothing</code>.
     * </p>
     *
     * @param onNothing Supplier ("nullary" function/deferred constant) of the default value in case it is 'Nothing'
     * @param onJust    Function (unary) (the "catamorphism") to be applied to this functor's value in case it is a 'Just'
     * @param <U>       The type of the folded/returning value
     * @return the folded value
     */
    public <U> U fold(Supplier<U> onNothing, Function<? super T, ? extends U> onJust) {
        return this.value.fold(onNothing, onJust);
    }
}
