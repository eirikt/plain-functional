package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.LocalDate;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import static java.lang.String.format;

public class Payment implements Value {

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
    public final transient LocalDate expirationDate;

    /**
     * Extended 'transient' semantics: Not to be processed in any kind of (side) effect!
     */
    public final transient Integer cvc;

    public final Double amount;

    public final boolean isPaymentReceived;


    public Payment() {
        this.cardNumber = null;
        this.cardHolderName = null;
        this.expirationDate = null;
        this.cvc = null;
        this.amount = null;
        this.isPaymentReceived = false;
    }

    public Payment(String cardNumber, String cardHolderName, LocalDate expirationDate, Integer cvc, Double amount) {
        this(cardNumber, cardHolderName, expirationDate, cvc, amount, false);
    }

    public Payment(String cardNumber, String cardHolderName, LocalDate expirationDate, Integer cvc, Double amount, boolean isPaymentReceived) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expirationDate = expirationDate;
        this.cvc = cvc;
        this.amount = amount;
        this.isPaymentReceived = isPaymentReceived;
    }


    public Payment cardNumber(String cardNumber) {
        return new Payment(
                cardNumber,
                this.cardHolderName,
                this.expirationDate,
                this.cvc,
                this.amount,
                this.isPaymentReceived
        );
    }

    public Payment cardHolderName(String cardHolderName) {
        return new Payment(
                this.cardNumber,
                cardHolderName,
                this.expirationDate,
                this.cvc,
                this.amount,
                this.isPaymentReceived
        );
    }

    public Payment expirationDate(LocalDate expirationDate) {
        return new Payment(
                this.cardNumber,
                this.cardHolderName,
                expirationDate,
                this.cvc,
                this.amount,
                this.isPaymentReceived
        );
    }

    public Payment cvc(Integer cvc) {
        return new Payment(
                this.cardNumber,
                this.cardHolderName,
                this.expirationDate,
                cvc,
                this.amount,
                this.isPaymentReceived
        );
    }

    public Payment amount(Double amount) {
        return new Payment(
                this.cardNumber,
                this.cardHolderName,
                this.expirationDate,
                this.cvc,
                amount,
                this.isPaymentReceived
        );
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.cardNumber)
                .append(this.cardHolderName)
                .append(this.expirationDate)
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
                .append(this.expirationDate, otherPayment.expirationDate)
                .append(this.cvc, otherPayment.cvc)
                .append(this.amount, otherPayment.amount)
                .append(this.isPaymentReceived, otherPayment.isPaymentReceived)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    /**
     * Equal to <code>toString</code>, except for Java object references, which are omitted.
     * Also, <i>transient</i> fields are not included in String representations of these objects.
     */
    public String toValueString() {
        return format(
                "%s[amount=%s, isPaymentReceived=%s]",
                this.getClass().getName(),
                this.amount, this.isPaymentReceived
        );
    }
}
