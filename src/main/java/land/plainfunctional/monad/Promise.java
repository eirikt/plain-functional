package land.plainfunctional.monad;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.ToStringBuilder;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.algebraicstructure.MonoidStructure;
import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.FunctorExtras;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
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
 * Most aspects of {@link Promise} instances are <i>asynchronous</i> in nature.
 * Unlike {@link Reader}s, {@link Promise}s may be asynchronously evaluated via the <code>evaluate</code> method.
 * The only exception is when <i>folding</i> an unresolved {@link Promise} instance.
 * (Folding methods delegate to the blocking <code>get()</code> methods, specified by the implemented {@link Future} interface.)
 * </p>
 *
 * @param <T> The type of the promised value
 */
public class Promise<T> implements Monad<T>, FunctorExtras<T>, Future<T> {

    // Switch for verbose logging to System.out/System.err
    private static final boolean DO_VERBOSE_LOGGING = true;


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

    // TODO: Should this be renamed to e.g. 'resolved' - it is not semantically equivalent with 'Reader::startingWith'...

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

    /**
     * Factory method.
     *
     * <p>
     * Parallel fetching of values, and then folding them using the provided monoid.
     * </p>
     *
     * @param supplierList The enumerated deferred values to be folded into this {@link Promise} functor
     * @param monoid       The monoid to be used for folding the deferred values
     * @param <T>          The type of the deferred values
     * @return A {@link Promise} {@link Maybe} value
     */
    public static <T> Promise<Maybe<T>> of(
        List<Supplier<? extends T>> supplierList,
        FreeMonoid<T> monoid
    ) {
        List<Function<? super T, ? extends T>> functionList = new ArrayList<>(supplierList.size());
        for (Supplier<? extends T> supplier : supplierList) {
            functionList.add((ignored) -> supplier.get());
        }
        return monoid
            .toPromiseIdentity()
            .map(
                Sequence.of(functionList),
                monoid
            );
    }

