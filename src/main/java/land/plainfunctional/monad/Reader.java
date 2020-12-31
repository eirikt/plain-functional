package land.plainfunctional.monad;

import java.util.function.Function;
import java.util.function.Supplier;

import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;
import land.plainfunctional.value.AbstractProtectedValue;

import static java.util.function.Function.identity;
import static land.plainfunctional.util.ReflectionUtils.createDefaultInstance;

/**
 * <p>
 * <i>Functor context:</i>
 * <b>
 * A deferred ("lazy-evaluated") value
 * </b><br>
 * The value is typically <i>read</i> from an external/shared environment, and may be arbitrary (neither <i>total</i> nor <i>deterministic</i>).
 * When evaluated (folded), the value will appear after an arbitrary amount of time.
 * Also, the value may not show up at all&mdash;like
 * {@link Either} and {@link Maybe}, the {@link Reader} context represents <a href="https://en.wikipedia.org/wiki/Partial_function">partiality</a>.
 * </p>
 *
 * <p>
 * Reader monads are functions from an shared environment to a value,
 * making it possible to bind variables to external sources.
 * In this implementation, <code>Reader</code> monads are composable "nullary" functions.
 * The {@link Supplier} which must be provided via the constructor, represents the "nullary" function.
 * Composing <code>Reader</code> functors (via <code>map</code>) is the same as <a href="https://en.wikipedia.org/wiki/Function_composition">function composition</a>.
 * </p>
 *
 * <p>
 * Do notice; When evaluated, {@link Reader} instances will <i>"stop the world" (blocking the thread)</i>,
 * while waiting for the value to be read from the environment.<br>
 * An (attempt on an) analogy might be a dinner recipe that you write yourself.
 * After composing it and listing up the ingredients, perhaps sorted by the most important ingredient&mdash;you
 * go out and fetch them yourself, one by one.<br>
 * So, <b>the {@link Reader} monad represents <i>synchronous</i> execution.</b>
 * (For <i>asynchronous</i> computations, the planned <code>Promise</code> monad must be used.)
 * </p>
 *
 * <p>
 * The <code>Reader</code> monad is also known as <code>Environment</code> and <code>Lazy</code>.
 * </p>
 *
 * @param <T> The type of the deferred value
 * @see <a href="https://bartoszmilewski.com/2015/01/20/functors/">Functors → The Reader Functor</a>
 */
public class Reader<T> extends AbstractProtectedValue<Supplier<T>> implements Monad<T> {

    ///////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Just for having a {@link Reader} instance to reach the member methods, e.g. <code>pure</code>.
     * <b>NB! The given type must have an available empty constructor.</b>
     */
    //@SuppressWarnings("unchecked")
    public static <T> Reader<T> asReader(Class<T> type) {
        return startingWith(createDefaultInstance(type));
    }

    /**
     * Alias for 'of(alreadyAvailableValue)'
     */
    public static <T> Reader<T> startingWith(T value) {
        return of(() -> value);
    }

    /* TODO: To Be removed, I guess
     * Factory method.
     *
     * @param value The (already available) value to be put into this {@link Reader} functor
     * @return A {@link Reader} value
     /
    public static <T> Reader<T> of(T value) {
        return Reader.of(() -> value);
    }
    */

