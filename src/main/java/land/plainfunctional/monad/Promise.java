package land.plainfunctional.monad;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.ToStringBuilder;

import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static land.plainfunctional.monad.Maybe.nothing;
import static land.plainfunctional.util.ReflectionUtils.createDefaultInstance;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * <i>Functor context:</i>
 * <b>
 * A future value&mdash;a promise of providing a value
 * </b><br>
 * The value is typically <i>read</i> from an external/shared environment, and may be arbitrary (neither <i>total</i> nor <i>deterministic</i>).
 * When evaluated (folded), the value will appear after an arbitrary amount of time.
 * Also, the value may not show up at all&mdash;like
 * {@link Either} and {@link Maybe}, and {@link Reader}, the {@link Promise} context represents <a href="https://en.wikipedia.org/wiki/Partial_function">partiality</a>.
 *
 * <p>
 * Promise monads are functions from an shared environment to a value,
 * making it possible to bind variables to external sources, for composition.
 * </p>
 *
 * <p>
 * Most aspects of {@link Promise} instances are <i>asynchronous</i> in nature&mdash;the
 * exception being if <i>folding</i> an unresolved {@link Promise} instance.
 * (Folding methods delegate to the <code>get()</code> methods, specified by the implemented {@link Future} interface.)
 * </p>
 *
 * @param <T> The type of the promised value
 */
public class Promise<T> implements Monad<T>, Future<T> {

    ///////////////////////////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Just for having a {@link Promise} instance to reach the member methods, e.g. <code>pure</code>.
     * <b>NB! The given type must have an available empty constructor.</b>
     */
    public static <T> Promise<T> asPromise(Class<T> type) {
        return startingWith(createDefaultInstance(type));
    }

    /**
     * Alias for <code>of(resolvedValue)</code>.
     */
    public static <T> Promise<T> startingWith(T value) {
        return of(value);
    }

    /**
     * Factory method for already resolved values.
     *
     * @param resolvedValue The (already) resolved value to be put into this {@link Promise} functor
     * @return A {@link Promise} value
     */
    public static <T> Promise<T> of(T resolvedValue) {
        return new Promise<>(resolvedValue);
    }

