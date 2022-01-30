package land.plainfunctional.typeclass;

import java.util.function.Function;

import land.plainfunctional.util.Arguments;

/**
 * <p>
 * <i>A <b>monad</b> is a composable computation description.</i>
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
 * Monads can enforce strict sequential processing of many (possible asynchronous) actions,
 * and control computational (side) effects.
 * </p>
 *
 * <p>
 * Another name for this Java interface could maybe have been <code>Chainable</code>.
 * </p>
 *
 * @param <T> The type of the value inside the monadic context, or just, the monad type
 * @see <a href="https://en.wikipedia.org/wiki/Monad_(functional_programming)">Monad (Wikipedia)</a>
 */
public interface Monad<T> extends Applicative<T> {

    /**
     * Alias of <code>bind</code>.
     */
    default <V, M extends Monad<V>>
    M then(Function<? super T, M> function) {
        return bind(function);
    }

    // TODO: Keep until a couple of more monads have been created...
    //@param <M>      Covariant type of the new monad
    //@param <M>      Covariant type of a function in a (monad) context
    //@throws ClassCastException If the type <code>M</code> of the new monad is not the same type as the calling monad
    //@Deprecated default <V, M extends Monad<V>> M OLD_bind(Function<? super T, M> function) {
    //Monad<V> mapped = (Monad<V>) map(function);
    //M mappedThenFlattened = (M) mapped.join();
    //return mappedThenFlattened;
    //}

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
     * The default implementation of <code>bind</code> (generally speaking, and in this library) is <i>join ∘ map</i> ⇔ <code>map(function).join()</code>,
     * as that can be derived from the type definitions.
     * </p>
     *
     * @param function The monad action
     * @param <V>      The type of the codomain / the new functor type
     * @param <M>      Monad-covariant type (of the new monad, or just, the new value context)
     * @return the new monad
     */
    //@SuppressWarnings("unchecked")
    default <V, M extends Monad<V>>
    M bind(Function<? super T, M> function) {
        Arguments.requireNotNull(function, "'function' argument cannot be null");

        // TODO: Try:
        //Functor<M> mappedMonad = map(function);
        //return (M) ((M) mappedMonad).join();

        Functor<Monad<V>> mappedMonad = map(function);
        Monad<V> layeredMonad = (Monad<V>) mappedMonad;

        // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
        return (M) layeredMonad.join();
    }

    /**
     * "Flattens"/"collapses" a "layered"/"wrapped" monad.
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
     * <p>
     * If not documented otherwise, the function will flatten the data structure recursively&mdash;
     * meaning that the monad layers may be arbitrary deep.
     * </p>
     *
     * @return the joined monad
     */
    Monad<T> join();
}
