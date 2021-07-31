package land.plainfunctional.algebraicstructure;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BinaryOperator;

import land.plainfunctional.monad.Sequence;
import land.plainfunctional.util.Arguments;

/**
 * {@inheritDoc}
 *
 * <p>...</p>
 *
 * <p>
 * This class resembles the {@link Monoid} class.
 * The difference is that {@link Monoid} instances are real monoids,
 * where the values is strictly defined by the monoid's set elements.
 * </p>
 *
 * <p>
 * This class is a (somewhat arbitrarily) named free monoid
 * where a subset of the monoid is selected, e.g. for folding.<br>
 * NB! The monoid set is still (only) bounded by the type <code>T</code>.<br>
 * (Other suitable names for this class would maybe be
 * <code>FreeMonoidSelection</code>,
 * <code>FreeMonoidProjection</code>,
 * <code>FreeMonoidSubset</code>,
 * <code>CappedFreeMonoid</code>, or ...)
 * </p>
 *
 * @param <T> The monoid type, all values of this type belongs to the monoid
 * @see <a href="https://en.wikipedia.org/wiki/Monoid">Monoid (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Identity_element">Identity element (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Free_monoid">Free monoid (Wikipedia)</a>
 */
public class MonoidStructure<T> extends FreeMonoid<T> {

    /**
     * This monoid's enumerated set of elements.
     */
    protected final Set<T> set;

    /**
     * @param linkedHashSet   set of elements which preserves its element insertion order when iterated
     * @param binaryOperation associative and closed binary operation
     * @param identityElement identity element
     */
    public MonoidStructure(
        LinkedHashSet<T> linkedHashSet,
        BinaryOperator<T> binaryOperation,
        T identityElement
    ) {
        super(binaryOperation, identityElement);
        Arguments.requireNotNull(linkedHashSet, "The 'LinkedHashSet' argument cannot be null");
        this.set = linkedHashSet;
    }

    /**
     * @param sortedSet       set of elements which iteration order is defined by its 'Comparator' member
     * @param binaryOperation associative and closed binary operation
     * @param identityElement identity element
     */
    public MonoidStructure(
        SortedSet<T> sortedSet,
        BinaryOperator<T> binaryOperation,
        T identityElement
    ) {
        super(binaryOperation, identityElement);
        Arguments.requireNotNull(sortedSet, "The 'SortedSet' argument cannot be null");
        this.set = sortedSet;
    }

    /**
     * @return 'true' if this monoid's set contains no elements, otherwise 'false'
     */
    public boolean isEmpty() {
        return size() < 1;
    }

    /**
     * @return the number of elements in this monoid's set of values
     */
    public long size() {
        return this.set.size();
    }

    /**
     * To <i>fold</i> a value (e.g. a monoid) means creating a new representation of it.
     *
     * <p>
     * In abstract algebra, this is known as a <i>catamorphism</i>.
     * A catamorphism deconstructs (destroys) structures
     * in contrast to the <i>homomorphic</i> <i>preservation</i> of structures,
     * and <i>isomorphisms</i> where one can <i>resurrect</i> the originating structure.
     * </p>
     *
     * Using Haskell-style function signatures, fold is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;(a -&gt; a -&gt; a) -&gt; a -&gt; t a -&gt; a
     * </code>
     * </p>
     *
     * <p>
     * <i>This means</i>: A binary function <code>a -&gt; a -&gt; a</code>,
     * together with an initial value, also of type <code>a</code>,
     * is applied to a monoidal structure <code>t</code> of type <code>a</code>,
     * return+ing a new value of type <code>a</code>.
     * </p>
     *
     * <p>...</p>
     *
     * <p>
     * For the time being, this fold implementation is a "left fold"/"fold-left",
     * starting with the identity value, and appending/adding the left-most (first) element,
     * defined by this monoid's sorted set order -
     * and then appending/adding the rest of the elements "going to the right".
     * Do notice that the first 'append' parameter acts as the accumulated value while folding.
     * </p>
     *
     * @return the folded value
     */
    public T fold() {
        T foldedValue = this.identityElement;
        for (T value : this.set) {
            foldedValue = append(foldedValue, value);
        }
        return foldedValue;
    }

    /**
     * Folds this monoid in parallel.
     */
    public T parallelFold() {
        return toSequence().parallelFold(toFreeMonoid());
    }

