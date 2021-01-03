package land.plainfunctional.monad;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

import land.plainfunctional.typeclass.Applicative;
import land.plainfunctional.typeclass.Monad;
import land.plainfunctional.util.Arguments;
import land.plainfunctional.value.AbstractProtectedValue;

/**
 * <i>Functor context:</i>
 * <b>
 * The value is either of the two given types
 * </b><br>
 * The left-situated type is treated as the exceptional one,
 * so alternatively, the functor context may be interpreted as "a value, or something else".
 *
 * <p>
 * Haskell type definition:<br><br>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;data Either a b = Left a | Right b</code>
 * </p>
 *
 * <p>
 * Here {@link Either} is the <i>type constructor</i>,
 * while <code>Left</code> and <code>Right</code> are <i>data constructors</i> (also known as <i>value constructors</i>).
 * Both data constructors have a parametric type variable <code>a</code>,
 * making the {@link Either} monad a <code>polymorphic</code> type.
 * Instances of {@link Either} will either be a <code>Left</code> or a <code>Right</code> value.
 * {@link Either} is the arch-typical <i>sum type</i> (also known as <i>tagged union</i>, <i>disjoint union</i>, <i>variant</i>, <i>coproduct</i>).
 * </p>
 *
 * <p>
 * The semantics of the "left" type value is that it will ignore any function parameters (morphisms) and just pass the current state along.
 * The "left" type will always represent a terminal value, there is no escape from it.
 * </p>
 *
 * <p>
 * The {@link Either} monad is also known as <code>Result</code>,
 * where the data constructors are given more specific semantics ("failure" or "success").
 * </p>
 *
 * @param <L> The type of the left value
 * @param <R> The type of the right value
 * @see <a href="https://hackage.haskell.org/package/base-4.14.0.0/docs/Data-Either.html">Either (Haskell)</a>
 * @see <a href="https://wiki.haskell.org/Constructor">Haskell constructors</a>
 * @see <a href="https://en.wikipedia.org/wiki/Tagged_union">Sum types</a>
 */
public interface Either<L, R> extends Monad<R> {

    ///////////////////////////////////////////////////////////////////////////
    // Data constructors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <code>Left</code> data constructor.
     */
    static <L, R> Either<L, R> left(L left) {
        return new Either.Left<>(left);
    }

    /**
     * <code>Right</code> data constructor.
     */
    static <L, R> Either<L, R> right(R right) {
        return new Either.Right<>(right);
    }


    ///////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////

    /**
     * Predicate used for pattern matching.
     *
     * @return 'true' if this 'Either' instance is created by the 'left' data constructor, otherwise 'false'
     */
    boolean isLeft();

    /**
     * Predicate used for pattern matching.
     *
     * @return 'true' if this 'Either' instance is created by the 'right' data constructor, otherwise 'false'
     */
    boolean isRight();

    /**
     * @return the L-typed value if this 'Either' instance is created by the 'left' data constructor
     * @throws NoSuchElementException if this 'Either' instance is created by the 'right' data constructor
     */
    L tryGetLeft();

    /**
     * @return the R-typed value if this 'Either' instance is created by the 'right' data constructor
     * @throws NoSuchElementException if this 'Either' instance is created by the 'left' data constructor
     */
    R tryGet();

    /**
     * <p>
     * To <i>fold</i> a value means creating a new representation of it.
     * </p>
     *
     * <p>
     * In abstract algebra, this is known as a <i>catamorphism</i>.
     * A catamorphism deconstructs (destroys) data structures
     * in contrast to the <i>homomorphic</i> <i>preservation</i> of data structures,
     * and <i>isomorphisms</i> where one can <i>resurrect</i> the originating data structure.
     * </p>
     *
     * "Plain functionally" (Haskell-style), "foldleft" <code>foldl</code> is defined as:
     * <p>
     * <code>
     * &nbsp;&nbsp;&nbsp;&nbsp;foldl :: (b -&gt; a -&gt; b) -&gt; b -&gt; f a -&gt; b
     * </code>
     * </p>
     *
     * <p>
     * <i>This means</i>: A binary function <code>b -&gt; a -&gt; b</code>,
     * together with an initial value of type <code>b</code>,
     * is applied to a functor <code>f</code> of type <code>a</code>,
     * returning a new value of type <code>b</code>.
     * </p>
     *
     * <p>
     * As {@link Either} is a single-value functor, there is no need for a <i>binary</i> function;
     * It is replaced by an unary function, which transforms the single <code>Right</code> value of this {@link Either}.
     * Also, the need for an initial value is redundant;
     * It is replaced by a special "nullary" function in case this {@link Either} is a <code>Left</code>.
     * </p>
     *
     * @param onLeft  Supplier ("nullary" function/deferred constant) of the default value in case it is a 'Left'
     * @param onRight Function (unary) (the "catamorphism") to be applied to this functor's value in case it is a 'Right'
     * @param <V>     The type of the folded/returning value
     * @return the folded value
     */
    <V> V fold(
        Supplier<V> onLeft,
        Function<? super R, ? extends V> onRight
    );


