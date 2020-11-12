package land.plainfunctional.algebraicstructure;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.Address;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySortedSet;
import static land.plainfunctional.algebraicstructure.FreeSemigroupSpecs.orderedByInsertTime;
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
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new MonoidStructure<String>(
            emptySortedSet(),
            null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a closed binary operation");

        assertThatThrownBy(() -> new MonoidStructure<>(
            emptySortedSet(),
            (BinaryOperator<String>) (s, s2) -> null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A monoid must have an identity element - a neutral element");

        assertThatThrownBy(() -> new MonoidStructure<String>(
            emptySortedSet(),
            null,
            ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a closed binary operation");
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
            .hasMessage("'element1' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMonoid.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element2' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMonoid.append(null, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be 'null'");
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
    void shouldFold_1() {
        String identityValue = "";

        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList("a", " ", "b"));

        MonoidStructure<String> helloWorldGeneratingSemigroup = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(helloWorldGeneratingSemigroup.fold()).isEqualTo("a b");
    }

    // TODO: Yields duplicated elements in set (a bug somewhere...)
    //@Test
    void shouldFold_2() {
        String identityValue = "";

        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList(
            "Hello", " ", "world", "!", "Hello", " ", "world", "!", "Hello", " ", "world", "!"
        ));

        MonoidStructure<String> helloWorldGeneratingSemigroup = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(helloWorldGeneratingSemigroup.fold()).isEqualTo("Hello world!");
    }

    // TODO: Yields duplicated elements in set (a bug somewhere...)
    //@Test
    void shouldFold_3() {
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

        MonoidStructure<String> helloWorldGeneratingSemigroup = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(helloWorldGeneratingSemigroup.fold()).isEqualTo("Hello world!");
    }

    @Test
    void shouldFold_4() {
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

        MonoidStructure<String> helloWorldGeneratingSemigroup = new MonoidStructure<>(
            chronologicallyEnumeratedSortedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(helloWorldGeneratingSemigroup.fold()).isEqualTo("Hello world!");
    }

    @Test
    void shouldFold_5() {
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

        MonoidStructure<String> helloWorldGeneratingSemigroup = new MonoidStructure<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2,
            identityValue
        );

        assertThat(helloWorldGeneratingSemigroup.fold()).isEqualTo("Hello world!");
    }
}
