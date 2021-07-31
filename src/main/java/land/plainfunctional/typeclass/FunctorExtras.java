package land.plainfunctional.typeclass;

import java.util.function.Consumer;
import java.util.function.Function;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.monad.Either;
import land.plainfunctional.monad.Maybe;
import land.plainfunctional.monad.Sequence;

/**
 * <i>Functor add-on functions</i>.
 *
 * <p>
 * These are not "official" {@link Functor}/{@link Applicative}/{@link Monad} functions.
 * All functors/applicatives/monads should implement these functions, but it is not mandatory.
 * </p>
 *
 * @param <T> The functor type
 */
public interface FunctorExtras<T> {

    /**
     * Partial mapping, compensated with a default value to be mapped if the functor value is a bottom value.
     *
     * @param function     The map function
     * @param defaultValue The default value to map if the functor value is a bottom value
     * @param <V>          The type of the codomain
     * @return the new/other functor
     * @deprecated Prefer handling partiality with the  {@link Maybe} or {@link Either} functor
     */
    @Deprecated
    <V>
    Functor<V> map(
        Function<? super T, ? extends V> function,
        T defaultValue
    );

    /**
     * Parallel mapping, and then folding the codomain values, before putting it into a new functor.
     *
     * @param functionSequence The enumerated map functions
     * @param <V>              The type of the codomain
     * @param monoid           The monoid to be used for folding the mapped values
     * @return the new/other functor, containing a {@link Maybe} value of the mapped and the folded codomain values
     */
    <V>
    Functor<Maybe<V>> map(
        Sequence<Function<? super T, ? extends V>> functionSequence,
        FreeMonoid<V> monoid
    );

    /**
     * Parallel mapping, and then folding the codomain values.
     *
     * @param functionSequence The enumerated map functions
     * @param <V>              The type of the codomain
     * @param monoid           The monoid to be used for folding the mapped values
     * @return the new/other functor, containing a {@link Maybe} value of the mapped and the folded codomain values
     */
    <V>
    Maybe<V> mapFold(
        Sequence<Function<? super T, ? extends V>> functionSequence,
        FreeMonoid<V> monoid
    );

    /**
     * Perform a (side) effect based on the functor value/values, if applicable.
     * This effect may be blocking/synchronous or non-blocking/asynchronous, depending on the implementation.
     *
     * <p>
     * The functor state should not be affected.
     * </p>
     *
     * @param effect The (side) effect
     * @return this same (unmodified) {@link Functor} instance
     */
    Functor<T> effect(Consumer<? super T> effect);
}