    /*
     * Recursive folding in parallel <i>with partitioning</i>.
     /
    BiFunction<Integer, MonoidStructure<T>, T> PARALLEL_FOLD =
        y2(
            new Function<BiFunction<Integer, MonoidStructure<T>, T>, BiFunction<Integer, MonoidStructure<T>, T>>() {
                @Override
                public BiFunction<Integer, MonoidStructure<T>, T> apply(BiFunction<Integer, MonoidStructure<T>, T> biFunction) {
                    return new BiFunction<Integer, MonoidStructure<T>, T>() {
                        @Override
                        public T apply(Integer numberOfElementsInEachPartition, MonoidStructure<T> monoid) {
                            Arguments.requireNotNull(numberOfElementsInEachPartition, "Argument 'numberOfElementsInEachPartition' is mandatory");

                            // Recursion guards
                            if (monoid.isEmpty()) {
                                return monoid.identityElement;
                            }
                            if (monoid.size() < 2) {
                                return monoid.iterator().next();
                            }

                            Sequence<MonoidStructure<T>> seq = monoid.partition(numberOfElementsInEachPartition);

                            Sequence<T> seq2 = seq.map(MonoidStructure::unconstrainedExhaustiveParallelFold);

                            MonoidStructure<T> monoid2 = seq2.toMonoid(monoid.identityElement, monoid.binaryOperation);

                            //T foldedValue = monoid2.parallelFold(numberOfElementsInEachPartition);

                            //return foldedValue;
                            T foldedValue = biFunction.apply(numberOfElementsInEachPartition, monoid2);

                            return foldedValue;
                        }
                    };
                }
            }
        );
    */
    /*
    protected BiFunction<Integer, MonoidStructure<T>, T> parallelFold =
        y2((biFunction) -> (numberOfElementsInEachPartition, monoid) -> {

                Arguments.requireNotNull(numberOfElementsInEachPartition, "Argument 'numberOfElementsInEachPartition' is mandatory");

                // Recursion guards
                if (monoid.isEmpty()) {
                    return monoid.identityElement;
                }
                if (monoid.size() < 2) {
                    return monoid.iterator().next();
                }

                return biFunction.apply(
                    numberOfElementsInEachPartition,
                    monoid.partition(numberOfElementsInEachPartition)
                          .map(MonoidStructure::unconstrainedExhaustiveParallelFold)
                          .toMonoid(monoid.identityElement, monoid.binaryOperation)
                );

            }
        );
    */

    /*
     * Recursive folding in parallel <i>with partitioning</i>.
     /
    protected T parallelFold(Integer numberOfElementsInEachPartition) {
        return PARALLEL_FOLD.apply(numberOfElementsInEachPartition, this);
    }
    */

    /*
     * Recursive folding in parallel <i>with partitioning</i>.
     /
    protected T parallelFold(Integer numberOfElementsInEachPartition) {
        Arguments.requireNotNull(numberOfElementsInEachPartition, "Argument 'numberOfElementsInEachPartition' is mandatory");

        if (isEmpty()) {
            return this.identityElement;
        }
        if (this.set.size() < 2) {
            return this.set.iterator().next();
        }

        // NB! Blocks current thread
        /
        return partition(numberOfElementsInEachPartition)
            .map(MonoidStructure::unconstrainedExhaustiveParallelFold)
            .toMonoid(this.identityElement, this.binaryOperation)
            .parallelFold(numberOfElementsInEachPartition);
        /

        /
        Sequence<MonoidStructure<T>> seq = partition(numberOfElementsInEachPartition);
        Sequence<T> seq2 = seq.map(MonoidStructure::unconstrainedExhaustiveParallelFold);
        MonoidStructure<T> monoid2 = seq2.toMonoid(this.identityElement, this.binaryOperation);
        T foldedValue = monoid2.parallelFold(numberOfElementsInEachPartition);
        return foldedValue;
        /

        return toSequence().parallelFold(toFreeMonoid());
    }
    */

