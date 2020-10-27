package land.plainfunctional.algebraicstructure;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BinaryOperator;

import land.plainfunctional.util.Arguments;

/**
 * A <b>semigroup</b> is an <i>associative magma</i>.
 *
 * <p>
 * <i>Formally:</i> To qualify as a semigroup, the set 𝕊 and the binary operation • must be associative:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;∀(𝓍,𝓎,𝓏) ∈ 𝕊 ⇒ (𝓍•𝓎)•𝓏 = 𝓍•(𝓎•𝓏)
 * </p>
 *
 * <p>
 * Associativity means that all (applications of the binary) operations may be applied in whatever order,
 * as long as the order of the values do not change.
 * This requirement is enforced by the {@link LinkedHashSet} constructor argument,
 * which preserves the insertion order when it is iterated.
 * For other ordering schemes, the requirement is enforced by the {@link SortedSet} constructor argument,
 * enumerating the semigroup elements via a the {@link SortedSet}'s mandatory {@link Comparator} member instance,
 * which may be set via one of the {@link SortedSet}'s constructors.
 * </p>
 *
 * @param <T> The semigroup type, all elements in this semigroup belongs to this type
 * @see <a href="https://en.wikipedia.org/wiki/Semigroup">Semigroup (Wikipedia)</a>
 */
public class Semigroup<T> extends Magma<T> {

    /**
     * @param linkedHashSet   set of elements which preserves its element insertion order when iterated
     * @param binaryOperation associative and closed binary operation
     */
    public Semigroup(
        LinkedHashSet<T> linkedHashSet,
        BinaryOperator<T> binaryOperation
    ) {
        // Creates a shallow copy of the provided 'LinkedHashSet' argument
        super(new LinkedHashSet<>(linkedHashSet), binaryOperation);
    }

    /**
     * @param sortedSet       set of elements which iteration order is defined by its 'Comparator' member
     * @param binaryOperation associative and closed binary operation
     */
    public Semigroup(
        SortedSet<T> sortedSet,
        BinaryOperator<T> binaryOperation
    ) {
        // Creates a shallow copy of the provided 'SortedSet' argument
        super(new ConcurrentSkipListSet<>(sortedSet), binaryOperation);
    }

    /**
     * By providing a identity element, this semigroup can be promoted to a {@link Monoid} instance.
     *
     * @param identityElement the monoid's identity element
     * @return a new 'Monoid' instance
     */
    public Monoid<T> toMonoid(T identityElement) {
        return new Monoid<>((SortedSet<T>) this.set, this.binaryOperation, identityElement);
    }
}
