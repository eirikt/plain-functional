package land.plainfunctional.algebraicstructure;

import java.util.SortedSet;
import java.util.function.BinaryOperator;

/**
 * A semigroup is an <i>associative</i> magma.
 *
 * <p>
 * <i>Formally:</i> To qualify as a semigroup, the set 𝕊 and the binary operation • must be associative:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;∀(𝓍,𝓎,𝓏) ∈ 𝕊 ⇒ (𝓍•𝓎)•𝓏 = 𝓍•(𝓎•𝓏)
 * </p>
 *
 * <p>
 * Associativity means that all (applications of the binary) operations may be applied in whatever order,
 * as long as the order of the values do not change.
 * This requirement is enforced by the {@link SortedSet} constructor argument,
 * enumerating the semigroup elements via a the {@link SortedSet}'s mandatory {@link Comparable} member instance,
 * that may be set via one of the {@link SortedSet}'s constructors.
 * </p>
 *
 * @param <T> The semigroup type
 * @see <a href="https://en.wikipedia.org/wiki/Semigroup">Semigroup (Wikipedia)</a>
 */
public class Semigroup<T> extends Magma<T> {

    public Semigroup(
        SortedSet<T> set,
        BinaryOperator<T> associativeAndClosedBinaryOperation
    ) {
        super(set, associativeAndClosedBinaryOperation);
    }
}
