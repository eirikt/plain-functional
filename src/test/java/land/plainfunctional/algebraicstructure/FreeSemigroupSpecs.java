package land.plainfunctional.algebraicstructure;

import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.Address;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreeSemigroupSpecs {

    //public static <T> SortedSet<T> singletonSortedSet(T singleElement) {
    //    //SortedSet<T> singletonSortedSet = new TreeSet<>((o1, o2) -> 0);
    //    //singletonSortedSet.add(singleElement);
    //    //return singletonSortedSet;
    //    return new TreeSet<>(singletonList(singleElement));
    //}

    public static <T> Comparator<T> chronologicalComparator() {
        return (element1, element2) -> {
            // No, no
            //if (element1 instanceof Comparable<?> && element2 instanceof Comparable<?>) {
            //    return ((Comparable<T>) element1).compareTo(element2);
            //}
            return Objects.equals(element1, element2)
                ? 0
                // By default: 'element1' is greater than 'element2'
                : 1;
        };
    }

    public static <T> Comparator<T> orderedByInsertTime() {
        return chronologicalComparator();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    void emptyConstructor_shouldNotCompile() {
        //Semigroup<String> magma = new FreeSemigroup<>();
    }

    void unaryConstructor_shouldNotCompile() {
        //Semigroup<String> magma = new FreeSemigroup<>(null);
    }

    @Test
    void binaryConstructor_whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> new FreeSemigroup<>(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a set of values");

        assertThatThrownBy(() -> new FreeSemigroup<String>(emptySortedSet(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a closed binary operation");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Totality / Closure
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenNullArgs_shouldThrowException() {
        FreeSemigroup<String> emptyStringAppendingSemigroup = new FreeSemigroup<>(
            emptySortedSet(),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> emptyStringAppendingSemigroup.append(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingSemigroup.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element2' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingSemigroup.append(null, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be 'null'");
    }

    @Test
    void shouldAppend_1() {
        FreeSemigroup<String> stringAppendingSemigroup = new FreeSemigroup<>(
            new TreeSet<>(asList("foo", "bar", "foobar")),
            (string1, string2) -> string1 + string2
        );

        assertThat(stringAppendingSemigroup.append("foo", "bar")).isEqualTo("foobar");
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

        FreeSemigroup<Address> addressAppendingSemigroup = new FreeSemigroup<>(
            chronologicallyEnumeratedSet,
            Address::append
        );

        assertThat(addressAppendingSemigroup.append(address1, address3)).isEqualTo(
            address1.postalLocation("The valley")
        );

        assertThat(addressAppendingSemigroup.append(address3, address1)).isEqualTo(
            address1.postalLocation("The valley")
        );
    }

    //@Test No, this is not a FreeSemigroup requirement - associativity only kicks in when combining more than 2 elements
    void whenViolatesDefinedEnumeration_shouldThrowException_1() {
        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add("foo");
        chronologicallyEnumeratedSet.add("bar");
        chronologicallyEnumeratedSet.add("barfoo");

        FreeSemigroup<String> stringAppendingSemigroup = new FreeSemigroup<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> stringAppendingSemigroup.append("bar", "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Illegal associative relation between the given elements in the FreeSemigroup");
    }

    //@Test No, this is not a semigroup requirement - associativity only kicks in when combining more than 2 elements
    void whenViolatesDefinedEnumeration_shouldThrowException_2() {
        Address address1 = new Address("The street 1", "1234");
        Address address2 = new Address("The street 2", "1234");
        Address address3 = new Address("The street 3", "1234");

        SortedSet<Address> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add(address1);
        chronologicallyEnumeratedSet.add(address2);
        chronologicallyEnumeratedSet.add(address3);

        FreeSemigroup<Address> stringAppendingSemigroup = new FreeSemigroup<>(
            chronologicallyEnumeratedSet,
            Address::append
        );

        assertThatThrownBy(() -> stringAppendingSemigroup.append(address3, address1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Illegal associative relation between the given elements in the semigroup");
    }

    @Test
    void shouldAppend_3() {
        SortedSet<Integer> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList(7, 6, 5, 4, 3, 2, 1));

        FreeSemigroup<Integer> numberAppendingSemigroup = new FreeSemigroup<>(
            chronologicallyEnumeratedSet,
            Integer::sum
        );

        assertThat(numberAppendingSemigroup.append(2, 5)).isEqualTo(7);
    }

    @Test
    void shouldAppend_4() {
        BinaryOperator<Integer> clockHourAdd =
            (hour1, hour2) -> ((hour1 + hour2) % 12 == 0)
                ? 12
                : (hour1 + hour2) % 12;

        SortedSet<Integer> set = new TreeSet<>(asList(1, 3, 5, 7, 9, 11, 2, 4, 6, 8, 10, 12));

        FreeSemigroup<Integer> clockHourAppendingSemigroup = new FreeSemigroup<>(set, clockHourAdd);

        assertThat(clockHourAppendingSemigroup.append(2, 5)).isEqualTo(7);
        assertThat(clockHourAppendingSemigroup.append(2, 10)).isEqualTo(12);
        assertThat(clockHourAppendingSemigroup.append(2, 12)).isEqualTo(2);
        assertThat(clockHourAppendingSemigroup.append(8, 11)).isEqualTo(7);
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

        FreeSemigroup<String> helloWorldGeneratingSemigroup = new FreeSemigroup<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2
        );

        String identityValue = "";
        String reducedString = helloWorldGeneratingSemigroup.set
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

        FreeSemigroup<String> helloWorldGeneratingSemigroup = new FreeSemigroup<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2
        );

        String identityValue = "";
        String reducedString = helloWorldGeneratingSemigroup.set
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
    void shouldFold() {
        String identityValue = "";

        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add(" ");

        FreeSemigroup<String> helloWorldGeneratingSemigroup = new FreeSemigroup<>(
            chronologicallyEnumeratedSet,
            (string1, string2) -> string1 + string2
        );

        assertThat(helloWorldGeneratingSemigroup.leftFold(identityValue)).isEqualTo("Hello world!");
    }
}
