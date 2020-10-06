package land.plainfunctional.algebraicstructure;

import java.util.Set;
import java.util.function.BinaryOperator;

import land.plainfunctional.util.Arguments;

import static java.util.Collections.emptySet;

/**
 * {@inheritDoc}
 *
 * <p>...</p>
 *
 * <p>
 * A <i>free</i> magma, 𝕄<sub>X</sub>, on a set X,
 * is the "most general possible" magma generated by X
 * (i.e., there are no relations or axioms imposed on the generators).
 * </p>
 *
 * <p>
 * When regarding <i>types as sets of values</i>,
 * one may treat types as (value) generators, predefined by the Java type system.<br>
 * So, a free magma, 𝕄<sub><code>T</code></sub>, for the (parametric/generic) type <code>T</code>,
 * is expressed as <code>FreeMagma&lt;T&gt;</code>.
 * </p>
 *
 * <p>...</p>
 *
 * <p>
 * <i>Disclaimer:</i><br>
 * Finding myself kind of on a limb when it comes to the theoretical concepts and terms regarding this algebraic <i>free</i> thingy...<br>
 * I think the use of it here is mostly viable... hope so.
 * </p>
 *
 * @param <T> The magma type
 * @see <a href="https://en.wikipedia.org/wiki/Magma_(algebra)#Free_magma">Free magma (Wikipedia)</a>
 */
public class FreeMagma<T> extends Magma<T> {

    public FreeMagma(BinaryOperator<T> closedBinaryOperation) {
        this(emptySet(), closedBinaryOperation);
    }

    public FreeMagma(Set<T> set, BinaryOperator<T> closedBinaryOperation) {
        super(set, closedBinaryOperation);
    }

    /**
     * Application of this free magma's operation, an <i>endofunction</i> for <code>T</code>.
     *
     * <p>
     * <i>NB! The totality property of this free magma is enforced by the compiler's type system.</i>
     * </p>
     *
     * @param element1 a magma element
     * @param element2 a magma element
     * @return a resulting magma element, or a bottom value if the result is not an element of this magma
     * @throws IllegalArgumentException if the element values are equal
     */
    @Override
    public T append(T element1, T element2) {
        Arguments.requireNotNull(element1, "'element1' argument cannot be 'null'");
        Arguments.requireNotNull(element2, "'element2' argument cannot be 'null'");

        if (element1.equals(element2)) {
            throw new IllegalArgumentException("Cannot append two equal element values in a magma");
        }

        return this.closedBinaryOperation.apply(element1, element2);
    }
}
