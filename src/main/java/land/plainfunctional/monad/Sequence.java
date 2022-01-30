package land.plainfunctional.monad;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;

import static java.lang.Runtime.getRuntime;

/**
 * <i>Functor context:</i>
 * <b>
 * Contains none, one, or many enumerated (possible duplicated) values
 * </b>
 *
 * <p>
 * Sequences is also known as <i>lists</i>.
 * ({@link Sequence} delegates to an {@link ArrayList} instance.)
 * </p>
 *
 * <p>
 * The contained values are also known as <i>elements</i> or <i>items</i> (of the sequence/list).
 * <code>null</code>s (and other forms of bottom value representations) are not allowed in (mathematical) sequences.
 * </p>
 *
 * @param <T> The type of the sequence elements/values
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
    public static <T> Sequence<T> asSequence() {
        return asSequence((Class<T>) null);
    }

    /**
     * Just for having a typed {@link Sequence} instance to reach the member methods, e.g. <code>pure</code>.
     */
    public static <T> Sequence<T> asSequence(Class<T> type) {
        return empty();
    }

    /**
     * Alias for <code>of(values)</code>.
     */
    // TODO: Possible heap pollution from parameterized vararg type (=> @SafeVarargs)
    // => https://www.baeldung.com/java-safevarargs
    //@SafeVarargs
    //@SuppressWarnings("varargs")
    public static <T> Sequence<T> asSequence(T... values) {
        return of(values);
    }

    /**
     * @return an empty {@link Sequence}
     */
    public static <T> Sequence<T> empty() {
        return new Sequence<>();
    }

    /**
     * @return a {@link Sequence} containing the given single value
     */
    public static <T> Sequence<T> of(T value) {
        return new Sequence<>(value);
    }

    /**
     * @return a {@link Sequence} containing the given values
     */
    // TODO: Possible heap pollution from parameterized vararg type (=> @SafeVarargs)
    // => https://www.baeldung.com/java-safevarargs
    //@SafeVarargs
    //@SuppressWarnings("varargs")
    public static <T> Sequence<T> of(T... values) {
        return new Sequence<>(values);
    }

    /**
     * @return a {@link Sequence} containing the given values
     */
    public static <T> Sequence<T> of(Iterable<? extends T> values) {
        return new Sequence<>(values);
    }

    /**
     * @return a {@link Sequence} containing the given values
     */
    public static <T> Sequence<T> of(Supplier<Iterable<T>> values) {
        return new Sequence<>(values.get());
    }


    ///////////////////////////////////////////////////////
    // State & Constructors
    ///////////////////////////////////////////////////////

    protected final List<T> values;

    protected Sequence() {
        this.values = new ArrayList<>();
    }

    protected Sequence(T value) {
        this();

        // TODO: Is this too strict?
        // => So far, it works as a fail-fast policy...
        Arguments.requireNotNull(value, "'Sequence' cannot contain null values");

        this.values.add(value);
    }

    // TODO: Possible heap pollution from parameterized vararg type (=> @SafeVarargs)
    // => https://www.baeldung.com/java-safevarargs
    protected Sequence(T... values) {
        this();
        for (T value : values) {
            // TODO: Is this too strict (and inefficient?)
            // => So far, it works as a fail-fast policy...
            Arguments.requireNotNull(value, "'Sequence' cannot contain null values");

            this.values.add(value);
        }
    }

    protected Sequence(Iterable<? extends T> iterable) {
        this();
        for (T value : iterable) {
            // TODO: Is this too strict (and inefficient?)
            // => So far, it works as a fail-fast policy...
            Arguments.requireNotNull(value, "'Sequence' cannot contain null values");

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
        return size() < 1;
    }

    /**
     * @return the number of elements in this sequence (of values)
     */
    public long size() {
        return this.values.size();
    }


    ///////////////////////////////////////////////////////
    // Functor
    ///////////////////////////////////////////////////////

    @Override
    public <V> Sequence<V> map(Function<? super T, ? extends V> function) {
        List<V> mappedValues = new ArrayList<>(this.values.size());
        for (T value : this.values) {
            try {
                V mappedValue = function.apply(value);

                // Partial function handling II:
                // NB! Skipping inclusion as mapped value has no representation in the codomain
                if (mappedValue == null) {
                    System.err.printf("NB! 'Sequence::map' (partial function handling II): Skipping mapping! Value has no representation in the codomain (%s) (%s)%n", value, "'null'");
                    continue;
                }
                mappedValues.add(mappedValue);

            } catch (Exception exception) {
                // Partial function handling I:
                System.err.printf("NB! 'Sequence::map' (partial function handling I): Skipping mapping! Value has no representation in the codomain (%s) (%s)%n", value, exception);
            }
        }
        return new Sequence<>(mappedValues);
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
     * <p>...</p>
     *
     * <p>
     * This is a variant of <code>map</code> that maps the elements/values in parallel.
     * It is implemented by <i>first partitioning this sequence into sub-sequences based on the number of available logical processors</i>.
     * These sub-sequences are then sequentially transformed from sequences of <code>T</code>-typed elements/values into sequences of <code>Promise&lt;T&gt;</code>-typed elements/values,
     * to which the mapping function is applied.
     * Then all promises are evaluated simultaneously (in their own native threads), and then finally sequentially folded to <code>V</code>-typed elements/values,
     * and added to a new <code>Sequence&lt;V&gt;</code>.
     * </p>
     *
     * <p>
     * Needless to say; This method brings some overhead.
     * It uses the common static {@link ForkJoinPool} (via the {@link Promise}'s {@link CompletableFuture},
     * so its efficiency depends on a lot of things:<br>
     * <ul>
     *     <li>the number of sequence elements/values</li>
     *     <li>the nature of the mapping function&mdash;somewhat heavy, (pure) computational functions are good, while blocking tasks may obstruct other tasks using the common Fork/Join thread pool (be careful with map functions which block the thread)</li>
     *     <li>the other activities in this JVM process and on this computer in general&mdash;CPUs are shared resources</li>
     * </ul>
     *
     * @param function The map function
     * @param <V>      The type of the codomain
     * @return the new/other functor
     * @see <a href="https://en.wikipedia.org/wiki/Hyper-threading">Logical cores (in hyper-threading)</a>
     */
    public <V> Sequence<V> parallelMap(Function<? super T, ? extends V> function) {
        return parallelMap(function, getRuntime().availableProcessors() - 2);
    }

    /**
     * Recursive mapping in parallel <i>with partitioning</i>.
     */
    protected <V> Sequence<V> parallelMap(Function<? super T, ? extends V> function, Integer partitionSize) {
        Sequence<V> mappedSequence = Sequence.empty();
        for (Sequence<T> subSequence : partition(partitionSize).values) {
            mappedSequence = mappedSequence.append(subSequence.unconstrainedParallelMap(function));
        }
        return mappedSequence;
    }

    /**
     * Recursive mapping in parallel <i>without partitioning</i>.
     */
    protected <V> Sequence<V> unconstrainedParallelMap(Function<? super T, ? extends V> function) {
        return
            map(
                (value) -> Promise
                    .of(value)
                    .map(function)
                    .evaluate()
            ).map(
                (Function<Promise<? extends V>, V>) (mappedValuePromise) ->
                    // NB! Blocks current thread while completing promise evaluation, one by one
                    mappedValuePromise.fold(
                        (exception) -> {
                            // Partial function handling I:
                            System.err.printf("NB! 'Sequence::parallelMap': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", exception.getMessage());
                            return null;
                        },
                        (mappedValue) -> {
                            // Partial function handling II:
                            if (mappedValue == null) {
                                System.err.printf("NB! 'Sequence::parallelMap': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", "'null'");
                                return null;
                            }
                            return mappedValue;
                        }
                    )
            ).filter(
                Objects::nonNull
            );
    }


    ///////////////////////////////////////////////////////
    // Applicative
    ///////////////////////////////////////////////////////

    //public Sequence<T> pure() {
    //    return new Sequence<>();
    //}

    @Override
    public Sequence<T> pure(T value) {
        return new Sequence<>(value);
    }

    //public Sequence<T> pure(T... values) {
    //    return new Sequence<>(values);
    //}

    //public Sequence<T> pure(Iterable<? extends T> iterable) {
    //    return new Sequence<>(iterable);
    //}


    // TODO: Add use case examples for sequence as applicative functor
    @Override
    public <V> Sequence<V> apply(Applicative<Function<? super T, ? extends V>> functionInContext) {
        Arguments.requireNotNull(functionInContext, "'functionInContext' argument cannot be null");

        // TODO: May throw 'ClassCastException'! (See inherited JavaDoc) Any chance of mitigating this - with Java's type system? (Lacking higher kinded types)
        Sequence<Function<? super T, ? extends V>> functionInSequence =
            (Sequence<Function<? super T, ? extends V>>) functionInContext;

        Function<? super T, ? extends V> function = functionInSequence.values.get(0);

        List<V> mappedValues = new ArrayList<>(this.values.size());
        for (T value : this.values) {
            V mappedValue = function.apply(value);

            // Partial function handling:
            // NB! Skipping inclusion as mapped value has no representation in the codomain
            if (mappedValue == null) {
                System.err.printf("NB! 'Sequence::apply': Skipping mapping! Value has no representation in the codomain (%s)%n", value);
                continue;
            }

            mappedValues.add(mappedValue);
        }
        return new Sequence<>(mappedValues);
    }


    ///////////////////////////////////////////////////////
    // Monad
    ///////////////////////////////////////////////////////

    @Override
    public Sequence<T> join() {
        List<T> flattenedValues = new ArrayList<>(this.values.size());
        for (T element : this.values) {
            if (element instanceof Sequence) {
                Sequence<?> wrappedSequenceElement = ((Sequence<?>) element);
                // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
                // TODO: Well, also argue that this must be the case...
                List<? extends T> wrappedSequenceElementElements = (List<? extends T>) wrappedSequenceElement.values;

                flattenedValues.addAll(wrappedSequenceElementElements);

            } else {
                flattenedValues.add(element);
            }
        }
        return new Sequence<>(flattenedValues);
    }


    ///////////////////////////////////////////////////////
    // Filter
    ///////////////////////////////////////////////////////

    /**
     * Alias for <code>filter</code>.
     */
    public Sequence<T> select(Predicate<T> predicate) {
        return filter(predicate);
    }

    /**
     * Alias of <code>filter</code>.
     */
    public Sequence<T> find(Predicate<T> predicate) {
        return filter(predicate);
    }

    /**
     * Alias of <code>filter</code>.
     */
    public Sequence<T> keep(Predicate<T> predicate) {
        return filter(predicate);
    }

    /**
     * "Plain functionally" (Haskell-style), 'filter' is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;filter :: (a -&gt; Boolean) -&gt; f a -&gt; f a
     * </code>
     * </p>
     *
     * <p>
     * <i>This means</i>:
     * The 'filter' function takes a predicate (a function which returns a boolean value), <code>a -&gt; Boolean</code>,
     * applies it a functor (typically with a container-like structure) of type <code>a</code>&mdash;returning
     * a <i>new functor of the same type containing all of the values from the original functor satisfying the <code>keepCondition</code> predicate</i>.
     * Do notice the phrase "values from the original functor".
     * One cannot include "new" values not already present in the input structure.
     * This is because there just isn't enough information to create a new value&mdash;the
     * only thing we know is that it is of type <code>a</code>.
     * This is an example of the strength of parametric polymorphism.
     * (The less we know about the type, the more we know about the implementation.)
     * </p>
     *
     * <p>
     * So, <code>filter</code> finds and returns all elements that satisfies the given predicate condition.
     * This projection operation is also known as 'find' and the relational 'select'.
     * </p>
     *
     * <p>
     * <code>filter</code> is a special case of <i>folding</i>.
     * It is implemented using <code>foldLeft</code> in this library:<br>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;foldl  &nbsp;:: (b &nbsp;&nbsp;-&gt; a -&gt; b&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;) -&gt; b &nbsp;&nbsp;-&gt; f a -&gt; b<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;filter :: (f a -&gt; a -&gt; Boolean) -&gt; f a -&gt; f a -&gt; f a &nbsp;&nbsp;&lt;=&gt;&nbsp;&nbsp; (a -&gt; Boolean) -&gt; f a -&gt; f a
     * </code>
     * </p>
     *
     * @param keepCondition The predicate function, acting like the "keep condition"
     * @return The new filtered sequence, empty if no elements satisfy the predicate condition
     */
    public Sequence<T> filter(Predicate<T> keepCondition) {
        Arguments.requireNotNull(keepCondition, "'keepCondition' argument cannot be null");
        return foldLeft(
            Sequence.empty(),
            (sequenceOfApprovedValues, value) -> keepCondition.test(value)
                ? sequenceOfApprovedValues.append(value)
                : sequenceOfApprovedValues
        );
    }

    /**
     * Complementary version of <code>filter</code>,
     * using the <code>predicate</code> parameter as the "remove condition".
     */
    public Sequence<T> remove(Predicate<T> removeCondition) {
        Arguments.requireNotNull(removeCondition, "'removeCondition' argument cannot be null");
        return foldLeft(
            Sequence.empty(),
            (sequenceOfApprovedValues, value) -> !removeCondition.test(value)
                ? sequenceOfApprovedValues.append(value)
                : sequenceOfApprovedValues
        );
    }


    ///////////////////////////////////////////////////////
    // Special case: Singleton
    ///////////////////////////////////////////////////////

    /**
     * Asserts this {@link Sequence} functor to be a <i>singleton</i>, and returns its single value,
     * otherwise throw a {@link IllegalStateException} (a bottom value).
     *
     * <p>
     * This is a very simple (and somewhat reckless and unforgiving) application of <code>fold</code>.
     * </p>
     *
     * @return this functor's value in case this is a 'Just'
     * @see <a href="https://en.wikipedia.org/wiki/Singleton_(mathematics)">Singleton (mathematics)</a>
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T trySingle() {
        return trySingle(IllegalStateException::new);
    }

    /**
     * Asserts this {@link Sequence} functor to be a <i>singleton</i>, and returns its single value,
     * otherwise throw the given exception (a bottom value).
     *
     * <p>
     * This is a very simple (and somewhat reckless and unforgiving) application of <code>fold</code>.
     * </p>
     *
     * @param onAssertionFailure To be thrown if the assertion is invalid
     * @return this functor's value in case this is a 'Just'
     * @see <a href="https://en.wikipedia.org/wiki/Singleton_(mathematics)">Singleton (mathematics)</a>
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T trySingle(Supplier<RuntimeException> onAssertionFailure) {
        return singleOr(() -> { throw onAssertionFailure.get(); });
    }


    /*
    public Maybe<T> single() {
        if (size() == 1) {
            return just(this.values.get(0));
        }
        System.err.printf("Sequence does not contain a single element, rather %d - returning Nothing%n", values.size());
        return nothing();
        // TODO: Include 'Unit' concept?
        / TODO: Try
        T val = singleOr(() -> (T) getUnit());
        return (isUnit(val))
            ? nothing()
            : just(val);
        /
    }
    */

    /**
     * Asserts this {@link Sequence} functor to be a <i>singleton</i>, and returns its single value,
     * otherwise throw a {@link IllegalStateException} (a bottom value).
     *
     * <p>
     * This is a very simple application of <code>fold</code>.
     * </p>
     *
     * @param onAssertionFailureDefaultValueSupplier Will be returned if the assertion is invalid
     * @return this functor's value in case this is a 'Just'
     * @see <a href="https://en.wikipedia.org/wiki/Singleton_(mathematics)">Singleton (mathematics)</a>
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type</a>
     */
    public T singleOr(Supplier<T> onAssertionFailureDefaultValueSupplier) {
        if (size() == 1) {
            return this.values.get(0);
        }
        return onAssertionFailureDefaultValueSupplier.get();
    }


    ///////////////////////////////////////////////////////
    // Fold
    ///////////////////////////////////////////////////////

    /**
     * Same-type folding via a given monoid.
     *
     * @param freeMonoid The same-type monoid to be used for folding this sequence
     * @return the folded value
     */
    public T foldLeft(FreeMonoid<T> freeMonoid) {
        return foldLeft(
            freeMonoid.identityElement,
            freeMonoid.binaryOperation
        );
    }

    /**
     * <code>foldLeft</code> variant with swapped parameters.
     */
    public <V> V foldLeft(BiFunction<V, ? super T, ? extends V> catamorphism, V identityValue) {
        return foldLeft(identityValue, catamorphism);
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
     * <i>This means</i>:
     * A binary function <code>b -&gt; a -&gt; b</code>,
     * together with an initial value of type <code>b</code>,
     * is applied to a functor <code>f</code> of type <code>a</code>,
     * returning a new value of type<code>b</code>.
     * </p>
     *
     * <p>
     * "Left fold"/"Fold-left" starts with the identity value,
     * and appends/adds the left-most (first) element in this sequence,
     * and then appends/adds the rest of the elements "going to the right".
     * Do notice that the <i>first</i> 'append' parameter acts as the accumulated value while folding.
     * </p>
     *
     * @param identityValue The identity value, acting as the initial value of this fold operation
     * @param catamorphism  The fold function
     * @param <V>           The type of the folded/returning value
     * @return the folded value
     */
    public <V> V foldLeft(V identityValue, BiFunction<V, ? super T, ? extends V> catamorphism) {
        V foldedValue = identityValue;
        for (T value : this.values) {
            foldedValue = catamorphism.apply(foldedValue, value);
        }
        return foldedValue;
    }

    /**
     * Same-type folding via a given monoid.
     *
     * @param freeMonoid The same-type monoid to be used for folding this sequence
     * @return the folded value
     */
    public T foldRight(FreeMonoid<T> freeMonoid) {
        return foldRight(freeMonoid.identityElement, freeMonoid.binaryOperation);
    }

    /**
     * <code>foldRight</code> variant with swapped parameters.
     */
    public <V> V foldRight(BiFunction<? super T, V, ? extends V> catamorphism, V identityValue) {
        return foldRight(identityValue, catamorphism);
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
     * "Plain functionally" (Haskell-style), "foldright" (<code>foldr</code>) is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;foldr :: (a -&gt; b -&gt; b) -&gt; b -&gt; f a -&gt; b
     * </code>
     * </p>
     *
     * <p>
     * <i>This means</i>:
     * A binary function <code>a -&gt; b -&gt; b</code>,
     * together with an initial value of type <code>b</code>,
     * is applied to a functor <code>f</code> of type <code>a</code>,
     * returning a new value of type<code>b</code>.
     * </p>
     *
     * <p>
     * "Right fold"/"Fold-right" starts with the identity value,
     * and appends/adds the right-most (first) element in this sequence,
     * and then appends/adds the rest of the elements "going to the left".
     * Do notice that the <i>second</i> 'append' parameter acts as the accumulated value while folding.
     * </p>
     *
     * @param identityValue The identity value, acting as the initial value of this fold operation
     * @param catamorphism  The fold function
     * @param <V>           The type of the folded/returning value
     * @return the folded value
     */
    public <V> V foldRight(V identityValue, BiFunction<? super T, V, ? extends V> catamorphism) {
        V foldedValue = identityValue;
        ListIterator<T> listIterator = this.values.listIterator(this.values.size());
        while (listIterator.hasPrevious()) {
            foldedValue = catamorphism.apply(listIterator.previous(), foldedValue);
        }
        return foldedValue;
    }

    /**
     * Folds in parallel.
     *
     * <p>
     * It is implemented by <i>first partitioning this associative set into sub-sets based on the number of available logical processors</i>.
     * These sub-sets are then sequentially transformed from sets of <code>T</code>-typed elements/values into sets of <code>Promise&lt;T&gt;</code>-typed element/value pairs,
     * to which the fold function is applied.
     * Then all promises are evaluated simultaneously (in their own native threads), recursively until only one value remains.
     * </p>
     *
     * <p>
     * Needless to say; This method brings some overhead.
     * It uses the common static {@link ForkJoinPool} (via the {@link Promise}'s {@link CompletableFuture},
     * so its efficiency depends on a lot of things:<br>
     * <ul>
     *     <li>the number of sequence elements/values</li>
     *     <li>the nature of the binary operation (append function)&mdash;somewhat heavy, (pure) computational functions are good, while blocking tasks may obstruct other tasks using the common Fork/Join thread pool (be careful with map functions which block the thread)</li>
     *     <li>the other activities in this JVM process and on this computer in general&mdash;CPUs are shared resources</li>
     * </ul>
     *
     * @param freeMonoid The free monoid to be used for folding this sequence
     * @return the folded value
     * @see <a href="https://en.wikipedia.org/wiki/Hyper-threading">Logical cores (in hyper-threading)</a>
     */
    public T parallelFold(FreeMonoid<T> freeMonoid) {
        Arguments.requireNotNull(freeMonoid, "'freeMonoid' argument cannot be null");

        return parallelFold(
            freeMonoid.binaryOperation,
            freeMonoid.identityElement,
            getDefaultPartitionSize()
        );
    }

    /**
     * Recursive folding in parallel <i><b>with</b> partitioning</i>.
     */
    protected T parallelFold(BinaryOperator<T> append, T identityValue, Integer partitionSize) {
        //System.out.printf("parallelFold (%s)%n", this);

        Arguments.requireNotNull(append, "'append' argument cannot be null");
        Arguments.requireNotNull(identityValue, "'identityValue' argument cannot be null");
        Arguments.requireNotNull(partitionSize, "'partitionSize' argument cannot be null");

        // Recursion guard 1: Empty sequence => identity value
        if (isEmpty()) {
            //System.out.println("parallelFold: Recursion guard 1: Empty sequence => identity value");
            return identityValue;
        }
        // Recursion guard 2: Singleton sequence
        if (size() < 2) {
            //System.out.println("parallelFold: Recursion guard 2: Singleton sequence");
            return this.values.get(0);
        }
        // Recursion guard 3: Single partition => no partitioning
        if (partitionSize < 2) {
            //System.out.println("parallelFold: Recursion guard 3: Single partition => no partitioning");
            return unconstrainedParallelFold(append, identityValue);
        }
        return partition(partitionSize)
            .map((subSequence) -> subSequence.unconstrainedParallelFold(append, identityValue))
            .parallelFold(append, identityValue, partitionSize);
    }

    /**
     * Recursive folding in parallel <i><b>without</b> partitioning</i>.
     */
    protected T unconstrainedParallelFold(BinaryOperator<T> append, T identityValue) {
        //System.out.printf("unconstrainedParallelFold (%s)%n", this);

        Arguments.requireNotNull(append, "'append' argument cannot be null");
        Arguments.requireNotNull(identityValue, "'identityValue' argument cannot be null");

        // Recursion guard 1: Empty sequence => identity value
        if (isEmpty()) {
            //System.out.println("unconstrainedParallelFold: parallelFold: Recursion guard 1: Empty sequence => identity value");
            return identityValue;
        }
        // Recursion guard 2: Singleton sequence
        if (size() < 2) {
            //System.out.println("unconstrainedParallelFold: Recursion guard 2: Singleton sequence");
            return this.values.get(0);
        }
        // Transform sequence of values to sequence of promises of pairs of elements
        // The provided append function (fold/catamorphism) are then immediately applied (in parallel)
        // (Threads are either fetched from the ForkJoinPool, or a new Thread is created per Promise evaluation)
        Sequence<Promise<T>> promiseSequence = Sequence.empty();
        Iterator<T> iterator = this.values.iterator();
        while (iterator.hasNext()) {
            T value1 = iterator.next();
            // TODO: Fails for some reason...
            //promiseSequence = promiseSequence.append(
            //    (iterator.hasNext())
            //        ? Promise.of(() -> append.apply(value1, iterator.next())).evaluate()
            //        : Promise.of(value1)
            //);
            T value2 = (iterator.hasNext())
                ? iterator.next()
                : null;

            Promise<T> promise = (value2 == null)
                // Resolved value
                ? Promise.of(value1)
                // Async computation
                : Promise.of(() -> append.apply(value1, value2));

            promiseSequence = promiseSequence.append(
                promise.evaluate()
            );
        }
        return promiseSequence
            .map(
                // NB! Blocks current thread while completing promise evaluation, one by one
                (promise) -> promise.fold(
                    // When bottom value:
                    (exception) -> {
                        // Partial function handling I:
                        System.err.printf("NB! 'Sequence::unconstrainedParallelFold': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", exception.getMessage());
                        return null;
                    },
                    // When successfully receiving the value:
                    (foldedValue) -> {
                        // Partial function handling II:
                        if (foldedValue == null) {
                            System.err.printf("NB! 'Sequence::unconstrainedParallelFold': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", "'null'");
                            return null;
                        }
                        return foldedValue;
                    }
                )
            )
            .unconstrainedParallelFold(append, identityValue);
    }


    ///////////////////////////////////////////////////////
    // Append
    ///////////////////////////////////////////////////////

    /**
     * @param iterable the values to be appended
     * @return a new sequence, consisting of this sequence's elements followed by the values in the given {@link Iterable} parameter
     */
    public Sequence<T> append(Iterable<T> iterable) {
        // TODO: Deliberately implemented with primary focus of readability/consistency, and not efficiency
        return append(new Sequence<>(iterable));
    }

    /**
     * @param value the value to be appended
     * @return a new sequence, consisting of this sequence's elements followed by the given value
     */
    public Sequence<T> append(T value) {
        // TODO: Deliberately implemented with primary focus of readability/consistency, and not efficiency
        return append(Sequence.of(value));
    }

    /**
     * @param sequence the sequence, which elements will be appended
     * @return a new sequence, consisting of this sequence's elements followed by the values in the given {@link Sequence} parameter
     */
    public Sequence<T> append(Sequence<T> sequence) {
        return append(this, sequence);
    }

    /**
     * @param sequence1 the original sequence
     * @param sequence2 a sequence, which elements will be appended to the first sequence parameter
     * @return a new sequence, consisting of the first sequence's elements followed by the second sequences' elements
     */
    public static <T> Sequence<T> append(Sequence<T> sequence1, Sequence<T> sequence2) {
        // TODO: Deliberately implemented with primary focus of readability/consistency, and not efficiency

        // TODO: Include 'Unit' concept?
        /*
        // NB! Removal of all unit/void values
        // => java.lang.StackOverflowError
        //sequence1 = sequence1.remove((value) -> value instanceof Unit);
        //sequence2 = sequence2.remove((value) -> value instanceof Unit);

        List<T> javaList1 = sequence1.toJavaList();
        javaList1.removeIf((value) -> value instanceof Unit);
        sequence1 = new Sequence<>(javaList1);

        List<T> javaList2 = sequence2.toJavaList();
        javaList2.removeIf((value) -> value instanceof Unit);
        sequence2 = new Sequence<>(javaList2);
        // /Removal of all unit/void values
        */

        List<T> appendedList = sequence1.toJavaList();
        appendedList.addAll(sequence2.values);

        return new Sequence<>(appendedList);
        /*
        List<T> javaList1 = sequence1.toJavaList();
        javaList1.removeIf((value) -> value instanceof Unit);
        sequence1 = new Sequence<>(javaList1);

        List<T> javaList2 = sequence2.toJavaList();
        javaList2.removeIf((value) -> value instanceof Unit);
        sequence2 = new Sequence<>(javaList2);

        javaList1.addAll(javaList2);

        return new Sequence<>(javaList1);
        */
    }


    ///////////////////////////////////////////////////////
    // Partition
    ///////////////////////////////////////////////////////

    /**
     * @return a hopefully suitable/optimized partition size based on the number of logical processors (hyper-threaded processor cores)
     */
    protected int getDefaultPartitionSize() {
        //System.out.printf("Number of elements in each partition: %d%n", numberOfElementsInEachPartition);
        return
            // Number of logical processors (hyper-threaded processor cores), minus two which is probably busy for other threads...
            (getRuntime().availableProcessors() - 2)
                // Monoid has binary operation => multiplied by 2
                * 2
                // Ad-hoc "spatial processing constant"
                // TODO: More ad-hoc testing; This constant is probably coupled with/affected by the binary operation's work load...
                * 4;
    }

    /**
     * Partition this promise based on the number of logical processors (hyper-threaded processor cores).
     *
     * @return the sequence chopped up in sub-sequences having the size of logical processors, minus two
     */
    protected Sequence<Sequence<T>> partition() {
        return partition(getDefaultPartitionSize());
    }

    /**
     * Partition this promise into sub-sequences having the having the given size.
     *
     * @param partitionSize The number of elements/values in each sub-sequence
     * @return the sequence chopped up in sub-sequences having the given size
     */
    protected Sequence<Sequence<T>> partition(Integer partitionSize) {
        Arguments.requireGreaterThanOrEqualTo(1, partitionSize, "'partitionSize' argument cannot be less than one");

        Sequence<Sequence<T>> partitionedSequence = null;
        Sequence<T> subSequence = null;

        int elementCounterWithinPartition = 0;

        // TODO: Efficiency: Investigate the possibility using 'System.arrayCopy' or something in that ballpark ("Spliterators" even maybe)
        for (T element : this.values) {
            if (elementCounterWithinPartition % partitionSize == 0) {
                partitionedSequence = (partitionedSequence == null)
                    ? Sequence.empty()
                    : partitionedSequence.append(subSequence);

                subSequence = Sequence.empty();
                elementCounterWithinPartition = 0;
            }
            if (elementCounterWithinPartition <= partitionSize) {
                subSequence = subSequence.append(element);
            }
            elementCounterWithinPartition += 1;
        }
        if (subSequence != null) {
            partitionedSequence = partitionedSequence.append(subSequence);
        }

        return (partitionedSequence == null)
            ? Sequence.empty()
            : partitionedSequence;
    }


    ///////////////////////////////////////////////////////
    // Transformations
    ///////////////////////////////////////////////////////

    /*
     * @return the internal data structure (NB! breaks 'Sequence' immutability)
     /
    protected List<T> _unsafe() {
        return this.values;
    }

    /
     * @return the internal data structure as an 'Iterator' (NB! breaks 'Sequence' immutability)
     /
    protected Iterator<T> _iterator() {
        return _unsafe().iterator();
    }
    */

    /**
     * @return a shallow copy of this sequence (of values)
     */
    public List<T> toJavaList() {
        return new ArrayList<>(this.values);
    }

    /*
     * <i><b>NB!</b> Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid.</i>
     *
     * @param freeMonoid free monoid (with a closed, associative binary operation and an identity element)
     * @return a new monoid structure based on this sequence's elements (as the monoid set), and the given free monoid
     * @deprecated Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid
     /
    @Deprecated // Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid
    public MonoidStructure<T> toMonoid(FreeMonoid<T> freeMonoid) {
        return toMonoid(
            freeMonoid.binaryOperation,
            freeMonoid.identityElement
        );
    }

    /
     * <i><b>NB!</b> Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid.</i>
     *
     * @param binaryOperation associative and closed binary operation
     * @param identityElement identity element
     * @return a new monoid based on this sequence's elements (as the monoid set), and the given operation and identity element
     * @deprecated Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid
     /
    @Deprecated // Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid
    public MonoidStructure<T> toMonoid(BinaryOperator<T> binaryOperation, T identityElement) {
        return toMonoid(
            identityElement,
            binaryOperation
        );
    }

    /
     * <code>toMonoid</code> variant with swapped parameters.
     *
     * @deprecated Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid
     /
    @Deprecated // Equal elements/values in this sequence will be omitted when transforming this sequence to a set-based monoid
    public MonoidStructure<T> toMonoid(T identityElement, BinaryOperator<T> binaryOperation) {
        return new MonoidStructure<>(
            new LinkedHashSet<>(this.values),
            binaryOperation,
            identityElement
        );
    }
    */


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
