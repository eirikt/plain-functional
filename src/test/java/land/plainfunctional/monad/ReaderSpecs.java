package land.plainfunctional.monad;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.testdomain.TestFunctions;
import land.plainfunctional.testdomain.vanillaecommerce.Customer;
import land.plainfunctional.testdomain.vanillaecommerce.Person;
import land.plainfunctional.testdomain.vanillaecommerce.VipCustomer;
import land.plainfunctional.util.RandomUtils;

import static java.lang.Integer.sum;
import static java.lang.Thread.sleep;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static land.plainfunctional.monad.Maybe.just;
import static land.plainfunctional.monad.Maybe.nothing;
import static land.plainfunctional.monad.Reader.asReader;
import static land.plainfunctional.monad.Reader.asReaderOfType;
import static land.plainfunctional.testdomain.TestFunctions.isEven;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Arrays.array;

class ReaderSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Random test values
    ///////////////////////////////////////////////////////////////////////////

    RandomUtils random = new RandomUtils();


    ///////////////////////////////////////////////////////////////////////////
    // Test suppliers
    // 'Supplier' instances are "nullary" functions (kind of a deferred value)
    // Here they will NOT be "deferred" constants, as the functions will neither be pure nor total, but partial.
    // For this implementation, as the function takes no input parameter the Reader monad's "environment" is implicit,
    // it will just be the 'Supplier' instance.
    //
    ///////////////////////////////////////////////////////////////////////////

    Supplier<?> runtimeExceptionSupplier = RuntimeException::new;
    Supplier<?> throwRuntimeExceptionSupplier =
        () -> {
            throw new RuntimeException();
        };
    Supplier<?> nullSupplier = () -> null;


    Supplier<Integer> zeroSupplier = () -> 0;
    Supplier<Integer> deferredZero = zeroSupplier;


    Supplier<Integer> oneSupplier = () -> 1;
    Supplier<Integer> deferredOne = () -> 1;
    Supplier<Integer> delayedOneSupplier =
        () -> {
            try {
                sleep(200); // ms
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return oneSupplier.get();
        };
    Supplier<Maybe<Integer>> delayedJustOneSupplier =
        () -> {
            try {
                sleep(random.pickRandomInteger(200, 5000)); // ms
                return just(oneSupplier.get());

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        };
    Supplier<Maybe<Integer>> delayedMaybeOneSupplier =
        () -> {
            try {
                sleep(random.pickRandomInteger(200, 5000)); // ms
                return random.pickRandomBoolean()
                    ? just(oneSupplier.get())
                    : nothing();

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        };


    Supplier<Integer> randomNumberSupplier = () -> random.pickRandomIntegerBetweenOneAnd(1000);

    Supplier<Maybe<Integer>> delayedMaybeRandomNumberSupplier =
        () -> {
            Instant start = now();
            try {
                sleep(random.pickRandomInteger(200, 3000)); // ms
                Maybe<Integer> maybeInteger = random.pickRandomBoolean()
                    ? just(randomNumberSupplier.get())
                    : nothing();
                System.out.printf(
                    "'delayedMaybeRandomNumberSupplier' -> %s, took %d ms%n",
                    maybeInteger.toStringMaybe(),
                    between(start, now()).toMillis()
                );
                return maybeInteger;

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        };

    // Emulating a typical remote operation, which includes a time-out mechanism
    static Integer GET_DELAYED_RANDOM_INT_OR_TIMEOUT(RandomUtils random) {
        Instant start = now();
        try {
            sleep(random.pickRandomInteger(200, 3000)); // ms
            boolean successfulEvaluation = random.pickRandomBoolean();
            if (successfulEvaluation) {
                int evaluatedValue = random.pickRandomIntegerBetweenOneAnd(1000);
                System.out.printf(
                    "'GET_DELAYED_RANDOM_INT_OR_TIMEOUT' -> %s, took %d ms%n",
                    evaluatedValue,
                    between(start, now()).toMillis()
                );
                return evaluatedValue;
            }
            System.err.printf(
                "'GET_DELAYED_RANDOM_INT_OR_TIMEOUT' -> %s, took %d ms%n",
                "runtime exception",
                between(start, now()).toMillis()
            );
            throw new RuntimeException(new TimeoutException("This took way too long!"));

        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    Supplier<Integer> delayedRandomNumberOrBottomSupplier = () -> GET_DELAYED_RANDOM_INT_OR_TIMEOUT(random);
    Reader<Integer> delayedRandomNumberOrBottomReader = Reader.of(delayedRandomNumberOrBottomSupplier);


    Supplier<LocalDate> todaySupplier = LocalDate::now;
    Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;


    Supplier<String> helloWorldSupplier = () -> "Hello World!";
    Supplier<String> delayedHelloWorldSupplier =
        () -> {
            try {
                sleep(200); // ms
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return helloWorldSupplier.get();
        };


    Supplier<String> randomStringSupplier = () -> random.generateRandomString(2, 6);

    // Emulating a typical over-the-wire invocation, which includes a time-out mechanism
    /*
    Supplier<String> delayedRandomStringOrBottomSupplier =
        () -> {
            Instant start = now();
            try {
                sleep(random.pickRandomInteger(200, 3000)); // ms
                boolean successfulEvaluation = random.pickRandomBoolean();
                if (successfulEvaluation) {
                    String evaluatedValue = randomStringSupplier.get();
                    System.out.printf(
                        "'delayedRandomStringOrBottomSupplier' -> %s, took %d ms%n",
                        evaluatedValue,
                        between(start, now()).toMillis()
                    );
                    return evaluatedValue;
                }
                System.out.printf(
                    "'delayedRandomStringOrBottomSupplier' -> %s, took %d ms%n",
                    "runtime exception",
                    between(start, now()).toMillis()
                );
                throw new RuntimeException(new TimeoutException("This took way too long!"));

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        };
    */
    static String GET_DELAYED_RANDOM_STRING_OR_TIMEOUT(RandomUtils random) {
        Instant start = now();
        try {
            sleep(random.pickRandomInteger(200, 3000)); // ms
            boolean successfulEvaluation = random.pickRandomBoolean();
            if (successfulEvaluation) {
                String evaluatedValue = random.generateRandomString(2, 6);
                System.out.printf(
                    "'delayedRandomStringOrBottomSupplier' -> %s, took %d ms%n",
                    evaluatedValue,
                    between(start, now()).toMillis()
                );
                return evaluatedValue;
            }
            System.out.printf(
                "'delayedRandomStringOrBottomSupplier' -> %s, took %d ms%n",
                "runtime exception",
                between(start, now()).toMillis()
            );
            throw new RuntimeException(new TimeoutException("This took way too long!"));

        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    Supplier<String> delayedRandomStringOrBottomSupplier = () -> GET_DELAYED_RANDOM_STRING_OR_TIMEOUT(random);
    Reader<String> delayedRandomStringOrBottomReader = Reader.of(delayedRandomStringOrBottomSupplier);


    ///////////////////////////////////////////////////////////////////////////
    // Test monoids
    ///////////////////////////////////////////////////////////////////////////

    public static final FreeMonoid<Integer> INTEGERS_UNDER_ADDITION_MONOID =
        new FreeMonoid<>(
            Integer::sum,
            0
        );


    ///////////////////////////////////////////////////////////////////////////
    // Reader properties
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldHoldValue_1() {
        Reader<String> reader = Reader.of(helloWorldSupplier);

        assertThat(reader.tryGet()).isSameAs("Hello World!");
    }

    @Test
    void shouldHoldValue_2() {
        Reader<String> reader = Reader.of(delayedHelloWorldSupplier);

        assertThat(reader.tryGet()).isSameAs("Hello World!");
    }

    @Test
    void shouldBeDeferred_1a() {
        Reader<?> lazy = Reader.of(throwRuntimeExceptionSupplier);

        assertThatThrownBy(lazy::tryGet).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldBeDeferred_1b() {
        Reader<?> lazy = Reader.of(runtimeExceptionSupplier);

        assertThat(lazy.tryGet()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldBeDeferred_2() {
        Reader<?> lazy = Reader.of(nullSupplier);

        assertThat(lazy.tryGet()).isNull();
    }

    @Test
    void shouldProhibitNullAsConstructorArgs() {
        assertThatThrownBy(() -> Reader.of((Supplier<?>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'Reader' cannot handle 'null' suppliers");
    }

    @Test
    void shouldBeCovariant() {
        Person person = new Person();
        person.name = "Paul";

        Customer customer = new Customer();
        customer.name = "Chris";

        Customer customer2 = new Customer();
        customer2.name = "Chrissie";

        VipCustomer vipCustomer = new VipCustomer();
        vipCustomer.vipCustomerSince = OffsetDateTime.now();

        Reader<Person> lazyPerson = Reader.of(vipCustomer);
        Reader<Customer> lazyCustomer = Reader.of(vipCustomer);
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
        String a = "Yes";
        String id_a = "";

        // TODO: Is this correctly set up?
        Reader<String> f_id_a = Reader.of(id_a);
        Reader<String> id_f_a = Function.<Reader<String>>identity().apply(f_id_a);

        assertThat(f_id_a).isSameAs(id_f_a);

        String folded1 = f_id_a.tryGet();
        String folded2 = id_f_a.tryGet();

        assertThat(folded1).isSameAs(folded2);
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
        Reader<Integer> lazyNumberOne = Reader.of(oneSupplier);

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Reader<Integer> F1 = lazyNumberOne.map(g.compose(f));
        Reader<Integer> F2 = lazyNumberOne.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F2);

        // Bonus
        Reader<Integer> F3 = lazyNumberOne.map(f.andThen(g));
        assertThat(F1).isNotSameAs(F3);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F3);

        assertThat(lazyNumberOne.tryGet()).isSameAs(1);
        assertThat(F1.tryGet()).isSameAs(1 + 13 - 5);
        assertThat(F2.tryGet()).isSameAs(1 + 13 - 5);
        assertThat(F3.tryGet()).isSameAs(1 + 13 - 5);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_2() {

        Reader<Maybe<Integer>> lazyMaybeNumberOne = Reader.of(delayedJustOneSupplier);

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Maybe<Integer>, Maybe<Integer>> f = (maybeInteger) -> maybeInteger.map(plus13);
        Function<Maybe<Integer>, Maybe<Integer>> g = (maybeInteger) -> maybeInteger.map(minus5);

        Reader<Maybe<Integer>> F1 = lazyMaybeNumberOne.map(g.compose(f));
        Reader<Maybe<Integer>> F2 = lazyMaybeNumberOne.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F2);

        // Bonus
        Reader<Maybe<Integer>> F3 = lazyMaybeNumberOne.map(f.andThen(g));
        assertThat(F1).isNotSameAs(F3);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F3);

        assertThat(lazyMaybeNumberOne.tryGet().tryGet()).isSameAs(1);
        assertThat(F1.tryGet().tryGet()).isSameAs(1 + 13 - 5);
        assertThat(F2.tryGet().tryGet()).isSameAs(1 + 13 - 5);
        assertThat(F3.tryGet().tryGet()).isSameAs(1 + 13 - 5);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldPutValuesInThisApplicativeFunctor() {
        Reader<?> reader = asReader().pure("JustDoIt");

        assertThat(reader.tryGet()).isEqualTo("JustDoIt");
    }

    @Test
    void shouldPutTypedValuesInThisApplicativeFunctor() {
        Reader<String> reader = asReaderOfType(String.class).pure("JustDoIt");

        assertThat(reader.tryGet()).isEqualTo("JustDoIt");
    }

    @Test
    void shouldComposeApplicativeFunctors() {
        Reader<String> oneStringReader = Reader.of(() -> "One");
        Reader<String> twoStringReader = Reader.of(() -> "Two");
        Reader<String> threeStringReader = Reader.of(() -> "Three");

        Reader<Integer> stringLengthSumReader = INTEGERS_UNDER_ADDITION_MONOID
            .toReaderIdentity()
            .apply(
                oneStringReader
                    .map(String::length)
                    .map(INTEGERS_UNDER_ADDITION_MONOID.curriedBinaryOperation())
            ).apply(
                twoStringReader
                    .map(String::length)
                    .map(INTEGERS_UNDER_ADDITION_MONOID.curriedBinaryOperation())
            ).apply(
                threeStringReader
                    .map(String::length)
                    .map(INTEGERS_UNDER_ADDITION_MONOID.curriedBinaryOperation())
            );

        assertThat(stringLengthSumReader.tryGet()).isEqualTo(3 + 3 + 5);
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
        //Function<String, Reader<Integer>> f = (s) -> Reader.of(() -> s.length());
        Function<String, Reader<Integer>> f = (s) -> Reader.of(s::length);

        // a
        String value = "Blue";

        // m (Data constructor)
        Function<String, Reader<String>> m = Reader::of;

        // m a
        Reader<String> m_a = m.apply(value);

        Reader<Integer> m_a_bind_f = m_a.bind(f);
        Reader<Integer> f_apply_value = f.apply(value);
        assertThat(m_a_bind_f).isNotSameAs(f_apply_value);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(m_a_bind_f).isEqualTo(f_apply_value);

        assertThat(m_a.bind(f)).isNotSameAs(f.apply(value));
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(m_a.bind(f)).isEqualTo(f.apply(value));

        Integer i = m_a_bind_f.tryGet();
        assertThat(i).isEqualTo(4);
        assertThat(f_apply_value.tryGet()).isEqualTo(4);
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

        // m (Data constructor)
        Function<String, Reader<String>> m = Reader::of;

        // m a
        Reader<String> m_a = m.apply(value);

        Reader<String> lhs = m_a.bind(m);
        Reader<String> rhs = m_a;

        assertThat(lhs).isNotSameAs(rhs);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(lhs).isEqualTo(rhs);

        assertThat(lhs.tryGet()).isEqualTo("Go");
        assertThat(rhs.tryGet()).isEqualTo("Go");
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
        Reader<String> m_a = Reader.of(() -> value);

        Function<String, Reader<Integer>> f = (string) -> Reader.of(string::length);
        Function<Integer, Reader<Boolean>> g = (integer) -> Reader.of(() -> isEven(integer));

        Reader<Boolean> lhs = m_a.bind(f).bind(g);
        Reader<Boolean> rhs = m_a.bind((a) -> f.apply(a).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(lhs).isEqualTo(rhs);

        assertThat(lhs.tryGet()).isTrue();
        assertThat(rhs.tryGet()).isTrue();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Misc. applications of lazy evaluation
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenMapping_shouldDeferExecution_1() {
        Instant start = now();

        Reader<Boolean> helloWorldLengthIsEvenNumberReader = Reader
            .of(delayedHelloWorldSupplier) // 200 ms delay
            .map(String::length)
            .map(TestFunctions::isEven)
            .map(TestFunctions::isTrue);

        //assertThat(between(start, now()).toMillis()).isLessThan(10); // ms
        // Blocks!
        assertThat(helloWorldLengthIsEvenNumberReader.tryGet()).isTrue();
        assertThat(between(start, now()).toMillis()).isGreaterThan(200); // ms
    }

    @Test
    void whenMapping_shouldDeferExecution_2() {
        Instant start = now();

        Reader<Maybe<Integer>> helloWorldLengthIsEvenNumberReader = Reader
            .of(delayedMaybeRandomNumberSupplier) // 200 - 3000 ms
            .map(new Function<Maybe<Integer>, Maybe<Integer>>() {
                @Override
                public Maybe<Integer> apply(Maybe<Integer> integerMaybe) {
                    // TODO: NB! This is not allowed! 'map' shouldn't change computational context
                    return nothing();
                }
            })
            .map(new Function<Maybe<Integer>, Maybe<Integer>>() {
                @Override
                public Maybe<Integer> apply(Maybe<Integer> maybeInteger) {
                    return maybeInteger.map(new Function<Integer, Integer>() {
                        @Override
                        public Integer apply(Integer integer) {
                            System.out.printf("Adding 1 to %d%n", integer);
                            return integer + 1;
                        }
                    });
                }
            })
            .map((maybeInteger) -> maybeInteger
                .map((integer) -> {
                    System.out.printf("Adding 1 to %d%n", integer);
                    return integer + 1;
                }))
            .map((maybeInteger) -> maybeInteger.map((integer) -> integer + 1))
            .map((maybeInteger) -> maybeInteger.map((integer) -> integer + 1));

        assertThat(between(start, now()).toMillis()).isLessThan(10); // ms
        System.out.printf(
            "Evaluation: %s, took %d ms",
            // Blocks!
            helloWorldLengthIsEvenNumberReader.toMaybe().toStringMaybe(),
            between(start, now()).toMillis()
        );
        assertThat(between(start, now()).toMillis()).isGreaterThan(200); // ms
    }

    @Test
    void whenBinding_shouldDeferExecution_1() {
        Instant start = now();

        Reader<Maybe<Integer>> helloWorldLengthIsEvenNumberReader = Reader
            .of(delayedMaybeRandomNumberSupplier)
            .bind(new Function<Maybe<Integer>, Reader<Maybe<Integer>>>() {
                @Override
                public Reader<Maybe<Integer>> apply(Maybe<Integer> maybeInteger) {
                    return Reader.of(
                        () -> maybeInteger.map(new Function<Integer, Integer>() {
                            @Override
                            public Integer apply(Integer integer) {
                                return integer + 1;
                            }
                        })
                    );
                }
            })
            .bind((maybeInteger) -> Reader.of(
                () -> maybeInteger.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) {
                        return integer + 1;
                    }
                })
            ))
            .bind((maybeInteger) -> Reader.of(
                () -> maybeInteger.map(
                    (integer) -> integer + 1)
            ))
            .bind((maybeInteger) -> Reader.of(maybeInteger.map((integer) -> integer + 1)));

        assertThat(between(start, now()).toMillis()).isLessThan(10); // ms

        System.out.printf(
            "Evaluation: %s, took %d ms",
            // Blocks!
            helloWorldLengthIsEvenNumberReader.toMaybe().toStringMaybe(),
            between(start, now()).toMillis()
        );

        assertThat(between(start, now()).toMillis()).isGreaterThan(200); // ms
    }


    public static final Function<Integer, Reader<Integer>> INCR_AS_READER_MONAD_ACTION =
        (integer) ->
            Reader.of(integer + 1);

    public static final Function<Integer, Reader<Integer>> INCR = INCR_AS_READER_MONAD_ACTION;

    @Test
    void whenBinding_shouldDeferExecution_2() {
        Instant start = now();

        Supplier<Integer> slowAndFragileOperation = delayedRandomNumberOrBottomSupplier;

        Function<Integer, Reader<Integer>> monadAction1 = (integer) -> Reader
            .of(slowAndFragileOperation)
            .apply(
                Reader
                    .of(integer)
                    .map(INTEGERS_UNDER_ADDITION_MONOID.curriedBinaryOperation())
            );

        Function<Integer, Reader<Integer>> monadAction2 = (integer) -> Reader
            .of(integer)
            .apply(
                Reader
                    .of(slowAndFragileOperation)
                    .map(INTEGERS_UNDER_ADDITION_MONOID.curriedBinaryOperation())
            );

        Reader<Integer> helloWorldLengthIsEvenNumberReader =
            INTEGERS_UNDER_ADDITION_MONOID
                .toReaderIdentity()
                .then(new Function<Integer, Reader<Integer>>() {
                    @Override
                    public Reader<Integer> apply(Integer integer) {
                        return Reader.of(integer + 1);
                    }
                })
                .then(INCR_AS_READER_MONAD_ACTION)
                .then(INCR)
                .then(monadAction1)
                .then(monadAction2);

        assertThat(between(start, now()).toMillis()).isLessThan(10); // ms

        System.out.printf(
            "Evaluation: %s, took %d ms",
            // NB! 'toMaybe' blocks and causes "all-or-nothing" semantics for the recursive evaluation of the "nullary" functions/'Supplier's
            helloWorldLengthIsEvenNumberReader.toMaybe().toStringMaybe(),
            between(start, now()).toMillis()
        );

        assertThat(between(start, now()).toMillis()).isGreaterThan(200); // ms
    }


    // Not supported with inferred types
    //static <T> Predicate<Maybe<T>> getOnlyValuesPredicate = (maybe) -> !maybe.isNothing();

    static <T> Predicate<Maybe<T>> getOnlyValuesPredicate() {
        return maybe -> !maybe.isNothing();
    }

    static <T> Predicate<Maybe<T>> onlyValues() {
        return getOnlyValuesPredicate();
    }

    // NB! Ad-hoc experimentations
    //@Test
    void shouldFoldDifferentStructures() {

        // Sequence<Integer>
        System.out.println();
        System.out.println("Folding 'Sequence<Integer>'");

        Sequence<Integer> sequenceOfIntegers = Sequence.of(0, 1, 2, 3, 4);
        Integer foldedValue = sequenceOfIntegers.foldLeft(
            0,
            new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer int1, Integer int2) {
                    return int1 + int2;
                }
            });
        assertThat(foldedValue).isEqualTo(0 + 1 + 2 + 3 + 4);


        // Sequence<Maybe<Integer>>
        System.out.println();
        System.out.println("Folding 'Sequence<Maybe<Integer>>' using 'Maybe::getOrDefault'");
        Sequence<Maybe<Integer>> sequenceOfMaybeIntegers = Sequence.of(
            just(0),
            just(1),
            just(2),
            nothing(),
            just(3),
            nothing()
        );
        Maybe<Integer> foldedMaybeValue = sequenceOfMaybeIntegers.foldLeft(
            nothing(),
            (maybeInt1, maybeInt2) ->
                just(maybeInt1.getOrDefault(0) + maybeInt2.getOrDefault(0))
        );
        assertThat(foldedMaybeValue.tryGet()).isEqualTo(0 + 1 + 2 + 3);


        // Sequence<Maybe<Integer>>
        System.out.println();
        System.out.println("Folding 'Sequence<Maybe<Integer>>' using filtering");
        //Collection<Maybe<Integer>> maybeIntegers = new ArrayList<>();
        //maybeIntegers.add(just(1));
        //maybeIntegers.add(nothing());
        //maybeIntegers.add(just(2));
        //maybeIntegers.add(just(3));
        //maybeIntegers.add(nothing());

        Maybe<Integer>[] maybeIntegerArray = array(
            just(0),
            just(1),
            nothing(),
            just(2),
            just(3),
            nothing()
        );
        foldedMaybeValue = Sequence
            //.of(maybeIntegers)                // OK
            //.of(asList(maybeIntegerArray))    // OK
            .of(maybeIntegerArray)
            .keep(onlyValues())
            .foldLeft(
                just(0),
                (maybeInt1, maybeInt2) ->
                    just(maybeInt1.tryGet() + maybeInt2.tryGet())
            );
        assertThat(foldedMaybeValue.tryGet()).isEqualTo(0 + 1 + 2 + 3);


        // Sequence<Reader<Maybe<Integer>>>
        System.out.println();
        System.out.println("Folding 'Sequence<Reader<Maybe<Integer>>>' using 'Maybe::getOrDefault'");
        Sequence<Reader<Maybe<Integer>>> sequenceOfMaybeIntegerReaders = Sequence.of(
            Reader.of(delayedMaybeRandomNumberSupplier),
            Reader.of(delayedMaybeRandomNumberSupplier),
            Reader.of(delayedMaybeRandomNumberSupplier)
        );
        Reader<Maybe<Integer>> foldedMaybeIntegerReader = sequenceOfMaybeIntegerReaders.foldLeft(
            Reader.of(Maybe::nothing),
            (lazyMaybeInt1, lazyMaybeInt2) ->
                Reader.of(
                    () ->
                        Maybe.just(
                            lazyMaybeInt1.tryGet().getOrDefault(0) +
                                lazyMaybeInt2.tryGet().getOrDefault(0)
                        )
                )
        );
        // Blocks!
        foldedValue = foldedMaybeIntegerReader.tryGet().tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Using Reader's built-in Maybe transformation 1
        // Sequence<Reader<Maybe<Integer>>>
        System.out.println();
        System.out.println("Folding 'Sequence<Reader<Maybe<Integer>>>' using 'Reader::toMaybe' (1)");
        sequenceOfMaybeIntegerReaders = Sequence.of(
            Reader.of(delayedMaybeRandomNumberSupplier),
            Reader.of(delayedMaybeRandomNumberSupplier),
            Reader.of(delayedMaybeRandomNumberSupplier)
        );
        foldedMaybeIntegerReader = sequenceOfMaybeIntegerReaders.foldLeft(
            Reader.of(Maybe::nothing),
            (lazyMaybeInt1, lazyMaybeInt2) ->
                Reader.of(
                    () ->
                        Maybe.just(
                            lazyMaybeInt1.toMaybe().tryGet().getOrDefault(0) +
                                lazyMaybeInt2.toMaybe().tryGet().getOrDefault(0)
                        )
                )
        );
        // Blocks!
        foldedValue = foldedMaybeIntegerReader.tryGet().tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Using Reader's built-in Maybe transformation 2
        // Sequence<Reader<Integer>>
        System.out.println();
        System.out.println("Folding \"flaky\" 'Sequence<Reader<Integer>>' using 'Reader::toMaybe' (2)");
        Sequence<Reader<Integer>> sequenceOfIntegerReaders = Sequence.of(
            Reader.of(delayedRandomNumberOrBottomSupplier),
            Reader.of(delayedRandomNumberOrBottomSupplier),
            Reader.of(delayedRandomNumberOrBottomSupplier)
        );
        Reader<Integer> foldedLazyInteger = sequenceOfIntegerReaders.foldLeft(
            Reader.of(() -> 0),
            (lazyInt1, lazyInt2) ->
                Reader.of(() ->
                    lazyInt1.toMaybe().getOrDefault(0) +
                        lazyInt2.toMaybe().getOrDefault(0)
                )
        );
        // Blocks!
        foldedValue = foldedLazyInteger.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Using Reader's built-in Maybe transformation 3 (+ a free monoid for folding)
        // Sequence<Lazy<Integer>>
        System.out.println();
        System.out.println("Map-folding (\"MapReducing\") \"flaky\" 'Sequence<Reader<Integer>>' using 'Reader::toMaybe' (3)");
        sequenceOfIntegerReaders = Sequence.of(
            Reader.of(delayedRandomNumberOrBottomSupplier),
            Reader.of(delayedRandomNumberOrBottomSupplier),
            Reader.of(delayedRandomNumberOrBottomSupplier)
        );
        Integer foldedSequenceOfIntegers =
            sequenceOfIntegerReaders
                // Blocks!
                .map((intReader) -> intReader.toMaybe().getOrDefault(0))
                .foldLeft(INTEGERS_UNDER_ADDITION_MONOID);
        System.out.println(foldedSequenceOfIntegers);
        assertThat(foldedSequenceOfIntegers).isGreaterThanOrEqualTo(0);


        // Sequence<Reader<Integer>>
        System.out.println();
        System.out.println("Using on-the-fly monoid structure for folding \"flaky\" 'Sequence<Reader<Integer>>'");

        Integer identity = 0;
        Reader<Integer> lazyIdentity = Reader.of(() -> identity);
        BinaryOperator<Reader<Integer>> lazyPlus = (int1, int2) ->
            Reader.of(() ->
                sum(
                    int1.toMaybe().getOrDefault(identity),
                    int2.toMaybe().getOrDefault(identity)
                )
            );
        sequenceOfIntegerReaders = Sequence.of(
            Reader.of(delayedRandomNumberOrBottomSupplier),
            Reader.of(delayedRandomNumberOrBottomSupplier),
            Reader.of(delayedRandomNumberOrBottomSupplier)
        );
        foldedLazyInteger = sequenceOfIntegerReaders.toMonoid(lazyIdentity, lazyPlus).fold();
        // Blocks!
        foldedValue = foldedLazyInteger.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        foldedValue = sequenceOfIntegerReaders
            .toMonoid(lazyIdentity, lazyPlus)
            .fold()
            // Blocks!
            .tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        // Sequence<Reader<Integer>>
        System.out.println();
        System.out.println("Using 'FreeMonoid' for folding \"flaky\" 'Sequence<Reader<Integer>>'");

        FreeMonoid<Reader<Integer>> freeMonoid = new FreeMonoid<>(lazyPlus, lazyIdentity);

        sequenceOfIntegerReaders = Sequence.of(
            delayedRandomNumberOrBottomReader,
            delayedRandomNumberOrBottomReader,
            delayedRandomNumberOrBottomReader
        );
        foldedLazyInteger = sequenceOfIntegerReaders.toMonoid(freeMonoid).fold();
        // Blocks! ?
        foldedValue = foldedLazyInteger.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        foldedValue = sequenceOfIntegerReaders
            .toMonoid(freeMonoid)
            .fold()
            // Blocks!
            .tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Ad-hoc experimentations
    //@Test
    void shouldFoldDifferentStructures_2() {
        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.println("#1");
        Reader<Integer> integerReader = Reader
            //.of(deferredZero) // Deferred identity value - together with 'curriedPlus' it forms a monoid
            .of(monoid.deferredIdentity())
            .apply(Reader
                //.of(delayedRandomNumberOrBottomSupplier)
                .of(randomNumberSupplier)
                //.map(curriedPlus)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Reader
                .of(randomNumberSupplier)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Reader
                .of(randomNumberSupplier)
                .map(monoid.curriedBinaryOperation())
            );
        int foldedValue = integerReader.toMaybe().getOrDefault(0);
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        System.out.println();
        System.out.println("#2");
        // TODO: Short-circuits computation with the first bottom...
        integerReader = Reader
            //.of(deferredZero) // Deferred identity value - together with 'curriedPlus' it forms a monoid
            .of(monoid.deferredIdentity())
            .apply(Reader
                .of(delayedRandomNumberOrBottomSupplier)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Reader
                .of(delayedRandomNumberOrBottomSupplier)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Reader
                .of(delayedRandomNumberOrBottomSupplier)
                .map(monoid.curriedBinaryOperation())
            );
        // Blocks!
        foldedValue = integerReader.toMaybe().getOrDefault(0);
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        System.out.println();
        System.out.println("#3");
        integerReader = Reader
            //.of(() -> 0)
            //.of(deferredZero) // Deferred identity value - together with 'curriedPlus' it forms a monoid
            .of(monoid.deferredIdentity())
            //.of(lazyMonoid.identityElement)

            .apply(delayedRandomNumberOrBottomReader
                .map(new Function<Integer, Function<? super Integer, ? extends Integer>>() {
                    @Override
                    public Function<? super Integer, ? extends Integer> apply(Integer int1) {
                        return new Function<Integer, Integer>() {
                            @Override
                            public Integer apply(Integer int2) {
                                return monoid.binaryOperation.apply(int1, int2);
                            }
                        };
                    }
                }, monoid.identityElement)
            )
            //.apply(delayedRandomNumberOrBottomReader.map2(curriedPlus, 0))
            .apply(delayedRandomNumberOrBottomReader.map(monoid.curriedBinaryOperation(), monoid.identityElement));

        foldedValue = integerReader.toMaybe().getOrDefault(0);
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Uses special Reader::map operation which protects against bottom values by using a 'Maybe' with a default value (internally) - in a deferred manner of course
    @Test
    void shouldUseMonoidAndApplicativeForFoldingDeferredSameTypeComputationalStructures_1() {
        System.out.println("Declaring the monoid (for some computational structure)...");
        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.println("Building deferred computational structure...");
        Supplier<Integer> slowAndFragileOperation1 = delayedRandomNumberOrBottomSupplier;
        Supplier<Integer> slowAndFragileOperation2 = delayedRandomNumberOrBottomSupplier;
        Supplier<Integer> slowAndFragileOperation3 = delayedRandomNumberOrBottomSupplier;

        int defaultValue = monoid.identityElement; // If map does not return a value
        Function<Integer, Function<Integer, Integer>> appendFunction = monoid.curriedBinaryOperation();
        Reader<Integer> readerIdentity = monoid.toReaderIdentity(); // Here just a deferred number zero wrapped in a 'Reader'

        Reader<Integer> reader1 = Reader.of(slowAndFragileOperation1);
        Reader<Integer> reader2 = Reader.of(slowAndFragileOperation2);
        Reader<Integer> reader3 = Reader.of(slowAndFragileOperation3);

        Reader<Integer> deferredComputation = readerIdentity
            .apply(reader1.map(appendFunction, defaultValue))
            .apply(reader2.map(appendFunction, defaultValue))
            .apply(reader3.map(appendFunction, defaultValue));

        System.out.println("Executing (deferred) computation...");
        // Blocks!
        int foldedValue = deferredComputation.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Ad-hoc experimentations
    //@Test
    void shouldUseMonoidAndApplicativeForFoldingDeferredSameTypeComputationalStructures_2() {
        System.out.println("Declaring the monoid (for some computational structure)...");
        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.println("Building deferred computational structure...");
        Supplier<String> slowAndFragileOperation = delayedRandomStringOrBottomSupplier;

        Reader<String> reader = Reader.of(slowAndFragileOperation);

        //Reader<Integer> foldedIntegerReader4 = Reader.of(monoid.deferredIdentity());
        //Reader<Integer> foldedIntegerReader3 = Reader.of(monoid.identityElement);
        //Reader<Integer> foldedIntegerReader2 = asReaderOfType(Integer.class).pure(monoid.identityElement);
        //Reader<Integer> foldedIntegerReader1 = Reader.startingWith(monoid.identityElement);
        Reader<Integer> foldedIntegerReader = monoid.toReaderIdentity();
        for (int i = 1; i <= 10; i += 1) {
            System.out.printf("    Applying (deferred) computation #%d%n", i);
            foldedIntegerReader = foldedIntegerReader
                .apply(
                    reader
                        .map(String::length)
                        .map(
                            monoid.curriedBinaryOperation(),
                            monoid.identityElement
                        )
                );
        }
        System.out.println("Executing (deferred) computation...");
        // Blocks!
        int foldedValue = foldedIntegerReader.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Ad-hoc experimentations
    //@Test
    void shouldHaveReadersAsSequenceElements() {
        System.out.println("Declaring the monoid (for some computational structure)...");
        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        FreeMonoid<Either<String, Integer>> freeMonoidWithEffect = new FreeMonoid<>(
            (accumulatedEither, eitherFailureStringOrValue) -> {
                if (eitherFailureStringOrValue.isLeft()) {
                    // (Side) effect
                    System.out.println(eitherFailureStringOrValue.tryGetLeft());

                    return accumulatedEither;
                }
                return Either.right(
                    //accumulatedEither.tryGet() + eitherFailureStringOrValue.tryGet()
                    monoid.append(accumulatedEither.tryGet(), eitherFailureStringOrValue.tryGet())
                );
            },
            Either.right(monoid.identityElement)
        );

        System.out.println();
        System.out.println("Building deferred computational structure...");
        Supplier<Integer> slowAndFragileOperationAsSupplier = delayedRandomNumberOrBottomSupplier;
        Reader<Integer> slowAndFragileOperation = Reader.of(slowAndFragileOperationAsSupplier);
        Sequence<Reader<Integer>> seqOfLazyAndFragileIntegers = Sequence.of(
            slowAndFragileOperation,
            slowAndFragileOperation,
            slowAndFragileOperation
        );

        System.out.println();
        System.out.println("Executing deferred computation...");
        // Blocks!
        Sequence<Either<String, Integer>> seqOfEithers = seqOfLazyAndFragileIntegers
            .map(Reader::toEither);

        System.out.println();
        System.out.println("Folding non-left values (using inlined folding function)...");
        int foldedValue = seqOfEithers.foldLeft(
            0,
            (integer, eitherFailureStringOrValue) ->
                eitherFailureStringOrValue.isRight()
                    ? integer + eitherFailureStringOrValue.tryGet()
                    : integer
        );
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        System.out.println();
        System.out.println("Folding non-left values (via pre-declared free monoid)...");
        foldedValue = seqOfEithers
            .toMonoid(freeMonoidWithEffect)
            .fold()
            .tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }


    ///////////////////////////////////////////////////////////////////////////
    // toEither, handling of bottom values (in deferred computations), including a failure reason message
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void toEither_whenMappingToBottomAsRuntimeException_shouldReturnLeft() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .map((integer) -> {
                throw new RuntimeException();
            });

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isNull();
    }

    @Test
    void toEither_whenMappingToBottomAsNull_shouldReturnLeft() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .map((integer) -> null);

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isSameAs("Cannot create an 'Either.Right' from a 'null' value");
    }

    @Test
    void toEither_whenBindingToBottomAsRuntimeException_shouldReturnLeft() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .bind((integer) ->
                Reader.of(
                    () -> { throw new RuntimeException(); }
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isNull();
    }

    @Test
    void toEither_whenBindingToBottomAsNull_shouldReturnLeft() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .bind((integer) ->
                Reader.of(
                    () -> null
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isSameAs("Cannot create an 'Either.Right' from a 'null' value");
    }

    @Test
    void toEither_whenMapping_shouldReturnRight() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .map((integer) -> integer + 1);

        Either<String, Integer> maybeInteger = lazyHelloWorldLengthIsEvenNumber.toEither();

        assertThat(maybeInteger.isLeft()).isFalse();
        assertThat(maybeInteger.tryGet()).isGreaterThanOrEqualTo(2);
    }


    ///////////////////////////////////////////////////////////////////////////
    // toMaybe, handling of bottom values (in deferred computations)
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void toMaybe_whenMappingToBottomAsRuntimeException_shouldReturnNothing() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .map((integer) -> {
                throw new RuntimeException();
            });

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenMappingToBottomAsNull_shouldReturnNothing() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .map((integer) -> null);

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenBindingToBottomAsRuntimeException_shouldReturnNothing() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .bind((integer) ->
                Reader.of(
                    () -> { throw new RuntimeException(); }
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenBindingToBottomAsNull_shouldReturnNothing() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .bind((integer) ->
                Reader.of(
                    () -> null
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenMapping_shouldReturnJust() {
        Reader<Integer> lazyHelloWorldLengthIsEvenNumber = Reader
            .of(randomNumberSupplier)
            .map((integer) -> integer + 1);

        Maybe<Integer> maybeInteger = lazyHelloWorldLengthIsEvenNumber.toMaybe();

        assertThat(maybeInteger.isNothing()).isFalse();
        assertThat(maybeInteger.tryGet()).isGreaterThanOrEqualTo(2);
    }
}
