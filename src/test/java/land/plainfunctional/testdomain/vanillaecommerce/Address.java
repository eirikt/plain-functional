package land.plainfunctional.testdomain.vanillaecommerce;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import land.plainfunctional.util.Arguments;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Address {

    public static final Address IDENTITY = new Address();

    public final String streetLine;
    public final String postalCode;
    public final String postalLocation;
    public final String country;

    protected Address() {
        this.streetLine = null;
        this.postalCode = null;
        this.postalLocation = null;
        this.country = null;
    }

    public Address(String streetLine, String postalCode) {
        this(streetLine, postalCode, null, null);
    }

    public Address(String streetLine, String postalCode, String postalLocation, String country) {
        Arguments.requireNotBlank(streetLine, "streetLine' argument cannot be blank");
        Arguments.requireNotBlank(postalCode, "postalCode' argument cannot be blank");
        this.streetLine = streetLine;
        this.postalCode = postalCode;
        this.postalLocation = postalLocation;
        this.country = country;
    }

    /**
     * Algebraic append<br>
     * <i>Strategy: If the property is missing, add the property of the other ("secondary") address.</i>
     *
     * <p>
     * This binary operation forms a monoid together with this class' 'identity' function (and an enumerated set of {@link Address}.
     * </p>
     *
     * @param address The "secondary" address
     * @return the appended/merged address
     */
    public Address append(Address address) {
        return new Address(
            isNotBlank(this.streetLine) ? this.streetLine : address.streetLine,
            isNotBlank(this.postalCode) ? this.postalCode : address.postalCode,
            isNotBlank(this.postalLocation) ? this.postalLocation : address.postalLocation,
            isNotBlank(this.country) ? this.country : address.country
        );
    }

    public Address postalLocation(String postalLocation) {
        return new Address(this.streetLine, this.postalCode, postalLocation, this.country);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(streetLine)
            .append(postalCode)
            .append(postalLocation)
            .append(country)
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
        Address otherAddress = (Address) otherObject;

        return new EqualsBuilder()
            .append(this.streetLine, otherAddress.streetLine)
            .append(this.postalCode, otherAddress.postalCode)
            .append(this.postalLocation, otherAddress.postalLocation)
            .append(this.country, otherAddress.country)
            .isEquals();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(
            this,
            ToStringStyle.SHORT_PREFIX_STYLE,
            true,
            false,
            true,
            Object.class
        );
    }
}
