package land.plainfunctional.monad;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import land.plainfunctional.algebraicstructure.FreeMonoid;
import land.plainfunctional.algebraicstructure.MonoidStructure;
import land.plainfunctional.testdomain.TestFunctions;
import land.plainfunctional.testdomain.vanillaecommerce.Customer;
import land.plainfunctional.testdomain.vanillaecommerce.Person;
import land.plainfunctional.testdomain.vanillaecommerce.VipCustomer;

import static java.lang.Integer.sum;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static land.plainfunctional.monad.Maybe.just;
import static land.plainfunctional.monad.Maybe.nothing;
import static land.plainfunctional.monad.Promise.asPromise;
import static land.plainfunctional.monad.ReaderSpecs.INTEGERS_UNDER_ADDITION_MONOID;
import static land.plainfunctional.monad.ReaderSpecs.delayedHelloWorldSupplier;
import static land.plainfunctional.monad.ReaderSpecs.delayedMaybeRandomIntegerSupplier;
import static land.plainfunctional.monad.ReaderSpecs.delayedRandomIntegerOrBottomSupplier;
import static land.plainfunctional.monad.ReaderSpecs.getDelayedInteger;
import static land.plainfunctional.monad.ReaderSpecs.getDelayedString;
import static land.plainfunctional.monad.ReaderSpecs.helloWorldSupplier;
import static land.plainfunctional.monad.ReaderSpecs.nullSupplier;
import static land.plainfunctional.monad.ReaderSpecs.oneSupplier;
import static land.plainfunctional.monad.ReaderSpecs.randomIntegerSupplier;
import static land.plainfunctional.monad.ReaderSpecs.runtimeExceptionSupplier;
import static land.plainfunctional.monad.ReaderSpecs.throwRuntimeExceptionSupplier;
import static land.plainfunctional.testdomain.TestFunctions.isEven;
import static land.plainfunctional.util.InstrumentationUtils.printExecutionStepInfo;
import static land.plainfunctional.util.InstrumentationUtils.printThreadInfo;
import static land.plainfunctional.util.InstrumentationUtils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Arrays.array;

class PromiseSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Promise properties
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldHoldValue() throws ExecutionException, InterruptedException {
        Promise<String> promise = Promise.of(helloWorldSupplier);

        assertThat(promise.evaluate().get()).isSameAs("Hello World!");
    }

    @Test
    void shouldNotBlockExecutingThread() throws ExecutionException, InterruptedException {
        Instant start = now();

        Promise<String> helloWorldLengthIsEvenNumberReader = Promise
            .of(delayedHelloWorldSupplier); // 1000 ms delay

        assertThat(between(start, now()).toMillis()).isLessThan(50); // ms (< 50 ms is considered non-blocking...)

        assertThat(helloWorldLengthIsEvenNumberReader.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberReader.isDone()).isFalse();

        Promise<String> resolvedPromise = helloWorldLengthIsEvenNumberReader.evaluate();

        assertThat(between(start, now()).toMillis()).isLessThan(250); // ms

        assertThat(helloWorldLengthIsEvenNumberReader).isNotEqualTo(resolvedPromise);

        assertThat(helloWorldLengthIsEvenNumberReader.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberReader.isDone()).isFalse();
        assertThat(resolvedPromise.isCancelled()).isFalse();
        assertThat(resolvedPromise.isDone()).isFalse();

        // Blocks!
        assertThat(resolvedPromise.get()).isSameAs("Hello World!");

        assertThat(between(start, now()).toMillis()).isGreaterThan(1000); // ms

        assertThat(helloWorldLengthIsEvenNumberReader.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberReader.isDone()).isFalse();
        assertThat(resolvedPromise.isCancelled()).isFalse();
        assertThat(resolvedPromise.isDone()).isTrue();
    }

    @Test
    void shouldBeDeferred_1a() {
        Promise<?> promise = Promise.of(throwRuntimeExceptionSupplier);

        assertThatThrownBy(promise::get);
    }

    @Test
    void shouldBeDeferred_1b() throws ExecutionException, InterruptedException {
        Promise<?> promise = Promise.of(runtimeExceptionSupplier);

        assertThat(promise.get()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldBeDeferred_2() throws ExecutionException, InterruptedException {
        Promise<?> promise = Promise.of(nullSupplier);

        assertThat(promise.get()).isNull();
    }

    @Test
    void shouldProhibitNullAsConstructorArgs() {
        assertThatThrownBy(() -> Promise.of((Supplier<?>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'Promise' cannot handle 'null' suppliers");
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

        Promise<Person> readPerson = Promise.of(vipCustomer);
        Promise<Customer> readCustomer = Promise.of(vipCustomer);
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
    void functorsShouldPreserveIdentityMorphism() throws ExecutionException, InterruptedException {
        String a = "Yes";
        String id_a = "";

        // TODO: Is this correctly set up?
        Promise<String> f_id_a = Promise.of(id_a);
        Promise<String> id_f_a = Function.<Promise<String>>identity().apply(f_id_a);

        assertThat(f_id_a).isSameAs(id_f_a);

        String folded1 = f_id_a.get();
        String folded2 = id_f_a.get();

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
    void functorsShouldPreserveCompositionOfMorphisms_resolvedValue() throws ExecutionException, InterruptedException {
        Promise<Integer> promise = Promise.of(oneSupplier.get());

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Promise<Integer> F1 = promise.map(g.compose(f));
        Promise<Integer> F2 = promise.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F2);

        // Bonus
        Promise<Integer> F3 = promise.map(f.andThen(g));
        assertThat(F1).isNotSameAs(F3);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F3);

        assertThat(promise.get()).isSameAs(1);
        assertThat(F1.get()).isSameAs(1 + 13 - 5);
        assertThat(F2.get()).isSameAs(1 + 13 - 5);
        assertThat(F3.get()).isSameAs(1 + 13 - 5);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_supplier() throws ExecutionException, InterruptedException {
        Promise<Integer> promise = Promise.of(oneSupplier);

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Promise<Integer> F1 = promise.map(g.compose(f));
        Promise<Integer> F2 = promise.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F2);

        // Bonus
        Promise<Integer> F3 = promise.map(f.andThen(g));
        assertThat(F1).isNotSameAs(F3);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(F1).isEqualTo(F3);

        assertThat(promise.get()).isSameAs(1);
        assertThat(F1.get()).isSameAs(1 + 13 - 5);
        assertThat(F2.get()).isSameAs(1 + 13 - 5);
        assertThat(F3.get()).isSameAs(1 + 13 - 5);
    }

    // TODO: ?
    //@Test
    //void functorsShouldPreserveCompositionOfMorphisms_future() {}


    ///////////////////////////////////////////////////////////////////////////
    // Applicative functor
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldPutValuesInThisApplicativeFunctor() throws ExecutionException, InterruptedException {
        Promise<?> promise = asPromise(String.class)
            .pure("JustDoIt")
            .map(new Function<String, String>() {
                @Override
                public String apply(String string) {
                    return string;
                }
            });

        assertThat(promise.get()).isEqualTo("JustDoIt");
    }

    @Test
    void shouldPutTypedValuesInThisApplicativeFunctor() throws ExecutionException, InterruptedException {
        Promise<String> promise = Promise.asPromise(String.class).pure("JustDoIt");

        assertThat(promise.get()).isEqualTo("JustDoIt");
    }

    @Test
    void shouldComposeApplicativeFunctors() throws ExecutionException, InterruptedException {
        Instant start = Instant.now();

        System.out.printf("#1: Promise building (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        printThreadInfo();

        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        Promise<String> oneStringPromise = Promise.of(
            () -> {
                sleep(500, MILLISECONDS);
                printThreadInfo();
                System.out.println("Expensive promise #1 done! (approx. 500 ms)");

                return "One";
            }
        );
        Promise<String> twoStringPromise = Promise.of(
            () -> {
                sleep(1000, MILLISECONDS);
                printThreadInfo();
                System.out.println("Expensive promise #2 done! (approx. 1000 ms)");

                return "Two";
            }
        );
        Promise<String> threeStringPromise = Promise.of(
            () -> {
                sleep(1500, MILLISECONDS);
                printThreadInfo();
                System.out.println("Expensive promise #3 done! (approx. 1500 ms)");

                return "Three";
            }
        );

        System.out.printf("#2: Applicative function composition (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        printThreadInfo();

        // Non-blocking function composition
        Promise<Integer> stringLengthSumPromise = monoid
            .toPromiseIdentity()
            .apply(
                oneStringPromise
                    .map(String::length)
                    .map(monoid.curriedBinaryOperation())
            ).apply(
                twoStringPromise
                    .map(String::length)
                    .map(monoid.curriedBinaryOperation())
            ).apply(
                threeStringPromise
                    .map(String::length)
                    .map(monoid.curriedBinaryOperation())
            );

        System.out.printf("#3: Evaluate (%d milliseconds elapsed)%n", between(start, now()).toMillis());

        printThreadInfo();

        // Non-blocking evaluation
        Promise<Integer> evaluatedStringLengthSumPromise = stringLengthSumPromise.evaluate();

        System.out.printf("#3b: Evaluating... (%d milliseconds elapsed) (waiting 4 seconds)%n", between(start, now()).toMillis());

        printThreadInfo();
        sleep(4, SECONDS);

        System.out.printf("#4: Fetch resolved value (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        printThreadInfo();

        // Blocking fetching of (monadic) value
        assertThat(evaluatedStringLengthSumPromise.get()).isEqualTo(3 + 3 + 5);

        System.out.printf("#5: Done! (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        printThreadInfo();
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
    public void shouldHaveLeftIdentity_0() throws ExecutionException, InterruptedException {
        // f (monad action)
        //Function<String, Promise<Integer>> f = (s) -> Promise.of(() -> s.length());
        Function<String, Promise<Integer>> f = (s) -> Promise.of(s::length);

        // a
        String value = "Blue";

        // m (Data constructor)
        Function<String, Promise<String>> m = Promise::of;

        // m a
        Promise<String> m_a = m.apply(value);

        Promise<Integer> m_a_bind_f = m_a.bind(f);
        Promise<Integer> f_apply_value = f.apply(value);
        assertThat(m_a_bind_f).isNotSameAs(f_apply_value);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(m_a_bind_f).isEqualTo(f_apply_value);

        assertThat(m_a.bind(f)).isNotSameAs(f.apply(value));
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(m_a.bind(f)).isEqualTo(f.apply(value));

        Integer i = m_a_bind_f.get();
        assertThat(i).isEqualTo(4);
        assertThat(f_apply_value.get()).isEqualTo(4);
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
    void shouldHaveRightIdentity() throws ExecutionException, InterruptedException {
        // a
        String value = "Go";

        // m (Data constructor)
        Function<String, Promise<String>> m = Promise::of;

        // m a
        Promise<String> m_a = m.apply(value);

        Promise<String> lhs = m_a.bind(m);
        Promise<String> rhs = m_a;

        assertThat(lhs).isNotSameAs(rhs);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(lhs).isEqualTo(rhs);

        assertThat(lhs.get()).isEqualTo("Go");
        assertThat(rhs.get()).isEqualTo("Go");
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
    void shouldHaveAssociativity_resolvedValue() throws ExecutionException, InterruptedException {
        // a
        String value = "Go";

        // m a
        Promise<String> m_a = Promise.of(value);

        // OK
        Function<String, Promise<Integer>> f = (string) -> Promise.of(() -> string.length());
        Function<Integer, Promise<Boolean>> g = (integer) -> Promise.of(isEven(integer));

        Promise<Boolean> lhs = m_a.bind(f).bind(g);
        Promise<Boolean> rhs = m_a.bind((a) -> f.apply(a).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(lhs).isEqualTo(rhs);

        assertThat(lhs.evaluate().get()).isTrue();
        assertThat(rhs.evaluate().get()).isTrue();

        assertThat(lhs.get()).isTrue();
        assertThat(rhs.get()).isTrue();
    }

    @Test
    void shouldHaveAssociativity_supplier() throws ExecutionException, InterruptedException {
        // a
        String value = "Go";

        // m a
        Promise<String> m_a = Promise.of(() -> value);

        Function<String, Promise<Integer>> f = (string) -> Promise.of(string::length);
        Function<Integer, Promise<Boolean>> g = (integer) -> Promise.of(() -> isEven(integer));

        Promise<Boolean> lhs = m_a.bind(f).bind(g);
        Promise<Boolean> rhs = m_a.bind((a) -> f.apply(a).bind(g));

        assertThat(lhs).isNotSameAs(rhs);
        // TODO: How to check for equality of a future value? (represented by a 'Supplier' object)
        //assertThat(lhs).isEqualTo(rhs);

        assertThat(lhs.get()).isTrue();
        assertThat(rhs.get()).isTrue();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Misc. applications of deferred evaluation
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenMapping_shouldDeferExecution_1() throws ExecutionException, InterruptedException {
        Instant start = now();

        System.out.printf("#1: Function composition... (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        Promise<Boolean> helloWorldLengthIsEvenNumberPromise = Promise
            .of(delayedHelloWorldSupplier) // 1000 ms delay
            .map(String::length)
            .map(TestFunctions::isEven)
            .map(TestFunctions::isTrue);
        System.out.printf("#1b: Function composition completed (%d milliseconds elapsed)%n", between(start, now()).toMillis());

        assertThat(helloWorldLengthIsEvenNumberPromise.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberPromise.isDone()).isFalse();

        System.out.printf("#2: Evaluating function... (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        assertThat(between(start, now()).toMillis()).isLessThan(200); // ms
        // Does not block
        Promise<Boolean> evaluatedPromise = helloWorldLengthIsEvenNumberPromise.evaluate();
        System.out.printf("#2b: Evaluating function completed (%d milliseconds elapsed)%n", between(start, now()).toMillis());

        assertThat(between(start, now()).toMillis()).isLessThan(250); // ms

        assertThat(helloWorldLengthIsEvenNumberPromise.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberPromise.isDone()).isFalse();
        assertThat(evaluatedPromise.isCancelled()).isFalse();
        assertThat(evaluatedPromise.isDone()).isFalse();

        System.out.println("Sleeping 1500 ms...");
        sleep(1500, MILLISECONDS);

        assertThat(helloWorldLengthIsEvenNumberPromise.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberPromise.isDone()).isFalse();
        assertThat(evaluatedPromise.isCancelled()).isFalse();
        assertThat(evaluatedPromise.isDone()).isTrue();

        System.out.printf("#3: \"Force-getting\" of evaluated promise I... (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        // Coupled with 'sleep' statement above
        //assertThat(between(start, now()).toMillis()).isLessThan(100); // ms
        assertThat(evaluatedPromise.get()).isTrue();
        System.out.printf("#3b: \"Force-getting\" of evaluated promise I completed (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        assertThat(between(start, now()).toMillis()).isGreaterThan(1000); // ms

        assertThat(helloWorldLengthIsEvenNumberPromise.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberPromise.isDone()).isFalse();
        assertThat(evaluatedPromise.isCancelled()).isFalse();
        assertThat(evaluatedPromise.isDone()).isTrue();

        System.out.printf("#4: \"Force-getting\" of evaluated promise II... (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        assertThat(evaluatedPromise.get()).isTrue();
        // Should be idempotent for evaluated promises ("memoizing" the resolved values)
        assertThat(between(start, now()).toMillis()).isLessThan(2000); // ms
        System.out.printf("#4b: \"Force-getting\" of evaluated promise II completed (%d milliseconds elapsed)%n", between(start, now()).toMillis());

        System.out.printf("#5: \"Force-getting\" of original promise... (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        assertThat(helloWorldLengthIsEvenNumberPromise.get()).isTrue();
        System.out.printf("#5b: \"Force-getting\" of original promise completed (%d milliseconds elapsed)%n", between(start, now()).toMillis());
        assertThat(between(start, now()).toMillis()).isGreaterThan(2000); // ms

        assertThat(helloWorldLengthIsEvenNumberPromise.isCancelled()).isFalse();
        assertThat(helloWorldLengthIsEvenNumberPromise.isDone()).isTrue();
        assertThat(evaluatedPromise.isCancelled()).isFalse();
        assertThat(evaluatedPromise.isDone()).isTrue();

        System.out.printf("#6: Done! (%d milliseconds elapsed)%n", between(start, now()).toMillis());
    }

    @Test
    void whenMapping_shouldDeferExecution_2() {
        Instant start = now();

        Promise<Maybe<Integer>> helloWorldLengthIsEvenNumberPromise = Promise
            .of(delayedMaybeRandomIntegerSupplier) // 200 - 3000 ms
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

        assertThat(between(start, now()).toMillis()).isLessThan(50); // ms
        System.out.printf(
            "Evaluation: %s, took %d ms",
            // Blocks!
            helloWorldLengthIsEvenNumberPromise.toMaybe().toStringMaybe(),
            between(start, now()).toMillis()
        );
        assertThat(between(start, now()).toMillis()).isGreaterThan(200); // ms
    }

    @Test
    void whenBinding_shouldDeferExecution_1() {
        Instant start = now();

        Promise<Maybe<Integer>> helloWorldLengthIsEvenNumberPromise = Promise
            .of(delayedMaybeRandomIntegerSupplier)
            .bind(new Function<Maybe<Integer>, Promise<Maybe<Integer>>>() {
                @Override
                public Promise<Maybe<Integer>> apply(Maybe<Integer> maybeInteger) {
                    return Promise.of(
                        () -> maybeInteger.map(new Function<Integer, Integer>() {
                            @Override
                            public Integer apply(Integer integer) {
                                return integer + 1;
                            }
                        })
                    );
                }
            })
            .bind((maybeInteger) -> Promise.of(
                () -> maybeInteger.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) {
                        return integer + 1;
                    }
                })
            ))
            .bind((maybeInteger) -> Promise.of(
                () -> maybeInteger.map(
                    (integer) -> integer + 1)
            ))
            .bind((maybeInteger) -> Promise.of(maybeInteger.map((integer) -> integer + 1)));

        assertThat(between(start, now()).toMillis()).isLessThan(50); // ms

        System.out.printf(
            "Evaluation: %s, took %d ms",
            // Blocks!
            helloWorldLengthIsEvenNumberPromise.toMaybe().toStringMaybe(),
            between(start, now()).toMillis()
        );

        assertThat(between(start, now()).toMillis()).isGreaterThan(200); // ms
    }


    public static final Function<Integer, Promise<Integer>> INCR_AS_PROMISE_MONAD_ACTION =
        (integer) ->
            Promise.of(integer + 1);

    public static final Function<Integer, Promise<Integer>> INCR = INCR_AS_PROMISE_MONAD_ACTION;

    @Test
    void whenBinding_shouldDeferExecution_2() {
        Instant start = now();

        Supplier<Integer> slowAndFragileOperation = delayedRandomIntegerOrBottomSupplier;

        Function<Integer, Promise<Integer>> monadAction1 = (integer) -> Promise
            .of(slowAndFragileOperation)
            .apply(
                Promise
                    .of(integer)
                    .map(INTEGERS_UNDER_ADDITION_MONOID.curriedBinaryOperation())
            );

        Function<Integer, Promise<Integer>> monadAction2 = (integer) -> Promise
            .of(integer)
            .apply(
                Promise
                    .of(slowAndFragileOperation)
                    .map(INTEGERS_UNDER_ADDITION_MONOID.curriedBinaryOperation())
            );

        Promise<Integer> helloWorldLengthIsEvenNumberPromise =
            INTEGERS_UNDER_ADDITION_MONOID
                .toPromiseIdentity()
                .then(new Function<Integer, Promise<Integer>>() {
                    @Override
                    public Promise<Integer> apply(Integer integer) {
                        return Promise.of(integer + 1);
                    }
                })
                .then(INCR_AS_PROMISE_MONAD_ACTION)
                .then(INCR)
                .then(monadAction1)
                .then(monadAction2);

        assertThat(between(start, now()).toMillis()).isLessThan(50); // ms

        System.out.printf(
            "Evaluation: %s, took %d ms",
            // NB! 'toMaybe' blocks and causes "all-or-nothing" semantics for the recursive evaluation of the "nullary" functions/'Supplier's
            helloWorldLengthIsEvenNumberPromise.toMaybe().toStringMaybe(),
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
    @Test
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


        // Sequence<Promise<Maybe<Integer>>>
        System.out.println();
        System.out.println("Folding 'Sequence<Promise<Maybe<Integer>>>' using 'Maybe::getOrDefault'");
        Sequence<Promise<Maybe<Integer>>> sequenceOfMaybeIntegerPromises = Sequence.of(
            Promise.of(delayedMaybeRandomIntegerSupplier),
            Promise.of(delayedMaybeRandomIntegerSupplier),
            Promise.of(delayedMaybeRandomIntegerSupplier)
        );
        Promise<Maybe<Integer>> foldedMaybeIntegerPromise = sequenceOfMaybeIntegerPromises.foldLeft(
            Promise.of(Maybe::nothing),
            (maybeIntPromise1, maybeIntPromise2) ->
                Promise.of(
                    () ->
                        just(
                            maybeIntPromise1.tryGet().getOrDefault(0) +
                                maybeIntPromise2.tryGet().getOrDefault(0)
                        )
                )
        );
        // Blocks!
        Maybe<Integer> maybeInteger = foldedMaybeIntegerPromise.tryGet();
        foldedValue = maybeInteger.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Using Promise's built-in Maybe transformation 1
        // Sequence<Promise<Maybe<Integer>>>
        System.out.println();
        System.out.println("Folding 'Sequence<Promise<Maybe<Integer>>>' using 'Promise::toMaybe' (1)");
        sequenceOfMaybeIntegerPromises = Sequence.of(
            Promise.of(delayedMaybeRandomIntegerSupplier),
            Promise.of(delayedMaybeRandomIntegerSupplier),
            Promise.of(delayedMaybeRandomIntegerSupplier)
        );
        foldedMaybeIntegerPromise = sequenceOfMaybeIntegerPromises.foldLeft(
            Promise.of(nothing()),
            (maybeIntPromise1, maybeIntPromise2) ->
                Promise.of(
                    just(
                        maybeIntPromise1.toMaybe().tryGet().getOrDefault(0) +
                            maybeIntPromise2.toMaybe().tryGet().getOrDefault(0)
                    )
                )
        );
        // Blocks!
        maybeInteger = foldedMaybeIntegerPromise.tryGet();
        foldedValue = maybeInteger.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Using Promise's built-in Maybe transformation 1b
        // Sequence<Promise<Maybe<Integer>>>
        System.out.println();
        System.out.println("Folding 'Sequence<Promise<Maybe<Integer>>>' using 'Promise::toMaybe' (1b)");
        sequenceOfMaybeIntegerPromises = Sequence.of(
            Promise.of(delayedMaybeRandomIntegerSupplier),
            Promise.of(delayedMaybeRandomIntegerSupplier),
            Promise.of(delayedMaybeRandomIntegerSupplier)
        );
        foldedMaybeIntegerPromise = sequenceOfMaybeIntegerPromises.foldLeft(
            Promise.of(Maybe::nothing),
            (maybeIntPromise1, maybeIntPromise2) ->
                Promise.of(
                    () ->
                        just(
                            maybeIntPromise1.toMaybe().tryGet().getOrDefault(0) +
                                maybeIntPromise2.toMaybe().tryGet().getOrDefault(0)
                        )
                )
        );
        // Blocks!
        maybeInteger = foldedMaybeIntegerPromise.tryGet();
        foldedValue = maybeInteger.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Using Promise's built-in Maybe transformation 2
        // Sequence<Promise<Integer>>
        System.out.println();
        System.out.println("Folding \"flaky\" 'Sequence<Promise<Integer>>' using 'Promise::toMaybe' (2)");
        Sequence<Promise<Integer>> sequenceOfIntegerPromises = Sequence.of(
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier)
        );
        Promise<Integer> foldedIntegerPromise = sequenceOfIntegerPromises.foldLeft(
            Promise.of(() -> 0),
            (intPromise1, intPromise2) ->
                Promise.of(
                    () ->
                        intPromise1.toMaybe().getOrDefault(0) +
                            intPromise2.toMaybe().getOrDefault(0)
                )
        );
        // Blocks!
        foldedValue = foldedIntegerPromise.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Using Promise's built-in Maybe transformation 3 (+ a free monoid for folding)
        // Sequence<Lazy<Integer>>
        System.out.println();
        System.out.println("Map-folding (\"MapReducing\") \"flaky\" 'Sequence<Promise<Integer>>' using 'Promise::toMaybe' (3)");
        sequenceOfIntegerPromises = Sequence.of(
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier)
        );
        Integer foldedSequenceOfIntegers =
            sequenceOfIntegerPromises
                // Blocks!
                .map((intPromise) -> intPromise.toMaybe().getOrDefault(0))
                .foldLeft(INTEGERS_UNDER_ADDITION_MONOID);
        System.out.println(foldedSequenceOfIntegers);
        assertThat(foldedSequenceOfIntegers).isGreaterThanOrEqualTo(0);


        // Sequence<Promise<Integer>>
        System.out.println();
        System.out.println("Using on-the-fly monoid structure for folding \"flaky\" 'Sequence<Promise<Integer>>'");

        Integer identity = 0;
        //Promise<Integer> lazyIdentity = Promise.of(() -> identity);
        Promise<Integer> identityPromise = Promise.of(identity);
        BinaryOperator<Promise<Integer>> plusPromise =
            (int1, int2) ->
                Promise.of(
                    //() ->
                    sum(
                        int1.toMaybe().getOrDefault(identity),
                        int2.toMaybe().getOrDefault(identity)
                    )
                );
        sequenceOfIntegerPromises = Sequence.of(
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier)
        );
        MonoidStructure<Promise<Integer>> promiseMonoid = sequenceOfIntegerPromises.toMonoid(identityPromise, plusPromise);
        assertThat(promiseMonoid.size()).isEqualTo(sequenceOfIntegerPromises.size());
        foldedIntegerPromise = promiseMonoid.fold();
        // Blocks!
        foldedValue = foldedIntegerPromise.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        System.out.println();
        System.out.println("(again...)");
        // TODO: Takes the result from above and run one more supplier...
        promiseMonoid = sequenceOfIntegerPromises.toMonoid(identityPromise, plusPromise);
        assertThat(promiseMonoid.size()).isEqualTo(sequenceOfIntegerPromises.size());
        foldedIntegerPromise = promiseMonoid.fold();
        // Blocks!
        foldedValue = foldedIntegerPromise.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        System.out.println();
        System.out.println("(again, now compacted)");
        // TODO: Just print out the above result...
        foldedValue = sequenceOfIntegerPromises
            .toMonoid(identityPromise, plusPromise)
            .fold()
            // Blocks!
            .tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        // Sequence<Promise<Integer>>
        System.out.println();
        System.out.println("Using 'FreeMonoid' for folding \"flaky\" 'Sequence<Promise<Integer>>'");

        FreeMonoid<Promise<Integer>> freeMonoid = new FreeMonoid<>(plusPromise, identityPromise);

        sequenceOfIntegerPromises = Sequence.of(
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier),
            Promise.of(delayedRandomIntegerOrBottomSupplier)
        );
        MonoidStructure<Promise<Integer>> monoidPromise = sequenceOfIntegerPromises.toMonoid(freeMonoid);
        assertThat(promiseMonoid.size()).isEqualTo(sequenceOfIntegerPromises.size());
        foldedIntegerPromise = monoidPromise.fold();
        // Blocks!
        foldedValue = foldedIntegerPromise.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        System.out.println();
        System.out.println("(again, now compacted)");
        foldedValue = sequenceOfIntegerPromises
            .toMonoid(freeMonoid)
            .fold()
            // Blocks!
            .tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Ad-hoc experimentations
    @Test
    void shouldFoldDifferentStructures_2() {
        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.println("#1");
        Promise<Integer> integerPromise = Promise
            //.of(deferredZero) // Deferred identity value - together with 'curriedPlus' it forms a monoid
            .of(monoid.deferredIdentity())
            .apply(Promise
                //.of(delayedRandomIntegerOrBottomSupplier)
                .of(randomIntegerSupplier)
                //.map(curriedPlus)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Promise
                .of(randomIntegerSupplier)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Promise
                .of(randomIntegerSupplier)
                .map(monoid.curriedBinaryOperation())
            );
        int foldedValue = integerPromise.toMaybe().getOrDefault(0);
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        System.out.println();
        System.out.println("#2");
        // TODO: Short-circuits computation with the first bottom...
        integerPromise = Promise
            //.of(deferredZero) // Deferred identity value - together with 'curriedPlus' it forms a monoid
            .of(monoid.deferredIdentity())
            .apply(Promise
                .of(delayedRandomIntegerOrBottomSupplier)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Promise
                .of(delayedRandomIntegerOrBottomSupplier)
                .map(monoid.curriedBinaryOperation())
            )
            .apply(Promise
                .of(delayedRandomIntegerOrBottomSupplier)
                .map(monoid.curriedBinaryOperation())
            );
        // Blocks!
        foldedValue = integerPromise.toMaybe().getOrDefault(0);
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);


        System.out.println();
        System.out.println("#3");
        integerPromise = Promise
            //.of(() -> 0)
            //.of(deferredZero) // Deferred identity value - together with 'curriedPlus' it forms a monoid
            .of(monoid.deferredIdentity())
            //.of(lazyMonoid.identityElement)

            .apply(Promise
                .of(delayedRandomIntegerOrBottomSupplier)
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
                     }//, monoid.identityElement
                )
            )
            //.apply(delayedRandomIntegerOrBottomReader.map2(curriedPlus, 0))
            .apply(Promise
                .of(delayedRandomIntegerOrBottomSupplier)
                .map(
                    monoid.curriedBinaryOperation()//,
                    //monoid.identityElement
                )
            );

        foldedValue = integerPromise.toMaybe().getOrDefault(0);
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Uses special Reader::map operation which protects against bottom values by using a 'Maybe' with a default value (internally) - in a deferred manner of course
    @Test
    void shouldUseMonoidAndApplicativeForFoldingDeferredSameTypeComputationalStructures_1() {
        System.out.println("Declaring the monoid (for some computational structure)...");
        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.println("Building deferred computational structure...");
        Supplier<Integer> slowAndFragileOperation1 = delayedRandomIntegerOrBottomSupplier;
        Supplier<Integer> slowAndFragileOperation2 = delayedRandomIntegerOrBottomSupplier;
        Supplier<Integer> slowAndFragileOperation3 = delayedRandomIntegerOrBottomSupplier;

        int defaultValue = monoid.identityElement; // If map does not return a value
        Function<Integer, Function<Integer, Integer>> appendFunction = monoid.curriedBinaryOperation();
        Promise<Integer> readerIdentity = monoid.toPromiseIdentity(); // Here just a deferred number zero wrapped in a 'Reader'

        Promise<Integer> promise1 = Promise.of(slowAndFragileOperation1);
        Promise<Integer> promise2 = Promise.of(slowAndFragileOperation2);
        Promise<Integer> promise3 = Promise.of(slowAndFragileOperation3);

        Promise<Function<? super Integer, ? extends Integer>> partiallyAppliedAppendFunctionPromise1 = promise1.map(appendFunction, defaultValue);
        Promise<Function<? super Integer, ? extends Integer>> partiallyAppliedAppendFunctionPromise2 = promise2.map(appendFunction, defaultValue);
        Promise<Function<? super Integer, ? extends Integer>> partiallyAppliedAppendFunctionPromise3 = promise3.map(appendFunction, defaultValue);

        Promise<Integer> deferredComputation = readerIdentity
            .apply(partiallyAppliedAppendFunctionPromise1)
            .apply(partiallyAppliedAppendFunctionPromise2)
            .apply(partiallyAppliedAppendFunctionPromise3);

        System.out.println("Executing (deferred) computation...");
        // Blocks!
        int foldedValue = deferredComputation.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);

        System.out.println("Again, compact version...");
        deferredComputation = readerIdentity
            .apply(promise1.map(appendFunction, defaultValue))
            .apply(promise2.map(appendFunction, defaultValue))
            .apply(promise3.map(appendFunction, defaultValue));
        // Blocks!
        foldedValue = deferredComputation.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Ad-hoc experimentations
    @Test
    void shouldUseMonoidAndApplicativeForFoldingDeferredSameTypeComputationalStructures_2() {
        System.out.println("Declaring the monoid (for some computational structure)...");
        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.println("Building deferred computational structure...");
        Supplier<String> slowAndFragileOperation = ReaderSpecs::getRandomlyDelayedOrTimedOutRandomString;

        Promise<String> reader = Promise.of(slowAndFragileOperation);

        Promise<Integer> foldedIntegerPromise = monoid.toPromiseIdentity();
        for (int i = 1; i <= 10; i += 1) {
            System.out.printf("    Applying (deferred) computation #%d%n", i);
            foldedIntegerPromise = foldedIntegerPromise
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
        int foldedValue = foldedIntegerPromise.tryGet();
        System.out.println(foldedValue);
        assertThat(foldedValue).isGreaterThanOrEqualTo(0);
    }

    // NB! Ad-hoc experimentations
    @Test
    void shouldHavePromisesAsSequenceElements() {
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
        Supplier<Integer> slowAndFragileOperationAsSupplier = delayedRandomIntegerOrBottomSupplier;
        Promise<Integer> slowAndFragileOperation = Promise.of(slowAndFragileOperationAsSupplier);
        Sequence<Promise<Integer>> seqOfLazyAndFragileIntegers = Sequence.of(
            slowAndFragileOperation,
            slowAndFragileOperation,
            slowAndFragileOperation
        );

        System.out.println();
        System.out.println("Executing deferred computation...");
        // Blocks!
        Sequence<Either<String, Integer>> seqOfEithers = seqOfLazyAndFragileIntegers
            .map(Promise::toEither);

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

    /*
    // NB! Ad-hoc experimentations
    @Test
    void shouldDoAsyncMapFold() {
        Instant start = Instant.now();

        //AtomicInteger counter = new AtomicInteger(0);
        final AtomicLong mark1 = new AtomicLong(0L);
        final AtomicLong mark2 = new AtomicLong(0L);
        final AtomicLong mark3 = new AtomicLong(0L);
        final AtomicLong mark4 = new AtomicLong(0L);
        final AtomicLong mark5 = new AtomicLong(0L);

        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.printf("%n%s%n",
            Promise
                .of(getDelayedString("Hello"))
                .tryEffect((string) -> mark1.set(between(start, now()).toMillis()))
                //.effect((string) -> printInfo(start, counter, string))
                .mapN(
                    asList(
                        (string) -> getDelayedInteger(string.length()),
                        (string) -> getDelayedInteger(string.length()),
                        (string) -> getDelayedInteger(string.length())
                    )
                    , monoid
                )
                .tryEffect((string) -> mark2.set(between(start, now()).toMillis()))
                //.effect((integer) -> printInfo(start, counter, integer))
                .evaluate() // Start async evaluation
                .tryEffect((string) -> mark3.set(between(start, now()).toMillis()))
                //.effect((integer) -> printInfo(start, counter, integer))
                .tryEffect((integer) -> sleep(800, MILLISECONDS))
                .tryEffect((string) -> mark4.set(between(start, now()).toMillis()))
                //.effect((integer) -> printInfo(start, counter, integer))
                .tryEffect((integer) -> sleep(300, MILLISECONDS))
                .tryEffect((string) -> mark5.set(between(start, now()).toMillis()))
                //.effect((integer) -> printInfo(start, counter, integer))
                // NB! Blocks current thread! (BUT PICKING UP ON THE ONGOING ASYNC EVALUATION! :-)
                .toMaybe()
                .tryGet()

        ).printf("%n(took %d ms) (should take 1000 + 1000 + ~100 ms)%n", between(start, now()).toMillis());

        assertThat(mark1.get()).isLessThan(1100); // ~100 ms in max computational startup "overhead"
        assertThat(mark2.get()).isLessThan(1100);
        assertThat(mark3.get()).isLessThan(1100);
        assertThat(mark4.get()).isLessThan(1100 + 800);
        assertThat(mark5.get()).isLessThan(1100 + 800 + 300);

        assertThat(between(start, now()).toMillis()).isLessThan(1000 + 1000 + 400); // ~400 ms in max total computational "overhead"
    }
    */

    /* TODO: Consider:
    @Test
    void shouldDoHandleAsyncEvaluationsAndEffects() {
        Instant start = Instant.now();

        AtomicInteger counter = new AtomicInteger(0);
        final AtomicLong mark1 = new AtomicLong(0);
        final AtomicLong mark2 = new AtomicLong(0);
        final AtomicLong mark3 = new AtomicLong(0);
        final AtomicLong mark4 = new AtomicLong(0);
        final AtomicLong mark5 = new AtomicLong(0);

        final AtomicLong onResolvedMark1 = new AtomicLong(0);
        final AtomicLong onResolvedMark2 = new AtomicLong(0);
        final AtomicLong onResolvedMark3 = new AtomicLong(0);

        FreeMonoid<Integer> monoid = INTEGERS_UNDER_ADDITION_MONOID;

        System.out.printf("%n%s%n",
            Promise
                .of(getDelayedString(1000, "Hello"))
                .effect((x) -> {
                    System.out.println("CALLBACK1");
                    printExecutionStepInfo(start, counter, x);
                    onResolvedMark1.set(between(start, now()).toMillis());
                })
                .effect((x) -> mark1.set(between(start, now()).toMillis()))
                .tryEffect((x) -> printExecutionStepInfo(start, counter, x))
                .effect((x) -> printExecutionStepInfo(start, counter, x))
                .map(
                    asList(
                        (string) -> getDelayedInteger(1000, string.length()),
                        (string) -> getDelayedInteger(1000, string.length()),
                        (string) -> getDelayedInteger(1000, string.length())
                    )
                    , monoid
                )
                .effect((x) -> {
                    System.out.println("CALLBACK2");
                    printExecutionStepInfo(start, counter, x);
                    onResolvedMark2.set(between(start, now()).toMillis());
                })
                .tryEffect((x) -> mark2.set(between(start, now()).toMillis()))
                .tryEffect((x) -> printExecutionStepInfo(start, counter, x))
                .effect((x) -> printExecutionStepInfo(start, counter, x))
                .evaluate() // Start async evaluation
                .tryEffect((x) -> mark3.set(between(start, now()).toMillis()))
                .tryEffect((x) -> printExecutionStepInfo(start, counter, x))
                .effect((x) -> printExecutionStepInfo(start, counter, x))
                // NB! Blocks current thread!
                .tryEffect((x) -> sleep(800, MILLISECONDS))
                .tryEffect((x) -> mark4.set(between(start, now()).toMillis()))
                .tryEffect((x) -> printExecutionStepInfo(start, counter, x))
                .effect((x) -> printExecutionStepInfo(start, counter, x))
                // NB! Blocks current thread!
                .tryEffect((x) -> sleep(300, MILLISECONDS))
                .tryEffect((x) -> mark5.set(between(start, now()).toMillis()))
                .tryEffect((x) -> printExecutionStepInfo(start, counter, x))
                .effect((x) -> printExecutionStepInfo(start, counter, x))
                .effect((x) -> {
                    System.out.println("CALLBACK3");
                    printExecutionStepInfo(start, counter, x);
                    onResolvedMark3.set(between(start, now()).toMillis());
                })
                // NB! Blocks current thread! (BUT PICKING UP ON THE ONGOING ASYNC EVALUATION! :-)
                .tryGet()
                .tryGet()

        ).printf("%n(took %d ms) (should take 1000 + 1000 + ~100 ms)%n", between(start, now()).toMillis());

        assertThat(mark1.get()).isGreaterThan(1000); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(mark1.get()).isLessThan(1000 + 150); // ~150 ms in max computational startup "overhead"
        assertThat(mark2.get()).isGreaterThan(1000); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(mark2.get()).isLessThan(1000 + 150); // ~150 ms in max computational startup "overhead"
        assertThat(mark3.get()).isGreaterThan(1000); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(mark3.get()).isLessThan(1000 + 150); // ~150 ms in max computational startup "overhead"
        assertThat(mark4.get()).isGreaterThan(1000 + 800); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(mark4.get()).isLessThan(1000 + 800 + 150); // ~150 ms in max computational startup "overhead"
        assertThat(mark5.get()).isGreaterThan(1000 + 800 + 300); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(mark5.get()).isLessThan(1000 + 800 + 300150); // ~150 ms in max computational startup "overhead"

        assertThat(onResolvedMark1.get()).isGreaterThan(1000); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(onResolvedMark1.get()).isLessThan(1150);
        assertThat(onResolvedMark2.get()).isGreaterThan(1000 + 1000); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(onResolvedMark2.get()).isLessThan(1000 + 1000 + 400); // ~400 ms in max total computational "overhead"
        assertThat(onResolvedMark3.get()).isGreaterThan(1000 + 1000); // 0 being default, i.e. not set, i.e. callback not invoked
        assertThat(onResolvedMark3.get()).isLessThan(1000 + 1000 + 400); // ~400 ms in max total computational "overhead"

        assertThat(between(start, now()).toMillis()).isLessThan(1000 + 1000 + 400); // ~400 ms in max total computational "overhead"
    }
    */


    ///////////////////////////////////////////////////////////////////////////
    // toEither, handling of bottom values (in deferred computations), including a failure reason message
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void toEither_whenMappingToBottomAsRuntimeException_shouldReturnLeft() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .map((integer) -> {
                throw new RuntimeException();
            });

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isSameAs("java.lang.RuntimeException");
    }

    @Test
    void toEither_whenMappingToBottomAsNull_shouldReturnLeft() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .map((integer) -> null);

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isSameAs("Cannot create an 'Either.Right' from a 'null' value");
    }

    @Test
    void toEither_whenBindingToBottomAsRuntimeException_shouldReturnLeft() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .bind((integer) ->
                Promise.of(
                    () -> { throw new RuntimeException(); }
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isNull();
    }

    @Test
    void toEither_whenBindingToBottomAsNull_shouldReturnLeft() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .bind((integer) ->
                Promise.of(
                    () -> null
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().isLeft()).isTrue();
        assertThat(lazyHelloWorldLengthIsEvenNumber.toEither().tryGetLeft()).isSameAs("Cannot create an 'Either.Right' from a 'null' value");
    }

    @Test
    void toEither_whenMapping_shouldReturnRight() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
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
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .map((integer) -> {
                throw new RuntimeException();
            });

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenMappingToBottomAsNull_shouldReturnNothing() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .map((integer) -> null);

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenBindingToBottomAsRuntimeException_shouldReturnNothing() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .bind((integer) ->
                Promise.of(
                    () -> { throw new RuntimeException(); }
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenBindingToBottomAsNull_shouldReturnNothing() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .bind((integer) ->
                Promise.of(
                    () -> null
                )
            );

        assertThat(lazyHelloWorldLengthIsEvenNumber.toMaybe().isNothing()).isTrue();
    }

    @Test
    void toMaybe_whenMapping_shouldReturnJust() {
        Promise<Integer> lazyHelloWorldLengthIsEvenNumber = Promise
            .of(randomIntegerSupplier)
            .map((integer) -> integer + 1);

        Maybe<Integer> maybeInteger = lazyHelloWorldLengthIsEvenNumber.toMaybe();

        assertThat(maybeInteger.isNothing()).isFalse();
        assertThat(maybeInteger.tryGet()).isGreaterThanOrEqualTo(2);
    }
}
