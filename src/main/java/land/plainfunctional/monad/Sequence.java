package land.plainfunctional.monad;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.algebraicstructure.MonoidStructure;
import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;

/**
 * <p>
 * <i>Functor context:</i>
 * <b>
 * Contains none, one, or many enumerated (possible duplicated) values
 * </b>
 * </p>
 *
 * <p>
 * Sequences is also known as <i>lists</i>.
 * ({@link Sequence} delegates to an {@link ArrayList} instance.)
 * </p>
 *
 * <p>
 * The contained values are also known as <i>elements</i> or <i>items</i> (of the sequence/list).
 * </p>
 *
 * @param <T> The type of the values in the sequence
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
        return asSequence(null);
    }

    /**
     * Just for having a typed {@link Sequence} instance to reach the member methods, e.g. <code>pure</code>.
     */
    public static <T> Sequence<T> asSequence(Class<T> type) {
        return empty();
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
        Arguments.requireNotNull(value, "'Sequence' cannot contain 'null' values");
        this.values.add(value);
    }

    // TODO: Possible heap pollution from parameterized vararg type (=> @SafeVarargs)
    // => https://www.baeldung.com/java-safevarargs
    protected Sequence(T... values) {
        this();
        for (T value : values) {
            Arguments.requireNotNull(value, "'Sequence' cannot contain 'null' values");
            this.values.add(value);
        }
    }

    protected Sequence(Iterable<? extends T> iterable) {
        this();
        for (T value : iterable) {
            Arguments.requireNotNull(value, "'Sequence' cannot contain 'null' values");
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
            V mappedValue = function.apply(value);

            // Partial function handling:
            // NB! Skipping inclusion as mapped value has no representation in the codomain
            if (mappedValue == null) {
                System.err.printf("NB! 'Sequence::map': Skipping mapping! Value has no representation in the codomain (%s)%n", value);
                continue;
            }

            mappedValues.add(mappedValue);
        }
        return new Sequence<>(mappedValues);
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
                List<? extends T> _unsafeWrappedElements = (List<? extends T>) wrappedSequenceElement._unsafe();

                flattenedValues.addAll(_unsafeWrappedElements);

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
     * Alias of <code>filter</code>.
     */
    public Sequence<T> select(Predicate<T> predicate) {
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
     * This value projection operation is also known as 'find' and the relational 'select'.
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
    // Fold
    ///////////////////////////////////////////////////////

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
     * @param monoid The same-type monoid used for folding this sequence
     * @return the folded value
     */
    public T foldLeft(FreeMonoid<T> monoid) {
        return foldLeft(
            monoid.identityElement,
            monoid.binaryOperation
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
    public <V> V foldLeft(BiFunction<V, ? super T, ? extends V> catamorphism, V identityValue) {
        return foldLeft(identityValue, catamorphism);
    }

    /**
     * <code>foldLeft</code> variant with swapped parameters.
     */
    public <V> V foldLeft(V identityValue, BiFunction<V, ? super T, ? extends V> catamorphism) {
        V foldedValue = identityValue;
        for (T value : this.values) {
            foldedValue = catamorphism.apply(foldedValue, value);
        }
        return foldedValue;
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
     * @param monoid The same-type monoid used for folding this sequence
     * @return the folded value
     */
    public T foldRight(FreeMonoid<T> monoid) {
        return foldRight(
            monoid.identityElement,
            monoid.binaryOperation
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
    public <V> V foldRight(BiFunction<? super T, V, ? extends V> catamorphism, V identityValue) {
        return foldRight(identityValue, catamorphism);
    }

    /**
     * <code>foldRight</code> variant with swapped parameters.
     */
    public <V> V foldRight(V identityValue, BiFunction<? super T, V, ? extends V> catamorphism) {
        V foldedValue = identityValue;
        ListIterator<T> listIterator = this.values.listIterator(this.values.size());
        while (listIterator.hasPrevious()) {
            foldedValue = catamorphism.apply(listIterator.previous(), foldedValue);
        }
        return foldedValue;
    }


    ///////////////////////////////////////////////////////
    // Append
    ///////////////////////////////////////////////////////

    /**
     * @param sequence1 the original sequence
     * @param sequence2 a sequence, which elements will be appended to the first sequence parameter
     * @return a new sequence, consisting of the first sequence's elements followed by the second sequences' elements
     */
    public static <T> Sequence<T> append(Sequence<T> sequence1, Sequence<T> sequence2) {
        List<T> appendedList = sequence1.toJavaList();
        appendedList.addAll(sequence2.values);

        return new Sequence<>(appendedList);
    }

    /**
     * @param sequence the original sequence
     * @param value    the value to be appended
     * @return a new sequence, consisting of the given sequence's elements followed by the given value
     */
    public static <T> Sequence<T> append(Sequence<T> sequence, T value) {
        List<T> appendedList = sequence.toJavaList();
        appendedList.add(value);

        return new Sequence<>(appendedList);
    }

    /**
     * @param sequence the sequence, which elements will be appended
     * @return a new sequence, consisting of this sequence's elements followed by the values in the given {@link Sequence} parameter
     */
    public Sequence<T> append(Sequence<T> sequence) {
        return append(this, sequence);
    }

    /**
     * @param iterable the values to be appended
     * @return a new sequence, consisting of this sequence's elements followed by the values in the given {@link Iterable} parameter
     */
    public Sequence<T> append(Iterable<T> iterable) {
        return append(new Sequence<>(iterable));
    }

    /**
     * @param value the value to be appended
     * @return a new sequence, consisting of this sequence's elements followed by the given value
     */
    public Sequence<T> append(T value) {
        return append(this, value);
    }


    ///////////////////////////////////////////////////////
    // Transformations
    ///////////////////////////////////////////////////////

    /**
     * @return the internal data structure
     */
    protected List<? extends T> _unsafe() {
        return this.values;
    }

    /**
     * @return a shallow copy of this sequence (of values)
     */
    public List<T> toJavaList() {
        return new ArrayList<>(this.values);
    }

    /**
     * @param freeMonoid free monoid (with a closed, associative binary operation and an identity element)
     * @return a new monoid structure based on this sequence's elements (as the monoid set), and the given free monoid
     */
    public MonoidStructure<T> toMonoid(FreeMonoid<T> freeMonoid) {
        return toMonoid(
            freeMonoid.binaryOperation,
            freeMonoid.identityElement
        );
    }

    /**
     * @param binaryOperation associative and closed binary operation
     * @param identityElement identity element
     * @return a new monoid based on this sequence's elements (as the monoid set), and the given operation and identity element
     */
    public MonoidStructure<T> toMonoid(BinaryOperator<T> binaryOperation, T identityElement) {
        return toMonoid(
            identityElement,
            binaryOperation
        );
    }

    /**
     * <code>toMonoid</code> variant with swapped parameters.
     */
    public MonoidStructure<T> toMonoid(T identityElement, BinaryOperator<T> binaryOperation) {
        return new MonoidStructure<>(
            new LinkedHashSet<>(this.values),
            //new LinkedHashSet<>(toJavaList()),
            binaryOperation,
            identityElement
        );
    }


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
