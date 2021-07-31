package land.plainfunctional.algebraicstructure;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import land.plainfunctional.monad.Maybe;
import land.plainfunctional.monad.Sequence;
import land.plainfunctional.testdomain.vanillaecommerce.Address;
import land.plainfunctional.testdomain.vanillaecommerce.Payment;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySortedSet;
import static land.plainfunctional.algebraicstructure.SemigroupSpecs.orderedByInsertTime;
import static land.plainfunctional.monad.Maybe.just;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonoidStructureSpec {

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    void emptyConstructor_shouldNotCompile() {
        //MonoidStructure<String> magma = new MonoidStructure<>();
    }

    void unaryConstructor_shouldNotCompile() {
        //MonoidStructure<String> magma = new MonoidStructure<>(null);
    }

    void binaryConstructor_shouldNotCompile() {
        //MonoidStructure<String> magma = new MonoidStructure<>(null, null);
    }

    @Test
    void binaryConstructor_whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> new MonoidStructure<>(
            (SortedSet<?>) null,
            null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A monoid must have a binary operation");

        assertThatThrownBy(() -> new MonoidStructure<String>(
            emptySortedSet(),
            null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A monoid must have a binary operation");

        assertThatThrownBy(() -> new MonoidStructure<>(
            emptySortedSet(),
            (BinaryOperator<String>) (s, s2) -> null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A monoid must have an identity element - a neutral element");

        assertThatThrownBy(() -> new MonoidStructure<>(
            emptySortedSet(),
            null,
            ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A monoid must have a binary operation");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Totality / Closure
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenNullArgs_shouldThrowException() {
        MonoidStructure<String> emptyStringAppendingMonoid = new MonoidStructure<>(
            emptySortedSet(),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThatThrownBy(() -> emptyStringAppendingMonoid.append(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be null");

        assertThatThrownBy(() -> emptyStringAppendingMonoid.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element2' argument cannot be null");

        assertThatThrownBy(() -> emptyStringAppendingMonoid.append(null, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be null");
    }

    @Test
    void shouldAppend_1() {
        MonoidStructure<String> stringAppendingMonoid = new MonoidStructure<>(
            new TreeSet<>(asList("foo", "bar", "foobar")),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThat(stringAppendingMonoid.append("foo", "bar")).isEqualTo("foobar");
    }

    @Test
    void shouldAppend_2() {
        Address address1 = new Address("The street 1", "1234");
        Address address2 = new Address("The street 2", "1234");
        Address address3 = new Address("The street 1", "1234", "The valley", null);

        SortedSet<Address> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add(address1);
        chronologicallyEnumeratedSet.add(address2);
        chronologicallyEnumeratedSet.add(address3);

        MonoidStructure<Address> addressAppendingMonoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            Address::append,
            Address.IDENTITY
        );

        assertThat(addressAppendingMonoid.append(address1, address3)).isEqualTo(
            address1.postalLocation("The valley")
        );

        assertThat(addressAppendingMonoid.append(address3, address1)).isEqualTo(
            address1.postalLocation("The valley")
        );
    }

    //@Test No, this is not a monoid requirement - associativity only kicks in when combining more than 2 elements
    void whenViolatesDefinedEnumeration_shouldThrowException_1() {
        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add("foo");
        chronologicallyEnumeratedSet.add("bar");
        chronologicallyEnumeratedSet.add("barfoo");

        MonoidStructure<String> stringAppendingMonoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThatThrownBy(() -> stringAppendingMonoid.append("bar", "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Illegal associative relation between the given elements in the Monoid");
    }

    //@Test No, this is not a monoid requirement - associativity only kicks in when combining more than 2 elements
    void whenViolatesDefinedEnumeration_shouldThrowException_2() {
        Address address1 = new Address("The street 1", "1234");
        Address address2 = new Address("The street 2", "1234");
        Address address3 = new Address("The street 3", "1234");

        SortedSet<Address> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add(address1);
        chronologicallyEnumeratedSet.add(address2);
        chronologicallyEnumeratedSet.add(address3);

        MonoidStructure<Address> stringAppendingMonoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            Address::append,
            Address.IDENTITY
        );

        assertThatThrownBy(() -> stringAppendingMonoid.append(address3, address1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Illegal associative relation between the given elements in the 'MonoidStructure'");
    }

    @Test
    void shouldAppend_3() {
        SortedSet<Integer> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList(7, 6, 5, 4, 3, 2, 1));

        MonoidStructure<Integer> numberAppendingMonoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            Integer::sum,
            0
        );

        assertThat(numberAppendingMonoid.append(2, 5)).isEqualTo(7);
    }

    @Test
    void shouldAppend_4() {
        BinaryOperator<Integer> clockHourAdd =
            (hour1, hour2) -> ((hour1 + hour2) % 12 == 0)
                ? 12
                : (hour1 + hour2) % 12;

        SortedSet<Integer> set = new TreeSet<>(asList(1, 3, 5, 7, 9, 11, 2, 4, 6, 8, 10, 12));

        MonoidStructure<Integer> clockHourAppendingMonoid = new MonoidStructure<>(
            set,
            clockHourAdd,
            0
        );

        assertThat(clockHourAppendingMonoid.append(2, 5)).isEqualTo(7);
        assertThat(clockHourAppendingMonoid.append(2, 10)).isEqualTo(12);
        assertThat(clockHourAppendingMonoid.append(2, 12)).isEqualTo(2);
        assertThat(clockHourAppendingMonoid.append(8, 11)).isEqualTo(7);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Associativity
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldBeAssociative_1() {
        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");

        MonoidStructure<String> helloWorldGeneratingMonoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        String identityValue = "";
        String reducedString = helloWorldGeneratingMonoid.set
            .parallelStream()
            .reduce(
                identityValue,
                (string1, string2) -> string1 + string2
            );
        assertThat(reducedString).isEqualTo("Hello world!");
    }

    @Test
    void shouldBeAssociative_2() {
        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("!");

        MonoidStructure<String> helloWorldGeneratingMonoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        String identityValue = "";
        String reducedString = helloWorldGeneratingMonoid.set
            .parallelStream()
            .reduce(
                identityValue,
                (string1, string2) -> string1 + string2
            );
        assertThat(reducedString).isEqualTo("worldHello !");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Fold / catamorphism
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenEmptyMonoid_shouldReturnIdentityElement() {
        MonoidStructure<String> monoid = new MonoidStructure<>(
            new LinkedHashSet<>(),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThat(monoid.fold()).isSameAs(monoid.identityElement);
    }

    @Test
    void shouldFold_1() {
        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList("a", " ", "b"));

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThat(monoid.fold()).isEqualTo("a b");
    }

    // TODO: Yields duplicated elements in set (a bug somewhere...) => Use 'LinkedHashSet' chronological ordering!
    //@Test
    void shouldFold_2() {
        String identityValue = "";

        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        System.out.println();
        chronologicallyEnumeratedSet.add("Hello");
        System.out.println();
        chronologicallyEnumeratedSet.add(" ");
        System.out.println();
        chronologicallyEnumeratedSet.add("world");
        System.out.println();
        chronologicallyEnumeratedSet.add("!");
        System.out.println();
        chronologicallyEnumeratedSet.add(" ");      // WORKS
        System.out.println();
        chronologicallyEnumeratedSet.add("world");  // WORKS
        System.out.println();
        chronologicallyEnumeratedSet.add("Hello");  // FAILS!

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(monoid.fold()).isEqualTo("Hello world!");
    }

    @Test
    void shouldFold_3() {
        String identityValue = "";

        LinkedHashSet<String> chronologicallyEnumeratedSet = new LinkedHashSet<>(asList(
            "Hello", " ", "world", "!", "Hello", " ", "world", "!", "Hello", " ", "world", "!"
        ));

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(monoid.fold()).isEqualTo("Hello world!");
    }

    // TODO: Fails now and then... => Use 'LinkedHashSet' chronological ordering!
    //@Test
    void shouldFold_4() {
        String identityValue = "";

        SortedSet<String> chronologicallyEnumeratedSet = new ConcurrentSkipListSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(monoid.fold()).isEqualTo("Hello world!");
    }

    @Test
    void shouldFold_5() {
        String identityValue = "";

        Set<String> chronologicallyEnumeratedSet = new LinkedHashSet<>();
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");

        SortedSet<String> chronologicallyEnumeratedSortedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSortedSet.addAll(chronologicallyEnumeratedSet);

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSortedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(monoid.fold()).isEqualTo("Hello world!");
    }

    @Test
    void shouldFold_6() {
        String identityValue = "";

        LinkedHashSet<String> chronologicallyEnumeratedSet = new LinkedHashSet<>();
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(monoid.fold()).isEqualTo("Hello world!");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Partitioning
    ///////////////////////////////////////////////////////////////////////////

    /*
    @Test
    void partition_whenZeroElementsInPartitions_shouldThrowException_1() {
        assertThatThrownBy(() ->
            new MonoidStructure<>(
                new LinkedHashSet<>(),
                (string1, string2) -> string1 + string2,
                ""
            ).partition(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'partitionSize' argument cannot be less than one");
    }

    @Test
    void partition_whenZeroElementsInPartitions_shouldThrowException_2() {
        LinkedHashSet<String> chronologicallyEnumeratedSet = new LinkedHashSet<>();
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("!");

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThatThrownBy(() -> monoid.partition(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'partitionSize' argument cannot be less than one");
    }

    @Test
    void partition_whenEmptyMonoid_shouldReturnEmptySequenceOfPartitions() {
        MonoidStructure<String> monoid = new MonoidStructure<>(
            new LinkedHashSet<>(),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThat(monoid.partition(1).isEmpty()).isTrue();
    }

    @Test
    void partition_shouldDistributeMonoidElements_1() {
        LinkedHashSet<String> chronologicallyEnumeratedSet = new LinkedHashSet<>();
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("!");

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThat(monoid.partition(1).size()).isSameAs(2L);
    }

    @Test
    void partition_shouldDistributeMonoidElements_2() {
        LinkedHashSet<String> chronologicallyEnumeratedSet = new LinkedHashSet<>();
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("World");
        chronologicallyEnumeratedSet.add("!");

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThat(monoid.partition(2).size()).isSameAs(2L);
        assertThat(monoid.partition(1).toJavaList().get(0).size()).isSameAs(1L);
        assertThat(monoid.partition(1).toJavaList().get(1).size()).isSameAs(1L);
    }

    @Test
    void partition_shouldDistributeMonoidElements_3() {
        LinkedHashSet<String> chronologicallyEnumeratedSet = new LinkedHashSet<>();
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("World");
        chronologicallyEnumeratedSet.add("!");

        MonoidStructure<String> monoid = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            ""
        );

        Sequence<MonoidStructure<String>> partitionedMonoids = monoid.partition(2);

        assertThat(partitionedMonoids.size()).isSameAs(2L);
        assertThat(partitionedMonoids.toJavaList().get(0).size()).isSameAs(2L);
        assertThat(partitionedMonoids.toJavaList().get(1).size()).isSameAs(2L);
    }
    */


    ///////////////////////////////////////////////////////////////////////////
    // Parallel fold / catamorphism
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void parallelFold_whenEmptyMonoid_shouldReturnIdentityElement_2() {
        MonoidStructure<String> monoid = new MonoidStructure<>(
            new LinkedHashSet<>(),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThat(monoid.parallelFold()).isSameAs(monoid.identityElement);
    }

    @Test
    void shouldFoldInParallel() {
        //Long range = null;
        //Long rangeSum = null;
        //Long range = 0L;
        //Long rangeSum = 0L;
        //Integer range = 1;
        //Long rangeSum = 1L;
        //Integer range = 2;
        //Long rangeSum = 3L;
        //Integer range = 3;        // NB! Fails when performing 'parallelFold' as two 3 values are added to a set-based monoid, and one of them is omitted...
        //Long rangeSum = 6L;
        //Integer range = 10;
        //Long rangeSum = 55L;
        //Integer range = 11;       // NB! Fails when performing 'parallelFold' as values are omitted in the set-based monoid...
        //Long rangeSum = 66L;
        //Integer range = 13;
        //Long rangeSum = 91L;
        //Integer range = 15;       // NB! Fails when performing 'parallelFold' as values are omitted in the set-based monoid...
        //Long rangeSum = 120L;

        //Integer range = 100;
        //Long rangeSum = 5_050L;
        Integer range = 1_000;
        Long rangeSum = 500_500L;
        //Integer range = 10_000;
        //Long rangeSum = 50_005_000L;
        //Integer range = 100_000;
        //Long rangeSum = 5_000_050_000L;
        //Integer range = 1_000_000;
        //Long rangeSum = 500_000_500_000L;


        // Sum of int range: Plain functional Java (sequence of supplied values)
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
        //Long sum = sequenceOfLongs.toMonoid(Long::sum, 0L).fold();
        Long sum = sequenceOfLongs.foldLeft(Long::sum, 0L);
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequenceOfLongs.size()).isEqualTo(range.longValue());
        assertThat(sum).isEqualTo(rangeSum);


        // Sum of int range: Plain functional Java (sequence of supplied values, with parallel folding)
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
        //MonoidStructure<Long> monoid = sequenceOfLongs.toMonoid(Long::sum, 0L);
        //sum = monoid.parallelFold();
        sum = sequenceOfLongs.parallelFold(new FreeMonoid<>(Long::sum, 0L));
        System.out.println();
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values, with parallel folding), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("Sum of int range: Plain functional Java (sequence of supplied values, with parallel folding), processing took %d ms%n", between(startProcessing, now()).toMillis());
        assertThat(sequenceOfLongs.size()).isEqualTo(range.longValue());
        assertThat(sum).isEqualTo(rangeSum);
    }

    @Test
    void shouldFoldInParallel_productTypes() {
        //Integer range = null;
        Integer range = 100;

        System.out.printf("Runtime.freeMemory: %d   Runtime.maxMemory: %d   Runtime.totalMemory: %d%n",
            Runtime.getRuntime().freeMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().totalMemory()
        );

        Function<Payment, Function<Payment, Payment>> curriedAppend =
            (payment1) ->
                (payment2) -> payment2.append(payment1);


        // "Sum" of Payments: Regular Java (Java Stream API)
        System.out.println();
        System.out.printf("\"Sum\" of %d Payments: Regular Java (Java Stream API)%n", range);
        Instant startGenerating = now();
        List<Payment> paymentList = new ArrayList<>();
        Payment firstRandomPayment = Payment.random();
        Payment randomPayment;
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
        Payment payment = paymentList
            .stream()
            .reduce(
                Payment.identity(),
                Payment::append
            );

        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(paymentList.size()).isEqualTo(range);
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
        //payment = sequenceOfPayments.toMonoid(Payment::append, Payment.identity()).fold();
        payment = sequenceOfPayments.foldLeft(Payment::append, Payment.identity());

        System.out.printf("\"Sum\" of Payments: Plain functional Java, generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java, processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(sequenceOfPayments.size()).isEqualTo(range.longValue());
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
            .foldLeft(
                (maybePayment1, maybePayment2) -> maybePayment1.apply(maybePayment2.map(curriedAppend)),
                just(Payment.identity())
            )
            .tryGet();

        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java (sequence of maybe values), processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(sequenceOfMaybePayments.size()).isEqualTo(range.longValue());
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
        System.out.printf("\"Sum\" of %d Payments: Plain functional Java [parallel folding]%n", range);
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
        //payment = sequenceOfPayments.toMonoid(Payment.identity(), Payment::append).parallelFold();
        payment = sequenceOfPayments.parallelFold(
            new FreeMonoid<>(Payment::append, Payment.identity())
        );

        System.out.printf("\"Sum\" of Payments: Plain functional Java [parallel folding], generating took %d ms%n", between(startGenerating, startProcessing).toMillis());
        System.out.printf("\"Sum\" of Payments: Plain functional Java [parallel folding], processing took %d ms%n", between(startProcessing, now()).toMillis());
        System.out.printf("Folded payment: %s%n", payment);
        assertThat(sequenceOfPayments.size()).isEqualTo(range.longValue());
        assertThat(payment).isNotNull();
        assertThat(payment.cardHolderName).isNotNull();
        assertThat(payment.cardHolderName).isEqualTo(firstRandomPayment.cardHolderName);
        assertThat(payment.expirationMonth).isGreaterThan(YearMonth.now());
        assertThat(payment.expirationMonth).isEqualTo(firstRandomPayment.expirationMonth);
        // ...
    }
}
