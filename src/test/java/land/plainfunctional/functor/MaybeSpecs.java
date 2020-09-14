package land.plainfunctional.functor;

import java.time.LocalDate;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.MutableCustomer;

import static land.plainfunctional.functor.Maybe.just;
import static land.plainfunctional.functor.Maybe.nothing;
import static land.plainfunctional.functor.Maybe.of;
import static land.plainfunctional.functor.Maybe.withMaybe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaybeSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Maybe semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldEncapsulateValues() {
        Maybe<Integer> maybe3 = just(3);
        assertThat(maybe3.isNothing()).isFalse();

        assertThat(maybe3).isNotSameAs(just(3));
        assertThat(maybe3).isEqualTo(just(3));
    }

    @Test
    void whenJustIsNullValue_shouldThrowException() {
        assertThatThrownBy(
            () -> just(null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null' value");
    }

    @Test
    void shouldEncapsulateNothing() {
        Maybe<String> nothing = nothing();
        assertThat(nothing.isNothing()).isTrue();
    }

    @Test
    void shouldEncapsulateNothingAndRespectReferentialTransparency() {
        Maybe<String> nothing = nothing();
        assertThat(nothing).isSameAs(nothing());
    }

    @Test
    void shouldEncapsulateNullValuesViaFactoryMethodOnly() {
        Maybe<String> maybe = of(null);
        assertThat(maybe.isNothing()).isTrue();
    }

    /**
     * @see <a href="http://blog.vavr.io/the-agonizing-death-of-an-astronaut/">Vavr blog</a>
     */
    @Test
    void shouldPreserveComputationalContext() {
        assertThatThrownBy(
            () -> of("someString").map((ignored) -> (Integer) null)

        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null' value");
    }

    /**
     * NB! Avoiding mutating shared state is the application logic's responsibility! Sorry!
     *
     * This may be accomplished e.g. via immutable domain classes.
     * See the {@link land.plainfunctional.testdomain.vanillaecommerce} test package on examples how to do that.
     */
    @Test
    void willMutateArgumentsIfAllowedToDoThat() {
        MutableCustomer customer = new MutableCustomer();
        customer.customerId = "Jon";

        of(customer)
            .map((c) -> {
                c.customerId = "Lisa";
                return c;
            });

        assertThat(customer.customerId).isEqualTo("Lisa");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor laws
    // See: https://wiki.haskell.org/Functor
    // See: http://eed3si9n.com/learning-scalaz/Functor+Laws.html
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Functors must preserve composition of morphisms:
     * map (g ∘ f) ≡ map g ∘ map f
     *
     * If two sequential mapping operations are performed one after the other using two functions,
     * the result should be the same as a single mapping operation with one function that is equivalent to applying the first function to the result of the second.
     */
    @Test
    void functorsShouldPreserveCompositionOfEndomorphisms_just() {
        Maybe<Integer> maybe3 = just(3);

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Maybe<Integer> F1 = maybe3.map(g.compose(f));
        Maybe<Integer> F2 = maybe3.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);

        // Bonus
        assertThat(F1.getOrDefault(0)).isEqualTo(3 + 13 - 5);
        assertThat(F2.getOrDefault(0)).isEqualTo(3 + 13 - 5);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_just() {
        Maybe<Integer> maybe3 = just(13);

        Function<Object, String> intToString = Object::toString;
        Function<String, Integer> stringLength = String::length;

        Function<Object, String> f = intToString;
        Function<String, Integer> g = stringLength;

        Maybe<Integer> F1 = maybe3.map(g.compose(f));
        Maybe<Integer> F2 = maybe3.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);

        // Bonus
        assertThat(F1.getOrDefault(0)).isEqualTo(2);
        assertThat(F2.getOrDefault(0)).isEqualTo(2);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_nothing() {
        Maybe<Integer> maybe = nothing();

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Maybe<Integer> F1 = maybe.map(g.compose(f));
        Maybe<Integer> F2 = maybe.map(f).map(g);

        assertThat(F1).isSameAs(F2);

        assertThat(F1.isNothing()).isTrue();
        assertThat(F2.isNothing()).isTrue();

        // Bonus
        assertThat(F1.getOrDefault(null)).isNull();
        assertThat(F2.getOrDefault(null)).isNull();

        assertThat(F1.getOrDefault(0)).isEqualTo(0);
        assertThat(F2.getOrDefault(0)).isEqualTo(0);
    }


    /**
     * Functors must preserve identity morphisms:
     * map id ≡ id
     *
     * equivalent to (like in https://bartoszmilewski.com/2015/01/20/functors/):
     * F id_a ≡ id_F a
     *
     * When performing the mapping operation,
     * if the values in the functor are mapped to themselves,
     * the result will be an unmodified functor.
     */
    @Test
    void functorsShouldPreserveIdentityMorphism_just() {
        // F id_a
        Maybe<String> F_id_a = just(Function.<String>identity().apply("yes"));

        // id_F a
        Maybe<String> id_F_a = Function.<Maybe<String>>identity().apply(just("yes"));

        assertThat(F_id_a).isNotSameAs(id_F_a);
        assertThat(F_id_a).isEqualTo(id_F_a);
    }

    @Test
    void functorsShouldPreserveIdentityMorphism_nothing() {
        // F id_a
        Maybe<String> F_id_a = nothing();

        // id_F a
        Maybe<String> id_F_a = Function.<Maybe<String>>identity().apply(nothing());

        assertThat(F_id_a).isSameAs(id_F_a);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldPutValuesInThisApplicativeFunctor() {
        Maybe<?> maybe = withMaybe().pure("JustDoIt");

        assertThat(maybe.tryGet()).isEqualTo("JustDoIt");
    }

    @Test
    void shouldPutTypedValuesInThisApplicativeFunctor() {
        Maybe<LocalDate> maybe = withMaybe(LocalDate.class).pure(LocalDate.of(2010, 10, 13));

        assertThat(maybe.tryGet()).isEqualTo(LocalDate.of(2010, 10, 13));
    }

    @Test
    void shouldComposeApplicativeEndoFunctors() {
        //Function<Integer, Function<Integer, Integer>> verboseCurriedPlus =
        //    new Function<Integer, Function<Integer, Integer>>() {
        //        @Override
        //        public Function<Integer, Integer> apply(Integer int1) {
        //            return new Function<Integer, Integer>() {
        //                @Override
        //                public Integer apply(Integer int2) {
        //                    return int1 + int2;
        //                }
        //            };
        //        }
        //    };

        //Function<Integer, Function<Integer, Integer>> curriedPlus =
        //    (int1) ->
        //        (int2) ->
        //            int1 + int2;

        //BiFunction<Integer, Integer, Integer> plus = Integer::sum;

        BinaryOperator<Integer> plus = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);

        Function<Integer, Integer> appliedCurriedPlusTwo = curriedPlus.apply(2);

        Maybe<Function<? super Integer, ? extends Integer>> maybeAppliedCurriedPlusTwo = just(appliedCurriedPlusTwo);
        Maybe<Integer> maybeSum = just(3).apply(maybeAppliedCurriedPlusTwo);

        assertThat(maybeSum.tryGet()).isEqualTo(5);
    }

    @Test
    void shouldComposeApplicativeFunctors() {
        Function<String, Function<String, Integer>> curriedStringLength =
            (string1) ->
                (string2) ->
                    string1.length() + string2.length();

        Function<String, Integer> appliedStringLength = curriedStringLength.apply("Two");

        Maybe<Function<? super String, ? extends Integer>> maybeStringLength = just(appliedStringLength);
        Maybe<Integer> maybeSum = just("Three").apply(maybeStringLength);

        assertThat(maybeSum.tryGet()).isEqualTo(8);
    }

    @Test
    void shouldDoAlgebraicOperationsOnApplicativeEndoFunctors_nothing() {
        Maybe<Integer> maybeSum = just(1)
            .apply(nothing());

        assertThat(maybeSum.isNothing()).isTrue();

        BinaryOperator<Integer> plus = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);

        maybeSum = just(1)
            .apply(just(curriedPlus.apply(22)))
            .apply(just(curriedPlus.apply(333)))
            .apply(nothing());

        assertThat(maybeSum.isNothing()).isTrue();
    }

    @Test
    void shouldDoAlgebraicOperationsOnApplicativeEndoFunctors_just() {
        BinaryOperator<Integer> plus = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);

        Maybe<Integer> maybeSum = just(1)
            .apply(just(curriedPlus.apply(2)))
            .apply(just(curriedPlus.apply(3)))
            .apply(just(curriedPlus.apply(4)));

        assertThat(maybeSum.tryGet()).isEqualTo(1 + 2 + 3 + 4);

        // Or as applicative functor all the way
        maybeSum = withMaybe(Integer.class)
            .pure(1)
            .apply(just(curriedPlus.apply(2)))
            .apply(just(curriedPlus.apply(3)))
            .apply(just(curriedPlus.apply(4)));

        assertThat(maybeSum.tryGet()).isEqualTo(1 + 2 + 3 + 4);
    }

    // TODO: ...
    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_nothing() {
        //Function<String, Integer> stringLength = String::length;

        //Function<? super String, Function<? super String, ? extends Integer>> curriedStringLength =
        //    (string1) ->
        //        (string2) -> stringLength.apply(string1) + stringLength.apply(string2);


        Maybe<Integer> maybeStringLength = nothing()
            .apply(nothing());

        assertThat(maybeStringLength.isNothing()).isTrue();


        maybeStringLength = just("One")
            .apply(nothing());

        assertThat(maybeStringLength.isNothing()).isTrue();


        //maybeStringLength = nothing()
        //    .apply(just(curriedStringLength.apply("One")));

        //assertThat(maybeStringLength.isNothing()).isTrue();

        /*
        Function<? super String, ? extends Integer> x = curriedStringLength.apply("Three");
        Maybe<Function<? super String, ? extends Integer>> xx = just(x);

        maybeStringLength = just("One")
            .apply(just(curriedStringLength.apply("Two")))
            .apply((Maybe<Function<? super String, ? extends Integer>>)xx)
            .apply(nothing())
        //.apply(just(curriedStringLength.apply("Four")))
        ;

        assertThat(maybeStringLength.isNothing()).isTrue();
        */
    }

    // TODO: ...
    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_just() {
        Function<String, Integer> stringLength = String::length;

        Function<? super String, Function<? super String, ? extends Integer>> curriedStringLength =
            (string1) ->
                (string2) -> stringLength.apply(string1) + stringLength.apply(string2);


        Maybe<String> justOneString = just("One");
        Maybe<String> justTwoString = just("Two");
        Maybe<String> justThreeString = just("Three");
        Maybe<String> justFourString = just("Four");

        Maybe<Integer> maybeOneStringLength = justOneString.map(stringLength);
        Maybe<Integer> maybeTwoStringLength = justTwoString.map(stringLength);
        Maybe<Integer> maybeThreeStringLength = justThreeString.map(stringLength);
        Maybe<Integer> maybeFourStringLength = justFourString.map(stringLength);


        Maybe<Integer> maybeStringLength = justOneString
            .apply(just(curriedStringLength.apply("Two")))
            //.apply(just(curriedStringLength.apply("Four"))) // Compiler won't handle this
            ;

        assertThat(maybeStringLength.isNothing()).isFalse();
        assertThat(maybeStringLength.tryGet()).isEqualTo(6);


        BinaryOperator<Integer> plus = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);

        Function<Integer, Integer> plusOne = (integer) -> integer + 1;
        Function<Integer, Integer> plusTwo = (integer) -> integer + 2;
        Function<Integer, Integer> plusThree = (integer) -> integer + 3;
        Function<Integer, Integer> plusFour = (integer) -> integer + 4;

        Maybe<Function<Integer, Integer>> maybePlusOne = just(plusOne);
        Maybe<Function<Integer, Integer>> maybePlusTwo = just(plusTwo);
        Maybe<Function<Integer, Integer>> maybePlusThree = just(plusThree);
        Maybe<Function<Integer, Integer>> maybePlusFour = just(plusFour);


        Maybe<Integer> maybeSum = maybeOneStringLength
            // TODO: Compiles, but yields 'java.lang.ClassCastException: land.plainfunctional.monad.MaybeSpecs$1 cannot be cast to land.plainfunctional.monad.Maybe'
            //.apply(
            //    new Functor<Function<? super Integer, ? extends Integer>>() {
            //        @Override
            //        public <U> Functor<U> map(Function<? super Function<? super Integer, ? extends Integer>, ? extends U> function) {
            //            return of(
            //                function.apply(
            //                    (Function<Integer, Integer>) integer -> integer + 2
            //                )
            //            );
            //        }
            //    })
            .apply(
                of(
                    (Function<Integer, Integer>) (int1) -> int1 + maybeTwoStringLength.getOrDefault(0)
                )
            )
            .apply(
                of(
                    new Function<Integer, Integer>() {
                        @Override
                        public Integer apply(Integer int1) {
                            return curriedPlus.apply(int1).apply(maybeThreeStringLength.getOrDefault(0));
                        }
                    }
                )
            )
            .apply(of(plusFour))
            //.apply(maybePlusFour) // Compiler won't handle this
            ;

        assertThat(maybeSum.tryGet()).isEqualTo(3 + 3 + 5 + 4);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fold (catamorphism) semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldFoldViaPatternMatching_nothing() {
        Maybe<String> maybe = of(null);

        Integer stringLength =
            maybe.map(String::length)
                 .fold(
                     () -> -1,
                     (length) -> length
                 );

        assertThat(stringLength).isEqualTo(-1);
    }

    @Test
    void getOrDefault_nothing() {
        assertThat(of(null).getOrDefault("Nope")).isEqualTo("Nope");
    }

    @Test
    void shouldFoldViaPatternMatching_just() {
        Maybe<String> maybe = just("Three");

        Integer stringLength =
            maybe.map(String::length)
                 .fold(
                     () -> -1,
                     (length) -> length
                 );

        assertThat(stringLength).isEqualTo(5);
    }

    @Test
    void getOrDefault_just() {
        assertThat(of("Three").getOrDefault("Nope")).isEqualTo("Three");
    }
}