    /*
     * Recursive folding in parallel <i>without partitioning</i>.
     /
    protected T unconstrainedExhaustiveParallelFold() {
        if (isEmpty()) {
            return this.identityElement;
        }

        // Recursion guard guard
        if (this.set.size() < 2) {
            return this.set.iterator().next();
        }

        // Transform monoid set to promises of pairs of elements, which are appended
        Sequence<Promise<T>> promiseSequence = Sequence.empty();
        Iterator<T> iterator = this.set.iterator();
        while (iterator.hasNext()) {
            T element1 = iterator.next();
            if (!iterator.hasNext()) {
                promiseSequence = promiseSequence.append(Promise.of(element1));

            } else {
                T element2 = iterator.next();

                promiseSequence = promiseSequence.append(
                    Promise.of(() -> append(element1, element2)).evaluate()
                );
            }
        }

        /
        // Evaluate these promises one-by-one, then create a new monoid of the evaluated values and recursively fold these
        return promiseSequence
            .map(
                // NB! Blocks current thread while completing promise evaluation, one by one
                promise -> promise.fold(
                    (exception) -> {
                        // Partial function handling I:
                        System.err.printf("NB! 'MonoidStructure::unconstrainedExhaustiveParallelLeftFold': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", exception.getMessage());
                        return null;
                    },
                    (foldedValue) -> {
                        // Partial function handling II:
                        if (foldedValue == null) {
                            System.err.printf("NB! 'MonoidStructure::unconstrainedExhaustiveParallelLeftFold': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", "'null'");
                            return null;
                        }
                        return foldedValue;
                    }
                )
            )
            .toMonoid(this.identityElement, this.binaryOperation)
            .unconstrainedExhaustiveParallelFold();
        /
        Sequence<T> j = promiseSequence
            .map(
                // NB! Blocks current thread while completing promise evaluation, one by one
                promise -> promise.fold(
                    (exception) -> {
                        // Partial function handling I:
                        System.err.printf("NB! 'MonoidStructure::unconstrainedExhaustiveParallelLeftFold': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", exception.getMessage());
                        return null;
                    },
                    (foldedValue) -> {
                        // Partial function handling II:
                        if (foldedValue == null) {
                            System.err.printf("NB! 'MonoidStructure::unconstrainedExhaustiveParallelLeftFold': Skipping mapping! Bottom value has no representation in the codomain (%s)%n", "'null'");
                            return null;
                        }
                        return foldedValue;
                    }
                )
            );

        MonoidStructure<T> jj = j.toMonoid(this.identityElement, this.binaryOperation);

        return jj.unconstrainedExhaustiveParallelFold();
    }
    */


    ///////////////////////////////////////////////////////
    // Partition
    ///////////////////////////////////////////////////////

    /*
     * Partition this monoid structure (associative set) into sub-sets having the having the given size.
     *
     * @param partitionSize The number of elements/values in each sub-set
     * @return the monoid structure chopped up in sub-sequences having the given size
     /
    protected Sequence<MonoidStructure<T>> partition(Integer partitionSize) {
        Arguments.requireGreaterThanOrEqualTo(1, partitionSize, "'partitionSize' argument cannot be less than one");

        Sequence<MonoidStructure<T>> monoidPartitions = Sequence.empty();
        MonoidStructure<T> monoidPartition = null;

        int elementCounterWithinPartition = 0;

        // TODO: Efficiency: Investigate the possibility using 'System.arrayCopy' or something in that ballpark ("Spliterators" even maybe)
        for (T element : this.set) {
            if (elementCounterWithinPartition % partitionSize == 0) {
                if (monoidPartition != null) {
                    monoidPartitions = monoidPartitions.append(monoidPartition);
                }
                monoidPartition = new MonoidStructure<>(new LinkedHashSet<>(), this.binaryOperation, this.identityElement);
                elementCounterWithinPartition = 0;
            }
            if (elementCounterWithinPartition <= partitionSize) {
                monoidPartition.set.add(element);
            }
            elementCounterWithinPartition += 1;
        }

        if (monoidPartition != null) {
            monoidPartitions = monoidPartitions.append(monoidPartition);
        }

        return monoidPartitions;
    }
    */

    /**
     * @return This monoid's binary operation and identity element as a 'FreeMonoid'
     */
    protected FreeMonoid<T> toFreeMonoid() {
        return new FreeMonoid<>(this.binaryOperation, this.identityElement);
    }

    /**
     * @return This monoid's enumerated set as a 'Sequence'
     */
    protected Sequence<T> toSequence() {
        return Sequence.of(this.set);
    }
}