    /**
     * Factory method.
     *
     * @param valueSupplier The supplied value to be put into this {@link Reader} functor
     * @return A {@link Reader} value
     */
    public static <T> Reader<T> of(Supplier<T> valueSupplier) {
        return new Reader<>(valueSupplier);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    protected Reader(Supplier<T> supplier) {
        super(supplier);
        Arguments.requireNotNull(supplier, "'Reader' cannot handle 'null' suppliers");
    }


    ///////////////////////////////////////////////////////////////////////////
    // State
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public <V> Reader<V> map(Function<? super T, ? extends V> function) {
        return map(function, null);
    }

    /**
     * The functor function.
     *
     * "Plain functionally" (Haskell-style), the functor function is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;map :: Functor f =&gt; (a -&gt; b) -&gt; f a -&gt; f b
     * </code>
     * </p>
     *
     * <p>
     * NB! In Haskell this function is named <code>fmap</code>.
     * </p>
     *
     * <i>This means</i>: A function <code>a -&gt; b</code> is applied to a functor <code>f</code> of type <code>a</code>,
     * returning a container structure of the same type <code>f</code>containing elements of type <code>b</code>.<br>
     *
     * <i>This reads</i>: The map function "is a member of" Functor type <code>f</code> "having the type constraint" of;
     * For an "<code>f</code> of <code>a</code>'s",
     * and a function taking an <code>a</code> returning a <code>b</code>,
     * it must return an "<code>f</code> of <code>b</code>'s"&mdash;
     * and all this is the definition of the "map" function.
     *
     * @param function     The map function
     * @param defaultValue If present (not <code>null</code>),
     *                     this parameter will be returned in case of this reader returns a bottom value.
     *                     (This is accomplished by transforming this reader to a maybe.)
     * @param <V>          The type of the codomain
     * @return the new/other functor
     */
    public <V> Reader<V> map(Function<? super T, ? extends V> function, T defaultValue) {
        Arguments.requireNotNull(function, "'function' argument cannot be null");
        return new Reader<>(
            () -> defaultValue == null
                ? function.apply(this.value.get())
                : function.apply(toMaybe().getOrDefault(defaultValue))
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Reader<T> pure(T value) {
        return Reader.of(() -> value);
    }

    @Override
    public <V> Reader<V> apply(Applicative<Function<? super T, ? extends V>> functionInContext) {
        Arguments.requireNotNull(functionInContext, "'functionInContext' argument cannot be null");

        // TODO: May throw 'ClassCastException'! (See inherited JavaDoc)
        // => Any chance of mitigating this - with Java's type system? (with the lack of higher-kinded types)
        Reader<Function<? super T, ? extends V>> readerFunction =
            (Reader<Function<? super T, ? extends V>>) functionInContext;

        return new Reader<>(
            () -> readerFunction.tryGet().apply(this.value.get())
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Monad
    ///////////////////////////////////////////////////////////////////////////

    @Override
    //TODO: @SuppressWarnings("unchecked")
    public Reader<T> join() {
        return new Reader<>(
            () -> {
                T value = this.value.get();

                if (value instanceof Reader<?>) {
                    // @SuppressWarnings: 'this.value' is an instance of 'Reader' and must be of type 'T'
                    return ((Reader<T>) value).value.get();
                }
                return value;
            }
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Filter
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Append
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Transformation
    // (Folding this 'Reader' functor wrapped in another functor for handling partial values)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Handling of bottom value evaluations using a {@link Maybe} functor.
     * It delegates to <code>toEither</code>.
     *
     * @return the evaluated (deferred) value in a {@link Maybe} context
     */
    public Maybe<T> toMaybe() {
        // NB! Blocks current thread!
        return toMaybeWithRetry(0);
    }

    /**
     * Handling of bottom value evaluations using a {@link Maybe} functor.
     * It delegates to <code>toEither</code>.
     *
     * @param numberOfRetries If reading fails, this function will retry <code>numberOfRetries</code> times before returning 'Nothing'
     * @return the evaluated (deferred) value in a {@link Maybe} context
     */
    public Maybe<T> toMaybeWithRetry(int numberOfRetries) {
        // NB! Blocks current thread!
        Either<String, T> either = toEitherWithRetry(numberOfRetries);

        // Handles exceptions
        if (either.isLeft()) {
            return Maybe.nothing();
        }
        // Handles 'null'
        return Maybe.of(either.tryGet());
    }

    /**
     * Handling of bottom value evaluations using an {@link Either} functor,
     * and the Java built-in <code>try</code> "monad".
     * This method is an application of <code>fold</code>.
     *
     * @return the evaluated (deferred) value in an {@link Either} context,
     * with a failure message as the 'Either.Left' if the value is for some reason is not available
     */
    public Either<String, T> toEither() {
        // NB! Blocks current thread!
        return toEitherWithRetry(0);
    }

    /**
     * Handling of bottom value evaluations using an {@link Either} functor,
     * and the Java built-in <code>try</code> "monad".
     * This method is an application of <code>fold</code>.
     *
     * @param numberOfRetries If reading fails, this function will retry 'numberOfRetries' times before returning the 'Left' variant
     * @return the evaluated (deferred) value in an {@link Either} context,
     * with a failure message as the 'Either.Left' if the value is for some reason is not available
     */
    public Either<String, T> toEitherWithRetry(int numberOfRetries) {
        // NB! Blocks current thread!
        return foldWithRetry(
            numberOfRetries,
            (exception) -> {
                System.err.printf(
                    "Reader evaluation failed, returning 'Either.Left'/'Maybe.Nothing', reason:%n%s%n",
                    exception.getMessage()
                );
                return Either.left(exception.getMessage());
            },
            Either::right
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fold
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Evaluate this {@link Reader} functor.
     * </p>
     *
     * <p>
     * This method is a very simple (and somewhat reckless and unforgiving) application of <code>fold</code>.
     * </p>
     *
     * @return this functor's value in case of successful evaluation/computation, otherwise return the bottom value representation
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T tryGet() {
        // NB! Blocks current thread!
        return fold(
            (exception) -> { throw new RuntimeException(exception); },
            identity()
        );
    }

    /**
     * <p>
     * To <i>fold</i> a value means creating a new representation of it.
     * For {@link Reader} instances this will force execution/evaluation.
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
     * <p>...</p>
     *
     * <p>
     * As {@link Reader} is a single-value functor, there is no need for a <i>binary</i> function;
     * It is replaced by an unary function, which transforms the single read value.
     * Also, the need for an initial value is redundant;
     * It is replaced by a special unary function in case this {@link Reader} return a bottom value, here an exception, which is provided as the function parameter.
     * </p>
     *
     * @param onRead Function (unary) (the "catamorphism") to be applied to the read value
     * @param <V>    The type of the folded/returning value
     * @return the folded value
     */
    public <V> V tryFold(Function<? super T, ? extends V> onRead) {
        // NB! Blocks current thread!
        return fold(
            (exception) -> { throw new RuntimeException(exception); },
            onRead
        );
    }

    /**
     * <p>
     * To <i>fold</i> a value means creating a new representation of it.
     * For {@link Reader} instances this will force execution/evaluation.
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
     * <p>...</p>
     *
     * <p>
     * As {@link Reader} is a single-value functor, there is no need for a <i>binary</i> function;
     * It is replaced by an unary function, which transforms the single read value.
     * Also, the need for an initial value is redundant;
     * It is replaced by a special unary function in case this {@link Reader} return a bottom value, here an exception, which is provided as the function parameter.
     * </p>
     *
     * @param onBottom Function (unary) with the bottom value (exception) provided as the function parameter
     * @param onRead   Function (unary) (the "catamorphism") to be applied to the read value
     * @param <V>      The type of the folded/returning value
     * @return the folded value
     */
    public <V> V fold(
        Function<Exception, ? extends V> onBottom,
        Function<? super T, ? extends V> onRead
    ) {
        return foldWithRetry(0, onBottom, onRead);
    }

    /**
     * <p>
     * To <i>fold</i> a value means creating a new representation of it.
     * For {@link Reader} instances this will force execution/evaluation.
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
     * <p>...</p>
     *
     * <p>
     * As {@link Reader} is a single-value functor, there is no need for a <i>binary</i> function;
     * It is replaced by an unary function, which transforms the single read value.
     * Also, the need for an initial value is redundant;
     * It is replaced by a special unary function in case this {@link Reader} return a bottom value, here an exception, which is provided as the function parameter.
     * </p>
     *
     * @param numberOfRetries If folding fails, this function will retry folding 'numberOfRetries' times before invoking the 'onBottom' callback
     * @param onBottom        Function (unary) with the bottom value (exception) provided as the function parameter
     * @param onRead          Function (unary) (the "catamorphism") to be applied to the read value
     * @param <V>             The type of the folded/returning value
     * @return the folded value
     */
    public <V> V foldWithRetry(
        int numberOfRetries,
        Function<Exception, ? extends V> onBottom,
        Function<? super T, ? extends V> onRead
    ) {
        return foldWithRetry(numberOfRetries, numberOfRetries, onBottom, onRead);
    }

    /**
     * Folding with recursion-based retry.
     *
     * @param retryIndex           The number of retries left for this function before invoking the 'onBottom' callback
     * @param totalNumberOfRetries The total number of retries this function will try before invoking the 'onBottom' callback
     * @param onBottom             Function (unary) with the bottom value (exception) provided as the function parameter
     * @param onRead               Function (unary) (the "catamorphism") to be applied to the read value
     * @param <V>                  The type of the folded/returning value
     * @return the folded value
     */
    <V> V foldWithRetry(
        int retryIndex,
        int totalNumberOfRetries,
        Function<Exception, ? extends V> onBottom,
        Function<? super T, ? extends V> onRead
    ) {
        try {
            // NB! Blocks current thread!
            return onRead.apply(this.value.get());

        } catch (Exception exception) {
            if (retryIndex > 0) {
                System.out.printf(
                    "'Reader.fold' evaluation failed, retrying... (%d/%d)%n",
                    retryIndex - 1, totalNumberOfRetries
                );
                return foldWithRetry(retryIndex - 1, totalNumberOfRetries, onBottom, onRead);
            }
            System.err.printf(
                "'Reader.fold' evaluation failed, executing 'onBottom' 'Supplier' (\"nullary\" function): reason:%n%s%n",
                exception.getMessage()
            );
            return onBottom.apply(exception);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // java.lang.Object
    //
    // NB! Must step back to Object.hashCode and Object.equals()
    // for avoiding throwing away 'Reader' instances e.g. in 'Set' semantics
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Reader<?> otherReader = (Reader<?>) other;

        return this.hashCode() == otherReader.hashCode();
    }
}
