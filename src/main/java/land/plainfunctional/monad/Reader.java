package land.plainfunctional.monad;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.ToStringBuilder;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.FunctorExtras;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;

import static java.util.function.Function.identity;
import static land.plainfunctional.monad.Maybe.just;
import static land.plainfunctional.monad.Maybe.nothing;
import static land.plainfunctional.util.ReflectionUtils.createDefaultInstance;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * <i>Functor context:</i>
 * <b>
 * A deferred ("lazy-evaluated") value ⇒ a function.
 * </b><br>
 * The value is typically <i>read</i> from an external/shared environment, and may be arbitrary (neither <i>total</i> nor <i>deterministic</i>).
 * When evaluated (folded), the value will appear after an arbitrary amount of time.
 * Also, the value may not show up at all&mdash;like
 * {@link Either} and {@link Maybe}, the {@link Reader} context represents <a href="https://en.wikipedia.org/wiki/Partial_function">partiality</a>.
 *
 * <p>
 * Reader monads are functions from a shared environment to a value,
 * making it possible to bind variables to external sources, for composition.
 * In this implementation, a {@link Supplier} parameter must be provided via the constructor,
 * representing the "nullary" starting function.
 * Composing {@link Reader} functors (via <code>map</code>) is the same as <a href="https://en.wikipedia.org/wiki/Function_composition">function composition</a>.
 * </p>
 *
 * <p>
 * Do notice; When being evaluated, {@link Reader} instances will <i>"stop the world" (blocking the current thread)</i>,
 * while waiting for the value to be read from the environment.<br>
 * An (attempt on an) analogy might be a dinner recipe that you write yourself.
 * After composing it and listing up the ingredients, perhaps sorted by the most important ingredient&mdash;you
 * go out and fetch them yourself, one by one.<br>
 * So, <b>the {@link Reader} monad represents <i>synchronous</i> execution.</b>
 * (For <i>asynchronous</i> computations, use <code>Promise</code>s.)
 * </p>
 *
 * <p>
 * The <code>Reader</code> monad is also known as <code>Environment</code> and <code>Lazy</code>.
 * </p>
 *
 * <p>
 * <i>Observation</i>:<br>
 * With the current implementation, you can use a {@link Promise} to accomplish everything a {@link Reader} can.
 * If the semantics of {@link Reader} are not extended, the {@link Reader} class may be deprecated...
 * The fever core monads, the better! Composition of well-defined monads is the goal.
 * </p>
 *
 * @param <T> The type of the deferred value
 * @see <a href="https://bartoszmilewski.com/2015/01/20/functors/">Functors → The Reader Functor</a>
 */
public class Reader<T> implements Monad<T>, FunctorExtras<T>, Future<T> {

    // Switch for verbose logging to System.out/System.err
    private static final boolean DO_VERBOSE_LOGGING = true;


    ///////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Just for having a {@link Reader} instance to reach the member methods, e.g. <code>pure</code>.
     * <b>NB! The given type must have an available empty constructor.</b>
     */
    public static <T> Reader<T> asReader(Class<T> type) {
        return of(createDefaultInstance(type));
    }

    /**
     * Alias for <code>of(value)</code>.
     */
    public static <T> Reader<T> startingWith(T value) {
        return of(value);
    }

    /**
     * Factory method for immediately available values.
     * <i>NB! May block current thread due to evaluation of parameter value.</i>
     *
     * @param value The deferred value to be put into this {@link Reader} functor
     * @param <T>   The type of the deferred value
     * @return A {@link Reader} value
     */
    public static <T> Reader<T> of(T value) {
        return new Reader<>(() -> value);
    }

    /**
     * Factory method.
     *
     * @param valueSupplier The deferred value to be put into this {@link Reader} functor
     * @param <T>           The type of the deferred value
     * @return A {@link Reader} value
     */
    public static <T> Reader<T> of(Supplier<T> valueSupplier) {
        return new Reader<>(valueSupplier);
    }

