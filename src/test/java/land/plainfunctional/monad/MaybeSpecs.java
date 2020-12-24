package land.plainfunctional.monad;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import land.plainfunctional.algebraicstructure.MonoidStructure;
import land.plainfunctional.testdomain.TestFunctions;
import land.plainfunctional.testdomain.vanillaecommerce.MutableCustomer;
import land.plainfunctional.testdomain.vanillaecommerce.Person;
import land.plainfunctional.typeclass.Applicative;

import static java.lang.Integer.sum;
import static java.lang.String.format;
import static land.plainfunctional.monad.Maybe.just;
import static land.plainfunctional.monad.Maybe.nothing;
import static land.plainfunctional.monad.Maybe.of;
import static land.plainfunctional.monad.Maybe.withMaybe;
import static land.plainfunctional.testdomain.TestFunctions.isEven;
import static land.plainfunctional.testdomain.vanillaecommerce.Person.Gender.MALE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaybeSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Maybe semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldEncapsulateValue() {
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
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null'/non-existing (/\"bottom\"/) value");
    }

    @Test
    void shouldEncapsulateNullValueViaFactoryMethodOnly() {
        Maybe<String> maybe = of(null);

        assertThat(maybe.isNothing()).isTrue();
    }

    @Test
    void shouldEncapsulateNothing() {
        Maybe<String> nothing = nothing();

        assertThat(nothing.isNothing()).isTrue();

        // Bonus: Functor behaviour
        Maybe<String> mapped = nothing.map((string) -> string + " morphed to 'Just'");
        assertThat(mapped.isNothing()).isTrue();

        // Bonus: Monad behaviour
        Maybe<String> next = nothing.bind((string) -> just(string + " morphed to 'Just'"));
        assertThat(next.isNothing()).isTrue();
    }

    @Test
    void shouldEncapsulateNothingAndRespectReferentialTransparency() {
        Maybe<String> nothing = nothing();

        assertThat(nothing).isSameAs(nothing());
    }

    /**
     * @see <a href="http://blog.vavr.io/the-agonizing-death-of-an-astronaut/">Vavr blog</a>
     */
    @Test
    void shouldPreserveComputationalContext() {
        assertThatThrownBy(
            () -> of("someString").map((ignored) -> (Integer) null)

        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null'/non-existing (/\"bottom\"/) value");
    }

    /**
     * NB! Avoiding mutating shared state is the application logic's responsibility! Sorry!
     *
     * This may be accomplished e.g. via immutable domain classes.
     * TODO: See the {@link land.plainfunctional.testdomain.vanillaecommerce} test package on examples how to do that.
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
    void functorsShouldPreserveIdentityMorphism_nothing() {
        Maybe<String> f_id_a = nothing();

        Maybe<String> id_f_a = Function.<Maybe<String>>identity().apply(nothing());

        assertThat(f_id_a).isSameAs(id_f_a);
    }

    @Test
    void functorsShouldPreserveIdentityMorphism_just() {
        Maybe<String> f_id_a = just(Function.<String>identity().apply("yes"));

        Maybe<String> id_f_a = Function.<Maybe<String>>identity().apply(just("yes"));

        assertThat(f_id_a).isNotSameAs(id_f_a);
        assertThat(f_id_a).isEqualTo(id_f_a);
    }


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
    void functorsShouldPreserveCompositionOfMorphisms_nothing() {
        Maybe<Integer> maybe = nothing();

        Function<Integer, String> intToString = Object::toString;
        Function<String, Integer> stringLength = String::length;

        Function<Integer, String> f = intToString;
        Function<String, Integer> g = stringLength;

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

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_just() {
        Maybe<Integer> maybe3 = just(13);

        Function<Integer, String> intToString = Object::toString;
        Function<String, Integer> stringLength = String::length;

        Function<Integer, String> f = intToString;
        Function<String, Integer> g = stringLength;

        Maybe<Integer> F1 = maybe3.map(g.compose(f));
        Maybe<Integer> F2 = maybe3.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);

        // Bonus
        assertThat(F1.getOrDefault(0)).isEqualTo(2);
        assertThat(F2.getOrDefault(0)).isEqualTo(2);
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
    void NB_whenApplyingSiblingApplicativeType_willThrowClassCastException() {
        Function<String, Function<String, Integer>> curriedStringLength =
            (string1) ->
                (string2) ->
                    string1.length() + string2.length();

        Function<String, Integer> appliedStringLength = curriedStringLength.apply("Two");

        assertThatThrownBy(
            () -> just("Three").apply(Sequence.of(appliedStringLength))
        ).isInstanceOf(ClassCastException.class)
         .hasMessageContaining("land.plainfunctional.monad.Sequence cannot be cast to land.plainfunctional.monad.Maybe");
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

        // Or, more interesting, a 'map' of curried binary functions
        maybeSum = just(1)
            .apply(just(2).map(curriedPlus))
            .apply(just(3).map(curriedPlus))
            .apply(just(4).map(curriedPlus));

        assertThat(maybeSum.tryGet()).isEqualTo(1 + 2 + 3 + 4);
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
                withMaybe(Integer.class)
                    .pure(1)
                    .apply(just(curriedPlus.apply(2)))
                    .apply(just(curriedPlus.apply(3)))
                    // NB! Partial function returning null/bottom leads to runtime error
                    .apply(just(nullFn.apply(100)))
                    .apply(just(curriedPlus.apply(4)))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null'/non-existing (/\"bottom\"/) value");

        // And when doing 'map' of curried binary functions
        assertThatThrownBy(
            () ->
                just(1)
                    .apply(just(2).map(curriedPlus))
                    .apply(just(3).map(curriedPlus))
                    .apply(just(3).map(nullFn))
                    .apply(just(4).map(curriedPlus))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null'/non-existing (/\"bottom\"/) value");
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
            .apply(of(null))
        // Won't compile
        //.apply(just("Two"))
        ;

        assertThat(maybeStringLength.isNothing()).isTrue();
    }

    @Test
    void shouldDoAlgebraicOperationsOnApplicativeFunctors_just() {
        Function<String, Integer> stringLength = String::length;

        BinaryOperator<Integer> plus = Integer::sum;

        BiFunction<Integer, Integer, Integer> biFunctionPlus = Integer::sum;

        Function<Integer, Function<Integer, Integer>> curriedPlus =
            (int1) ->
                (int2) -> plus.apply(int1, int2);

        Maybe<Integer> maybeStringLength = just(0)
            .apply(just("One").map(stringLength).map(curriedPlus))
            .apply(just("Two").map(stringLength).map(curriedPlus))
            .apply(just("Three").map(stringLength).map(curriedPlus));

        assertThat(maybeStringLength.tryGet()).isEqualTo(3 + 3 + 5);
    }

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

    /*
    @Test
    void shouldDoValidationAndStuffLikeThat_2() {
        Function<Sequence<? super String>, Function<Sequence<? extends String>, Sequence<String>>> curriedSequenceOfStringAppend =
            (list1) ->
                (list2) -> {
                    if (isEmpty(list1)) {
                        return list2;
                    }
                    List<String> stringList = list2.toJavaList();
                    stringList.addAll(list1.toJavaList());
                    return Sequence.of(stringList);
                };

        Function<Integer, Sequence<String>> getNegativeNumberInfo =
            (integer) ->
                integer < 0
                    ? Sequence.of(format("%d is a negative number", integer))
                    : Sequence.empty();

        Function<Integer, Sequence<String>> getGreaterThanTenInfo =
            (integer) ->
                integer > 10
                    ? Sequence.of(format("%d is greater than 10", integer))
                    : Sequence.empty();

        Maybe<Integer> justMinus13 = just(-13);

        Maybe<Sequence<String>> maybeInfoString = just(Sequence.empty());
        Maybe<Sequence<String>> d1 = justMinus13.map(getGreaterThanTenInfo);
        Maybe<Function<Sequence<String>, Sequence<String>>> d2 = d1.map(curriedSequenceOfStringAppend);

        maybeInfoString = maybeInfoString
            .apply(d2);

        Maybe<Sequence<String>> d3 = justMinus13.map(getNegativeNumberInfo);
        Maybe<Function<Sequence<String>, Sequence<String>>> d4 = d3.map(curriedSequenceOfStringAppend);

        maybeInfoString = maybeInfoString
            .apply(d4);

        Sequence<String> stringList = maybeInfoString.tryGet();
        assertThat(stringList.size()).isEqualTo(1);
        assertThat(stringList.toJavaList().get(0)).isEqualTo("-13 is a negative number");
    }
    */

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


        /*
        Maybe<Sequence<String>> maybeInfoString = just(Sequence.empty());
        Maybe<Function<Sequence<String>, Sequence<String>>> d2 =
            justMinus13
                .map(getNegativeNumberInfo)
                .map(curriedSequenceOfStringAppend);
        maybeInfoString = maybeInfoString.apply2(d2);

        Sequence<String> stringList = maybeInfoString.tryGet();
        assertThat(stringList.size()).isEqualTo(1);
        assertThat(stringList.toJavaList().get(0)).isEqualTo("-13 is a negative number");
        */
    }

    @Test
    void shouldDoValidationAndStuffLikeThat_4() {
        Function<String, Function<String, String>> curriedStringAppender =
            (string1) ->
                (string2) ->
                    isBlank(string2)
                        ? string1
                        : string1 + ", " + string2;

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

        LinkedHashSet<Maybe<String>> set = new LinkedHashSet<>();
        set.add(just7.map(getGreaterThanTenInfo));
        set.add(just7.map(getNegativeNumberInfo));

        BinaryOperator<Maybe<String>> operation = (maybeString1, maybeString2) -> {
            if (maybeString2.isNothing()) {
                return maybeString1;
            }
            if (maybeString1.isNothing()) {
                return maybeString2;
            }
            String string1 = maybeString1.getOrNull();
            String string2 = maybeString2.getOrNull();

            //String appendedString = curriedStringAppender.apply(string1).apply(string2);

            //return isBlank(appendedString)
            //    ? nothing()
            //    : just(appendedString
            //);

            return Maybe.ofNonBlankString(curriedStringAppender.apply(string1).apply(string2));
        };

        MonoidStructure<Maybe<String>> m = new MonoidStructure<>(set, operation, just(""));

        Maybe<String> maybeInfoString = m.fold();
        assertThat(maybeInfoString.isNothing()).isTrue();

        set.clear();
        set.add(justMinus13.map(getGreaterThanTenInfo));
        set.add(justMinus13.map(getNegativeNumberInfo));

        m = new MonoidStructure<>(set, operation, just(""));

        maybeInfoString = m.fold();
        assertThat(maybeInfoString.isNothing()).isFalse();
        assertThat(maybeInfoString.tryGet()).isEqualTo("-13 is a negative number");
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
    public void shouldHaveLeftIdentity_nothing() {
        // f (monad action)
        Function<String, Maybe<Integer>> f = (ignored) -> nothing();

        // a
        String value = null;

        // m (Maybe Nothing's data constructor)
        Function<String, Maybe<String>> m = (s) -> nothing();

        // m a (same as 'nothing()')
        Maybe<String> m_a = m.apply(value);

        assertThat(m_a.bind(f)).isSameAs(f.apply(value));
    }

    @Test
    public void shouldHaveLeftIdentity_just() {
        // f (monad action)
        Function<String, Maybe<Integer>> f = (s) -> just(s.length());

        // a
        String value = "Blue";

        // m (Maybe Just's data constructor)
        Function<String, Maybe<String>> m = Maybe::just;

        // m a (same as 'just(value)')
        //Maybe<String> m_a = just(value);
        Maybe<String> m_a = m.apply(value);

        assertThat(m_a.bind(f)).isNotSameAs(f.apply(value));
        assertThat(m_a.bind(f)).isEqualTo(f.apply(value));
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
    public void shouldHaveRightIdentity_nothing() {
        // a
        String value = null;

        // m (Maybe Nothing's data constructor)
        Function<String, Maybe<String>> m = (ignored) -> Maybe.nothing();

        // m a
        Maybe<String> m_a = m.apply(value);

        assertThat(m_a.bind(m)).isSameAs(m_a);
    }

    @Test
    public void shouldHaveRightIdentity_just() {
        // a
        String value = "myValue";

        // m (Maybe Just's data constructor)
        Function<String, Maybe<String>> m = Maybe::just;

        // m a
        Maybe<String> m_a = m.apply(value);

        assertThat(m_a.bind(m)).isNotSameAs(m_a);
        assertThat(m_a.bind(m)).isEqualTo(m_a);
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
    public void shouldHaveAssociativity_nothing() {
        // Monad actions
        Function<String, Maybe<Integer>> f = s -> just(s.length());
        Function<Integer, Maybe<Boolean>> g = i -> just(isEven(i));

        // a
        String value = "N/A";

        // m (Maybe Nothing's data constructor)
        Function<String, Maybe<String>> m = (ignored) -> Maybe.nothing();

        // m a (same as 'M a')
        Maybe<String> m_a = m.apply(value);

        Maybe<Boolean> lhs = m_a.bind(f).bind(g);
        Maybe<Boolean> rhs = m_a.bind((a) -> f.apply(a).bind(g));

        assertThat(lhs).isSameAs(rhs);
    }

    @Test
    public void shouldHaveAssociativity_just() {
        // Monad actions
        Function<String, Maybe<Integer>> f = s -> just(s.length());
        Function<Integer, Maybe<Boolean>> g = i -> just(isEven(i));

        // a
        String value = "myValue";

        // m (Maybe Just's data constructor)
        Function<String, Maybe<String>> m = Maybe::just;

        // m a (same as 'M a')
        Maybe<String> m_a = m.apply(value);

        Maybe<Boolean> lhs = m_a.bind(f).bind(g);
        Maybe<Boolean> rhs = m_a.bind((a) -> f.apply(a).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);
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

    @Test
    void shouldUsePredicatesAsMapFunctionAsFilter() {
        Maybe<Integer> maybe10 = nothing();
        Maybe<Integer> just10 = just(10);
        Maybe<Integer> just11 = just(11);


        Maybe<Boolean> maybeEven = maybe10.map(TestFunctions::isEven);

        //assertThat(maybeEven.tryGet()).isTrue();
        assertThat(maybeEven.getOrDefault(false)).isFalse();
        assertThat(maybeEven.getOrNull()).isNull();


        maybeEven = just10.map(TestFunctions::isEven);

        assertThat(maybeEven.tryGet()).isTrue();
        assertThat(maybeEven.getOrDefault(false)).isTrue();
        assertThat(maybeEven.getOrNull()).isTrue();


        maybeEven = just11.map(TestFunctions::isEven);

        assertThat(maybeEven.tryGet()).isFalse();
        assertThat(maybeEven.getOrDefault(true)).isFalse();
        assertThat(maybeEven.getOrNull()).isFalse();
    }

    @Test
    void shouldUseFoldAsFilter() {
        //Function<Integer, ?> nullFunction = (ignored) -> null;
        Supplier<Boolean> nullBooleanSupplier = () -> null;
        Supplier<?> nullSupplier = () -> null;


        Maybe<Integer> maybe10 = nothing();
        Maybe<Integer> just10 = just(10);
        Maybe<Integer> just11 = just(11);

        assertThat(nothing(Integer.class).fold(
            nullBooleanSupplier,
            TestFunctions::isEven
        )).isNull();

        assertThat(maybe10.fold(
            nullBooleanSupplier,
            TestFunctions::isEven
        )).isNull();


        Boolean isEven = just10.fold(
            //() -> { throw new IllegalStateException(); },
            //() -> null,
            (Supplier<Boolean>) nullSupplier,
            //nullBooleanSupplier,

            //(Supplier<Boolean>) nullSupplier,
            TestFunctions::isEven
        );
        assertThat(isEven).isTrue();


        isEven = just11.fold(
            nullBooleanSupplier,
            TestFunctions::isEven
        );
        assertThat(isEven).isFalse();
    }

    @Test
    void shouldUseFoldAsLens() {
        Person person = new Person();
        person.name = "John";
        person.gender = MALE;
        person.birthDate = LocalDate.of(1980, 1, 10);

        Maybe<Person> maybePerson = just(person);

        LocalDate johnsBirthDate = maybePerson.fold(
            () -> null,
            (p) -> p.birthDate
        );
        assertThat(johnsBirthDate).isEqualTo(person.birthDate);

        // Alternatively, the more "uglier"
        johnsBirthDate = maybePerson.getOrDefault(Person.IDENTITY).birthDate;
        assertThat(johnsBirthDate).isEqualTo(person.birthDate);

        // Alternatively, the even more "uglier"
        johnsBirthDate = maybePerson.getOrNull().birthDate;
        assertThat(johnsBirthDate).isEqualTo(person.birthDate);

        // Alternatively, the even even more "uglier"
        johnsBirthDate = maybePerson.tryGet().birthDate;
        assertThat(johnsBirthDate).isEqualTo(person.birthDate);
    }
}
