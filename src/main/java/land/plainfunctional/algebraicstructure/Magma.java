package land.plainfunctional.algebraicstructure;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BinaryOperator;

import land.plainfunctional.util.Arguments;

/**
 * In abstract algebra, a <b>magma</b> is a basic kind of <i>algebraic structure</i>.
 * A magma consists of a set of values equipped with a single binary operation that must by definition be <i>closed</i>.
 *
 * <p>
 * <i>Formally:</i> To qualify as a magma, the set 𝕊 and the binary operation, •, must satisfy the following requirement, known as the <i>magma-</i>, <i>totality-</i> or <i>closure axiom</i>:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;∀(𝓍,𝓎) ∈ 𝕊 ⇒ 𝓍 • 𝓎 ∈ 𝕊
 * </p>
 *
 * <p>
 * For all 𝓍,𝓎 ∈ 𝕊, the result of the operation 𝓍 • 𝓎 is (also) an element in 𝕊.
 * </p>
 *
 * <p>
 * Functions whose domain is equal to its codomain, are known as <i>endofunctions</i>.
 * (A homomorphic endofunction is an <i>endomorphism</i>, and that is mostly the case.)
 * </p>
 *
 * <p>
 * The magma operation can be regarded as <i>addition</i>,
 * and could have been named <code>add</code> (or <code>merge</code>).
 * <code>append</code> is chosen because it is the more generic term.
 * When regarding <i>types as sets of values</i>,
 * a magma is like a user-defined subtype with a way to add values of that type together.
 * </p>
 *
 * @param <T> The magma type, all elements in this magma belongs to this type
 * @see <a href="https://en.wikipedia.org/wiki/Algebraic_structure">Algebraic structure (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Set_(mathematics)">Set (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Magma_(algebra)">Magma (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Endomorphism">Endomorphism (Wikipedia)</a>
 * @see <a href="https://en.wikipedia.org/wiki/Homomorphism">Homomorphism (Wikipedia)</a>
 */
public class Magma<T> {

    /**
     * This magma's set of elements.
     */
    protected final Set<T> set;

    /**
     * This magma's binary operation.
     *
     * <p>
     * The totality/closure property which the binary operation also must inhibit,
     * is enforced via the single-parametric {@link BinaryOperator} class,
     * in addition to constraints defined in the <code>append</code> method.
     * </p>
     */
    protected final BinaryOperator<T> binaryOperation;

    /**
     * @param set             set of elements
     * @param binaryOperation closed binary operation
     */
    public Magma(Set<T> set, BinaryOperator<T> binaryOperation) {
        Arguments.requireNotNull(set, "A magma must have a set of values");
        Arguments.requireNotNull(binaryOperation, "A magma must have a closed binary operation");
        if (set instanceof SortedSet || set instanceof LinkedHashSet) {
            this.set = set;
        } else {
            // Creates a shallow copy of the provided 'Set' argument
            this.set = new HashSet<>(set);
        }
        this.binaryOperation = binaryOperation;
    }

    /**
     * Application of this magma's operation, •<br>
     * This is an <i>endofunction</i>/<i>endomorphism</i>.
     *
     * <p>
     * <i>
     * NB! Partial method:<br>
     * The parameter values as well as the result must be actual elements of this magma.
     * </i><br>
     * Violations of this rule will result in a <i>bottom</i> value&mdash;here <code>null</code> or runtime exceptions.
     * </p>
     *
     * @param element1 a magma element
     * @param element2 a magma element
     * @return a resulting magma element, or a bottom value if one of the arguments or the result is not an element of this monoid
     * @throws IllegalArgumentException if one or both of the arguments are not elements of this magma
     * @throws IllegalStateException    if the result of the applied operation is not an element of this magma
     * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom values (Wikipedia)</a>
     */
    public T append(T element1, T element2) {
        Arguments.requireNotNull(element1, "'element1' argument cannot be 'null'");
        Arguments.requireNotNull(element2, "'element2' argument cannot be 'null'");

        if (element1.equals(element2)) {
            throw new IllegalArgumentException("Cannot append two equal element values in a magma");
        }

        if (this.set.parallelStream().noneMatch(element1::equals)) {
            throw new IllegalArgumentException("'element1' argument is not an element of this magma");
        }
        if (this.set.parallelStream().noneMatch(element2::equals)) {
            throw new IllegalArgumentException("'element2' argument is not an element of this magma");
        }

        T result = this.binaryOperation.apply(element1, element2);

        if (this.set.parallelStream().noneMatch(result::equals)) {
            throw new IllegalStateException("The result of the applied binary operation is not an element of this magma");
        }

        return result;
    }
}
