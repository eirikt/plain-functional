package land.plainfunctional.algebraicstructure;

import java.time.YearMonth;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.Payment;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreeMagmaSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    void emptyConstructor_shouldNotCompile() {
        //FreeMagma<String> FreeMagma = new FreeMagma<>();
    }

    @Test
    void unaryConstructor_whenNullArg_shouldThrowException() {
        assertThatThrownBy(() -> new FreeMagma<>(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a closed binary operation");
    }

    @Test
    void binaryConstructor_whenNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> new FreeMagma<>(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a set of values");

        assertThatThrownBy(() -> new FreeMagma<>(emptySet(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A magma must have a closed binary operation");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Totality / Closure
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void whenNullArgs_shouldThrowException_1() {
        FreeMagma<String> emptyStringAppendingMagma = new FreeMagma<>(
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> emptyStringAppendingMagma.append(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value1' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMagma.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value2' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMagma.append(null, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value1' argument cannot be 'null'");
    }

    @Test
    void whenNullArgs_shouldThrowException_2() {
        FreeMagma<String> emptyStringAppendingMagma = new FreeMagma<>(
            emptySet(),
            (string1, string2) -> string1 + string2
        );

        assertThatThrownBy(() -> emptyStringAppendingMagma.append(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value1' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMagma.append("foo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value2' argument cannot be 'null'");

        assertThatThrownBy(() -> emptyStringAppendingMagma.append(null, "foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'value1' argument cannot be 'null'");
    }

    @Test
    void whenUnknownArg_shouldAppend() {
        FreeMagma<String> stringAppendingMagma = new FreeMagma<>(
            (string1, string2) -> string1 + string2
        );

        assertThat(stringAppendingMagma.append("foo", "bar")).isEqualTo("foobar");
    }

    @Test
    void whenUnknownArgsAndResult_shouldAppend() {
        FreeMagma<String> stringAppendingMagma = new FreeMagma<>(
            singleton("foo"),
            (string1, string2) -> string1 + string2
        );

        assertThat(stringAppendingMagma.append("foo", "bar")).isEqualTo("foobar");
    }

    @Test
    void shouldAppend_1() {
        FreeMagma<String> singletonStringAppendingMagma = new FreeMagma<>(
            new HashSet<>(asList("foo", "bar", "foobar")),
            (string1, string2) -> string1 + string2
        );

        assertThat(singletonStringAppendingMagma.append("foo", "bar")).isEqualTo("foobar");
    }

    @Test
    void shouldAppend_2() {
        FreeMagma<Integer> numberAppendingMagma = new FreeMagma<>(
            new HashSet<>(asList(1, 2, 3, 4, 5, 6, 7, 8, 9)),
            Integer::sum
        );

        assertThat(numberAppendingMagma.append(8, 9)).isEqualTo(17);
    }

    @Test
    void shouldAppend_3() {
        FreeMagma<Integer> numberAppendingMagma = new FreeMagma<>(
            new HashSet<>(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)),
            (hour1, hour2) -> ((hour1 + hour2) % 12 == 0)
                ? 12
                : (hour1 + hour2) % 12
        );
        assertThat(numberAppendingMagma.append(2, 5)).isEqualTo(7);
        assertThat(numberAppendingMagma.append(2, 10)).isEqualTo(12);
        assertThat(numberAppendingMagma.append(8, 11)).isEqualTo(7);
    }

    @Test
    void shouldAppend_4() {
        Payment payment1 = new Payment()
            .cardNumber("1234 1234")
            .cardHolderName("JOHN JAMES")
            .expirationDate(YearMonth.of(2022, 11))
            .cvc(123);

        Payment payment2 = new Payment()
            .cardNumber("1234 1234")
            .cardHolderName("JOHN JAMES SR.")
            .amount(12.45);

        FreeMagma<Payment> paymentAppendingMagma = new FreeMagma<>(
            new HashSet<>(),
            Payment::append
        );

        Payment mergedPayment = paymentAppendingMagma.append(payment1, payment2);
        assertThat(mergedPayment.cardNumber).isEqualTo("1234 1234");
        assertThat(mergedPayment.cardHolderName).isEqualTo("JOHN JAMES");
        assertThat(mergedPayment.expirationMonth).isEqualTo(YearMonth.of(2022, 11));
        assertThat(mergedPayment.cvc).isEqualTo(123);
        assertThat(mergedPayment.amount).isEqualTo(12.45);
        assertThat(mergedPayment.isPaymentReceived).isFalse();
    }
}
