package land.plainfunctional.algebraicstructure;

import java.time.LocalDate;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.Payment;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagmaSpecs {

    //@Test
    void shouldNotCompile_1() {
        //Magma<String> magma = new Magma<>();
        assertThat(true).isTrue();
    }

    //@Test
    void shouldNotCompile_2() {
        //Magma<String> magma = new Magma<>(null);
        assertThat(true).isTrue();
    }

    @Test
    void whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> new Magma<>(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A 'Magma' instance must have a set of values");

        assertThatThrownBy(() -> new Magma<>(new HashSet<>(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A 'Magma' instance must have a closed binary operation");
    }

    @Test
    void append_whenNullArgs_shouldThrowException() {
        Magma<String> emptyStringAppendingMagma = new Magma<>(
            emptySet(),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> emptyStringAppendingMagma.append(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value1' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMagma.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value2' argument cannot be 'null'");
    }

    @Test
    void append_whenUnknownArgs_shouldThrowException() {
        Magma<String> stringAppendingMagma = new Magma<>(
            singleton("foo"),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> stringAppendingMagma.append("foo", "bar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value2' argument is not an element of this magma");

        assertThatThrownBy(() -> stringAppendingMagma.append("bar", "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value1' argument is not an element of this magma");
    }

    @Test
    void append_shouldAppend_1() {
        Magma<String> stringAppendingMagma = new Magma<>(
            new HashSet<>(asList("foo", "bar")),
            (string1, string2) -> string1 + string2
        );

        assertThat(stringAppendingMagma.append("foo", "bar")).isEqualTo("foobar");
    }

    @Test
    void append_shouldAppend_2() {
        Magma<Integer> numberAppendingMagma = new Magma<>(
            new HashSet<>(asList(1, 2, 3, 4, 5, 6, 7, 8, 9)),
            Integer::sum
        );

        assertThat(numberAppendingMagma.append(8, 9)).isEqualTo(17);
    }

    @Test
    void append_shouldAppend_3() {
        Payment payment1 = new Payment()
            .cardNumber("1234 1234")
            .cardHolderName("JOHN JAMES")
            .expirationDate(LocalDate.of(2022, 11, 1))
            .cvc(123);

        Payment payment2 = new Payment()
            .cardNumber("1234 1234")
            .cardHolderName("JOHN JAMES SR.")
            .amount(12.45);

        Magma<Payment> paymentAppendingMagma = new Magma<>(
            new HashSet<>(asList(payment1, payment2)),
            Payment::append
        );

        Payment mergedPayment = paymentAppendingMagma.append(payment1, payment2);
        assertThat(mergedPayment.cardNumber).isEqualTo("1234 1234");
        assertThat(mergedPayment.cardHolderName).isEqualTo("JOHN JAMES");
        assertThat(mergedPayment.expirationDate).isEqualTo(LocalDate.of(2022, 11, 1));
        assertThat(mergedPayment.cvc).isEqualTo(123);
        assertThat(mergedPayment.amount).isEqualTo(12.45);
        assertThat(mergedPayment.isPaymentReceived).isFalse();
    }
}
