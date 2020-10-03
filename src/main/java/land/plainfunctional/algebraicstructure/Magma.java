package land.plainfunctional.algebraicstructure;

import java.util.Set;
import java.util.function.BinaryOperator;

import land.plainfunctional.util.Arguments;

/**
 * In abstract algebra, a <b>magma</b> is a basic kind of <i>algebraic structure</i>.
 * A magma consists of a set of values equipped with a single binary operation that must be <i>closed</i> by definition.
 *
 * <p>
 * <i>Formally:</i> To qualify as a magma, the set 𝕊 and the binary operation • must satisfy the following requirement, known as the <i>magma-</i>, <i>totality-</i> or <i>closure axiom</i>:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;∀(𝓍,𝓎) ∈ 𝕊 ⇒ 𝓍•𝓎 ∈ 𝕊
 * </p>
 *
 * <p>
 * For all 𝓍,𝓎 ∈ 𝕊, the result of the operation 𝓍•𝓎 is (also) an element in 𝕊.
 * </p>
 *
 * <p>
 * Functions whose domain is equal to its codomain, are known as <i>endofunctions</i>.
 * (A homomorphic endofunction is an <i>endomorphism</i>, and is the usually the case).
 * </p>
 *
 * <p>
 * The magma operation can be regarded as <i>addition</i>,
 * and could have been named <code>add</code> (or <code>merge</code>).
 * <code>append</code> is chosen because it is the more generic term.
 * </p>
 *
 * @param <T> The magma type
 * @see <a href="https://en.wikipedia.org/wiki/Algebraic_structure">Algebraic structure (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Set_(mathematics)">Set (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Magma_(algebra)">Magma (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Endomorphism">Endomorphism (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Homomorphism">Homomorphism (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom values (Wikipedia)</a>
 */
public class Magma<T> {

    /**
     * This magma's set.
     */
    protected final Set<T> set;

    /**
     * This magma's binary operation.
     *
     * <p>
     * The totality/closure property which the binary operation also must inhibit,
     * is enforced via the single-parametric {@link BinaryOperator} class,
     * in addition to internal constraints defined in the <code>append</code> method.
     * </p>
     */
    protected final BinaryOperator<T> closedBinaryOperation;

    public Magma(Set<T> set, BinaryOperator<T> closedBinaryOperation) {
        Arguments.requireNotNull(set, "A magma must have a set of values");
        Arguments.requireNotNull(closedBinaryOperation, "A magma must have a closed binary operation");
        this.set = set;
        this.closedBinaryOperation = closedBinaryOperation;
    }

    /**
     * Application of this magma's operation, a constrained <i>endofunction</i>.
     *
     * <p>
     * <i>
     * NB! Partial method:<br>
     * The parameter values as well as the result must be actual elements of this magma.
     * </i><br>
     * Violations of this rule will result in a <i>bottom</i> value&mdash;here runtime exceptions.
     * </p>
     *
     * @param element1 a magma element
     * @param element2 a magma element
     * @return a resulting magma element, or a bottom value if the result is not an element of the magma
     * @throws IllegalArgumentException if one or both of the arguments are not elements of this magma
     * @throws IllegalStateException    if the result of the applied operation is not element of this magma
     */
    public T append(T element1, T element2) {
        Arguments.requireNotNull(element1, "'element1' argument cannot be 'null'");
        Arguments.requireNotNull(element2, "'element2' argument cannot be 'null'");

        if (this.set.parallelStream().noneMatch(element1::equals)) {
            throw new IllegalArgumentException("'element1' argument is not an element of this magma");
        }
        if (this.set.parallelStream().noneMatch(element2::equals)) {
            throw new IllegalArgumentException("'element2' argument is not an element of this magma");
        }

        T result = this.closedBinaryOperation.apply(element1, element2);

        if (this.set.parallelStream().noneMatch(result::equals)) {
            throw new IllegalStateException("The result of the applied binary operation is not an element of this magma");
        }

        return result;
    }
}
