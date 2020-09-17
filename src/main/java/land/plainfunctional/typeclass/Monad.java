package land.plainfunctional.typeclass;

import java.util.function.Function;

/**
 * <p>
 * <i><b>Monads</b> are composable computation descriptions.</i>
 * </p>
 *
 * <p>
 * Monads are an extremely useful programming abstraction.
 * The essence of a monad is separation of composition timeline from the composed computation's execution timeline,
 * as well as the ability of computation to implicitly carry extra data,
 * as pertaining to the computation itself,
 * in addition to its one (hence the name) output,
 * that it will produce when run (or queried, or called upon).
 * This lends monads to supplementing pure calculations with features like I/O, common environment, updatable state, etc.
 * </p>
 *
 * <p>
 * A monad can be thought of as an abstract datatype of actions.
 * Monads can enforce strict sequential processing of many (possible asynchronous actions),
 * and control computational effects.
 * </p>
 *
 * <p>
 * If ignoring the mathematical roots of this concept, this Java interface would maybe have been called <code>Chainable</code>.
 * </p>
 *
 * @param <T> The type of the value inside the monadic context, or just, the monad type
 * @see <a href="https://en.wikipedia.org/wiki/Monad_(functional_programming)">Monad (Wikipedia)</a>
 */
public interface Monad<T> extends Applicative<T> {

    /**
     * "Flattens" a "layered"/"wrapped" monad.
     *
     * <p>
     * "Plain functionally" (Haskell-style), this function is defined as:
     * </p>
     *
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;join :: Monad m =&gt; m (m a) -&gt; m a
     * </code>
     * </p>
     *
     * @return the joined monad
     */
    Monad<T> join();

    /**
     * The monad function.
     *
     * <p>
     * "Plain functionally" (Haskell-style), the functor function is defined as:
     * </p>
     *
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;bind :: Monad m =&gt; (a -&gt; m b) -&gt; m a -&gt; m b
     * </code>
     * </p>
     *
     * <p>
     * We can easily see why this always will be a sequential execution&mdash;it is because the monadic function itself creates the next monad to be processed.
     * Monads are sometimes called "programmable semicolons".
     * </p>
     *
     * <p>
     * The name "bind" refers to the binding (composing) of computational steps/actions.
     * It is also known as <code>flatMap</code>/<code>flatmap</code>, <code>chain</code>, and <code>then</code>.
     * In C# LINQ it is named <code>SelectMany</code>.
     * </p>
     *
     * <p>
     * The default implementation of <code>bind</code> (generally speaking, and in this library) is <code>map(function).join()</code>,
     * as that can be derived from the type definitions.
     * </p>
     *
     * @param function The monad action
     * @param <V>      The type of the codomain
     * @param <M>      Covariant type of the new monad
     * @return the new monad
     */
    default <V, M extends Monad<V>> M bind(Function<? super T, M> function) {
        Monad<V> mapped = (Monad<V>) map(function);

        // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
        M mappedThenFlattened = (M) mapped.join();

        return mappedThenFlattened;
    }
}
