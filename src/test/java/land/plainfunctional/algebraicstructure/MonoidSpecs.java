package land.plainfunctional.algebraicstructure;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.Address;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.singletonList;
import static land.plainfunctional.algebraicstructure.FreeSemigroupSpecs.orderedByInsertTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonoidSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    void emptyConstructor_shouldNotCompile() {
        //Monoid<String> magma = new Monoid<>();
    }

    void unaryConstructor_shouldNotCompile() {
        //Monoid<String> magma = new Monoid<>(null);
    }

    void binaryConstructor_shouldNotCompile() {
        //FreeMonoid<String> magma = new FreeMonoid<>(null, null);
    }

    @Test
    void binaryConstructor_whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> new Monoid<>(
            (SortedSet<?>) null,
            null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a set of values");

        assertThatThrownBy(() -> new Monoid<String>(
            emptySortedSet(),
            null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a closed binary operation");

        assertThatThrownBy(() -> new Monoid<>(
            emptySortedSet(),
            (BinaryOperator<String>) (s, s2) -> null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A monoid must have an identity element - a neutral element");

        assertThatThrownBy(() -> new Monoid<String>(
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
        Monoid<String> emptyStringAppendingMonoid = new Monoid<>(
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
    void whenUnknownArgs_shouldThrowException_1() {
        Monoid<String> stringAppendingMonoid = new Monoid<>(
            emptySortedSet(),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThatThrownBy(() -> stringAppendingMonoid.append("foo", "bar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument is not an element of this magma");

        assertThatThrownBy(() -> stringAppendingMonoid.append("bar", "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument is not an element of this magma");
    }

    @Test
    void whenUnknownArgs_shouldThrowException_2() {
        Monoid<String> stringAppendingMonoid = new Monoid<>(
            new TreeSet<>(singletonList("foo")),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThatThrownBy(() -> stringAppendingMonoid.append("foo", "bar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element2' argument is not an element of this magma");

        assertThatThrownBy(() -> stringAppendingMonoid.append("bar", "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument is not an element of this magma");
    }

    @Test
    void whenUnknownResult_shouldThrowException() {
        Monoid<String> stringAppendingMonoid = new Monoid<>(
            new TreeSet<>(asList("foo", "bar")),
            (string1, string2) -> string1 + string2,
            ""
        );

        assertThatThrownBy(() -> stringAppendingMonoid.append("foo", "bar"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The result of the applied binary operation is not an element of this magma");
    }

    @Test
    void shouldAppend_1() {
        Monoid<String> stringAppendingMonoid = new Monoid<>(
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

        Monoid<Address> addressAppendingMonoid = new Monoid<>(
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

        Monoid<String> stringAppendingMonoid = new Monoid<>(
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

        Monoid<Address> stringAppendingMonoid = new Monoid<>(
            chronologicallyEnumeratedSet,
            Address::append,
            Address.IDENTITY
        );

        assertThatThrownBy(() -> stringAppendingMonoid.append(address3, address1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Illegal associative relation between the given elements in the Monoid");
    }

    @Test
    void shouldAppend_3() {
        SortedSet<Integer> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList(7, 6, 5, 4, 3, 2, 1));

        Monoid<Integer> numberAppendingMonoid = new Monoid<>(
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

        Monoid<Integer> clockHourAppendingMonoid = new Monoid<>(
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

        Monoid<String> helloWorldGeneratingMonoid = new Monoid<>(
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

        Monoid<String> helloWorldGeneratingMonoid = new Monoid<>(
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
}
