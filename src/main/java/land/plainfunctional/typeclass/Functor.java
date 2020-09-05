package land.plainfunctional.typeclass;

import java.util.function.Function;

/**
 * <p>
 * <i>A <b>functor</b> is a value/values in a context.</i>
 * The context may range from being structural (e.g. a container of some kind), to being semantical (e.g. "the value may or may not be present").
 * </p>
 * <p>
 * In mathematics, a <i>functor</i> is a type of mapping between categories arising in category theory.
 * Functors can be thought of as <i>homomorphisms between categories</i>.
 * </p>
 * <p>
 * In abstract algebra, a <i>homomorphism is a structure-preserving mapping between two algebraic structures of the same kind</i>.
 * </p>
 * <p>
 * If ignoring the mathematical roots of this concept, this Java interface would maybe have been called <code>Mappable</code>.
 * </p>
 *
 * @param <T> The type of the value in the context, or, the type of the functor value, or just, the functor type
 * @see <a href="https://en.wikipedia.org/wiki/Functor_(functional_programming)">Functor (Wikipedia)</a>
 * @see <a href="https://bartoszmilewski.com/2015/01/20/functors/">Functors (Category Theory for Programmers)</a>
 */
public interface Functor<T> {

    /**
     * The functor function.
     *
     * "Plain functionally" (Haskell-style), the functor function is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;map :: Functor f =&gt; (a -&gt; b) -&gt; f a -&gt; f b
     * </code>
     * </p>
     * <i>This means</i>: A function <code>a -&gt; b</code> is applied to a context of type f containing elements of type <code>a</code>, returning a container structure of the same type <code>f</code>containing elements of type <code>b</code>.<br>
     * <i>This reads</i>: The map function "is a member of" Functor type <code>f</code> "having the type constraint" of for an "f of a's",
     * and a function taking an "a" returning a "b",
     * it must return an "f of b's"&mdash;and all this is the definition of the "map" function.
     *
     * @param function The map function
     * @param <U>      The type of the codomain
     * @return the new/other functor
     */
    <U> Functor<U> map(Function<? super T, ? extends U> function);
}
