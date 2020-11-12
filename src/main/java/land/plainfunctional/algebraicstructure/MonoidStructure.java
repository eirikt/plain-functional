package land.plainfunctional.algebraicstructure;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BinaryOperator;

import land.plainfunctional.util.Arguments;

/**
 * {@inheritDoc}
 *
 * <p>...</p>
 *
 * <p>
 * This class is a (somewhat arbitrarily) named free monoid
 * where a subset of the monoid is selected, e.g. for folding.<br>
 * NB! The monoid set is still (only) bounded by the type <code>T</code>.<br>
 * (Other suitable names for this class would maybe be
 * <code>FreeMonoidSelection</code>,
 * <code>FreeMonoidProjection</code>, or
 * <code>FreeMonoidSubset</code>.)
 * </p>
 *
 * @param <T> The monoid type, all values of this type belongs to the monoid
 * @see <a href="https://en.wikipedia.org/wiki/Monoid">Monoid (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Identity_element">Identity element (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Free_monoid">Free monoid (Wikipedia)</a>
 */
public class MonoidStructure<T> extends FreeMonoid<T> {

    /**
     * This monoid's set of elements.
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
        Arguments.requireNotNull(linkedHashSet, "The 'LinkedHashSet' argument cannot be 'null'");
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
        Arguments.requireNotNull(sortedSet, "The 'SortedSet' argument cannot be 'null'");
        this.set = sortedSet;
    }

    /**
     * <p>
     * To <i>fold</i> a value (e.g. a monoid) means creating a new representation of it.
     * </p>
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
     * &nbsp;&nbsp;&nbsp;&nbsp;(a -&gt; a -&gt; a) -&gt; a -&gt; s a -&gt; a
     * </code>
     * </p>
     *
     * <p>
     * <i>This means</i>: A binary function <code>a -&gt; a -&gt; a</code>,
     * together with an initial value, also of type <code>a</code>,
     * is applied to a monoidal structure <code>s</code> of type <code>a</code>,
     * returning a new value of type <code>a</code>.
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
}
