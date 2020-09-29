package land.plainfunctional.algebraicstructure;

import java.util.Set;
import java.util.function.BinaryOperator;

import land.plainfunctional.util.Arguments;

/**
 * In abstract algebra, a <b>magma</b> is a basic kind of <i>algebraic structure</i>.
 * A magma consists of a set of values equipped with a single binary operation that must be <i>closed</i> by definition.
 *
 * <p>
 * <i>Formally:</i> To qualify as a magma, the set 𝕊 and the binary operation • must satisfy the following requirement, known as the <i>magma-</i> or <i>closure axiom</i>:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;∀(𝓍,𝓎) ∈ 𝕊 ⇒ 𝓍•𝓎 ∈ 𝕊
 * </p>
 *
 * <p>
 * For all 𝓍,𝓎 ∈ 𝕊, the result of the operation 𝓍•𝓎 is (also) an element in 𝕊.<br>
 * When regarding <i>types as sets of values</i>, one sees that this is equivalent with returning the same type.
 * Functions whose domain is equal to its codomain, are known as <i>endofunctions</i>.
 * (A homomorphic endofunction is an <i>endomorphism</i>, which is the usually the case).
 * </p>
 *
 * @param <T> The magma type
 * @see <a href="https://en.wikipedia.org/wiki/Algebraic_structure">Algebraic structure (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Set_(mathematics)">Set (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Magma_(algebra)">Magma (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Endomorphism">Endomorphism (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Homomorphism">Homomorphism (Wikipedia)</a>
 */
public class Magma<T> {

    /**
     * The magma set.
     */
    protected final Set<T> set;

    /**
     * The binary operation, making this set-of-values a magma.
     * The totality/closure property ("closed") which the binary operation also must inhibit,
     * is enforced via the {@link BinaryOperator} class.
     */
    protected final BinaryOperator<T> closedBinaryOperation;

    public Magma(Set<T> set, BinaryOperator<T> closedBinaryOperation) {
        Arguments.requireNotNull(set, "A 'Magma' instance must have a set of values");
        Arguments.requireNotNull(closedBinaryOperation, "A 'Magma' instance must have a closed binary operation");
        this.set = set;
        this.closedBinaryOperation = closedBinaryOperation;
    }

    /**
     * Application of this magma's operation.
     *
     * <p>
     * <i>NB! The arguments must be elements of this magma.</i>
     * </p>
     *
     * @throws IllegalArgumentException if one or both of the arguments are not elements of this magma
     */
    public T append(T value1, T value2) {
        Arguments.requireNotNull(value1, "'value1' argument cannot be 'null'");
        Arguments.requireNotNull(value2, "'value2' argument cannot be 'null'");
        if (!this.set.contains(value1)) {
            throw new IllegalArgumentException("'value1' argument is not an element of this magma");
        }
        if (!this.set.contains(value2)) {
            throw new IllegalArgumentException("'value2' argument is not an element of this magma");
        }
        return this.closedBinaryOperation.apply(value1, value2);
    }
}
