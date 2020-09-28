package land.plainfunctional.monad;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.TestFunctions;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static land.plainfunctional.monad.Sequence.empty;
import static land.plainfunctional.monad.Sequence.of;
import static land.plainfunctional.monad.Sequence.withSequence;
import static land.plainfunctional.testdomain.TestFunctions.isEven;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SequenceTests {

    ///////////////////////////////////////////////////////////////////////////
    // Sequence properties
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldProhibitNullValues() {
        assertThatThrownBy(() -> of((String) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'Sequence' cannot contain 'null' values");

        assertThatThrownBy(() -> of("Value1", null, "Value2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'Sequence' cannot contain 'null' values");
    }

    @Test
    void shouldBeEmpty() {
        Sequence<Integer> emptySequence = of();

        assertThat(emptySequence).isNotNull();
        assertThat(emptySequence.isEmpty()).isTrue();
        assertThat(emptySequence.size()).isEqualTo(0);


        Sequence<Integer> emptySequence2 = empty();

        assertThat(emptySequence2).isNotNull();
        assertThat(emptySequence2.isEmpty()).isTrue();
        assertThat(emptySequence2.size()).isEqualTo(0);
    }

    @Test
    void shouldContainValue() {
        Sequence<String> sequence = of("Single value");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(1);
    }

    @Test
    void shouldContainValues() {
        Sequence<Integer> sequence = of(1, 2, 3, 4, 5, 6);

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(6);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor laws
    // See: https://wiki.haskell.org/Functor
    // See: http://eed3si9n.com/learning-scalaz/Functor+Laws.html
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Functors must preserve identity morphisms:
     * map id ≡ id
     *
     * equivalent to (like in https://bartoszmilewski.com/2015/01/20/functors/):
     * f id_a ≡ id_f a
     *
     * When performing the mapping operation,
     * if the values in the functor are mapped to themselves,
     * the result will be an unmodified functor.
     */
    @Test
    void functorsShouldPreserveIdentityMorphism() {
        Sequence<String> f_id_a = of(Function.<String>identity().apply("yes"));

        Sequence<String> id_f_a = Function.<Sequence<String>>identity().apply(of("yes"));

        assertThat(f_id_a).isNotSameAs(id_f_a);
        assertThat(f_id_a).isEqualTo(id_f_a);

        // Bonus
        assertThat(f_id_a.isEmpty()).isFalse();
        assertThat(f_id_a.size()).isEqualTo(1L);

        assertThat(id_f_a.isEmpty()).isFalse();
        assertThat(id_f_a.size()).isEqualTo(1L);
    }


    /**
     * Functors must preserve composition of morphisms:
     * map (g ∘ f) ≡ map g ∘ map f
     *
     * If two sequential mapping operations are performed one after the other using two functions,
     * the result should be the same as a single mapping operation with one function that is equivalent to applying the first function to the result of the second.
     */
    @Test
    void functorsShouldPreserveCompositionOfMorphisms() {
        Sequence<Integer> sequence3 = of(3, 4);

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Sequence<Integer> F1 = sequence3.map(g.compose(f));
        Sequence<Integer> F2 = sequence3.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);

        // Bonus
        Sequence<Integer> F3 = sequence3.map(f.andThen(g));
        assertThat(F1).isNotSameAs(F3);
        assertThat(F1).isEqualTo(F3);

        // Bonus
        assertThat(F1.isEmpty()).isFalse();
        assertThat(F1.size()).isEqualTo(2L);

        assertThat(F2.isEmpty()).isFalse();
        assertThat(F2.size()).isEqualTo(2L);

        assertThat(F1._unsafe().get(0)).isEqualTo(11);
        assertThat(F2._unsafe().get(0)).isEqualTo(11);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldPutValuesInThisApplicativeFunctor() {
        Sequence<?> sequence = withSequence().pure("JustDoIt");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(1L);
        assertThat(sequence._unsafe().get(0)).isEqualTo("JustDoIt");
    }

    @Test
    void shouldPutTypedValuesInThisApplicativeFunctor() {
        Sequence<LocalDate> sequence = withSequence(LocalDate.class).pure(LocalDate.of(2010, 10, 13));

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(1L);
        assertThat(sequence._unsafe().get(0)).isEqualTo(LocalDate.of(2010, 10, 13));
    }

    // TODO: ...
    /*
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
    */

    // TODO: ...
    /*
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
    */

    // TODO: ...
    /*
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
    */

    // TODO: ...
    /*
    @Test
    void shouldDoAlgebraicOperationsOnApplicativeEndoFunctors_just() {
        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) -> (int2) -> sum(int1, int2);

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

        // Or 'map' of curried binary functions
        maybeSum = just(1)
            .apply(just(2).map(curriedPlus))
            .apply(just(3).map(curriedPlus))
            .apply(just(4).map(curriedPlus));

        assertThat(maybeSum.tryGet()).isEqualTo(1 + 2 + 3 + 4);
    }
    */

    // TODO: ...
    /*
    @Test
    void whenPartialFunctionReturnsNull_shouldThrowException() {
        BinaryOperator<Integer> plus = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);

        Function<Integer, Function<Integer, Integer>> nullFn =
            (int1) ->
                (int2) -> null;

        assertThatThrownBy(
            () ->
                withMaybe(Integer.class)
                    .pure(1)
                    .apply(just(curriedPlus.apply(2)))
                    .apply(just(curriedPlus.apply(3)))
                    // NB! Partial function returning null/bottom leads to runtime error
                    .apply(just(nullFn.apply(100)))
                    .apply(just(curriedPlus.apply(4)))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null' value");

        // And when doing 'map' of curried binary functions
        assertThatThrownBy(
            () ->
                just(1)
                    .apply(just(2).map(curriedPlus))
                    .apply(just(3).map(curriedPlus))
                    .apply(just(3).map(nullFn))
                    .apply(just(4).map(curriedPlus))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null' value");
    }
    */

    // TODO: ...
    /*
    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_nothing() {
        Maybe<Integer> maybeStringLength = nothing()
            .apply(nothing());

        assertThat(maybeStringLength.isNothing()).isTrue();

        // Won't compile
        //maybeStringLength = nothing()
        //.apply(just("One"));

        assertThat(maybeStringLength.isNothing()).isTrue();

        maybeStringLength = just("One")
            .apply(nothing())
        // Won't compile
        //.apply(just("Two"))
        ;

        assertThat(maybeStringLength.isNothing()).isTrue();

        maybeStringLength = just("One")
            .apply(of(null))
        // Won't compile
        //.apply(just("Two"))
        ;

        assertThat(maybeStringLength.isNothing()).isTrue();
    }
    */

    // TODO: ...
    /*
    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_just() {
        Function<String, Integer> stringLength = String::length;

        //Function<? super String, Function<? super String, ? extends Integer>> curriedStringLength =
        //    (string1) ->
        //        (string2) -> stringLength.apply(string1) + stringLength.apply(string2);

        BinaryOperator<Integer> plus = Integer::sum;

        BiFunction<Integer, Integer, Integer> plus2 = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);


        //Maybe<String> justOneString = just("One");
        //Maybe<String> justTwoString = just("Two");
        //Maybe<String> justThreeString = just("Three");
        //Maybe<String> justFourString = just("Four");

        //Maybe<Integer> maybeOneStringLength = justOneString.map(String::length);
        //Maybe<Integer> maybeTwoStringLength = justTwoString.map(String::length);
        //Maybe<Integer> maybeThreeStringLength = justThreeString.map(String::length);
        //Maybe<Integer> maybeFourStringLength = justFourString.map(String::length);


        //Maybe<Integer> maybeStringLength = justOneString
        //    .apply(just(curriedStringLength.apply("Two")))
        //    .apply(just(curriedStringLength.apply("Four"))) // Compiler won't handle this
        //    ;

        //assertThat(maybeStringLength.isNothing()).isFalse();
        //assertThat(maybeStringLength.tryGet()).isEqualTo(6);

        //Maybe<Integer> maybeStringLength = just("One")
        //    .apply(just("Two").map(curriedStringLength))
        //    .apply(just("Three").map(curriedStringLength)) // Compiler won't handle this
        //    ;

        Maybe<Integer> maybeStringLength = just(0)
            .apply(just("One").map(stringLength).map(curriedPlus))
            .apply(just("Two").map(stringLength).map(curriedPlus))
            .apply(just("Three").map(stringLength).map(curriedPlus));

        //assertThat(maybeStringLength.isNothing()).isFalse();
        assertThat(maybeStringLength.tryGet()).isEqualTo(3 + 3 + 5);


        /
        BinaryOperator<Integer> plus = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) -> (int2) -> plus.apply(int1, int2);

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
            .apply(
                new Functor<Function<? super Integer, ? extends Integer>>() {
                    @Override
                    public <U> Functor<U> map(Function<? super Function<? super Integer, ? extends Integer>, ? extends U> function) {
                        return of(
                            function.apply(
                                (Function<Integer, Integer>) integer -> integer + 2
                            )
                        );
                    }
                })
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
            .apply(just(plusFour))
            //.apply(maybePlusFour) // Compiler won't handle this
            ;

        assertThat(maybeSum.tryGet()).isEqualTo(3 + 3 + 5 + 4);
        /
    }
    */

    // TODO: ...
    /*
    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_2_just() {
        Function<Integer, String> getNegativeNumberInfo =
            (integer) ->
                integer < 0
                    ? format("%d is a negative number", integer)
                    : format("%d is a natural number", integer);

        Function<Integer, String> getGreaterThanTenInfo =
            (integer) ->
                integer > 10
                    ? format("%d is greater than 10", integer)
                    : format("%d is less or equal to 10", integer);

        Function<String, Function<String, String>> curriedStringAppender =
            (string1) ->
                (string2) ->
                    isBlank(string2) ? string1 : string1 + ", " + string2;

        Maybe<String> maybeInfoString = just("")
            .apply(just(7).map(getGreaterThanTenInfo).map(curriedStringAppender))
            .apply(just(7).map(getNegativeNumberInfo).map(curriedStringAppender));

        assertThat(maybeInfoString.tryGet()).isEqualTo("7 is a natural number, 7 is less or equal to 10");
    }
    */

    // TODO: ...
    /*
    @Test
    void shouldDoValidationAndStuffLikeThat() {
        Function<String, Function<String, String>> curriedStringAppender =
            (string1) -> (string2) -> isBlank(string2) ? string1 : string1 + ", " + string2;

        Function<Integer, String> getNegativeNumberInfo =
            (integer) ->
                integer < 0
                    ? format("%d is a negative number", integer)
                    : "";

        Function<Integer, String> getGreaterThanTenInfo =
            (integer) ->
                integer > 10
                    ? format("%d is greater than 10", integer)
                    : "";

        Maybe<Integer> justMinus13 = just(-13);
        Maybe<Integer> just7 = just(7);

        Maybe<String> maybeInfoString = of(
            just("")
                .apply(justMinus13
                    .map(getGreaterThanTenInfo)
                    .map(curriedStringAppender)
                )
                .apply(justMinus13
                    .map(getNegativeNumberInfo)
                    .map(curriedStringAppender)
                )
                .fold(
                    () -> null,
                    (string) -> isBlank(string) ? null : string
                )
        );
        assertThat(maybeInfoString.isNothing()).isFalse();
        assertThat(maybeInfoString.tryGet()).isEqualTo("-13 is a negative number");

        maybeInfoString = just("")
            .apply(just7
                .map(getGreaterThanTenInfo)
                .map(curriedStringAppender)
            )
            .apply(just7
                .map(getNegativeNumberInfo)
                .map(curriedStringAppender)
            );
        assertThat(maybeInfoString.tryGet()).isEqualTo("");

        maybeInfoString = of(maybeInfoString.tryGet());
        assertThat(maybeInfoString.tryGet()).isEqualTo("");

        maybeInfoString = of(
            maybeInfoString
                .fold(
                    () -> null,
                    (string) -> isBlank(string) ? null : string
                )
        );
        assertThat(maybeInfoString.isNothing()).isTrue();

        // TODO: Possible extension 1
        //String numberInfo = maybeInfoString.transformOrDefault(
        //    (string) -> isBlank(string) ? null : string,
        //    null
        //);
        //assertThat(numberInfo).isNull();

        //maybeInfoString = of(numberInfo);
        //assertThat(maybeInfoString.isNothing()).isTrue();

        //maybeInfoString = of(
        //    maybeInfoString.transformOrDefault(
        //        (string) -> isBlank(string) ? null : string,
        //        null
        //    )
        //);
        //assertThat(maybeInfoString.isNothing()).isTrue();

        // TODO: Possible extension 2
        //maybeInfoString = of(
        //    maybeInfoString.transformOrNull(
        //        (string) -> isBlank(string) ? null : string
        //    )
        //);
        //assertThat(maybeInfoString.isNothing()).isTrue();

        // TODO: Possible extension 3
        //maybeInfoString = of(
        //    maybeInfoString.tryTransform(
        //        (string) -> isBlank(string) ? null : string
        //    )
        //);
        //assertThat(maybeInfoString.isNothing()).isTrue();
    }
    */


    ///////////////////////////////////////////////////////////////////////////
    // Monad laws
    // https://wiki.haskell.org/Monad_laws
    // See: https://devth.com/2015/monad-laws-in-scala
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Left identity:
     * If we take a value and put it in a context with 'pure' and then feed it to a monad action using >>= ('bind'),
     * it is the same as just taking the value and applying the function to it.
     *
     * ...
     *
     * Haskell:
     * return a >>= f ≡ f a
     *
     * Where:
     * - 'return' is the same as 'pure'/'of' static factory methods, or 'just'/'nothing' data constructors for 'Maybe'
     * - 'a' is parameterized type
     * - '>>=' is 'bind'
     * - 'f' is the monad action function - 'f' has the same (Haskell-style) type signature as 'return': a -> m a
     *
     * ...
     *
     * Java (pseudocode):
     * (m a).bind(f) ≡ f(a)
     *
     * Where:
     * - 'm' is the monad (here represented by one of its data constructors)
     * - 'a' is a (generic) value
     * - 'f' is the monad action function - 'f' has the same (Haskell-style) type signature as 'return': a -> m a
     */
    @Test
    public void shouldHaveLeftIdentity_0() {
        // f (monad action)
        Function<String, Sequence<Integer>> f = (s) -> of(s.length());

        // a
        String value = "Blue";

        // m (Sequence data constructor)
        Function<String, Sequence<String>> m = Sequence::of;

        // m a (same as 'Sequence.of(value)')
        //Sequence<String> m_a = Sequence.of(value);
        Sequence<String> m_a = m.apply(value);

        assertThat(m_a.bind(f)).isNotSameAs(f.apply(value));
        assertThat(m_a.bind(f)).isEqualTo(f.apply(value));

        // Bonus
        assertThat(m_a.bind(f)._unsafe().get(0)).isEqualTo(4);
        assertThat(f.apply(value)._unsafe().get(0)).isEqualTo(4);
    }

    /**
     * Right identity:
     * If we have a monad and we use >>= ('bind') to feed it to 'pure'',
     * the result is our original monad.
     *
     * Haskell:
     * m >>= return ≡ m
     *
     * Where:
     * - 'return' is the same as 'pure'/'of' static factory methods, or 'just'/'nothing' data constructors for 'Maybe'
     * - '>>=' is 'bind'
     * - 'm' is the monad (function) - 'm' has the same (Haskell-style) type signature as 'return': a -> m a
     *
     * ...
     *
     * Java (pseudocode):
     * (m a).bind(m) ≡ m a
     *
     * Where:
     * - 'm' is the monad (here represented by one of its data constructors)
     * - 'm' is the has the same (Haskell-style) type signature as 'return': a -> m a
     * - 'a' is a (generic) value
     * - 'm a' is a value in a monad (in a monadic context) - same as 'return a' above
     */
    @Test
    void shouldHaveRightIdentity() {
        // a
        String value = "Go";

        // m (Sequence data constructor)
        Function<String, Sequence<String>> m = Sequence::of;

        // m a
        Sequence<String> m_a = m.apply(value);

        //// m.bind(Sequence(_))
        Sequence<String> lhs = m_a.bind(m);

        Sequence<String> rhs = m_a;

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);
    }

    /**
     * Associativity:
     * When we have a chain of monadic function applications with >>= ('bind')
     * it should not matter how they are nested.
     *
     * Haskell:
     * (m >>= f) >>= g ≡ m >>= (λx -> f x >>= g)
     */
    @Test
    void shouldHaveAssociativity() {
        // a
        String value = "Go";

        // m a
        Sequence<String> m = of(value);

        Function<String, Sequence<Integer>> f = s -> of(s.length());
        Function<Integer, Sequence<Boolean>> g = i -> of(isEven(i));

        Sequence<Boolean> lhs = m.bind(f).bind(g);
        Sequence<Boolean> rhs = m.bind(x -> f.apply(x).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);

        assertThat(lhs.isEmpty()).isFalse();
        assertThat(lhs.size()).isEqualTo(1L);
        assertThat(rhs.isEmpty()).isFalse();
        assertThat(rhs.size()).isEqualTo(1L);

        // Bonus
        assertThat(lhs._unsafe().get(0)).isTrue(); // => Even number
        assertThat(rhs._unsafe().get(0)).isTrue(); // => Even number

        // Bonus: Using 'map'
        Sequence<Boolean> usingMap = m
            .map(String::length)
            .map(TestFunctions::isEven);
        assertThat(usingMap._unsafe().get(0)).isTrue(); // => Even number
    }


    ///////////////////////////////////////////////////////////////////////////
    // Misc. 'Sequence' applications
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldMapItsValues() {
        Sequence<String> sequence = of("one", "two", "three", "four");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(4);

        Sequence<Integer> mappedSequence = sequence.map(String::length);

        assertThat(mappedSequence.isEmpty()).isFalse();
        assertThat(mappedSequence.size()).isEqualTo(4);
    }

    @Test
    void shouldBeTransformedToJavaUtilList() {
        Sequence<Integer> sequence = of("one", "two", "three", "four")
            .map(String::length);

        List<Integer> list = sequence.toJavaList();

        // => Get by index
        assertThat(list.get(0)).isEqualTo(3);
        assertThat(list.get(1)).isEqualTo(3);
        assertThat(list.get(2)).isEqualTo(5);
        assertThat(list.get(3)).isEqualTo(4);

        // => Iteration
        int sum = 0;
        for (int value : list) {
            sum += value;
        }
        assertThat(sum).isEqualTo(3 + 3 + 5 + 4);

        // => Reduction
        sum = list.stream().reduce(0, Integer::sum);
        assertThat(sum).isEqualTo(3 + 3 + 5 + 4);
    }

    // TODO: ...
    @Test
    void shouldFold() {
        int range = 0;
        long rangeSum = 0;
        //int range = 1;
        //long rangeSum = 1;
        //int range = 100;
        //long rangeSum = 1000; // ?

        // Sum of int range: Regular Java (with shortcuts)
        Instant start = now();
        long sum = 0;
        for (int i = 1; i <= range; i += 1) {
            sum = sum + i;
        }
        //System.out.printf("Sum of int range: Regular Java (with shortcuts), took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Regular Java (with shortcuts), took %d ms%n", between(start, now()).toMillis());
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Regular Java
        start = now();
        long[] ints = new long[range + 1]; // 0-based
        sum = 0;
        for (int i = 1; i <= range; i += 1) {
            ints[i] = i; // [0..range]
        }
        for (int i = 1; i <= range; i += 1) {
            sum = sum + ints[i];
        }
        //System.out.printf("Sum of int range: Regular Java, took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Regular Java, took %d ms%n", between(start, now()).toMillis());
        assertThat(ints.length).isEqualTo(range + 1); // 0-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of long range: Regular Java (via loads of helpers)
        start = now();
        ints = LongStream.rangeClosed(0, range).toArray(); // [0..range]
        sum = stream(ints).sum();
        //System.out.printf("Sum of int range: Regular Java (via loads of helpers), took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Regular Java (via loads of helpers), took %d ms%n", between(start, now()).toMillis());
        assertThat(ints.length).isEqualTo(range + 1); // 0-based
        assertThat(sum).isEqualTo(rangeSum);


        // TODO:
        /*
        // Sum of long range: Plain functional Java
        start = now();
        Sequence<Long> sequence = empty();
        for (long i = 1; i <= range; i += 1) {
            Sequence<Long> sequenceOfLongs = Sequence.of(i);
            sequence = sequence.append(sequenceOfLongs);

            // TODO: Try:
            //sequence = sequence.append(Sequence.of(just(i)));
            //sequence = sequence.append(sequence.pure(just(i)));
        }
        sum = sequence.toMonoid(
            0,
            (long1, long2) -> {
                return long1 + long2;
            }
        ).fold();
        //System.out.printf("Sum of int range: Plain functional Java, took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Plain functional Java, took %d ms%n", between(start, now()).toMillis());
        assertThat(ints.length).isEqualTo(range + 1); // 0-based
        assertThat(sum).isEqualTo(rangeSum);
        */


        // TODO:
        /*
        // Sum of long range: Plain functional Java (sequence of maybe values)
        start = now();
        Sequence<Maybe<Long>> sequence = empty();
        for (long i = 1; i <= range; i += 1) {
            Maybe<Long> justlong = just(i);
            Sequence<Maybe<Long>> sequenceOfJustLongs = Sequence.of(singletonList(justlong));
            //Sequence<Maybe<Long>> sequenceOfJustLongs = of(justlong);
            sequence = sequence.append(sequenceOfJustLongs);

            // TODO: Try:
            //sequence = sequence.append(Sequence.of(just(i)));
            //sequence = sequence.append(sequence.pure(just(i)));
        }
        sum = sequence.toMonoid(
            0,
            (maybeLong1, maybeLong2) -> {
                return maybeLong1.tryGet() + maybeLong2.tryGet();
            }
        ).fold();
        //System.out.printf("Sum of int range: Plain functional Java, took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Plain functional Java, took %d ms%n", between(start, now()).toMillis());
        assertThat(ints.length).isEqualTo(range + 1); // 0-based
        assertThat(sum).isEqualTo(rangeSum);
        */
    }
}