    /**
     * Factory method.
     *
     * <p>
     * Parallel fetching of values, and then folding them using the provided monoid.
     * </p>
     *
     * @param supplierSequence The enumerated deferred values to be folded into this {@link Promise} functor
     * @param monoid           The monoid to be used for folding the deferred values
     * @param <T>              The type of the deferred values
     * @return A {@link Promise} {@link Maybe} value
     */
    public static <T> Promise<Maybe<T>> of(
        Sequence<Supplier<? extends T>> supplierSequence,
        FreeMonoid<T> monoid
    ) {
        //throw new UnsupportedOperationException();

        //List<Function<? super T, ? extends T>> functionList = new ArrayList<>(supplierList.size());
        //for (Supplier<? extends T> supplier : supplierList) {
        //    functionList.add((ignored) -> supplier.get());
        //}
        return monoid
            .toPromiseIdentity()
            .map(
                //new Sequence<Function<? super T, ? extends T>> functionList,
                supplierSequence.map(
                    new Function<Supplier<? extends T>, Function<? super T, ? extends T>>() {
                        @Override
                        public Function<? super T, ? extends T> apply(Supplier<? extends T> supplier) {
                            return (ignored) -> supplier.get();
                        }
                    }
                ), monoid
            );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////
    // State & Constructors
    ///////////////////////////////////////////////////////

    protected Supplier<T> valueSupplier;
    protected CompletableFuture<T> valueFuture;
    protected T resolvedValue;

    //protected AtomicBoolean done = new AtomicBoolean(false);

    // TODO: Add cancel/interrupt mechanism
    //protected AtomicBoolean cancelled = new AtomicBoolean(false);

    protected List<Consumer<? super T>> onResolvedEffectList;

    protected Promise(T resolvedValue) {
        Arguments.requireNotNull(resolvedValue, "'Promise' cannot handle null values");
        this.onResolvedEffectList = new CopyOnWriteArrayList<>();
        resolve(resolvedValue);
        if (DO_VERBOSE_LOGGING) {
            System.out.printf("Promise(resolvedValue): Value (immediately) resolved: %s (thread \"%s\")%n", resolvedValue, Thread.currentThread().getName());
        }
    }

    protected Promise(Supplier<T> nullaryFunction) {
        Arguments.requireNotNull(nullaryFunction, "'Promise' cannot handle null functions");
        this.valueSupplier = nullaryFunction;
        this.onResolvedEffectList = new CopyOnWriteArrayList<>();
    }

    protected Promise(CompletableFuture<T> completableFuture) {
        Arguments.requireNotNull(completableFuture, "'Promise' cannot handle null futures");
        this.valueFuture = completableFuture;
        this.onResolvedEffectList = new CopyOnWriteArrayList<>();
        this.valueFuture.whenComplete(
            (value, throwable) -> {
                if (throwable != null) {
                    throw new RuntimeException(throwable);
                }
                resolve(value);
            });
    }


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
    // TODO: Consider promoting this (partial) function to a typeclass function (together with 'effect')...
    public Promise<T> tryEffect(Consumer<? super T> effect) {
        Arguments.requireNotNull(effect, "'effect' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }
        if (isResolved()) {
            if (DO_VERBOSE_LOGGING) {
                System.out.println("'Promise::tryEffect' (with resolved value): Invoking effect immediately!");
            }
        } else {
            System.err.println("'Promise::tryEffect' (with supplier/future value): NB! Invoking effect immediately with null as argument!");
        }
        effect.accept(this.resolvedValue);

        return this;
    }

    /**
     * Alias for <code>effect</code>.
     */
    public Promise<T> onResolved(Consumer<? super T> effect) {
        return effect(effect);
    }

    /**
     * {@inheritDoc}
     *
     * <p>...</p>
     *
     * Subscribe for a (side) effect to be executed when this {@link Promise} is resolved.
     * <i>This effect (callback function) will only be executed once.</i>
     *
     * @param effect The (side) effect
     * @return this (unmodified) same {@link Promise} instance
     */
    public Promise<T> effect(Consumer<? super T> effect) {
        Arguments.requireNotNull(effect, "'effect' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }
        if (isResolved()) {
            if (DO_VERBOSE_LOGGING) {
                System.out.println("'Promise::effect' (with resolved value): Invoking effect immediately!");
            }
            effect.accept(this.resolvedValue);

        } else {
            if (DO_VERBOSE_LOGGING) {
                System.out.println("'Promise::effect' (with supplier/future): Effect added/subscribed");
            }
            this.onResolvedEffectList.add(effect);
        }

        return this;
    }

    /**
     * Common function for <i>resolving</i> this {@link Promise}.
     * NB! Will invoke all registered effect/(event/callback) functions.
     *
     * @param resolvedValue The resolved value for this {@link Promise}
     */
    private void resolve(T resolvedValue) {
        this.resolvedValue = resolvedValue;
        if (DO_VERBOSE_LOGGING) {
            System.out.printf("'Promise::resolve' (with future), value resolved: '%s' (thread \"%s\")%n", this.resolvedValue, Thread.currentThread().getName());
        }
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

    // Here: Implemented as an alias for 'isResolved</code>'
    @Override
    public boolean isDone() {
        return isResolved();
    }

    @Override
    public boolean isCancelled() {
        // TODO: Add cancel/interrupt mechanism
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
            // NB! Blocks current thread!
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
            Instant start = now();
            if (DO_VERBOSE_LOGGING) {
                System.out.println("'Promise::get' (with supplier): NB! Blocking current thread!");
            }
            // NB! Blocks current thread!
            resolve(this.valueSupplier.get());
            if (DO_VERBOSE_LOGGING) {
                long blockingTimeInMillis = between(start, now()).toMillis();
                if (blockingTimeInMillis > 10L) {
                    System.err.printf("'Promise::get' (with supplier): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
                }
                System.out.printf("'Promise::get' (with supplier), value resolved: '%s' (thread \"%s\")%n", this.resolvedValue, Thread.currentThread().getName());
            }

            // Allow null return values; Mostly handled by functions calling this one
            //if (this.resolvedValue == null) {
            //    throw new NullPointerException();
            //}

            return this.resolvedValue;
        }

        if (hasValueFuture()) {
            try {
                Instant start = now();
                if (DO_VERBOSE_LOGGING) {
                    System.out.println("Promise::get() (with future): NB! Blocking current thread!");
                }
                resolve(this.valueFuture.get());
                if (DO_VERBOSE_LOGGING) {
                    long blockingTimeInMillis = between(start, now()).toMillis();
                    if (blockingTimeInMillis > 10L) {
                        System.err.printf("Promise::get() (future): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
                    }
                }
                if (this.resolvedValue == null) {
                    throw new NullPointerException();
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
    @Override
    public <V> Promise<V> map(
        Function<? super T, ? extends V> function,
        T defaultValue
    ) {
        Arguments.requireNotNull(function, "'function' argument cannot be null");

        if (isCancelled()) {
            throw new CancellationException();
        }

        if (isResolved()) {
            for (Consumer<? super T> effect : this.onResolvedEffectList) {
                effect.accept(this.resolvedValue);
            }
            return new Promise<>(
                () -> function.apply(this.resolvedValue)
            );
        }

        if (hasValueSupplier()) {
            // Effect list must be mapped as well
            //List<Consumer<? super V>> mappedOnResolvedEffectList = new CopyOnWriteArrayList<>();
            /*
            for (Consumer<? super T> effect : this.onResolvedEffectList) {
                mappedOnResolvedEffectList.add(
                    new Consumer<V>() {
                        @Override
                        public void accept(V v) {
                            //effect.accept(resolvedValue);
                            effect.accept((T) v);
                        }
                    }
                );
            }
            */
            return new Promise<>(
                () -> {
                    try {
                        T resolvedValue = this.valueSupplier.get();
                        //V mappedValue = function.apply(unMappedValue);

                        for (Consumer<? super T> effect : this.onResolvedEffectList) {
                            /*
                            mappedOnResolvedEffectList.add(
                                new Consumer<V>() {
                                    @Override
                                    public void accept(V v) {
                                        //effect.accept(resolvedValue);
                                        //effect.accept((T) v);
                                        effect.accept(mappedValue);
                                    }
                                }
                            );
                            */
                            effect.accept(resolvedValue);
                        }
                        return function.apply(resolvedValue);

                    } catch (Exception exception) {
                        if (defaultValue != null) {
                            System.err.printf(
                                "'Promise::map' (with supplier) FAILED, reason: %s. Using default value '%s'.%n",
                                getRootCauseMessage(exception), defaultValue
                            );
                            return function.apply(defaultValue);
                        }
                        System.err.printf(
                            "'Promise::map' (with supplier) FAILED, reason: %s (No default value available!)%n",
                            getRootCauseMessage(exception)
                        );
                        throw new RuntimeException(exception);
                    }
                }//,
                //(List<Consumer<? super V>>) this.onResolvedEffectList
                //mappedOnResolvedEffectList
            );
        }

        if (hasValueFuture()) {
            try {
                System.err.println("'Promise::map' (with future, default timeout): NB! Blocking current thread!");
                T resolvedValue = this.valueFuture.get();
                for (Consumer<? super T> effect : this.onResolvedEffectList) {
                    effect.accept(resolvedValue);
                }

                return new Promise<>(
                    function.apply(resolvedValue)
                );

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

    @Override
    public <V> Promise<Maybe<V>> map(
        Sequence<Function<? super T, ? extends V>> functionList,
        FreeMonoid<V> monoid
    ) {
        return new Promise<>(() -> mapFold(functionList, monoid));
    }


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
                    if (DO_VERBOSE_LOGGING) {
                        System.out.println("'Promise::join' (with supplier): NB! Blocking current thread!");
                    }

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
                    if (DO_VERBOSE_LOGGING) {
                        long blockingTimeInMillis = between(start, now()).toMillis();
                        if (blockingTimeInMillis > 10L) {
                            System.err.printf("'Promise::join' (with supplier): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
                        }
                    }
                    return value;
                }
            );
        }
        if (hasValueFuture()) {
            try {
                Instant start = now();
                if (DO_VERBOSE_LOGGING) {
                    System.out.println("'Promise::join' (with future): NB! Blocking current thread!");
                }
                T resolvedValue = this.valueFuture.get();
                if (DO_VERBOSE_LOGGING) {
                    long blockingTimeInMillis = between(start, now()).toMillis();
                    if (blockingTimeInMillis > 10L) {
                        System.err.printf("'Promise::join' (with future): NB! Blocked current thread for %d ms%n", blockingTimeInMillis);
                    }
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
        Either<String, T> either = toEither();

        // Handles exceptions
        if (either.isLeft()) {
            return nothing();
        }
        // Handles nulls
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
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Asynchronously evaluate/execute this 'Promise'.
     * If not already evaluated (and resolved), or an async execution is already started,
     * then a {@link CompletableFuture} of this promise's {@link Supplier} value will be created and started.
     * NB! This {@link CompletableFuture} will use the common Fork/Join thread pool.
     *
     * @return A new {@link Promise} coupled to an executing {@link CompletableFuture}
     */
    public Promise<T> evaluate() {
        if (isCancelled()) {
            throw new CancellationException();
        }
        if (isResolved()) {
            if (DO_VERBOSE_LOGGING) {
                System.out.println("( isResolved() (already) )");
            }
            return this;
        }
        if (hasValueFuture()) {
            if (DO_VERBOSE_LOGGING) {
                System.out.println("( hasFuture() (already) )");
            }
            return this;
        }

        // Starts async/non-blocking execution
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
        return fold(
            (exception) -> { throw new RuntimeException(exception); },
            identity()
        );
    }

    /**
     * To <i>fold</i> a value means creating a new representation of it.
     * For {@link Promise} instances this will force execution/evaluation.
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
            return onRead.apply(
                // NB! Blocks current thread!
                get()
            );

        } catch (Exception exception) {
            System.err.printf(
                "'Promise::fold' FAILED, reason: %s. Executing 'onBottom' function.%n",
                getRootCauseMessage(exception)
            );
            return onBottom.apply(exception);
        }
    }

    @Override
    public <V> Maybe<V> mapFold(
        Sequence<Function<? super T, ? extends V>> functionList,
        FreeMonoid<V> monoid
    ) {
        Maybe<T> maybePromisedValue = toMaybe();
        if (maybePromisedValue.isNothing()) {
            return nothing();
        }

        try {
            // Partiality: Handles null values
            return Maybe.of(tryMapFold(maybePromisedValue.tryGet(), functionList, monoid));

        } catch (Exception exception) {
            System.err.printf(
                "'Promise::mapFold' FAILED, reason: %s. Returning 'Maybe.Nothing'.%n",
                getRootCauseMessage(exception)
            );
            // Partiality: Handles exceptions
            return nothing();
        }
    }

    static <T, V> V tryMapFold(
        T initialValue,
        Sequence<Function<? super T, ? extends V>> functionList,
        FreeMonoid<V> monoid
    ) throws Exception {
        Arguments.requireNotNull(initialValue, "'initialValue' argument cannot be null");
        Arguments.requireNotNull(functionList, "'functionList' argument cannot be null");
        Arguments.requireNotNull(monoid, "'monoid' argument cannot be null");

        CompletableFuture<T> initialFuture = completedFuture(initialValue);

        List<CompletableFuture<V>> asyncFunctorList = new ArrayList<>();
        for (Function<? super T, ? extends V> function : functionList.toJavaList()) {
            asyncFunctorList.add(
                initialFuture
                    .thenApplyAsync(
                        function
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
