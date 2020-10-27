package land.plainfunctional.monad;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.Payment;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static land.plainfunctional.monad.Either.left;
import static land.plainfunctional.monad.Either.right;
import static land.plainfunctional.testdomain.TestFunctions.isEven;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class EitherSpecs {

    @Test
    void shouldCompile() {
        Either<String, Payment> right = right(new Payment());
        Either<String, Payment> left = left("Failure!");

        Function<Payment, Payment> addAmount = (myPayment) -> myPayment.amount(123.45);
        Function<Payment, Payment> addCardholderName = (myPayment) -> myPayment.cardHolderName("John James");

        Function<Payment, Payment> f = addAmount;
        Function<Payment, Payment> g = addCardholderName;

        Either<String, Payment> F1 = right.map(g.compose(f));
        Either<String, Payment> F2 = right.map(f).map(g);

        Either<String, Payment> F3 = left.map(g.compose(f));
        Either<String, Payment> F4 = left.map(f).map(g);

        //NB! Won't compile as 'String' type gets erased
        //Either<String, Payment> F5 = Either.left("Failure").map(f);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Either semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldEncapsulateValue_left() {
        Either<String, LocalDate> left = left("Left");

        assertThat(left.isLeft()).isTrue();
        assertThat(left.isRight()).isFalse();

        assertThat(left).isNotSameAs(left("Left"));
        assertThat(left).isEqualTo(left("Left"));

        // Bonus: Functor behaviour
        Either<String, LocalDate> mapped = left.map((string) -> LocalDate.now());
        assertThat(mapped.isLeft()).isTrue();

        // Bonus: Monad behaviour
        Either<String, LocalDate> next = left.bind((string) -> right(LocalDate.now()));
        assertThat(next.isLeft()).isTrue();
    }

    @Test
    void shouldEncapsulateNull_left() {
        Either<String, LocalDate> left = left(null);

        assertThat(left.isLeft()).isTrue();
        assertThat(left.isRight()).isFalse();

        assertThat(left.tryGetLeft()).isNull();
    }

    @Test
    void shouldEncapsulateValue_right() {
        Either<String, LocalDate> right = right(now());

        assertThat(right.isLeft()).isFalse();
        assertThat(right.isRight()).isTrue();

        assertThat(right).isNotSameAs(right(now()));
        assertThat(right).isEqualTo(right(now()));

        // Bonus: Functor behaviour
        Either<String, LocalDate> mapped = right.map((string) -> LocalDate.now());
        assertThat(mapped.isLeft()).isFalse();

        // Bonus: Monad behaviour
        Either<String, LocalDate> next = right.bind((string) -> right(now()));
        assertThat(next.isLeft()).isFalse();
    }

    @Test
    void whenRightIsNullValue_shouldThrowException() {
        assertThatThrownBy(
            () -> right(null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Either.Right' from a 'null' value");
    }

    @Test
    void shouldPreserveComputationalContext() {
        Either<?, ?> left = left("someLeftString")
            .map((ignored) -> right("someRightString"));

        assertThat(left).isEqualTo(left("someLeftString"));

        Either<?, ?> right = right("someRightString")
            .map((ignored) -> left("someLeftString"));

        assertThat(right).isNotEqualTo(right("someRightString"));
        assertThat(right).isNotEqualTo(left("someLeftString"));
    }

    @Test
    void shouldExtractValue() {
        Either<String, Payment> left = Either.left("Failure!");
        Either<String, Payment> right = Either.right(new Payment());

        Function<Payment, Payment> addAmount = (payment) -> payment.amount(123.45);
        Function<Payment, Payment> addCardholderName = (payment) -> payment.cardHolderName("John James");

        Function<Payment, Payment> f = addAmount;
        Function<Payment, Payment> g = addCardholderName;

        Either<String, Payment> F1 = left.map(g.compose(f));
        Either<String, Payment> F2 = left.map(f).map(g);

        Either<String, Payment> F3 = right.map(g.compose(f));
        Either<String, Payment> F4 = right.map(f).map(g);

        assertThat(F1.tryGetLeft()).isSameAs("Failure!");
        assertThat(F2.tryGetLeft()).isSameAs("Failure!");
        assertThat(F3.tryGet()).isEqualTo(new Payment().amount(123.45).cardHolderName("John James"));
        assertThat(F4.tryGet()).isEqualTo(new Payment().amount(123.45).cardHolderName("John James"));
    }

    @Test
    void shouldThrowExceptionWhenUnwrappingTheWrongValue() {
        Either<String, Payment> left = Either.left("Failure!");
        Either<String, Payment> right = Either.right(new Payment());

        Function<Payment, Payment> addAmount = (payment) -> payment.amount(123.45);
        Function<Payment, Payment> addCardholderName = (payment) -> payment.cardHolderName("John James");

        Function<Payment, Payment> f = addAmount;
        Function<Payment, Payment> g = addCardholderName;

        Either<String, Payment> F1 = left.map(g.compose(f));
        Either<String, Payment> F2 = left.map(f).map(g);

        Either<String, Payment> F3 = right.map(g.compose(f));
        Either<String, Payment> F4 = right.map(f).map(g);

        assertThatThrownBy(F1::tryGet)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Right' from a 'Left'");

        assertThatThrownBy(F2::tryGet)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Right' from a 'Left'");

        assertThatThrownBy(F3::tryGetLeft)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Left' from a 'Right'");

        assertThatThrownBy(F4::tryGetLeft)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Left' from a 'Right'");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor laws
    // See: https://wiki.haskell.org/Functor
    // See: http://eed3si9n.com/learning-scalaz/Functor+Laws.html
    ///////////////////////////////////////////////////////////////////////////

    Function<Payment, Payment> addAmount = (myPayment) -> myPayment.amount(123.45);
    Function<Payment, Payment> addCardholderName = (myPayment) -> myPayment.cardHolderName("John James");

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
    void functorsShouldPreserveIdentityMorphism_left() {
        //Either<String, Payment> f_id_a = Either.left(Basics.identity("Failure!"));
        Either<String, Payment> f_id_a = left(Function.<String>identity()
            .apply("Left"));

        Either<String, Payment> id_f_a = Function.<Either<String, Payment>>identity()
            .apply(left("Left"));

        assertThat(f_id_a).isNotSameAs(id_f_a);
        assertThat(f_id_a).isEqualTo(id_f_a);

        // Bonus
        assertThat(f_id_a.tryGetLeft()).isEqualTo("Left");
    }

    @Test
    void functorsShouldPreserveIdentityMorphism_right() {
        Either<String, Payment> f_id_a = right(Function.<Payment>identity()
            .apply(new Payment()
                .amount(123.45)
            )
        );

        Either<String, Payment> id_f_a = Function.<Either<String, Payment>>identity()
            .apply(right(
                new Payment()
                    .amount(123.45)
                )
            );

        assertThat(f_id_a).isNotSameAs(id_f_a);
        assertThat(f_id_a).isEqualTo(id_f_a);

        // Bonus
        assertThat(f_id_a.tryGet().amount).isEqualTo(123.45);
    }


    /**
     * Functors must preserve composition of morphisms:
     * map (g ∘ f) ≡ map g ∘ map f
     *
     * If two sequential mapping operations are performed one after the other using two functions,
     * the result should be the same as a single mapping operation with one function that is equivalent to applying the first function to the result of the second.
     */
    @Test
    void functorsShouldPreserveCompositionOfEndomorphisms_left() {
        Either<String, Payment> left = left("Failure!");

        Function<Payment, Payment> f = addAmount;
        Function<Payment, Payment> g = addCardholderName;

        Either<String, Payment> F1 = left.map(g.compose(f));
        Either<String, Payment> F2 = left.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);
    }

    @Test
    void functorsShouldPreserveCompositionOfEndomorphisms_right() {
        Either<String, Payment> right = right(new Payment());

        Function<Payment, Payment> f = addAmount;
        Function<Payment, Payment> g = addCardholderName;

        Either<String, Payment> F1 = right.map(g.compose(f));
        Either<String, Payment> F2 = right.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_left() {
        Either<String, Integer> left = left("Failure!");

        Function<Integer, String> intToString = Object::toString;
        Function<String, Integer> stringLength = String::length;

        Function<Integer, String> f = intToString;
        Function<String, Integer> g = stringLength;

        Either<String, Integer> F1 = left.map(g.compose(f));
        Either<String, Integer> F2 = left.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_right() {
        Either<String, Integer> three = right(3);

        Function<Integer, String> intToString = Object::toString;
        Function<String, Integer> stringLength = String::length;

        Function<Integer, String> f = intToString;
        Function<String, Integer> g = stringLength;

        Either<String, Integer> F1 = three.map(g.compose(f));
        Either<String, Integer> F2 = three.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    // TODO: Implement Either::pure?
    //@Test
    //void shouldPutValuesInThisApplicativeFunctor_left() {
    //    Either<String, LocalDate> left = withEitherLeft().pure("Failure!");
    //    assertThat(left.tryGetLeft()).isEqualTo("Failure!");
    //}

    // TODO: Implement Either::pure?
    //@Test
    //void shouldPutValuesInThisApplicativeFunctor_right() {
    //    Either<String, LocalDate> left = withEitherRight().pure("Failure!");
    //    assertThat(left.tryGet()).isEqualTo("Failure!");
    //}

    // TODO: Composition of applicative functors

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


        Either<String, Integer> rightMinus13 = right(-13);
        Either<String, Integer> right7 = right(7);


        Either<?, String> eitherInfoString = right("")
            .apply(rightMinus13
                .map(getGreaterThanTenInfo)
                .map(curriedStringAppender)
            )
            .apply(rightMinus13
                .map(getNegativeNumberInfo)
                .map(curriedStringAppender)
            ).fold(
                () -> left(null),
                (string) -> isNotBlank(string) ? left(string) : right(null)
            );

        assertThat(eitherInfoString.isLeft()).isTrue();
        assertThat(eitherInfoString.isRight()).isFalse();
        assertThat(eitherInfoString.tryGetLeft()).isEqualTo("-13 is a negative number");


        eitherInfoString = right("")
            .apply(right7
                .map(getGreaterThanTenInfo)
                .map(curriedStringAppender))
            .apply(right7
                .map(getNegativeNumberInfo)
                .map(curriedStringAppender))
            .fold(
                () -> left(null),
                (string) -> isNotBlank(string) ? left(string) : right("")
            );

        assertThat(eitherInfoString.tryGet()).isNotNull();
        assertThat(eitherInfoString.tryGet()).isBlank();
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
     * - 'a' is a parametrically typed value/generic value
     * - 'f' is the monad action function - 'f' has the same (Haskell-style) type signature as 'return': a -> m a
     */
    @Test
    public void whenLeftValue_forLeftIdentity_shouldNotCompile() {
        // a
        String failureMessage = "Left";

        // m (Either Left's data constructor)
        Function<String, Either<String, Payment>> m = Either::left;

        // m a
        Either<String, Payment> m_a = m.apply(failureMessage);

        // (m a).bind(f)
        // NB! Does not compile
        //Either<String, Payment> m_a_bind_f = m_a.bind(m);
    }

    @Test
    public void shouldHaveLeftIdentity() {
        // a
        Payment payment = new Payment().amount(123.45);

        // m (Either Right's data constructor)
        Function<Payment, Either<String, Payment>> m = Either::right;

        // m a
        //Either<String, Payment> right = Either.right(payment);
        Either<String, Payment> m_a = m.apply(payment);

        assertThat(m_a.bind(m)).isNotSameAs(m_a);
        assertThat(m_a.bind(m)).isEqualTo(m_a);
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
     * - 'a' parametrically typed value/generic value
     * - 'm a' is a value in a monad (in a monadic context) - same as 'return a' above
     */
    @Test
    public void whenLeftValue_forRightIdentity_shouldNotCompile() {
        // a
        String failureMessage = "Left";

        // m (Either Left's data constructor)
        Function<String, Either<String, Payment>> m = Either::left;

        // m a
        Either<String, Payment> m_a = m.apply(failureMessage);

        // NB! Does not compile
        //Either<String, Payment> lhs = m_a.bind(m);
        Either<String, Payment> rhs = m_a;

        //assertThat(lhs).isNotSameAs(rhs);
        //assertThat(lhs).isEqualTo(rhs);
    }

    @Test
    public void shouldHaveRightIdentity() {
        // a
        Payment payment = new Payment().amount(123.45);

        // m (Either Right's data constructor)
        Function<Payment, Either<String, Payment>> m = Either::right;

        // m a
        //Either<String, Payment> m_a = Either.right(payment);
        Either<String, Payment> m_a = m.apply(payment);

        Either<String, Payment> lhs = m_a.bind(m);
        Either<String, Payment> rhs = m_a;

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);
    }


    Function<String, Either<String, Integer>> eitherStringLengthOrFailureMessage =
        (s) -> right(s.length());

    Function<Integer, Either<String, Boolean>> eitherEvenOrFailureMessage =
        (i) -> !isEven(i)
            ? left("Not even")
            : right(true);

    /**
     * Associativity:
     * When we have a chain of monadic function applications with >>= ('bind')
     * it should not matter how they are nested.
     *
     * Haskell:
     * (m >>= f) >>= g ≡ m >>= (λx -> f x >>= g)
     */
    @Test
    public void shouldHaveAssociativity_left() {
        // a
        String myString = "Left";

        // m (Either Left's data constructor)
        Function<String, Either<String, String>> m = Either::left;

        // m a
        Either<String, String> m_a = m.apply(myString);

        Function<String, Either<String, Integer>> f = eitherStringLengthOrFailureMessage;
        Function<Integer, Either<String, Boolean>> g = eitherEvenOrFailureMessage;

        Either<String, Boolean> lhs = m_a.bind(f).bind(g);
        Either<String, Boolean> rhs = m_a.bind(x -> f.apply(x).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);

        // Bonus checks
        assertThatThrownBy(lhs::tryGet)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Right' from a 'Left'");

        assertThat(lhs.tryGetLeft()).isEqualTo("Left");

        assertThatThrownBy(rhs::tryGet)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Right' from a 'Left'");

        assertThat(rhs.tryGetLeft()).isEqualTo("Left");
    }

    @Test
    public void shouldHaveAssociativity_right_turnedLeft() {
        // a
        String myString = "notEvenLengthString";

        // m (Either Right's data constructor)
        Function<String, Either<String, String>> m = Either::right;

        // m a
        //Either<String, String> m_a_2 = Either.right(myString);
        Either<String, String> m_a = m.apply(myString);

        Function<String, Either<String, Integer>> f = eitherStringLengthOrFailureMessage;
        Function<Integer, Either<String, Boolean>> g = eitherEvenOrFailureMessage;

        Either<String, Boolean> lhs = m_a.bind(f).bind(g);
        Either<String, Boolean> rhs = m_a.bind(x -> f.apply(x).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);

        // Bonus checks
        assertThatThrownBy(lhs::tryGet)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Right' from a 'Left'");

        assertThat(lhs.tryGetLeft()).isEqualTo("Not even");

        assertThatThrownBy(rhs::tryGet)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Right' from a 'Left'");

        assertThat(rhs.tryGetLeft()).isEqualTo("Not even");
    }

    @Test
    public void shouldHaveAssociativity_right_allTheWayThrough() {
        // a
        String myString = "evenLengthString";

        // m (Either Right's data constructor)
        Function<String, Either<String, String>> m = Either::right;

        // m a
        //Either<String, String> m_a_2 = Either.right(myString);
        Either<String, String> m_a = m.apply(myString);

        Function<String, Either<String, Integer>> f = eitherStringLengthOrFailureMessage;
        Function<Integer, Either<String, Boolean>> g = eitherEvenOrFailureMessage;

        Either<String, Boolean> lhs = m_a.bind(f).bind(g);
        Either<String, Boolean> rhs = m_a.bind(x -> f.apply(x).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        assertThat(lhs).isEqualTo(rhs);

        // Bonus checks
        assertThatThrownBy(lhs::tryGetLeft)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Left' from a 'Right'");

        assertThat(lhs.tryGet()).isTrue();

        assertThatThrownBy(rhs::tryGetLeft)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Trying to get a 'Left' from a 'Right'");

        assertThat(rhs.tryGet()).isTrue();
    }
}
