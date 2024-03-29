package land.plainfunctional.monad;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.testdomain.TestFunctions;
import land.plainfunctional.testdomain.vanillaecommerce.Customer;
import land.plainfunctional.testdomain.vanillaecommerce.Payment;
import land.plainfunctional.testdomain.vanillaecommerce.Person;
import land.plainfunctional.testdomain.vanillaecommerce.VipCustomer;
import land.plainfunctional.typeclass.Applicative;

import static java.lang.Integer.sum;
import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static land.plainfunctional.monad.Maybe.asMaybe;
import static land.plainfunctional.monad.Maybe.just;
import static land.plainfunctional.monad.Maybe.nothing;
import static land.plainfunctional.monad.Sequence.asSequence;
import static land.plainfunctional.monad.TestData.CSV_MONOID;
import static land.plainfunctional.monad.TestData.getDelayedInteger;
import static land.plainfunctional.testdomain.TestFunctions.isEven;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SequenceSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Sequence properties
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldProhibitNullValues() {
        assertThatThrownBy(() -> Sequence.of((String) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'Sequence' cannot contain null values");

        assertThatThrownBy(() -> Sequence.of("Value1", null, "Value2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'Sequence' cannot contain null values");
    }

    @Test
    void shouldBeEmpty() {
        Sequence<Integer> emptySequence = Sequence.empty();

        assertThat(emptySequence).isNotNull();
        assertThat(emptySequence.isEmpty()).isTrue();
        assertThat(emptySequence.size()).isEqualTo(0);

        Sequence<Integer> emptySequence2 = Sequence.of();

        assertThat(emptySequence2).isNotNull();
        assertThat(emptySequence2.isEmpty()).isTrue();
        assertThat(emptySequence2.size()).isEqualTo(0);
    }

    @Test
    void shouldContainValue() {
        Sequence<String> sequence = Sequence.of("Single value");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(1);
    }

    @Test
    void shouldContainValues() {
        Sequence<Integer> sequence = Sequence.of(1, 2, 3, 4, 5, 6);

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(6);
    }

    @Test
    void shouldBeParametricPolymorphicCovariant() {
        Person person = new Person("Paul");

        Customer customer = new Customer("Chris");

        Customer customer2 = new Customer("Chrissie");

        VipCustomer vipCustomer = new VipCustomer();
        //vipCustomer.vipCustomerSince = OffsetDateTime.now();

        Sequence<Person> sequence = Sequence.of(person, customer);

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(2);


        Sequence<Customer> sequence2 = Sequence.of(customer, customer2, vipCustomer);

        Person foldedPerson = sequence2.foldLeft(
            (person1, person2) -> person2,
            Person.getIdentity()
        );
        assertThat(foldedPerson).isNotNull();
        assertThat(foldedPerson).isInstanceOf(Person.class);
        assertThat(foldedPerson).isInstanceOf(Customer.class);
        assertThat(foldedPerson).isInstanceOf(VipCustomer.class);

        assertThat(sequence2.isEmpty()).isFalse();
        assertThat(sequence2.size()).isEqualTo(3);
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
        Sequence<String> f_id_a = Sequence.of(Function.<String>identity().apply("yes"));

        Sequence<String> id_f_a = Function.<Sequence<String>>identity().apply(Sequence.of("yes"));

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
        Sequence<Integer> sequence3 = Sequence.of(3, 4);

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

        assertThat(F1.values.get(0)).isEqualTo(11);
        assertThat(F2.values.get(0)).isEqualTo(11);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldPutValuesInThisApplicativeFunctor() {
        Sequence<?> sequence = Sequence.asSequence().pure("JustDoIt");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(1L);
        assertThat(sequence.values.get(0)).isEqualTo("JustDoIt");
    }

    @Test
    void shouldPutTypedValuesInThisApplicativeFunctor() {
        Sequence<LocalDate> sequence = asSequence(LocalDate.class).pure(LocalDate.of(2010, 10, 13));

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(1L);
        assertThat(sequence.values.get(0)).isEqualTo(LocalDate.of(2010, 10, 13));
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
    void NB_whenApplyingSiblingApplicativeType_willThrowClassCastException() {
        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) -> (int2) -> sum(int1, int2);

        assertThatThrownBy(
            () -> Sequence.of(1)
                          .apply(just(curriedPlus.apply(2)))
                          .apply(just(curriedPlus.apply(3)))
                          .apply(just(curriedPlus.apply(4)))
        ).isInstanceOf(ClassCastException.class)
         .hasMessageContaining("land.plainfunctional.monad.Maybe cannot be cast to ");

        assertThatThrownBy(
            () -> Sequence.of(1)
                          .apply(just(curriedPlus.apply(2)))
                          .apply(just(curriedPlus.apply(3)))
                          .apply(just(curriedPlus.apply(4)))
        ).isInstanceOf(ClassCastException.class)
         .hasMessageContaining("land.plainfunctional.monad.Sequence");
    }

    @Test
    void shouldDoValidationAndStuffLikeThat_3() {
        Function<Sequence<String>, Function<Sequence<String>, Sequence<String>>> curriedSequenceOfStringAppend =
            (list1) -> (list2) -> {
                if (list2.isEmpty()) {
                    return list1;
                }
                list1.values.addAll(list2.values);
                return list1;
            };

        Function<Integer, Sequence<String>> getNegativeNumberInfo =
            (integer) ->
                integer < 0
                    ? Sequence.of(format("%d is a negative number", integer))
                    : Sequence.empty();

        Maybe<Integer> justMinus13 = just(-13);


        Maybe<Sequence<String>> maybeInfoString1 = just(Sequence.empty());
        Applicative<Function<? super Sequence<String>, ? extends Sequence<String>>> d4 =
            justMinus13
                .map(getNegativeNumberInfo)
                .map(curriedSequenceOfStringAppend);
        maybeInfoString1 = maybeInfoString1.apply(d4);

        Sequence<String> stringList = maybeInfoString1.tryGet();
        assertThat(stringList.size()).isEqualTo(1);
        assertThat(stringList.toJavaList().get(0)).isEqualTo("-13 is a negative number");


        Maybe<Sequence<String>> maybeInfoString2 =
            just(Sequence.<String>empty())
                .apply(justMinus13
                    .map(getNegativeNumberInfo)
                    .map(curriedSequenceOfStringAppend)
                );

        assertThat(maybeInfoString2.tryGet().size()).isEqualTo(1);
        assertThat(maybeInfoString2.tryGet().values.get(0)).isEqualTo("-13 is a negative number");
    }

    @Test
    void shouldDoAlgebraicOperationsOnApplicativeEndoFunctors_just() {
        Function<Sequence<Integer>, Function<Sequence<Integer>, Sequence<Integer>>> curriedIntegerSequenceAppend =
            new Function<Sequence<Integer>, Function<Sequence<Integer>, Sequence<Integer>>>() {
                @Override
                public Function<Sequence<Integer>, Sequence<Integer>> apply(Sequence<Integer> seq1) {
                    return new Function<Sequence<Integer>, Sequence<Integer>>() {
                        @Override
                        public Sequence<Integer> apply(Sequence<Integer> seq2) {
                            List<Integer> list1 = seq1.values;
                            List<Integer> list2 = seq2.values;

                            list1.addAll(list2);

                            return Sequence.of(list1);
                        }
                    };
                }
            };

        Function<Integer, Sequence<Integer>> a =
            (integer) ->
                Sequence.of(integer);

        Function<Integer, Function<Integer, Sequence<Integer>>> aa =
            (int1) ->
                (int2) ->
                    Sequence.of(int1, int2);

        //Sequence<Integer> appendedSequence = Sequence
        //.of(1, 2, 3)
        //.apply(Sequence.of(curriedIntegerSequenceAppend.apply(Sequence.of(4, 5, 6))));
        //.apply(aa.apply(1));

        //assertThat(appendedSequence.size()).isEqualTo(6);
    }

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
                asMaybe(Integer.class)
                    .pure(1)
                    .apply(just(curriedPlus.apply(2)))
                    .apply(just(curriedPlus.apply(3)))
                    // NB! Partial function returning null/bottom leads to runtime error
                    .apply(just(nullFn.apply(100)))
                    .apply(just(curriedPlus.apply(4)))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a null/non-existing (\"bottom\") value");

        // And when doing 'map' of curried binary functions
        assertThatThrownBy(
            () ->
                just(1)
                    .apply(just(2).map(curriedPlus))
                    .apply(just(3).map(curriedPlus))
                    .apply(just(3).map(nullFn))
                    .apply(just(4).map(curriedPlus))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a null/non-existing (\"bottom\") value");
    }

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
            .apply(Maybe.of(null))
        // Won't compile
        //.apply(just("Two"))
        ;

        assertThat(maybeStringLength.isNothing()).isTrue();
    }

    // TODO: Investigate further, and clean up...
    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_just() {
        Function<String, Integer> stringLength = String::length;

        Function<? super String, Function<? super String, ? extends Integer>> curriedStringLength =
            (string1) ->
                (string2) -> stringLength.apply(string1) + stringLength.apply(string2);

        BinaryOperator<Integer> plus = Integer::sum;

        BiFunction<Integer, Integer, Integer> plus2 = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);


        Maybe<String> justOneString = just("One");
        Maybe<String> justTwoString = just("Two");
        Maybe<String> justThreeString = just("Three");
        Maybe<String> justFourString = just("Four");

        Maybe<Integer> maybeOneStringLength = justOneString.map(String::length);
        Maybe<Integer> maybeTwoStringLength = justTwoString.map(String::length);
        Maybe<Integer> maybeThreeStringLength = justThreeString.map(String::length);
        Maybe<Integer> maybeFourStringLength = justFourString.map(String::length);


        //Maybe<Integer> maybeStringLength = justOneString
        //    .apply(just(curriedStringLength.apply("Two")))
        //    //.apply(just(curriedStringLength.apply("Four"))) // Compiler won't handle this
        //    ;
        //assertThat(maybeStringLength.isNothing()).isFalse();
        //assertThat(maybeStringLength.tryGet()).isEqualTo(10);

        //Maybe<Integer> maybeStringLength = just("One")
        //    .apply(just("Two").map(curriedStringLength))
        //    //.apply(just("Three").map(curriedStringLength)) // Compiler won't handle this
        //    ;
        //assertThat(maybeStringLength.isNothing()).isFalse();
        //assertThat(maybeStringLength.tryGet()).isEqualTo(11);

        Maybe<Integer> maybeStringLength = just(0)
            .apply(just("One").map(stringLength).map(curriedPlus))
            .apply(just("Two").map(stringLength).map(curriedPlus))
            .apply(just("Three").map(stringLength).map(curriedPlus));

        assertThat(maybeStringLength.isNothing()).isFalse();
        assertThat(maybeStringLength.tryGet()).isEqualTo(0 + 3 + 3 + 5);


        Function<Integer, Integer> plusOne = (integer) -> integer + 1;
        Function<Integer, Integer> plusTwo = (integer) -> integer + 2;
        Function<Integer, Integer> plusThree = (integer) -> integer + 3;
        Function<Integer, Integer> plusFour = (integer) -> integer + 4;

        Maybe<Function<Integer, Integer>> maybePlusOne = just(plusOne);
        Maybe<Function<Integer, Integer>> maybePlusTwo = just(plusTwo);
        Maybe<Function<Integer, Integer>> maybePlusThree = just(plusThree);
        Maybe<Function<Integer, Integer>> maybePlusFour = just(plusFour);


        Maybe<Integer> maybeSum = maybeOneStringLength
            // TODO: Compiles, but yields 'java.lang.ClassCastException: land.plainfunctional.monad.SequenceSpecs$9 cannot be cast to land.plainfunctional.monad.Maybe'
            //.apply(
            //    new Applicative<Function<? super Integer, ? extends Integer>>() {
            //        @Override
            //        public Applicative<Function<? super Integer, ? extends Integer>> pure(Function<? super Integer, ? extends Integer> value) {
            //            throw new UnsupportedOperationException();
            //        }
            //        @Override
            //        public <V> Applicative<V> apply(Applicative<Function<? super Function<? super Integer, ? extends Integer>, ? extends V>> functionInContext) {
            //            throw new UnsupportedOperationException();
            //        }
            //        @Override
            //        public <U> Functor<U> map(Function<? super Function<? super Integer, ? extends Integer>, ? extends U> function) {
            //            return Sequence.of(
            //                function.apply(
            //                    (Function<Integer, Integer>) integer -> integer + 2
            //                )
            //            );
            //        }
            //    }
            //)

            // TODO: Compiles, but yields 'java.lang.ClassCastException: land.plainfunctional.monad.Sequence cannot be cast to land.plainfunctional.monad.Maybe'
            //.apply(
            //    Sequence.of(
            //        new Function<Integer, Integer>() {
            //            @Override
            //            public Integer apply(Integer int1) {
            //                return curriedPlus.apply(int1).apply(maybeTwoStringLength.getOrDefault(0));
            //            }
            //        }
            //    )
            //)

            // TODO: Compiles, but yields 'java.lang.ClassCastException: land.plainfunctional.monad.Sequence cannot be cast to land.plainfunctional.monad.Maybe'
            //.apply(
            //    Sequence.of(
            //        (Function<Integer, Integer>) (int1) -> int1 + maybeThreeStringLength.getOrDefault(0)
            //    )
            //)

            .apply(just(plusFour))

            // Compiler won't handle this
            //.apply(maybePlusFour)
            ;

        //assertThat(maybeSum.tryGet()).isEqualTo(3);
        assertThat(maybeSum.tryGet()).isEqualTo(3 + 4);
        //assertThat(maybeSum.tryGet()).isEqualTo(3 + 3 + 5 + 4);
    }

    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_just_2() {
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

        Maybe<String> maybeInfoString = Maybe.of(
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

        maybeInfoString = Maybe.of(maybeInfoString.tryGet());
        assertThat(maybeInfoString.tryGet()).isEqualTo("");

        maybeInfoString = Maybe.of(
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

        //maybeInfoString = Sequence.of(numberInfo);
        //assertThat(maybeInfoString.isNothing()).isTrue();

        //maybeInfoString = Sequence.of(
        //    maybeInfoString.transformOrDefault(
        //        (string) -> isBlank(string) ? null : string,
        //        null
        //    )
        //);
        //assertThat(maybeInfoString.isNothing()).isTrue();

        // TODO: Possible extension 2
        //maybeInfoString = Sequence.of(
        //    maybeInfoString.transformOrNull(
        //        (string) -> isBlank(string) ? null : string
        //    )
        //);
        //assertThat(maybeInfoString.isNothing()).isTrue();

        // TODO: Possible extension 3
        //maybeInfoString = Sequence.of(
        //    maybeInfoString.tryTransform(
        //        (string) -> isBlank(string) ? null : string
        //    )
        //);
        //assertThat(maybeInfoString.isNothing()).isTrue();
    }

    @Test
    void shouldDoValidationAndStuffLikeThat_2() {
        Function<Integer, String> getNegativeNumberInfo =
            (integer) ->
                integer < 0
                    ? format("%d is a negative number", integer)
                    : CSV_MONOID.identityElement;

        Function<Integer, String> getGreaterThanTenInfo =
            (integer) ->
                integer > 10
                    ? format("%d is greater than 10", integer)
                    : CSV_MONOID.identityElement;

        Maybe<Integer> justMinus13 = just(-13);
        Maybe<Integer> just7 = just(7);

        Maybe<String> maybeInfoString = Maybe.of(
            just(CSV_MONOID.identityElement)
                .apply(justMinus13
                    .map(getGreaterThanTenInfo)
                    .map(CSV_MONOID.curriedBinaryOperation())
                )
                .apply(justMinus13
                    .map(getNegativeNumberInfo)
                    .map(CSV_MONOID.curriedBinaryOperation())
                )
                .fold(
                    () -> null,
                    (string) -> isBlank(string) ? null : string
                )
        );
        assertThat(maybeInfoString.isNothing()).isFalse();
        assertThat(maybeInfoString.tryGet()).isEqualTo("-13 is a negative number");

        maybeInfoString = just(CSV_MONOID.identityElement)
            .apply(
                just7
                    .map(getGreaterThanTenInfo)
                    .map(CSV_MONOID.curriedBinaryOperation())
            )
            .apply(
                just7
                    .map(getNegativeNumberInfo)
                    .map(CSV_MONOID.curriedBinaryOperation())
            );
        assertThat(maybeInfoString.tryGet()).isEqualTo("");

        maybeInfoString = Maybe.of(maybeInfoString.tryGet());
        assertThat(maybeInfoString.tryGet()).isEqualTo("");

        maybeInfoString = Maybe.of(
            maybeInfoString
                .fold(
                    () -> null,
                    (string) -> isBlank(string) ? null : string
                )
        );
        assertThat(maybeInfoString.isNothing()).isTrue();
    }


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
        Function<String, Sequence<Integer>> f = (s) -> Sequence.of(s.length());

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
        assertThat(m_a.bind(f).values.get(0)).isEqualTo(4);
        assertThat(f.apply(value).values.get(0)).isEqualTo(4);
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
        Sequence<String> m = Sequence.of(value);

        Function<String, Sequence<Integer>> f = s -> Sequence.of(s.length());
        Function<Integer, Sequence<Boolean>> g = i -> Sequence.of(isEven(i));

        Sequence<Boolean> lhs = m.bind(f).bind(g);
        Sequence<Boolean> rhs = m.bind((a) -> f.apply(a).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);

        assertThat(lhs.isEmpty()).isFalse();
        assertThat(lhs.size()).isEqualTo(1L);
        assertThat(rhs.isEmpty()).isFalse();
        assertThat(rhs.size()).isEqualTo(1L);

        // Bonus
        assertThat(lhs.values.get(0)).isTrue(); // => Even number
        assertThat(rhs.values.get(0)).isTrue(); // => Even number

        // Bonus: Using 'map'
        Sequence<Boolean> usingMap = m
            .map(String::length)
            .map(TestFunctions::isEven);
        assertThat(usingMap.values.get(0)).isTrue(); // => Even number
    }


    ///////////////////////////////////////////////////////////////////////////
    // Misc. monad applications
    ///////////////////////////////////////////////////////////////////////////

    // N/A any more...
    //@Test
    //void shouldBind() {
    //    Sequence<Integer> sequence = Sequence.of(1, 2, 3);
    //
    //    Function<Integer, Maybe<Integer>> f = (integer) -> just(integer);
    //
    //    //Sequence<Maybe<Integer>> mappedSequence = (Sequence<Maybe<Integer>>) sequence.bind(f);
    //    ////Sequence<Maybe<Integer>> mappedSequence2 = sequence.safeBind(f);
    //
    //    //Maybe<Integer> classCastMappedSequence = sequence.bind(f);
    //    Sequence<Integer> classCastMappedSequence2 = sequence.bind(f);
    //    ////Maybe<Integer> classCastMappedSequence2 = sequence.safeBind(f);
    //
    //    int sum = classCastMappedSequence2.toFreeMonoid(Integer::sum, 0).fold();
    //    assertThat(sum).isEqualTo(1 + 2 + 3);
    //
    //    Object o = sequence.bind(f);
    //    Sequence<Maybe<Integer>> mappedSequence2 = (Sequence<Maybe<Integer>>) o;
    //
    //    assertThat(mappedSequence2.size()).isEqualTo(3);
    //}

    // TODO: Complete these...
    @Test
    void when_shouldBind_00() {
        Sequence<Integer> sequence = Sequence.of(1, 2, 3);

        Function<Integer, Sequence<Maybe<Integer>>> f =
            (integer) -> Sequence.of(just(integer));

        Sequence<Maybe<Integer>> mappedSequence = sequence.bind(f);


        Function<Integer, Maybe<Integer>> f0 = Maybe::just;

        f = (integer) -> Sequence.of(f0.apply(integer));

        f = (integer) -> f0.andThen(Sequence::of).apply(integer);

        mappedSequence = sequence.bind(f);
    }

    @Test
    void whenBinding_shouldJoinLayeredSequences() {
        Sequence<Integer> sequence = Sequence.of(1, 2, 3);

        Function<Integer, Sequence<Integer>> integerElementRemover =
            (integer) -> Sequence.empty();

        //Sequence<Integer> mappedSequence = sequence.map(integerElementRemover); // Nope, must be joined/flattened
        Sequence<Integer> mappedSequence = sequence.bind(integerElementRemover);

        assertThat(mappedSequence.isEmpty()).isTrue();

        assertThat(mappedSequence.foldLeft(Integer::sum, 0)).isEqualTo(0);
    }

    @Test
    void whenBinding_shouldExpandSequenceByJoiningLayeredSequences() {
        Sequence<Integer> sequence = Sequence.of(1, 2, 3);

        Function<Integer, Sequence<Integer>> integerElementDuplicator =
            (integer) -> Sequence.of(integer, integer);

        Sequence<Integer> mappedSequence = sequence.bind(integerElementDuplicator);

        assertThat(mappedSequence.isEmpty()).isFalse();
        assertThat(mappedSequence.size()).isEqualTo(3 * 2);

        assertThat(mappedSequence.foldLeft(Integer::sum, 0))
            .isEqualTo(1 + 1 + 2 + 2 + 3 + 3);

        assertThat(mappedSequence.foldRight(Integer::sum, 0))
            .isEqualTo(1 + 1 + 2 + 2 + 3 + 3);

        assertThat(
            Sequence.of(new HashSet<>(mappedSequence.toJavaList())).foldRight(Integer::sum, 0)
        ).isEqualTo(1 + 2 + 3);
    }

    @Test
    void whenBinding_shouldDownCast() {
        VipCustomer vipCustomer1 = new VipCustomer();
        VipCustomer vipCustomer2 = new VipCustomer();

        Sequence<VipCustomer> sequence = Sequence.of(vipCustomer1, vipCustomer2);

        Function<VipCustomer, Sequence<Customer>> vipRevoker =
            (vipCustomer) -> Sequence.of(new Customer());

        Sequence<Customer> mappedSequence = sequence.bind(vipRevoker);

        assertThat(mappedSequence.isEmpty()).isFalse();


        Function<VipCustomer, Sequence<Person>> customerRevoker =
            (vipCustomer) -> Sequence.of(new Person());

        Sequence<Person> mappedSequence2 = sequence.bind(customerRevoker);

        assertThat(mappedSequence2.isEmpty()).isFalse();
    }

    @Test
    void whenBinding_shouldUpCast() {
        Customer customer1 = new Customer();
        Customer customer2 = new Customer();

        Sequence<Customer> customers = Sequence.of(customer1, customer2);

        Function<Customer, Sequence<VipCustomer>> vipPromoter =
            (customer) -> Sequence.of(new VipCustomer());

        Sequence<VipCustomer> mappedSequence = customers.bind(vipPromoter);

        assertThat(mappedSequence.isEmpty()).isFalse();
    }

    @Test
    void shouldBind_2() {
        Sequence<String> sequence = Sequence.of("one", "two", "three", "four");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(4);

        /*
        Sequence<Integer> mappedSequence = sequence.bind(new Function<String, Sequence<Integer>>() {
            @Override
            public Sequence<Integer> apply(String string) {
                return Sequence.of(string.length());
            }
        });
        assertThat(mappedSequence.isEmpty()).isFalse();
        assertThat(mappedSequence.size()).isEqualTo(4);
        */

        //Sequence<Maybe<Integer>> mappedSequence2 = sequence.bind((string) -> null);
        //assertThat(mappedSequence2.isEmpty()).isTrue();

        Sequence<Integer> mappedSequence3 = sequence.bind((string -> Sequence.of(3, 3, 5, 4)));
        assertThat(mappedSequence3.isEmpty()).isFalse();
        assertThat(mappedSequence3.size()).isEqualTo(16);

        /*
        mappedSequence2 = sequence.bind(
            new Function<String, Sequence<Maybe<Integer>>>() {
                @Override
                public Sequence<Maybe<Integer>> apply(String string) {
                    return Sequence.of(just(string.length()));
                }
            });
        assertThat(mappedSequence2.isEmpty()).isFalse();
        assertThat(mappedSequence2.size()).isEqualTo(4);
        */
    }


    ///////////////////////////////////////////////////////////////////////////
    // Misc. 'Sequence' applications
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldMapItsValues() {
        Sequence<String> sequence = Sequence.of("one", "two", "three", "four");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(4);

        Sequence<Integer> mappedSequence = sequence.map(String::length);

        assertThat(mappedSequence.isEmpty()).isFalse();
        assertThat(mappedSequence.size()).isEqualTo(4);
    }

    @Test
    void whenMappingToBottomValue_shouldIgnoreIt_1() {
        Sequence<String> sequence = Sequence.of("one", "two", "three", "four");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(4);

        Sequence<Integer> mappedSequence = sequence.map(
            (string) -> {
                throw new RuntimeException();
            });

        assertThat(mappedSequence.isEmpty()).isTrue();
    }

    @Test
    void whenMappingToBottomValue_shouldIgnoreIt_2() {
        Sequence<String> sequence = Sequence.of("one", "two", "three", "four");

        assertThat(sequence.isEmpty()).isFalse();
        assertThat(sequence.size()).isEqualTo(4);

        Sequence<Integer> mappedSequence = sequence.map(
            (string) -> null
        );

        assertThat(mappedSequence.isEmpty()).isTrue();
    }

    @Test
    void shouldBeTransformedToJavaUtilList() {
        Sequence<Integer> sequence = Sequence
            .of("one", "two", "three", "four")
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

    @Test
    void shouldParallelMap() {
        Sequence<String> sequence = Sequence.of("Hello", " ", "World", "!");

        Function<String, Integer> f = (string) -> getDelayedInteger(1000, string.length());

        Instant start = Instant.now();

        // One partition only
        Sequence<Integer> mappedSequence = sequence.parallelMap(f, 4);

        assertThat(between(start, now()).toMillis()).isLessThan(1000 + 50); // ~50 ms in max total computational "overhead"

        assertThat(mappedSequence.size()).isEqualTo(4);
        assertThat(mappedSequence.values.get(0)).isEqualTo(5);
        assertThat(mappedSequence.values.get(1)).isEqualTo(1);
        assertThat(mappedSequence.values.get(2)).isEqualTo(5);
        assertThat(mappedSequence.values.get(3)).isEqualTo(1);
    }

    @Test
    void shouldParallelMapByPartitioning() {
        Sequence<Integer> sequence = Sequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        Function<Integer, Integer> f = (integer) -> {
            try {
                sleep(1000);
                return integer + 1;

            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
            return Integer.MIN_VALUE;
        };

        Instant start = Instant.now();

        // Yields 4 partitions, 3 full and the last containing 3 elements
        Sequence<Integer> mappedSequence = sequence.parallelMap(f, 5);

        assertThat(between(start, now()).toMillis()).isLessThan(4 * 1000 + 100); // ~100 ms in max total computational "overhead"

        assertThat(mappedSequence.size()).isEqualTo(18);
        assertThat(mappedSequence.values.get(0)).isEqualTo(2);
        assertThat(mappedSequence.values.get(mappedSequence.values.size() - 1)).isEqualTo(19);
    }

    @Test
    void whenMappingToBottomValue_shouldIgnoreIt_3() {
        Sequence<Integer> sequence = Sequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        Function<Integer, Integer> f = (integer) -> {
            throw new RuntimeException();
        };

        // Yields 4 partitions, 3 full and the last containing 3 elements
        Sequence<Integer> mappedSequence = sequence.parallelMap(f, 5);

        assertThat(mappedSequence.isEmpty()).isTrue();
    }

    @Test
    void whenMappingToBottomValue_shouldIgnoreIt_4() {
        Sequence<Integer> sequence = Sequence.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        Function<Integer, Integer> f = (integer) -> null;

        // Yields 4 partitions, 3 full and the last containing 3 elements
        Sequence<Integer> mappedSequence = sequence.parallelMap(f, 5);

        assertThat(mappedSequence.isEmpty()).isTrue();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Append/Add semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldAppendElement() {
        Sequence<Integer> seq1 = Sequence.of(1, 2, 3);

        assertThat(seq1.isEmpty()).isFalse();
        assertThat(seq1.size()).isEqualTo(3);

        Sequence<Integer> seq2 = seq1.append(4);

        assertThat(seq2.isEmpty()).isFalse();
        assertThat(seq2.size()).isEqualTo(4);

        // Original Sequence should not be mutated
        assertThat(seq1).isNotEqualTo(seq2);
        assertThat(seq1).isNotSameAs(seq2);
        assertThat(seq1.isEmpty()).isFalse();
        assertThat(seq1.size()).isEqualTo(3);
    }

    @Test
    void shouldAppendElements() {
        Sequence<Integer> seq1 = Sequence.of(1, 2, 3);

        assertThat(seq1.isEmpty()).isFalse();
        assertThat(seq1.size()).isEqualTo(3);

        Sequence<Integer> seq2 = Sequence.of(1, 2, 3);

        assertThat(seq1).isEqualTo(seq2);
        assertThat(seq1).isNotSameAs(seq2);

        Sequence<Integer> seq3 = seq1.append(seq2);

        assertThat(seq3.size()).isEqualTo(6);

        // Original Sequence should not be mutated
        assertThat(seq1).isEqualTo(seq2);
        assertThat(seq1).isNotSameAs(seq2);
        assertThat(seq1).isNotEqualTo(seq3);
        assertThat(seq1).isNotSameAs(seq3);
        assertThat(seq2).isEqualTo(seq1);
        assertThat(seq2).isNotSameAs(seq1);
        assertThat(seq2).isNotEqualTo(seq3);
        assertThat(seq2).isNotSameAs(seq3);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Singleton semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldExtractSingleElementFromSingletonSequence_1() {
        Sequence<String> singletonSequence = Sequence.of("one");

        assertThat(singletonSequence.trySingle()).isSameAs("one");
    }

    @Test
    void shouldExtractSingleElementFromSingletonSequence_2() {
        Sequence<String> singletonSequence = Sequence.of("one");

        assertThat(singletonSequence.singleOr(
            () -> format("No a singleton, silly - got %d elements, damnit!", singletonSequence.size()))
        ).isEqualTo("one");
    }

    @Test
    void whenTryingToExtractSingleElementFromNotSingletonSequence_shouldThrowException_1() {
        Sequence<String> singletonSequence = Sequence.of("one", "two");

        assertThatThrownBy(singletonSequence::trySingle)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void whenTryingToExtractSingleElementFromNotSingletonSequence_shouldThrowException_2() {
        Sequence<String> singletonSequence = Sequence.of("one", "two");

        assertThat(singletonSequence.singleOr(
            () -> format("No a singleton, silly - got %d elements, damnit!", singletonSequence.size()))
        ).isEqualTo("No a singleton, silly - got 2 elements, damnit!");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fold (catamorphism) semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldFold() {
        int min = Sequence.of("one", "two", "three", "four")
                          .map(String::length)
                          .foldLeft(Integer.MAX_VALUE, Integer::min);
        assertThat(min).isEqualTo(3);

        int max = Sequence.of("one", "two", "three", "four")
                          .map(String::length)
                          .foldLeft(Integer.MIN_VALUE, Integer::max);
        assertThat(max).isEqualTo(5);

        int sum = Sequence.of("one", "two", "three", "four")
                          .map(String::length)
                          .foldLeft(0, Integer::sum);
        assertThat(sum).isEqualTo(3 + 3 + 5 + 4);

        sum = Sequence.of("one", "two", "three", "four", "five", "six")
                      .foldLeft(0, (foldedValue, string) -> foldedValue + string.length());
        assertThat(sum).isEqualTo(3 + 3 + 5 + 4 + 4 + 3);

        // TODO: Rewrite to plain-functional :-)
        Function<Character, BinaryOperator<String>> keepOnlyCharacter =
            (character) -> (string1, string2) ->
                string1 + string2
                    .chars()
                    .mapToObj(i -> (char) i)
                    .filter((c) -> c.equals(character))
                    .collect(toList())
                    .stream()
                    .map(Object::toString)
                    .collect(joining());

        String foldedString = Sequence
            .of("one", "two", "three", "four", "five", "six")
            .foldLeft(
                "",
                //(string1, string2) -> {
                //    List<Character> filteredCharacterList = string2
                //        .chars()
                //        .mapToObj(i -> (char) i)
                //        .filter((character) -> character.equals('e'))
                //        .collect(toList());

                //    return string1 + filteredCharacterList
                //        .stream()
                //        .map(Object::toString)
                //        .collect(joining());
                //})
                keepOnlyCharacter.apply('e'));
        assertThat(foldedString).isEqualTo("eeee");

        // TODO: Rewrite to plain-functional :-)
        Function<Character, Function<String, String>> keepOnlyChar =
            (character) -> (string) ->
                string
                    .chars()
                    .mapToObj(i -> (char) i)
                    .filter((c) -> c.equals(character))
                    .collect(toList())
                    .stream()
                    .map(Object::toString)
                    .collect(joining());

        foldedString = Sequence.of("one", "two", "three", "four", "five", "six")
                               .map(keepOnlyChar.apply('e'))
                               .foldLeft("", String::concat);
        assertThat(foldedString).isEqualTo("eeee");
    }

    // TODO: Complete these...
    // TODO: Keep?
    @Test
    void should__() {
        Person person = new Person("Paul");

        Customer customer = new Customer("Chris");

        Customer customer2 = new Customer("Chrissie");

        VipCustomer vipCustomer = new VipCustomer();
        //vipCustomer.vipCustomerSince = OffsetDateTime.now();


        Sequence<Person> sequence = Sequence.of(person, customer, customer2, vipCustomer);
        assertThat(sequence.size()).isEqualTo(4);

        Maybe<Sequence<Person>> maybeSequenceOfPersons = just(sequence);
        Customer foldedPerson = maybeSequenceOfPersons.fold(
            new Supplier<VipCustomer>() {
                @Override
                public VipCustomer get() {
                    return new VipCustomer();
                }
            },
            new Function<Sequence<Person>, VipCustomer>() {
                @Override
                public VipCustomer apply(Sequence<Person> personSequence) {
                    return personSequence.foldLeft(
                        new VipCustomer(),
                        new BiFunction<VipCustomer, Person, VipCustomer>() {
                            @Override
                            public VipCustomer apply(VipCustomer vipCustomer, Person person) {
                                return vipCustomer;
                            }
                        }
                    );
                }
            }
        );


        Sequence<Customer> sequence2 = Sequence.of(customer, customer2, vipCustomer);
        assertThat(sequence2.size()).isEqualTo(3);

        Maybe<Sequence<Customer>> maybeSequenceOfCustomers = just(sequence2);
        Person foldedPerson2 = maybeSequenceOfCustomers.fold(
            new Supplier<VipCustomer>() {
                @Override
                public VipCustomer get() {
                    return new VipCustomer();
                }
            },
            new Function<Sequence<Customer>, VipCustomer>() {
                @Override
                public VipCustomer apply(Sequence<Customer> personSequence) {
                    return personSequence.foldLeft(
                        new VipCustomer(),
                        new BiFunction<VipCustomer, Customer, VipCustomer>() {
                            @Override
                            public VipCustomer apply(VipCustomer vipCustomer, Customer person) {
                                return vipCustomer;
                            }
                        }
                    );
                }
            }
        );
        assertThat(foldedPerson).isNotNull();
        assertThat(foldedPerson).isInstanceOf(VipCustomer.class);
    }

    // TODO: Keep?
    /*
    @Test
    void should___() {
        Person person = new Person("Paul");

        Customer customer = new Customer("Chris");

        Customer customer2 = new Customer("Chrissie");

        VipCustomer vipCustomer = new VipCustomer("William III");
        //vipCustomer.vipCustomerSince = OffsetDateTime.now();


        Sequence<Person> sequence = Sequence.of(person, customer, customer2, vipCustomer);
        assertThat(sequence.size()).isEqualTo(4);

        Person foldedSequence = sequence.foldLeft(
            new VipCustomer(),
            new BiFunction<VipCustomer, Person, VipCustomer>() {
                @Override
                public VipCustomer apply(VipCustomer vipCustomer, Person person) {
                    //throw new UnsupportedOperationException();
                    //return null;
                    //return new VipCustomer();
                    return vipCustomer;
                    // ClassCastException:
                    //return vipCustomer.append((VipCustomer) person);
                }
            }
        );
        assertThat(foldedSequence).isNotNull();
        assertThat(foldedSequence.name).isNull();

        foldedSequence = sequence.foldLeft(
            new Person(),
            new BinaryOperator<Person>() {
                @Override
                public Person apply(Person accumulatedPerson, Person person) {
                    return accumulatedPerson.append(person);
                }
            }
        );
        assertThat(foldedSequence.name).isEqualTo("Paul"); // Folded left-wise

        foldedSequence = sequence.parallelFold(new FreeMonoid<>(Person::append, Person.getIdentity()));
        assertThat(foldedSequence.name).isEqualTo("Paul"); // Folded left-wise


        // Nope!
        //foldedSequence = sequence.foldRight(new Person(), Person::append);
        //assertThat(foldedSequence.name).isEqualTo("William III"); // Folded right-wise ("backwards")

        foldedSequence = sequence.foldRight(
            Person.getIdentity(),
            (person2Append, accumulatedPerson) -> accumulatedPerson.append(person2Append)
        );
        assertThat(foldedSequence.name).isEqualTo("William III"); // Folded right-wise ("backwards")

        // Nope!
        //Person.IDENTITY.name = "";
        //foldedSequence = sequence.foldRight(Person::append, Person.identity());
        //assertThat(foldedSequence.name).isEqualTo("William III"); // Folded right-wise ("backwards")

        foldedSequence = sequence.foldRight(
            (person2Append, accumulatedPerson) -> accumulatedPerson.append(person2Append),
            Person.getIdentity()
        );
        assertThat(foldedSequence.name).isEqualTo("William III"); // Folded right-wise ("backwards")
    }
    */


    ///////////////////////////////////////////////////////////////////////////
    // Fold (catamorphism) semantics, in parallel
    ///////////////////////////////////////////////////////////////////////////

    // Ad-hoc test
    //@Test
    void shouldParallelFold_explicitPartitionSize() {
        //int range = 0;
        //long rangeSum = 0;
        //int range = 1;
        //long rangeSum = 1;
        //int range = 2;
        //long rangeSum = 3;
        //int range = 3;
        //long rangeSum = 6;
        //int range = 4;
        //long rangeSum = 10;
        //int range = 5;
        //long rangeSum = 15;
        //int range = 10;
        //long rangeSum = 55;
        //int range = 100;
        //long rangeSum = 5_050;
        //int range = 1_000;
        //long rangeSum = 500_500;
        //int range = 10_000;
        //long rangeSum = 50_005_000;
        int range = 100_000;
        long rangeSum = 5_000_050_000L;
        //int range = 1_000_000;
        //long rangeSum = 500_000_500_000L;

        List<Integer> partitionSizeList = asList(
            0, 1, 2, 4
            //, 6, 8, 10, 14, 16, 18, 19, 20, 21, 22, 25, 30
            //, 40, 50, 60, 70, 75, 80, 85, 90, 100, 125, 150, 200, 400, 800, 1200
        );

        for (int partitionSize : partitionSizeList) {
            Instant startGenerating = now();
            Supplier<Iterable<Long>> intSupplier = () -> {
                Long[] intArray = new Long[range];
                for (long i = 0; i < range; i += 1) {
                    intArray[(int) i] = i + 1;
                }
                return asList(intArray);
            };
            Sequence<Long> sequenceOfLongs = Sequence.of(intSupplier);
            Instant startProcessing = now();
            long sum = sequenceOfLongs
                .parallelFold(
                    Long::sum,
                    0L,
                    partitionSize
                );
            System.out.println();
            System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding, partition size: %d), generating took %d ms%n", partitionSize, between(startGenerating, startProcessing).toMillis());
            System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding, partition size: %d), processing took %d ms%n", partitionSize, between(startProcessing, now()).toMillis());
            assertThat(sequenceOfLongs.size()).isEqualTo(range); // 1-based
            assertThat(sum).isEqualTo(rangeSum);
        }
    }

    @Test
    void shouldParallelFold() {
        //int range = 0;
        //long rangeSum = 0;
        //int range = 1;
        //long rangeSum = 1;
        //int range = 2;
        //long rangeSum = 3;
        //int range = 3;
        //long rangeSum = 6;
        //int range = 4;
        //long rangeSum = 10;
        //int range = 5;
        //long rangeSum = 15;
        //int range = 10;
        //long rangeSum = 55;
        //int range = 100;
        //long rangeSum = 5_050;
        //int range = 1_000;
        //long rangeSum = 500_500;
        //int range = 10_000;
        //long rangeSum = 50_005_000;
        int range = 100_000;
        long rangeSum = 5_000_050_000L;
        //int range = 1_000_000;
        //long rangeSum = 500_000_500_000L;

        Instant startGenerating = now();
        Supplier<Iterable<Long>> intSupplier = () -> {
            Long[] intArray = new Long[range];
            for (long i = 0; i < range; i += 1) {
                intArray[(int) i] = i + 1;
            }
            return asList(intArray);
        };
        Sequence<Long> sequenceOfLongs = Sequence.of(intSupplier);
        Instant startProcessing = now();
        long sum = sequenceOfLongs
            .parallelFold(
                new FreeMonoid<>(
                    Long::sum,
                    0L
                )
            );
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequenceOfLongs.size()).isEqualTo(range); // 1-based
        assertThat(sum).isEqualTo(rangeSum);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Filter semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldFilter() {
        Predicate<String> fourLetterWords = (string) -> string.length() == 4;

        Sequence<String> seq = Sequence.of("one", "two", "three", "four", "five", "six");

        assertThat(seq.filter(fourLetterWords)).isEqualTo(Sequence.of("four", "five"));
    }

    @Test
    void shouldFilterNestedMonads() {
        Predicate<Maybe<String>> justFourLetterWords = (maybeString) ->
            maybeString.isNothing()
                ? false
                : maybeString.tryGet().length() == 4;

        Sequence<Maybe<String>> seq = Sequence.of(just("one"), nothing(), just("two"), just("three"), just("four"), just("five"), nothing());

        assertThat(seq.keep(justFourLetterWords).size()).isEqualTo(2);
    }

    @Test
    void shouldRemove() {
        Predicate<String> fourLetterWords = (string) -> string.length() == 4;

        Sequence<String> seq = Sequence.of("one", "two", "three", "four", "five", "six");

        assertThat(seq.remove(fourLetterWords)).isEqualTo(Sequence.of("one", "two", "three", "six"));
    }

    @Test
    void filterAndRemoveShouldBeComplementary() {
        Predicate<String> fourLetterWords = (string) -> string.length() == 4;

        Sequence<String> seq = Sequence.of("one", "two", "three", "four", "five", "six");

        Sequence<String> seq2 = seq
            .keep(fourLetterWords)
            .remove(fourLetterWords);

        assertThat(seq2.toJavaList()).isEmpty();
        assertThat(seq2).isEqualTo(Sequence.empty());
    }


    ///////////////////////////////////////////////////////////////////////////
    // Partitioning
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void partition_whenZeroElementsInPartitions_shouldThrowException_1() {
        assertThatThrownBy(() ->
            Sequence.empty().partition(0)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("'partitionSize' argument cannot be less than one");
    }

    @Test
    void partition_whenZeroElementsInPartitions_shouldThrowException_2() {
        assertThatThrownBy(() ->
            Sequence.of("Hello", "!").partition(0)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("'partitionSize' argument cannot be less than one");
    }

    @Test
    void partition_whenEmptyMonoid_shouldReturnEmptySequenceOfPartitions() {
        Sequence<String> sequence = Sequence.empty();

        assertThat(sequence.partition(1).isEmpty()).isTrue();
    }

    @Test
    void partition_shouldDistributeMonoidElements_1() {
        Sequence<String> sequence = Sequence.of("Hello", "!");

        assertThat(sequence.partition(1).size()).isSameAs(2L);
    }

    // TODO: Complete these

    @Test
    void partition_shouldDistributeMonoidElements_2() {
        Set<String> chronologicallyEnumeratedSet = new LinkedHashSet<>();
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("World");
        chronologicallyEnumeratedSet.add("!");

        //MonoidStructure<String> monoid = new MonoidStructure<>(
        //    chronologicallyEnumeratedSet,
        //    (string1, string2) -> string1 + string2,
        //    ""
        //);

        //assertThat(monoid.partition(2).size()).isSameAs(2L);
        //assertThat(monoid.partition(1).toJavaList().get(0).size()).isSameAs(1L);
        //assertThat(monoid.partition(1).toJavaList().get(1).size()).isSameAs(1L);

        FreeMonoid<String> stringConcatenation = new FreeMonoid<>(
            (string1, string2) -> string1 + string2,
            ""
        );

        Sequence<String> sequence = Sequence.of(chronologicallyEnumeratedSet);

        assertThat(sequence.partition(2).size()).isSameAs(2L);
        assertThat(sequence.partition(1).toJavaList().get(0).size()).isSameAs(1L);
        assertThat(sequence.partition(1).toJavaList().get(1).size()).isSameAs(1L);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Parallel fold
    ///////////////////////////////////////////////////////////////////////////

    /* On-demand test */
    @Test
    void shouldFoldPrimitives() {
        //int range = 0;
        //long rangeSum = 0;
        //int range = 1;
        //long rangeSum = 1;
        int range = 3;
        long rangeSum = 6;
        //int range = 10;
        //long rangeSum = 55;
        //int range = 100;
        //long rangeSum = 5_050;
        //int range = 1_000;
        //long rangeSum = 500_500;
        //int range = 10_000;
        //long rangeSum = 50_005_000;
        //int range = 100_000;
        //long rangeSum = 5_000_050_000L;
        //int range = 1_000_000;
        //long rangeSum = 500_000_500_000L;


        // Sum of int range: Regular Java (with shortcuts)
        Instant start = now();
        long sum = 0;
        for (int i = 1; i <= range; i += 1) {
            sum = sum + i;
        }
        System.out.println();
        //System.out.printf("Sum of int range: Regular Java (with shortcuts), took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Regular Java (with shortcuts), took %d ms%n", between(start, now()).toMillis());
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Regular Java (via loads of helpers)
        Instant startGenerating = now();
        long[] ints = LongStream.rangeClosed(0, range).toArray(); // [0..range]
        Instant startProcessing = now();
        sum = stream(ints).sum();
        System.out.println();
        //System.out.printf("Sum of int range: Regular Java (via loads of helpers), took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Regular Java (via loads of helpers), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Regular Java (via loads of helpers), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(ints.length).isEqualTo(range + 1); // 0-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Regular Java
        startGenerating = now();
        ints = new long[range + 1]; // 0-based
        sum = 0;
        for (int i = 1; i <= range; i += 1) {
            ints[i] = i; // [0..range]
        }
        startProcessing = now();
        for (int i = 1; i <= range; i += 1) {
            sum = sum + ints[i];
        }
        System.out.println();
        //System.out.printf("Sum of int range: Regular Java, took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Regular Java, generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Regular Java, processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(ints.length).isEqualTo(range + 1); // 0-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Plain functional Java (with ready-made array)
        List<Long> listOfLongs = new ArrayList<>();
        for (long i = 1; i <= range; i += 1) {
            listOfLongs.add(i);
        }
        startGenerating = now();
        Sequence<Long> sequenceOfLongs = Sequence.of(listOfLongs);
        startProcessing = now();
        sum = sequenceOfLongs
            //.toMonoid(
            //    Long::sum,
            //    0L
            //)
            //.fold();
            .foldLeft(
                Long::sum,
                0L
            );
        System.out.println();
        //System.out.printf("Sum of int range: Plain functional Java, took %d ns%n", between(start, now()).toNanos());
        System.out.printf("Sum of int range: Plain functional Java (with ready-made array), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (with ready-made array), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(listOfLongs.size()).isEqualTo(range); // 1-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Plain functional Java
        startGenerating = now();
        Sequence<Long> sequence = Sequence.empty();
        for (long i = 1; i <= range; i += 1) {
            //sequenceOfLongs = Sequence.of(i);
            //sequence = sequence.append(sequenceOfLongs);
            sequence = sequence.append(Sequence.of(i));
            //sequence = sequence.append(sequence.pure(i));
        }
        startProcessing = now();
        sum = sequence
            //.toMonoid(
            //    Long::sum,
            //    0L
            //)
            //.fold();
            .foldLeft(
                Long::sum,
                0L
            );
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java, generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java, processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequence.size()).isEqualTo(range); // 1-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Plain functional Java (sequence of maybe values)
        BinaryOperator<Long> longSum = Long::sum;

        Function<Long, Function<Long, Long>> curriedLongSum =
            (long1) ->
                //(long2) -> long1 + long2;
                (long2) -> longSum.apply(long1, long2);

        startGenerating = now();
        Sequence<Maybe<Long>> sequenceOfMaybeLongs = Sequence.empty();
        for (long i = 1; i <= range; i += 1) {
            //Maybe<Long> justlong = just(i);
            //Sequence<Maybe<Long>> sequenceOfJustLongs = Sequence.of(singletonList(justlong));

            //Sequence<Maybe<Long>> sequenceOfJustLongs = Sequence.of(justlong);
            //sequenceOfMaybeLongs = sequenceOfMaybeLongs.append(sequenceOfJustLongs);

            sequenceOfMaybeLongs = sequenceOfMaybeLongs.append(Sequence.of(just(i)));
            //sequenceOfMaybeLongs = sequenceOfMaybeLongs.append(sequenceOfMaybeLongs.pure(just(i)));
        }
        startProcessing = now();
        sum = sequenceOfMaybeLongs
            //.toMonoid(
            //    //(maybeLong1, maybeLong2) -> just(maybeLong1.tryGet() + maybeLong2.tryGet())
            //    (maybeLong1, maybeLong2) -> maybeLong1.apply(maybeLong2.map(curriedLongSum)),
            //    just(0L)
            //)
            //.fold()
            //.tryGet();
            .foldLeft(
                (maybeLong1, maybeLong2) -> maybeLong1.apply(maybeLong2.map(curriedLongSum)),
                just(0L)
            )
            .tryGet();
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java (sequence of maybe values), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (sequence of maybe values), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequenceOfMaybeLongs.size()).isEqualTo(range); // 1-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Plain functional Java (sequence of supplied values)
        startGenerating = now();
        Supplier<Iterable<Long>> intSupplier = () -> {
            Long[] intArray = new Long[range];
            for (long i = 0; i < range; i += 1) {
                intArray[(int) i] = i + 1;
            }
            return asList(intArray);
        };
        sequenceOfLongs = Sequence.of(intSupplier);
        startProcessing = now();
        sum = sequenceOfLongs
            //.toMonoid(
            //    Long::sum,
            //    0L
            //)
            //.fold();
            .foldLeft(
                Long::sum,
                0L
            );
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequenceOfLongs.size()).isEqualTo(range); // 1-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding)
        startGenerating = now();
        intSupplier = () -> {
            Long[] intArray = new Long[range];
            for (long i = 0; i < range; i += 1) {
                intArray[(int) i] = i + 1;
            }
            return asList(intArray);
        };
        sequenceOfLongs = Sequence.of(intSupplier);
        startProcessing = now();
        sum = sequenceOfLongs
            //.toMonoid(
            //    Long::sum,
            //    0L
            //)
            //.parallelFold();
            .parallelFold(
                Long::sum,
                0L,
                1
            );
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequenceOfLongs.size()).isEqualTo(range); // 1-based
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding)
        startGenerating = now();
        intSupplier = () -> {
            Long[] intArray = new Long[range];
            for (long i = 0; i < range; i += 1) {
                intArray[(int) i] = i + 1;
            }
            return asList(intArray);
        };
        sequenceOfLongs = Sequence.of(intSupplier);
        startProcessing = now();
        sum = sequenceOfLongs
            //.toMonoid(
            //    Long::sum,
            //    0L
            //)
            //.parallelFold();
            .parallelFold(
                new FreeMonoid<>(
                    Long::sum,
                    0L
                )
            );
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values) (parallel folding), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequenceOfLongs.size()).isEqualTo(range); // 1-based
        assertThat(sum).isEqualTo(rangeSum);
    }


    /* On-demand test */
    @Test
    void shouldFoldProductTypes() {
        int range = 1_000;

        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );

        //BinaryOperator<Payment> append = Payment::append;

        Function<Payment, Function<Payment, Payment>> curriedAppend =
            (payment1) ->
                (payment2) -> payment2.append(payment1);


        // TODO: ...Nope
        // "Sum" of Payments: Regular Java (with shortcuts)


        // TODO: ...Nope
        // "Sum" of Payments: Regular Java (via loads of helpers)


        // "Sum" of Payments: Regular Java
        System.out.printf("\"Sum\" of %d Payments: Regular Java%n", range);
        Instant startGenerating = now();
        List<Payment> paymentList = new ArrayList<>();
        Payment randomPayment;
        Payment firstRandomPayment = Payment.random();
        for (long i = 1; i <= range; i += 1) {
            if (i == 1) {
                System.out.printf("First enumerated payment: %s%n", firstRandomPayment);
                randomPayment = firstRandomPayment;
            } else {
                randomPayment = Payment.random();
            }
            paymentList.add(randomPayment);
        }

        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );

        Instant startProcessing = now();
        Payment payment = null;
        for (Payment currentPayment : paymentList) {
            if (payment == null) {
                payment = new Payment();
            }
            String cardNumber = payment.cardNumber;
            String cardHolderName = payment.cardHolderName;
            YearMonth expirationMonth = payment.expirationMonth;
            Integer cvc = payment.cvc;
            Double amount = payment.amount;
            boolean isPaymentReceived = payment.isPaymentReceived;

            if (isBlank(cardNumber)) {
                cardNumber = currentPayment.cardNumber;
            }
            if (isBlank(cardHolderName)) {
                cardHolderName = currentPayment.cardHolderName;
            }
            if (expirationMonth == null) {
                expirationMonth = currentPayment.expirationMonth;
            }
            if (cvc == null) {
                cvc = currentPayment.cvc;
            }
            if (amount == null) {
                amount = currentPayment.amount;
            }

            payment = new Payment(cardNumber, cardHolderName, expirationMonth, cvc, amount, isPaymentReceived);
        }
        System.out.printf("\"Sum\" of Payments: Regular Java, generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Regular Java, processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(paymentList.size()).isEqualTo(range); // 1-based
        assertThat(payment).isNotNull();
        assertThat(payment.cardHolderName).isNotNull();
        assertThat(payment.cardHolderName).isEqualTo(firstRandomPayment.cardHolderName);
        assertThat(payment.expirationMonth).isGreaterThan(YearMonth.now());
        assertThat(payment.expirationMonth).isEqualTo(firstRandomPayment.expirationMonth);


        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );


        // "Sum" of Payments: Regular Java (Java Stream API)
        System.out.println();
        System.out.printf("\"Sum\" of %d Payments: Regular Java (Java Stream API)%n", range);
        startGenerating = now();
        paymentList = new ArrayList<>();
        firstRandomPayment = Payment.random();
        for (long i = 1; i <= range; i += 1) {
            if (i == 1) {
                System.out.printf("First enumerated payment: %s%n", firstRandomPayment);
                randomPayment = firstRandomPayment;
            } else {
                randomPayment = Payment.random();
            }
            paymentList.add(randomPayment);
        }

        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );

        startProcessing = now();
        payment = paymentList
            .stream()
            .reduce(
                Payment.identity(),
                Payment::append
            );

        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(paymentList.size()).isEqualTo(range); // 1-based
        assertThat(payment).isNotNull();
        assertThat(payment.cardHolderName).isNotNull();
        assertThat(payment.cardHolderName).isEqualTo(firstRandomPayment.cardHolderName);
        assertThat(payment.expirationMonth).isGreaterThan(YearMonth.now());
        assertThat(payment.expirationMonth).isEqualTo(firstRandomPayment.expirationMonth);


        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );


        // TODO: ...
        // "Sum" of Payments: Plain functional Java (with ready-made array)
        // Nope


        // TODO: ...
        // "Sum" of Payments: Plain functional Java
        System.out.println();
        System.out.printf("\"Sum\" of %d Payments: Plain functional Java%n", range);
        startGenerating = now();
        Sequence<Payment> sequenceOfPayments = Sequence.empty();
        firstRandomPayment = Payment.random();
        for (long i = 1; i <= range; i += 1) {
            if (i == 1) {
                System.out.printf("First enumerated payment: %s%n", firstRandomPayment);
                randomPayment = firstRandomPayment;
            } else {
                randomPayment = Payment.random();
            }
            sequenceOfPayments = sequenceOfPayments.append(randomPayment);
        }

        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );

        startProcessing = now();
        payment = sequenceOfPayments
            //.toMonoid(
            //    Payment::append,
            //    Payment.identity()
            //)
            //.fold()
            .foldLeft(
                Payment::append,
                Payment.identity()
            );

        System.out.printf("\"Sum\" of Payments: Plain functional Java, generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java, processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(sequenceOfPayments.size()).isEqualTo(range); // 1-based
        assertThat(payment).isNotNull();
        assertThat(payment.cardHolderName).isNotNull();
        assertThat(payment.cardHolderName).isEqualTo(firstRandomPayment.cardHolderName);
        assertThat(payment.expirationMonth).isGreaterThan(YearMonth.now());
        assertThat(payment.expirationMonth).isEqualTo(firstRandomPayment.expirationMonth);
        // ...


        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );


        // "Sum" of Payments: Plain functional Java (sequence of maybe values)
        System.out.println();
        System.out.printf("\"Sum\" of %d Payments: Plain functional Java (sequence of maybe values)%n", range);
        startGenerating = now();
        Sequence<Maybe<Payment>> sequenceOfMaybePayments = Sequence.empty();
        firstRandomPayment = Payment.random();
        for (long i = 1; i <= range; i += 1) {
            if (i == 1) {
                System.out.printf("First enumerated payment: %s%n", firstRandomPayment);
                randomPayment = firstRandomPayment;
            } else {

                randomPayment = Payment.random();
            }
            sequenceOfMaybePayments = sequenceOfMaybePayments.append(just(randomPayment));
        }

        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );

        startProcessing = now();
        payment = sequenceOfMaybePayments
            //.toMonoid(
            //    (maybePayment1, maybePayment2) -> maybePayment1.apply(maybePayment2.map(curriedAppend)),
            //    just(Payment.identity())
            //)
            //.fold()
            .foldLeft(
                (maybePayment1, maybePayment2) -> maybePayment1.apply(maybePayment2.map(curriedAppend)),
                just(Payment.identity())
            )
            .tryGet();

        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(sequenceOfMaybePayments.size()).isEqualTo(range); // 1-based
        assertThat(payment).isNotNull();
        assertThat(payment.cardHolderName).isNotNull();
        assertThat(payment.cardHolderName).isEqualTo(firstRandomPayment.cardHolderName);
        assertThat(payment.expirationMonth).isGreaterThan(YearMonth.now());
        assertThat(payment.expirationMonth).isEqualTo(firstRandomPayment.expirationMonth);
        // ...


        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );


        // "Sum" of Payments: Plain functional Java [parallel folding II]
        System.out.println();
        System.out.printf("\"Sum\" of %d Payments: Plain functional Java [parallel folding II]%n", range);
        startGenerating = now();
        sequenceOfPayments = Sequence.empty();
        firstRandomPayment = Payment.random();
        for (long i = 1; i <= range; i += 1) {
            if (i == 1) {
                System.out.printf("First enumerated payment: %s%n", firstRandomPayment);
                randomPayment = firstRandomPayment;
            } else {
                randomPayment = Payment.random();
            }
            sequenceOfPayments = sequenceOfPayments.append(randomPayment);
        }

        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );

        startProcessing = now();
        payment = sequenceOfPayments
            //.toMonoid(
            //    Payment.identity(),
            //    Payment::append
            //).parallelFold();
            .parallelFold(
                new FreeMonoid<>(
                    Payment::append,
                    Payment.identity()
                )
            );

        System.out.printf("\"Sum\" of Payments: Plain functional Java [parallel folding II], generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java [parallel folding II], processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(sequenceOfPayments.size()).isEqualTo(range); // 1-based
        assertThat(payment).isNotNull();
        assertThat(payment.cardHolderName).isNotNull();
        assertThat(payment.cardHolderName).isEqualTo(firstRandomPayment.cardHolderName);
        assertThat(payment.expirationMonth).isGreaterThan(YearMonth.now());
        assertThat(payment.expirationMonth).isEqualTo(firstRandomPayment.expirationMonth);
        // ...
    }
}