    ///////////////////////////////////////////////////////////////////////////
    // Functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    <V> Either<L, V> map(Function<? super R, ? extends V> function);


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Override
    default Either<L, R> pure(R value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    default <V> Either<L, V> apply(Applicative<Function<? super R, ? extends V>> functionInContext) {
        Arguments.requireNotNull(functionInContext, "'functionInContext' argument cannot be null");

        // TODO: May throw 'ClassCastException'! (See inherited JavaDoc) Any chance of mitigating this - with Java's type system? (Lacking higher kinded types)
        Either<L, Function<? super R, ? extends V>> eitherFunctionInContext =
            (Either<L, Function<? super R, ? extends V>>) functionInContext;

        return eitherFunctionInContext.isLeft()
            // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
            // TODO: Well, also argue that this must be the case...
            ? (Either.Left<L, V>) tryGetLeft()
            // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
            // TODO: Well, also argue that this must be the case...
            : (Either.Right<L, V>) right(eitherFunctionInContext.tryGet().apply(tryGet()));
    }


    ///////////////////////////////////////////////////////////////////////////
    // Monad
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////
    // Implementations (sum types)
    ///////////////////////////////////////////////////////

    /**
     * The 'Left' data constructor.
     */
    class Left<L, R> extends AbstractProtectedValue<L> implements Either<L, R> {

        protected Left(L value) {
            super(value);
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public L tryGetLeft() {
            return this.value;
        }

        @Override
        public R tryGet() {
            throw new NoSuchElementException("Trying to get a 'Right' from a 'Left'");
        }

        @Override
        public <V> V fold(
            Supplier<V> defaultValueSupplier,
            Function<? super R, ? extends V> catamorphism
        ) {
            Arguments.requireNotNull(defaultValueSupplier, "'defaultValueSupplier' argument cannot be null");
            return defaultValueSupplier.get();
        }

        @Override
        public <V> Either<L, V> map(Function<? super R, ? extends V> function) {
            Arguments.requireNotNull(function, "'function' argument cannot be null");
            return left(this.value);
        }

        @Override
        public Either<L, R> join() {
            L left = this.value;
            return left instanceof Either<?, ?>
                // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
                // TODO: Well, also argue that this must be the case...
                ? (Either<L, R>) ((Either<?, ?>) left).join()
                : this;
        }
    }


    /**
     * The 'Right' data constructor.
     */
    class Right<L, R> extends AbstractProtectedValue<R> implements Either<L, R> {

        protected Right(R value) {
            super(value);
            Arguments.requireNotNull(value, "Cannot create an 'Either.Right' from a 'null' value");
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public L tryGetLeft() {
            throw new NoSuchElementException("Trying to get a 'Left' from a 'Right'");
        }

        @Override
        public R tryGet() {
            return this.value;
        }

        @Override
        public <V> V fold(
            Supplier<V> defaultValueSupplier,
            Function<? super R, ? extends V> catamorphism
        ) {
            Arguments.requireNotNull(catamorphism, "'catamorphism' argument cannot be null");
            return catamorphism.apply(this.value);
        }

        @Override
        public <V> Either<L, V> map(Function<? super R, ? extends V> function) {
            Arguments.requireNotNull(function, "'function' argument cannot be null");
            // Implementing homomorphism using a catamorphism.
            // Because here, the "catamorphism" is accidentally equal to a regular homomorphism...
            return right(
                fold(
                    () -> { throw new IllegalStateException(); },
                    function
                )
            );
        }

        @Override
        public Either<?, R> join() {
            R right = this.value;
            return right instanceof Either<?, ?>
                // TODO: Verify type casting validity with tests, then mark with @SuppressWarnings("unchecked")
                // TODO: Well, also argue that this must be the case...
                ? (Either<?, R>) ((Either<?, ?>) right).join()
                : this;
        }
    }
}