    /**
     * Factory method.
     *
     * @param valueSupplier The deferred value to be put into this {@link Promise} functor, represented by a "nullary" function/{@link Supplier}
     * @return A {@link Promise} value
     */
    public static <T> Promise<T> of(Supplier<T> valueSupplier) {
        return new Promise<>(valueSupplier);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    protected Promise(T resolvedValue) {
        Arguments.requireNotNull(resolvedValue, "'Promise' cannot handle 'null' values");
        resolve(resolvedValue);
        System.out.printf("Promise(resolvedValue): Value (immediately) resolved: %s (thread \"%s\")%n", resolvedValue, Thread.currentThread().getName());
    }

    protected Promise(Supplier<T> nullaryFunction) {
        Arguments.requireNotNull(nullaryFunction, "'Promise' cannot handle 'null' suppliers");
        this.valueSupplier = nullaryFunction;
    }

    protected Promise(CompletableFuture<T> completableFuture) {
        Arguments.requireNotNull(completableFuture, "'Promise' cannot handle 'null' futures");
        this.valueFuture = completableFuture;
        this.valueFuture.whenComplete(
            (value, throwable) -> {
                if (throwable != null) {
                    throw new RuntimeException(throwable);
                }
                resolve(value);
            });
    }


    ///////////////////////////////////////////////////////////////////////////
    // State
    ///////////////////////////////////////////////////////////////////////////

    protected Supplier<T> valueSupplier;
    protected CompletableFuture<T> valueFuture;
    protected T resolvedValue;

    //protected AtomicBoolean done = new AtomicBoolean(false);

    // TODO: Add cancel/interrupt mechanism
    //protected AtomicBoolean cancelled = new AtomicBoolean(false);

    protected List<Consumer<? super T>> onResolvedEffectList = new ArrayList<>();


    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return <code>true</code> if this {@link Promise} instance has a "nullary" function/{@link Supplier} (for providing the value)
     */
    public boolean hasValueSupplier() {
        return this.valueSupplier != null;
    }

    /**
     * @return <code>true</code> if this {@link Promise} instance has a {@link Future} (for providing the value)
     */
    public boolean hasValueFuture() {
        return this.valueFuture != null;
    }

    /**
     * The <code>isDone</code> method (specified by the implemented {@link Future} interface) delegates to this method.
     *
     * @return <code>true</code> if this {@link Promise} instance's value is resolved
     */
    public boolean isResolved() {
        if (isCancelled()) {
            throw new CancellationException();
        }
        //synchronized (this) {
        //    boolean isDone = this.done.get();
        //    if (isDone && this.resolvedValue == null) {
        //        throw new IllegalStateException("'Promise' flagged as 'done', but has no resolved value");
        //    }
        //    return isDone;
        //}
        return this.resolvedValue != null;
    }

    /**
     * Execute a (side) effect using this promised value.
     * <i>This effect (callback function) will always be executed immediately.</i>
     *
     * <p>
     * <b>NB!</b> If this {@link Promise} is not yet resolved,
     * <code>null</code> will be provided as the effect function's argument.
     * (That is the reason why this method is prefixed with <code>try</code>...)
     * </p>
     *
     * @param effect The (side) effect
     * @return this (unmodified) {@link Promise} instance
     */
    public Promise<T> tryEffect(Consumer<? super T> effect) {
        Arguments.requireNotNull(effect, "'effect' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }
        if (isDone()) {
            System.out.println("'Promise::tryEffect' (with resolved value): Invoking effect immediately!");
        } else {
            System.err.println("'Promise::tryEffect' (with supplier/future value): NB! Invoking effect immediately with 'null' as argument!");
        }
        effect.accept(this.resolvedValue);

        return this;
    }

    /**
     * Subscribe for a (side) effect to be executed when this {@link Promise} is resolved.
     * <i>This effect (callback function) will only be executed once.</i>
     *
     * <p>
     * Another name for this method could have been <code>onResolved</code>.
     * </p>
     *
     * @param effect The (side) effect
     * @return this (unmodified) {@link Promise} instance
     */
    public Promise<T> effect(Consumer<? super T> effect) {
        Arguments.requireNotNull(effect, "'effect' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }
        if (isDone()) {
            System.out.println("'Promise::effect' (with resolved value): Invoking effect immediately!");
            effect.accept(this.resolvedValue);

        } else {
            synchronized (this) {
                System.out.println("'Promise::effect' (with supplier/future): Effect added/subscribed");
                this.onResolvedEffectList.add(effect);
            }
        }

        return this;
    }

    /**
     * Common function for <i>resolving</i> this {@link Promise}.
     * NB! Will invoke all registered effect/(event/callback) functions.
     *
     * @param resolvedValue The resolved value for this {@link Promise}
     */
    private synchronized void resolve(T resolvedValue) {
        this.resolvedValue = resolvedValue;
        //this.done.set(true);
        System.out.printf("'Promise::resolve' (with future), value resolved: '%s' (thread \"%s\")%n", this.resolvedValue, Thread.currentThread().getName());
        for (Consumer<? super T> subscribedEffect : this.onResolvedEffectList) {
            subscribedEffect.accept(this.resolvedValue);
            if (!this.onResolvedEffectList.remove(subscribedEffect)) {
                System.err.println("NB! Unable to remove used callback/effect ('Supplier') function");
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Future
    ///////////////////////////////////////////////////////////////////////////

    // Here: Implemented as an alias of 'isResolved</code>'
    @Override
    public boolean isDone() {
        return isResolved();
    }

    @Override
    public boolean isCancelled() {
        // TODO: Add cancel/interrupt mechanism
        //throw new UnsupportedOperationException("Not yet implemented");
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use one of the folding methods instead&mdash;for explicit handling of bottom values.
     */
    @Override
    @Deprecated
    public T get() throws InterruptedException, ExecutionException {
        try {
            return get(0, null);

        } catch (TimeoutException exception) {
            System.err.printf("'Promise::get' FAILED, reason: %s%n", getRootCauseMessage(exception));
            throw new RuntimeException(exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use one of the folding methods instead&mdash;for explicit handling of bottom values.
     */
    @Override
    @Deprecated
    // TODO: Add timeout mechanism
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //Arguments.requireNotNull(unit, "'unit' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }

        if (isResolved()) {
            return this.resolvedValue;
        }

        if (hasValueSupplier()) {
            // NB! Blocks current thread!
            Instant start = now();
            System.out.println("'Promise::get' (with supplier): NB! Blocking current thread!");
            resolve(this.valueSupplier.get());
            long blockingTimeInMillis = between(start, now()).toMillis();
            if (blockingTimeInMillis > 10L) {
                System.err.printf("'Promise::get' (with supplier): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
            }
            System.out.printf("'Promise::get' (with supplier), value resolved: '%s' (thread \"%s\")%n", this.resolvedValue, Thread.currentThread().getName());

            return this.resolvedValue;
        }

        if (hasValueFuture()) {
            try {
                Instant start = now();
                System.out.println("Promise::get() (with future): NB! Blocking current thread!");
                //T localResolvedValue = this.valueFuture.get();
                resolve(this.valueFuture.get());
                long blockingTimeInMillis = between(start, now()).toMillis();
                if (blockingTimeInMillis > 10L) {
                    System.err.printf("Promise::get() (future): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
                }
                return this.resolvedValue;

            } catch (Exception exception) {
                System.err.printf("'Promise::get' FAILED! Reason: %s%n", getRootCauseMessage(exception));
                throw new RuntimeException(exception);
            }
        }

        throw new IllegalStateException("'Promise' must either have a resolved value, a value supplier (\"nullary\" function), or a 'Future'-based value");
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO: Add cancel/interrupt mechanism
        throw new UnsupportedOperationException("Not yet implemented");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public <V> Promise<V> map(Function<? super T, ? extends V> function) {
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
     *                     this parameter will be used as mapping value if this {@link Promise} returns a bottom value.
     *                     (This is accomplished by transforming this {@link Promise} to a {@link Maybe}.)
     * @param <V>          The type of the codomain
     * @return the new/other functor
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom values (Wikipedia)</a>
     */
    public <V> Promise<V> map(
        Function<? super T, ? extends V> function,
        T defaultValue
    ) {
        Arguments.requireNotNull(function, "'function' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }
        if (isResolved()) {
            return new Promise<>(
                () -> function.apply(this.resolvedValue)
            );
        }
        if (hasValueSupplier()) {
            return new Promise<>(
                () -> {
                    try {
                        return function.apply(this.valueSupplier.get());

                    } catch (Exception exception) {
                        if (defaultValue != null) {
                            System.err.printf(
                                "'Promise::map' (with supplier) FAILED, reason: %s. Using default value '%s'.%n",
                                getRootCauseMessage(exception), defaultValue
                            );
                            return function.apply(defaultValue);
                        }
                        System.err.printf(
                            "'Promise::map' (with supplier) FAILED, reason: %s%n",
                            getRootCauseMessage(exception)
                        );
                        throw new RuntimeException(exception);
                    }
                });
        }
        if (hasValueFuture()) {
            try {
                System.err.println("'Promise::map' (with future, default timeout): NB! Blocking current thread!");
                T localResolvedValue = this.valueFuture.get();
                V mappedValue = function.apply(localResolvedValue);

                return new Promise<>(mappedValue);

            } catch (Exception exception) {
                System.err.printf(
                    "'Promise::map' (with future) FAILED, reason: %s. Using default value '%s'.%n",
                    getRootCauseMessage(exception), defaultValue
                );
                throw new RuntimeException(exception);
            }
        }

        throw new IllegalStateException("'Promise' must either have a resolved value, a value supplier (\"nullary\" function), or a 'Future'-based value");
    }

    /* TODO: Consider:
    public <V> Promise<Maybe<V>> map(
        Function<? super T, ? extends V> function,
        T defaultValue
    ) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    */

    /* TODO: Consider:
    public <V> Promise<Maybe<V>> map(
        List<Function<? super T, ? extends V>> functorList,
        FreeMonoid<V> monoid
    ) {
        return new Promise<>(() -> mapFold(functorList, monoid));
    }
    */


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Promise<T> pure(T value) {
        return of(value);
    }

    @Override
    public <V> Promise<V> apply(Applicative<Function<? super T, ? extends V>> functionInContext) {
        Arguments.requireNotNull(functionInContext, "'functionInContext' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }

        // TODO: May throw 'ClassCastException'! (See inherited JavaDoc)
        // => Any chance of mitigating this - with Java's type system? (with the lack of higher-kinded types)
        Promise<Function<? super T, ? extends V>> promiseFunction =
            (Promise<Function<? super T, ? extends V>>) functionInContext;

        if (isResolved()) {
            try {
                return new Promise<>(promiseFunction.get().apply(this.resolvedValue));

            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        if (hasValueSupplier()) {
            return new Promise<>(
                () -> {
                    try {
                        return promiseFunction.get().apply(this.valueSupplier.get());

                    } catch (Exception exception) {
                        System.err.printf(
                            "'Promise.apply' (with supplier) FAILED, reason: %s%n",
                            getRootCauseMessage(exception)
                        );
                        throw new RuntimeException(exception);
                    }
                }
            );
        }
        if (hasValueFuture()) {
            return new Promise<>(
                () -> {
                    try {
                        return promiseFunction.get().apply(this.valueFuture.get());

                    } catch (Exception exception) {
                        System.err.printf(
                            "'Promise.apply' (with future) FAILED, reason: %s%n",
                            getRootCauseMessage(exception)
                        );
                        throw new RuntimeException(exception);
                    }
                }
            );
        }

        throw new IllegalStateException("'Promise' must either have a resolved value, a value supplier (\"nullary\" function), or a 'Future'-based value");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Monad
    ///////////////////////////////////////////////////////////////////////////

    @Override
    //TODO: @SuppressWarnings("unchecked")
    public Promise<T> join() {
        if (isCancelled()) {
            throw new CancellationException();
        }

        if (isResolved()) {
            if (this.resolvedValue instanceof Promise<?>) {
                // @SuppressWarnings: TODO: <some reasoning why it is ok>
                return ((Promise<T>) this.resolvedValue);
            }
            return this;
        }
        if (hasValueSupplier()) {
            return new Promise<>(
                () -> {
                    Instant start = now();
                    System.out.println("Promise::join() (with supplier): NB! Blocking current thread!");

                    T value = this.valueSupplier.get();

                    if (value instanceof Promise<?>) {
                        // @SuppressWarnings: TODO: <some reasoning why it is ok>
                        Promise<T> promise = ((Promise<T>) value).join();
                        if (promise.hasValueSupplier()) {
                            value = promise.valueSupplier.get();
                        } else {
                            value = promise.resolvedValue;
                        }
                    }
                    long blockingTimeInMillis = between(start, now()).toMillis();
                    if (blockingTimeInMillis > 10L) {
                        System.err.printf("Promise::join() (with supplier): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
                    }
                    return value;
                }
            );
        }
        if (hasValueFuture()) {
            try {
                Instant start = now();
                System.out.println("Promise::join (with future): NB! Blocking current thread!");
                T resolvedValue = this.valueFuture.get();
                long blockingTimeInMillis = between(start, now()).toMillis();
                if (blockingTimeInMillis > 10L) {
                    System.err.printf("Promise::join (with future): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
                }
                return new Promise<>(resolvedValue);

            } catch (Exception exception) {
                System.err.printf(
                    "'Promise::join' (with future) FAILED, reason: %s%n",
                    getRootCauseMessage(exception)
                );
                throw new RuntimeException(exception);
            }
        }

        throw new IllegalStateException("'Promise' must either have a resolved value, a value supplier (\"nullary\" function), or a 'Future'-based value");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Filter
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Append
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // Transformation
    // (Folding this 'Promise' functor wrapped in another functor for handling partial values)
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Handling of bottom value evaluations using a {@link Maybe} functor.
     * It delegates to <code>toEither</code>.
     *
     * @return the evaluated (deferred) value in a {@link Maybe} context
     */
    public Maybe<T> toMaybe() {
        // NB! Blocks current thread!
        Either<String, T> either = toEither();

        // Handles exceptions
        if (either.isLeft()) {
            return nothing();
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
        return fold(
            (exception) -> {
                System.err.printf(
                    "Promise::toEither FAILED, reason: %s. Returning 'Either.Left'/'Maybe.Nothing'.%n",
                    getRootCauseMessage(exception)
                );
                return Either.left(exception.getMessage());
            },
            Either::right
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Asynchronous evaluation
    // "Async/await", well...
    ///////////////////////////////////////////////////////////////////////////

    /*
     * Alias of <code>evaluate</code>.
     /
    public Promise<T> async() {
        return evaluate();
    }

    /
     * Alias of <code>tryGet</code>.
     /
    public T await() {
        // NB! Blocks current thread!
        return tryGet();
    }
    */

    /**
     * Asynchronously evaluate/execute this 'Promise'.
     * If not already evaluated (and resolved), or an async execution is already started,
     * then a {@link CompletableFuture} of this promise's {@link Supplier} value will be created and started.
     *
     * @return A new {@link Promise} coupled to an executing {@link CompletableFuture}
     */
    public Promise<T> evaluate() {
        if (isCancelled()) {
            throw new CancellationException();
        }
        if (isResolved()) {
            System.out.println("( isResolved() (already) )");
            return this;
        }
        if (hasValueFuture()) {
            System.out.println("( hasFuture() (already) )");
            return this;
        }

        // Async execution
        Promise<T> futureBasedPromise = new Promise<>(
            supplyAsync(
                this.valueSupplier
                //, runnable -> new Thread(runnable).start()     // New thread per future evaluation (thread will be properly disposed of)
                //, mySharedAndManagedAndOptimizedThreadExecutor // Either a member or method-injected 'Executor' instance
            )
        );
        // Just copying/sharing the resolved-callback list reference
        futureBasedPromise.onResolvedEffectList = this.onResolvedEffectList;

        return futureBasedPromise;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fold
    ///////////////////////////////////////////////////////////////////////////

    /**
     * This method is a very simple (and somewhat reckless and unforgiving) application of <code>fold</code>.
     *
     * @return this functor's value in case of successful evaluation/computation, otherwise return the bottom value representation, here an {@link Exception}
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
     * As {@link Promise} is a single-value functor, there is no need for a <i>binary</i> function;
     * It is replaced by an unary function, which transforms the single read value.
     * Also, the need for an initial value is redundant;
     * It is replaced by a special unary function in case this {@link Promise} return a bottom value, here an exception, which is provided as the function parameter.
     * </p>
     *
     * <p>...</p>
     *
     * <p>
     * <b>NB! Will force-evaluate and block the current thread if this {@link Promise} has not completed its evaluation (not <i>done</i>)</b>.
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
        if (isCancelled()) {
            throw new CancellationException();
        }
        try {
            // NB! Blocks current thread!
            return onRead.apply(get());

        } catch (Exception exception) {
            System.err.printf(
                "'Promise::fold' FAILED, reason: %s. Executing 'onBottom' function.%n",
                getRootCauseMessage(exception)
            );
            return onBottom.apply(exception);
        }
    }

    /* TODO: Consider:
    public <V> V tryMapFold(
        List<Function<? super T, ? extends V>> functorList,
        FreeMonoid<V> monoid
    ) throws Exception {
        if (isCancelled()) {
            throw new CancellationException();
        }
        // NB! Blocks current thread!
        return mapFold(tryGet(), functorList, monoid);
    }
    */

    /* TODO: Consider:
    public <V> Maybe<V> mapFold(
        List<Function<? super T, ? extends V>> functorList,
        FreeMonoid<V> monoid
    ) {
        // NB! Blocks current thread!
        Maybe<T> promisedValue = toMaybe();
        if (promisedValue.isNothing()) {
            return nothing();
        }
        try {
            return Maybe.of(mapFold(promisedValue.tryGet(), functorList, monoid));

        } catch (Exception exception) {
            System.err.printf(
                "'Promise::mapFold' FAILED, reason: %s. Returning 'Maybe.Nothing'.%n",
                getRootCauseMessage(exception)
            );
            return nothing();
        }
    }
    */

    /* TODO: Consider:
    static <T, V> V mapFold(
        T initialValue,
        List<Function<? super T, ? extends V>> functorList,
        FreeMonoid<V> monoid
    ) throws ExecutionException, InterruptedException {

        CompletableFuture<T> initialFuture = completedFuture(initialValue);

        List<CompletableFuture<V>> asyncFunctorList = new ArrayList<>();
        for (Function<? super T, ? extends V> mapFunction : functorList) {
            asyncFunctorList.add(
                initialFuture
                    .thenApplyAsync(
                        mapFunction
                        //, runnable -> new Thread(runnable).start()     // New thread per future evaluation (thread will be properly disposed of)
                        //, mySharedAndManagedAndOptimizedThreadExecutor // Either an instance member or method-injected 'ThreadExecutor'
                    )
            );
        }
        return new MonoidStructure<>(
            allOf(
                asyncFunctorList.toArray(new CompletableFuture[0])
            ).thenApply(
                (ignored) -> asyncFunctorList
                    .stream()
                    .map(CompletableFuture::join)
                    .collect(toCollection(LinkedHashSet::new))
            ).get()
            , monoid.binaryOperation
            , monoid.identityElement
        ).fold();
    }
    */


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
