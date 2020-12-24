package land.plainfunctional.typeclass;

import java.util.function.Function;

/**
 * <p>
 * <i>An <b>applicative functor</b> adds new capabilities to a functor.</i>
 * </p>
 *
 * <p>
 * Applicative functors (also known as just <i>applicatives</i>) define a way to inject values into a functor context (with <code>pure</code>).
 * Also, it make it possible to compose functors (with <code>apply</code>).
 * </p>
 *
 * @param <T> The type of the value context, or just, the functor type
 * @see <a href="https://softwaremill.com/applicative-functor/">The underrated applicative functor</a>
 */
public interface Applicative<T> extends Functor<T> {

    /**
     * "Injects" a value into the functor context.
     * <p>
     * Other terms used for this operation are <i>wrapping</i>, <i>lifting</i>, <i>elevating</i>, and (just) <i>putting</i> a value (into the functor context).
     * </p>
     *
     * "Plain functionally" (Haskell-style), this "wrapper" function is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;pure :: Applicative f =&gt; a -&gt; f a
     * </code>
     * </p>
     *
     * @param value The value to be injected into this applicative functor
     * @return the new applicative functor
     */
    Applicative<T> pure(T value);

    /**
     * <p>
     * Apply a function to the functor.
     * </p>
     *
     * <p>
     * "Plain functionally" (Haskell-style), the applicative functor function is defined as:
     * </p>
     *
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;apply :: Applicative f =&gt; f (a -&gt; b) -&gt; f a -&gt; f b
     * </code>
     * </p>
     *
     * <p>
     * <i>This means</i>: A function <code>a -&gt; b</code> <i>already <b>in</b> a functor context</i> of type <code>f</code>,
     * is applied to a context of the same type containing elements of type <code>a</code>,
     * returning a container structure of the same type <code>f</code>containing elements of type <code>b</code>.
     * </p>
     *
     * <p>
     * <i>Observation</i>:<br>
     * <code>apply</code> feels like an <code>append</code> function of algebraic structures.
     * While <code>map</code> is suitable for composing unary functions transforming the functor value,
     * <code>apply</code> seems to be suitable for composing (curried) binary functions.
     * This bridges a gap between typeclasses (e.g. Functors and Monads) and algebraic structures (e.g. Sets, Semigroups, Monoids, Groups, Rings, Lattices).
     * For algebraic structures inhibiting the <i>totality</i> property (e.g. Semigroups and Monoids), the latter requires <i>endofunctors</i> (<code>map :: Endofunctor f =&gt; (a -&gt; a) -&gt; f a -&gt; f a</code>).
     * </p>
     *
     * @param functionInContext The (possibly curried and applied) function already in this functor context
     * @param <V>               The type of the codomain, hence the new value context, or just, the new functor type
     * @return the new applicative functor
     * @throws ClassCastException NB! If the type <code>A</code> is a "sibling type" of this applicative functor instance rather than the same or an extension of it (e.g. a <code>Maybe</code> applied to a <code>Sequence</code>)
     * @see <a href="https://en.wikipedia.org/wiki/Algebraic_structure">Algebraic structures</a>
     * @see <a href="https://en.wikipedia.org/wiki/Magma_(algebra)">Magma (algebraic structure)</a>
     */
    <V> Applicative<V> apply(Applicative<Function<? super T, ? extends V>> functionInContext);
}
