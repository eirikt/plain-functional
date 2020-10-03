package land.plainfunctional.testdomain.vanillaecommerce;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

/**
 * An immutable payment class.
 */
public class Payment {

    private static final Payment IDENTITY = new Payment();

    public static Payment identity() {
        return IDENTITY;
    }

    public static Payment random() {
        String cardNumber = randomNumeric(4) + " " + randomNumeric(4) + " " + randomNumeric(4) + " " + randomNumeric(4);
        String cardHolderName = randomAlphabetic(2, 9) + " " + randomAlphabetic(2, 13);
        YearMonth expirationMonth = YearMonth.of(2020 + nextInt(1, 7), nextInt(1, 13));
        Integer cvc = parseInt(randomNumeric(3));
        Double amount = new BigDecimal(randomNumeric(2, 6) + "." + randomNumeric(1, 100))
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
        boolean isPaymentReceived = nextInt(0, 2) != 0;

        return new Payment(cardNumber, cardHolderName, expirationMonth, cvc, amount, isPaymentReceived);
    }


    /**
     * Extended 'transient' semantics: Not to be processed in any kind of (side) effect!
     */
    public final transient String cardNumber;

    /**
     * Extended 'transient' semantics: Not to be processed in any kind of (side) effect!
     */
    public final transient String cardHolderName;

    /**
     * Extended 'transient' semantics: Not to be processed in any kind of (side) effect!
     */
    public final transient YearMonth expirationMonth;

    /**
     * Extended 'transient' semantics: Not to be processed in any kind of (side) effect!
     */
    public final transient Integer cvc;

    public final Double amount;

    public final boolean isPaymentReceived;


    public Payment() {
        this.cardNumber = null;
        this.cardHolderName = null;
        this.expirationMonth = null;
        this.cvc = null;
        this.amount = null;
        this.isPaymentReceived = false;
    }

    public Payment(String cardNumber, String cardHolderName, YearMonth expirationMonth, Integer cvc, Double amount) {
        this(cardNumber, cardHolderName, expirationMonth, cvc, amount, false);
    }

    public Payment(String cardNumber, String cardHolderName, YearMonth expirationMonth, Integer cvc, Double amount, boolean isPaymentReceived) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expirationMonth = expirationMonth;
        this.cvc = cvc;
        this.amount = amount;
        this.isPaymentReceived = isPaymentReceived;
    }


    public Payment cardNumber(String cardNumber) {
        return new Payment(
            cardNumber,
            this.cardHolderName,
            this.expirationMonth,
            this.cvc,
            this.amount,
            this.isPaymentReceived
        );
    }

    public Payment cardHolderName(String cardHolderName) {
        return new Payment(
            this.cardNumber,
            cardHolderName,
            this.expirationMonth,
            this.cvc,
            this.amount,
            this.isPaymentReceived
        );
    }

    public Payment expirationDate(YearMonth expirationMonth) {
        return new Payment(
            this.cardNumber,
            this.cardHolderName,
            expirationMonth,
            this.cvc,
            this.amount,
            this.isPaymentReceived
        );
    }

    public Payment cvc(Integer cvc) {
        return new Payment(
            this.cardNumber,
            this.cardHolderName,
            this.expirationMonth,
            cvc,
            this.amount,
            this.isPaymentReceived
        );
    }

    public Payment amount(Double amount) {
        return new Payment(
            this.cardNumber,
            this.cardHolderName,
            this.expirationMonth,
            this.cvc,
            amount,
            this.isPaymentReceived
        );
    }


    /**
     * Algebraic append, strategy: ...
     *
     * This binary operation forms a monoid together with this class' 'identity' function (and an enumerated set of {@link Payment}.
     *
     * @param payment The "secondary" payment
     * @return the merged payment
     */
    public Payment append(Payment payment) {
        return new Payment(
            isBlank(this.cardNumber) ? payment.cardNumber : this.cardNumber,
            isBlank(this.cardHolderName) ? payment.cardHolderName : this.cardHolderName,
            this.expirationMonth == null ? payment.expirationMonth : this.expirationMonth,
            this.cvc == null ? payment.cvc : this.cvc,
            this.amount == null ? payment.amount : this.amount,
            payment.isPaymentReceived
        );
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.cardNumber)
            .append(this.cardHolderName)
            .append(this.expirationMonth)
            .append(this.cvc)
            .append(this.amount)
            .append(this.isPaymentReceived)
            .toHashCode();
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }
        Payment otherPayment = (Payment) otherObject;

        return new EqualsBuilder()
            .append(this.cardNumber, otherPayment.cardNumber)
            .append(this.cardHolderName, otherPayment.cardHolderName)
            .append(this.expirationMonth, otherPayment.expirationMonth)
            .append(this.cvc, otherPayment.cvc)
            .append(this.amount, otherPayment.amount)
            .append(this.isPaymentReceived, otherPayment.isPaymentReceived)
            .isEquals();
    }

    @Override
    public String toString() {
        return reflectionToString(
            this, ToStringStyle.SHORT_PREFIX_STYLE, true
        );
    }

    /**
     * Equal to <code>toString</code>, except for Java object references, which are omitted.
     * Also, <i>transient</i> fields are not included in String representations of these objects.
     */
    public String toStringNoTransient() {
        //return format(
        //    "%s[amount=%s, isPaymentReceived=%s]",
        //    this.getClass().getName(),
        //    this.amount, this.isPaymentReceived
        //);
        return reflectionToString(
            this, ToStringStyle.SHORT_PREFIX_STYLE
        );
    }
}