    /**
     * Factory method.
     *
     * <p>
     * Parallel fetching of values, and then folding them using the provided monoid.
     * </p>
     *
     * @param supplierSequence The enumerated deferred values to be folded into this {@link Reader} functor
     * @param monoid           The monoid to be used for folding the deferred values
     * @param <T>              The type of the deferred values
     * @return A {@link Reader} {@link Maybe} value
     */
    public static <T> Reader<Maybe<T>> of(
        Sequence<Supplier<? extends T>> supplierSequence,
        FreeMonoid<T> monoid
    ) {
        return monoid
            .toReaderIdentity()
            .map(
                supplierSequence.map(
                    (supplier) -> (ignored) -> supplier.get()
                )
                , monoid
            );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////
    // State & Constructors
    ///////////////////////////////////////////////////////

    protected Promise<T> promise;

    protected Reader(Supplier<T> supplier) {
        Arguments.requireNotNull(supplier, "'Reader' cannot handle 'null' suppliers");
        this.promise = Promise.of(supplier);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The <code>isDone</code> method (specified by the implemented {@link Reader} interface) delegates to this method.
     *
     * @return <code>true</code> if this {@link Reader} value is resolved
     */
    public boolean isResolved() {
        if (isCancelled()) {
            throw new CancellationException();
        }
        return this.promise.isDone();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Future
    ///////////////////////////////////////////////////////////////////////////

    // Here: Implemented as an alias for 'isResolved</code>'
    @Override
    public boolean isDone() {
        return isResolved();
    }

    @Override
    public boolean isCancelled() {
        return this.promise.isCancelled();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use one of the folding methods instead&mdash;for explicit handling of bottom values.
     */
    @Override
    @Deprecated
    public T get() throws InterruptedException, ExecutionException {
        return this.promise.get();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use one of the folding methods instead&mdash;for explicit handling of bottom values.
     */
    @Override
    @Deprecated
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.promise.get(timeout, unit);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.promise.cancel(mayInterruptIfRunning);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public <V> Reader<V> map(Function<? super T, ? extends V> function) {
        return map(function, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>...</p>
     *
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
     * <p>...</p>
     * TODO: ...
     *
     * @param function     The map function
     * @param defaultValue If present (not <code>null</code>),
     *                     this parameter will be used as the mapping value if this {@link Reader} returns a bottom value.
     *                     (This is accomplished by transforming this {@link Reader} to a {@link Maybe}.)
     * @param <V>          The type of the codomain
     * @return the new/other functor
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom values (Wikipedia)</a>
     */
    public <V> Reader<V> map(
        Function<? super T, ? extends V> function,
        T defaultValue
    ) {
        Arguments.requireNotNull(function, "'function' argument cannot be null");

        return new Reader<>(
            (defaultValue == null)
                ? () -> function.apply(this.promise.tryGet())
                : () -> function.apply(toMaybe().getOrDefault(defaultValue))
        );
    }

    @Override
    public <V> Reader<Maybe<V>> map(
        Sequence<Function<? super T, ? extends V>> functionSequence,
        FreeMonoid<V> monoid
    ) {
        return new Reader<>(() -> mapFold(functionSequence, monoid));
    }

    /**
     * {@inheritDoc}
     *
     * <p>...</p>
     *
     * NB! Synchronous implementation;
     * Will block the current thread and fetch the value of this {@link Reader}.
     *
     * @param effect The (side) effect
     * @return this (unmodified) same {@link Reader} instance
     */
    @Override
    public Reader<T> effect(Consumer<? super T> effect) {
        // TODO: How to implement this? Blocking vs. non-blocking/best-effort

        //if (this.promise.isResolved()) {
        //    effect.accept(this.promise.resolvedValue);
        //} else {
        //    System.err.println("'Reader::effect' FAILED, value is not available");
        //}

        // NB! Blocks current thread!
        effect.accept(this.promise.tryGet());

        return this;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Reader<T> pure(T value) {
        return new Reader<>(() -> value);
    }

    @Override
    public <V> Reader<V> apply(Applicative<Function<? super T, ? extends V>> functionInContext) {
        Arguments.requireNotNull(functionInContext, "'functionInContext' argument cannot be null");

        // TODO: May throw 'ClassCastException'! (See inherited JavaDoc)
        // => Any chance of mitigating this - with Java's type system? (with the lack of higher-kinded types)
        Reader<Function<? super T, ? extends V>> readerFunction =
            (Reader<Function<? super T, ? extends V>>) functionInContext;

        return new Reader<>(
            () -> readerFunction.tryGet().apply(this.promise.tryGet())
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
                // NB! Blocks current thread!
                T value = this.promise.tryGet();

                if (value instanceof Reader<?>) {
                    // @SuppressWarnings: 'this.value' is an instance of 'Reader' and must be of type 'T'
                    return ((Reader<T>) value).promise.tryGet();
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

        // Partiality: Handles exceptions
        if (either.isLeft()) {
            return nothing();
        }
        // Partiality: Handles null values
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
            (maybeException) -> {
                String failureReason = (maybeException.isNothing())
                    ? "null as folded value"
                    : getRootCauseMessage(maybeException.tryGet());

                System.err.printf(
                    "'Reader::toEitherWithRetry' FAILED, returning 'Either.Left'/'Maybe.Nothing'. Reason: %s%n",
                    failureReason
                );
                return Either.left(failureReason);
            },
            Either::right
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fold
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Evaluate this {@link Reader} functor.
     *
     * <p>
     * This method is a very simple (and somewhat reckless and unforgiving) application of <code>fold</code>.
     * </p>
     *
     * @return this functor's value in case of successful evaluation/computation, otherwise return the bottom value representation, here an {@link Exception}
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T tryGet() {
        // NB! Blocks current thread!
        return fold(
            (exception) -> {
                throw new RuntimeException(
                    (exception.isNothing())
                        ? "null as folded value"
                        : exception.tryGet().getMessage()
                );
            },
            identity()
        );
    }

    /**
     * To <i>fold</i> a value means creating a new representation of it.
     * For {@link Reader} instances this will force execution/evaluation.
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
        Function<Maybe<Exception>, ? extends V> onBottom,
        Function<? super T, ? extends V> onRead
    ) {
        return foldWithRetry(0, onBottom, onRead);
    }

    /**
     * To <i>fold</i> a value means creating a new representation of it.
     * For {@link Reader} instances this will force execution/evaluation.
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
     * It is replaced by a special unary function in case this {@link Reader} return a bottom value, here an exception,
     * which is provided as the function parameter.
     * </p>
     *
     * <p>...</p>
     *
     * <p>
     * TODO: ...
     * As...
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
        Function<Maybe<Exception>, ? extends V> onBottom,
        Function<? super T, ? extends V> onRead
    ) {
        return foldWithRetry(numberOfRetries, numberOfRetries, onBottom, onRead);
    }

    /**
     * Folding with a recursion-based retry mechanism included.
     *
     * @param retryIndex           The number of retries left for this function before invoking the 'onBottom' callback
     * @param totalNumberOfRetries The total number of retries this function will try before invoking the 'onBottom' callback
     * @param onBottom             Function (unary) maybe with the bottom value (exception) provided as the function parameter, <code>Maybe.Nothing</code> if the folded value is 'null'
     * @param onRead               Function (unary) (the "catamorphism") to be applied to the read value
     * @param <V>                  The type of the folded/returning value
     * @return the folded value
     */
    protected <V> V foldWithRetry(
        int retryIndex,
        int totalNumberOfRetries,
        Function<Maybe<Exception>, ? extends V> onBottom,
        Function<? super T, ? extends V> onRead
    ) {
        try {
            // NB! Blocks current thread!
            T foldedValue = get();
            if (foldedValue == null) {
                if (retryIndex > 0 && DO_VERBOSE_LOGGING) {
                    System.out.printf(
                        "'Reader::foldWithRetry' FAILED, retrying... (%d/%d)%n",
                        retryIndex - 1, totalNumberOfRetries
                    );
                    return foldWithRetry(retryIndex - 1, totalNumberOfRetries, onBottom, onRead);
                }
                System.err.printf(
                    "'Reader::foldWithRetry' FAILED, executing 'onBottom' 'Supplier' (\"nullary\" function). Reason: %s%n",
                    "null as folded value"
                );
                return onBottom.apply(nothing());
            }
            return onRead.apply(foldedValue);

        } catch (Exception exception) {
            if (retryIndex > 0 && DO_VERBOSE_LOGGING) {
                System.out.printf(
                    "'Reader::foldWithRetry' FAILED, retrying... (%d/%d)%n",
                    retryIndex - 1, totalNumberOfRetries
                );
                return foldWithRetry(retryIndex - 1, totalNumberOfRetries, onBottom, onRead);
            }
            System.err.printf(
                "'Reader::foldWithRetry' FAILED, executing 'onBottom' 'Supplier' (\"nullary\" function). Reason: %s%n",
                getRootCauseMessage(exception)
            );
            return onBottom.apply(just(exception));
        }
    }

    @Override
    public <V> Maybe<V> mapFold(
        Sequence<Function<? super T, ? extends V>> functionSequence,
        FreeMonoid<V> monoid
    ) {
        if (isCancelled()) {
            throw new CancellationException();
        }
        try {
            // NB! Blocks current thread!
            return Maybe.of(Promise.tryMapFold(tryGet(), functionSequence, monoid));

        } catch (Exception exception) {
            //exception.printStackTrace(System.err);
            return nothing();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // java.lang.Object
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return ToStringBuilder
            .reflectionToString(this,
                SHORT_PREFIX_STYLE,
                true
            );
    }
}
