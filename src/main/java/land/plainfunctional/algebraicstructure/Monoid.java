package land.plainfunctional.algebraicstructure;

import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.function.BinaryOperator;

import land.plainfunctional.util.Arguments;

/**
 * A <b>monoid</b> is a <i>semigroup with an identity element</i>.
 *
 * <p>
 * <i>Formally:</i> A monoid is a set 𝕊 with a closed, and associative binary operation, •,
 * which has an <i>identity element</i> <i>e</i> defined as:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;∃(<i>e</i>) ∈ 𝕊 ⇒ ∀(𝓍) ∈ 𝕊 ⇒ <i>e</i> • x = x • <i>e</i> = x
 * </p>
 *
 * <p>
 * There exists an element <i>e</i> in 𝕊 such that for every element 𝓍 in 𝕊,
 * the equation <i>e</i> • x = x • <i>e</i> = x holds.
 * The element <i>e</i> is called an identity element.
 * </p>
 *
 * <p>
 * The identity element/identity value is also called "the neutral element".
 * </p>
 *
 * <p>
 * With the identity element and the associative operation, we have all we need for "collapsing the monoid";
 * Reducing all the elements into one single element&mdash;<i>folding</i>.
 * </p>
 *
 * @param <T> The monoid type, all elements in this monoid belongs to this type
 * @see <a href="https://en.wikipedia.org/wiki/Monoid">Monoid (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Identity_element">Identity element (Wikipedia)</a>
 */
public class Monoid<T> extends Semigroup<T> {

    /**
     * This monoid's identity element.
     */
    protected final T identityElement;

    /**
     * @param linkedHashSet   set of elements which preserves its element insertion order when iterated
     * @param binaryOperation associative and closed binary operation
     * @param identityElement identity element
     */
    public Monoid(
        LinkedHashSet<T> linkedHashSet,
        BinaryOperator<T> binaryOperation,
        T identityElement
    ) {
        super(linkedHashSet, binaryOperation);
        Arguments.requireNotNull(identityElement, "A monoid must have an identity element - a neutral element");
        this.identityElement = identityElement;
    }

    /**
     * @param sortedSet       set of elements which iteration order is defined by its 'Comparator' member
     * @param binaryOperation associative and closed binary operation
     * @param identityElement identity element
     */
    public Monoid(
        SortedSet<T> sortedSet,
        BinaryOperator<T> binaryOperation,
        T identityElement
    ) {
        super(sortedSet, binaryOperation);
        Arguments.requireNotNull(identityElement, "A monoid must have an identity element - a neutral element");
        this.identityElement = identityElement;
    }
}
