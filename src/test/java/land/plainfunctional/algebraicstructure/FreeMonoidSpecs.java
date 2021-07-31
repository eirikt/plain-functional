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
import static land.plainfunctional.algebraicstructure.SemigroupSpecs.orderedByInsertTime;
import static land.plainfunctional.monad.TestData.INTEGERS_UNDER_ADDITION_MONOID;
import static land.plainfunctional.monad.TestData.STRING_APPENDING_MONOID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreeMonoidSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    void emptyConstructor_shouldNotCompile() {
        //FreeMonoid<String> magma = new FreeMonoid<>();
    }

    void unaryConstructor_shouldNotCompile_1() {
        //FreeMonoid<String> magma = new FreeMonoid<>((BinaryOperator<String>) null);
    }

    void unaryConstructor_shouldNotCompile_2() {
        //FreeMonoid<String> magma = new FreeMonoid<>((String) null);
    }

    @Test
    void binaryConstructor_whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> new FreeMonoid<>(
                null,
                null
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("A semigroup must have an identity element - a neutral element");

        assertThatThrownBy(() -> new FreeMonoid<>(
                (BinaryOperator<String>) (s, s2) -> null,
                null
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("A monoid must have an identity element - a neutral element");

        assertThatThrownBy(() -> new FreeMonoid<>(
                null,
                ""
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("A semigroup must have an identity element - a neutral element");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Totality / Closure
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> STRING_APPENDING_MONOID.append(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be null");

        assertThatThrownBy(() -> STRING_APPENDING_MONOID.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element2' argument cannot be null");

        assertThatThrownBy(() -> STRING_APPENDING_MONOID.append(null, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be null");
    }

    @Test
    void shouldAppend_1() {
        assertThat(STRING_APPENDING_MONOID.append("foo", "bar")).isEqualTo("foobar");
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

        FreeMonoid<Address> addressAppendingMonoid = new FreeMonoid<>(
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

        assertThatThrownBy(() -> STRING_APPENDING_MONOID.append("bar", "foo"))
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

        FreeMonoid<Address> addressAppendingMonoid = new FreeMonoid<>(
            Address::append,
            Address.IDENTITY
        );

        assertThatThrownBy(() -> addressAppendingMonoid.append(address3, address1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Illegal associative relation between the given elements in the FreeMonoid");
    }

    @Test
    void shouldAppend_3() {
        assertThat(INTEGERS_UNDER_ADDITION_MONOID.append(2, 5)).isEqualTo(7);
    }

    @Test
    void shouldAppend_4() {
        BinaryOperator<Integer> clockHourAdd =
            (hour1, hour2) -> ((hour1 + hour2) % 12 == 0)
                ? 12
                : (hour1 + hour2) % 12;

        FreeMonoid<Integer> clockHourAppendingMonoid = new FreeMonoid<>(
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

        FreeMonoid<String> helloWorldGeneratingMonoid = new FreeMonoid<>(
            STRING_APPENDING_MONOID.binaryOperation,
            STRING_APPENDING_MONOID.identityElement
        );

        String identityValue = "";
        String reducedString = helloWorldGeneratingMonoid
            .toMonoidStructure(chronologicallyEnumeratedSet)
            .set
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

        FreeMonoid<String> helloWorldGeneratingMonoid = new FreeMonoid<>(
            STRING_APPENDING_MONOID.binaryOperation,
            STRING_APPENDING_MONOID.identityElement
        );

        String identityValue = "";
        String reducedString = helloWorldGeneratingMonoid
            .toMonoidStructure(chronologicallyEnumeratedSet)
            .set
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
        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList("a", " ", "b"));

        FreeMonoid<String> helloWorldGeneratingMonoid = new FreeMonoid<>(
            STRING_APPENDING_MONOID.binaryOperation,
            STRING_APPENDING_MONOID.identityElement
        );

        assertThat(helloWorldGeneratingMonoid.fold(chronologicallyEnumeratedSet)).isEqualTo("a b");
    }

    // TODO: Yields duplicated elements in set (a bug somewhere...)
    //@Test
    void shouldFold_2() {
        SortedSet<String> chronologicallyEnumeratedSet = new TreeSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.addAll(asList(
            "Hello", " ", "world", "!", "Hello", " ", "world", "!", "Hello", " ", "world", "!"
        ));

        FreeMonoid<String> helloWorldGeneratingMonoid = STRING_APPENDING_MONOID;

        assertThat(helloWorldGeneratingMonoid.fold(chronologicallyEnumeratedSet)).isEqualTo("Hello world!");
    }

    // TODO: Yields duplicated elements in set (a bug somewhere...)
    @Test
    void shouldFold_3() {
        SortedSet<String> chronologicallyEnumeratedSet = new ConcurrentSkipListSet<>(orderedByInsertTime());
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add("Hello");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add(" ");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("world");
        chronologicallyEnumeratedSet.add("!");
        chronologicallyEnumeratedSet.add("!");

        FreeMonoid<String> helloWorldGeneratingMonoid = STRING_APPENDING_MONOID;

        assertThat(helloWorldGeneratingMonoid.fold(chronologicallyEnumeratedSet)).isEqualTo("Hello world!");
    }

    @Test
    void shouldFold_4() {
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

        FreeMonoid<String> helloWorldGeneratingMonoid = STRING_APPENDING_MONOID;

        assertThat(helloWorldGeneratingMonoid.fold(chronologicallyEnumeratedSortedSet)).isEqualTo("Hello world!");
    }

    @Test
    void shouldFold_5() {
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

        FreeMonoid<String> helloWorldGeneratingMonoid = STRING_APPENDING_MONOID;

        assertThat(helloWorldGeneratingMonoid.fold(chronologicallyEnumeratedSet)).isEqualTo("Hello world!");
    }
}
