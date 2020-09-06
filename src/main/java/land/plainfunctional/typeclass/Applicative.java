package land.plainfunctional.typeclass;

import java.util.function.Function;

/**
 * <p>
 * <i>An <b>applicative functor</b> adds new capabilities to a functor.</i>
 * Applicative functors (also known as just <i>applicatives</i>) define a way to put values into a functor context (<code>pure</code>).
 * Also, it make it possible to compose functors (<code>apply</code>).
 * </p>
 *
 * @param <T> The type of the value in the context, or, the type of the functor value, or just, the functor type
 * @see <a href="https://softwaremill.com/applicative-functor/">The underrated applicative functor</a>
 */
public interface Applicative<T> extends Functor<T> {

    /**
     * Puts a value into the functor context.
     * <p>
     * Other terms used for this operation are <i>wrapping</i>, <i>lifting</i>, and <i>elevating</i> a value (into the functor context).
     * </p>
     * "Plain functionally" (Haskell-style), this "wrapper" function is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;pure :: Applicative f =&gt; a -&gt; f a
     * </code>
     * </p>
     *
     * @param value The value to be put into this applicative functor
     * @return the new applicative functor
     */
    Functor<T> pure(T value);

    /**
     * "Plain functionally" (Haskell-style), the applicative functor function is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;apply :: Applicative f =&gt; f (a -&gt; b) -&gt; f a -&gt; f b
     * </code>
     * </p>
     *
     * <i>This means</i>: A function <code>a -&gt; b</code> <i>already <b>in</b> a functor context</i> of type <code>f</code>,
     * is applied to a context of the same type containing elements of type <code>a</code>,
     * returning a container structure of the same type <code>f</code>containing elements of type <code>b</code>.
     *
     * @param functionInContext The (possibly curried and applied) function already in this functor context
     * @return the new applicative functor
     */
    <U> Functor<U> apply(Functor<Function<T, U>> functionInContext);
}
