package land.plainfunctional.algebraicstructure;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.Address;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagmaSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    void emptyConstructor_shouldNotCompile() {
        //Magma<String> magma = new Magma<>();
    }

    void unaryConstructor_shouldNotCompile() {
        //Magma<String> magma = new Magma<>(null);
    }

    @Test
    void binaryConstructor_whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> new Magma<>(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a set of values");

        assertThatThrownBy(() -> new Magma<>(emptySet(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a closed binary operation");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Totality / Closure
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenNullArgs_shouldThrowException() {
        Magma<String> emptyStringAppendingMagma = new Magma<>(
            emptySet(),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> emptyStringAppendingMagma.append(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMagma.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element2' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMagma.append(null, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument cannot be 'null'");
    }

    @Test
    void whenUnknownArgs_shouldThrowException() {
        Magma<String> stringAppendingMagma = new Magma<>(
            singleton("foo"),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> stringAppendingMagma.append("foo", "bar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element2' argument is not an element of this magma");

        assertThatThrownBy(() -> stringAppendingMagma.append("bar", "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'element1' argument is not an element of this magma");
    }

    @Test
    void whenUnknownResult_shouldThrowException() {
        Magma<String> stringAppendingMagma = new Magma<>(
            Stream.of("foo", "bar").collect(toSet()),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> stringAppendingMagma.append("foo", "bar"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The result of the applied binary operation is not an element of this magma");
    }

    @Test
    void shouldAppend_1() {
        Magma<String> stringAppendingMagma = new Magma<>(
            new HashSet<>(asList("foo", "bar", "foobar")),
            (string1, string2) -> string1 + string2
        );

        assertThat(stringAppendingMagma.append("foo", "bar")).isEqualTo("foobar");
    }

    @Test
    void shouldAppend_2() {
        Magma<Integer> numberAppendingMagma = new Magma<>(
            new HashSet<>(asList(1, 2, 3, 4, 5, 6, 7, 8, 9)),
            Integer::sum
        );

        assertThat(numberAppendingMagma.append(2, 5)).isEqualTo(7);
    }

    @Test
    void shouldAppend_3() {
        Magma<Integer> numberAppendingMagma = new Magma<>(
            new HashSet<>(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)),
            (hour1, hour2) -> ((hour1 + hour2) % 12 == 0)
                ? 12
                : (hour1 + hour2) % 12
        );
        assertThat(numberAppendingMagma.append(2, 5)).isEqualTo(7);
        assertThat(numberAppendingMagma.append(2, 10)).isEqualTo(12);
        assertThat(numberAppendingMagma.append(2, 12)).isEqualTo(2);
        assertThat(numberAppendingMagma.append(8, 11)).isEqualTo(7);
    }

    @Test
    void shouldAppend_4() {
        Address address1 = new Address("The street 1", "1234");
        Address address2 = new Address("The street 2", "1234");
        Address address3 = new Address("The street 1", "1234", "The valley", null);

        Set<Address> set = new HashSet<>();
        set.add(address1);
        set.add(address2);
        set.add(address3);

        Magma<Address> singletonStringAppendingMagma = new Magma<>(
            set,
            Address::append
        );

        assertThat(singletonStringAppendingMagma.append(address1, address3)).isEqualTo(
            address1.postalLocation("The valley")
        );
    }


    ///////////////////////////////////////////////////////////////////////////
    // Set semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldRemoveIdenticalElements() {
        Semigroup<String> singletonStringAppendingSemigroup = new Semigroup<>(
            new TreeSet<>(asList("foo", "bar", "foo", "bar", "foobar")),
            (string1, string2) -> string1 + string2
        );

        assertThat(singletonStringAppendingSemigroup.set.size()).isEqualTo(3);
    }

    @Test
    void whenEqualElements_shouldThrowException() {
        Magma<String> singletonStringAppendingMagma = new Magma<>(
            new TreeSet<>(asList("foo", "foofoo")),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> singletonStringAppendingMagma.append("foo", "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot append two equal element values in a magma");
    }
}
