package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import land.plainfunctional.Immutable;

@Immutable
public class Customer extends Person {

    private static final Customer IDENTITY = new Customer();

    /**
     * Customer identification.
     */
    @Immutable
    public final String customerId;

    /**
     * Customer authentication.
     */
    @Immutable
    public final String authenticationToken;

    @Immutable
    public final OffsetDateTime membershipFromDate;

    @Immutable
    public final OffsetDateTime membershipToDate;

    @Immutable
    public final Customer enlistedBy;

    @Immutable
    public final Set<Customer> enlistedCustomers;

    public Customer() {
        super(null);
        this.customerId = null;
        this.authenticationToken = null;
        this.membershipFromDate = null;
        this.membershipToDate = null;
        this.enlistedBy = null;
        this.enlistedCustomers = null;
    }

    public Customer(String name) {
        super(name);
        this.customerId = null;
        this.authenticationToken = null;
        this.membershipFromDate = null;
        this.membershipToDate = null;
        this.enlistedBy = null;
        this.enlistedCustomers = null;
    }

    public Customer(String name, String customerId) {
        super(name);
        this.customerId = customerId;
        this.authenticationToken = null;
        this.membershipFromDate = null;
        this.membershipToDate = null;
        this.enlistedBy = null;
        this.enlistedCustomers = null;
    }

    public Customer(
        String entityId,
        OffsetDateTime entityCreateTime,
        OffsetDateTime entityLastModifyTime,
        String name,
        Gender gender,
        LocalDate birthDate,
        String customerId,
        String authenticationToken,
        OffsetDateTime membershipFromDate,
        OffsetDateTime membershipToDate,
        Customer enlistedBy,
        Set<Customer> enlistedCustomers
    ) {
        super(entityId, entityCreateTime, entityLastModifyTime, name, gender, birthDate);
        this.customerId = customerId;
        this.authenticationToken = authenticationToken;
        this.membershipFromDate = membershipFromDate;
        this.membershipToDate = membershipToDate;
        this.enlistedBy = enlistedBy;
        this.enlistedCustomers = enlistedCustomers;
    }

    public Customer append(Customer customer) {
        throw new UnsupportedOperationException();
        /*
        return new Customer(
            this.entityId.toString(),
            this.entityCreateTime,
            now(),
            isNotBlank(this.name) ? this.name : customer.name,
            (this.gender != null) ? this.gender : customer.gender,
            (this.birthDate != null) ? this.birthDate : customer.birthDate,
            (this.customerId != null) ? this.customerId : customer.customerId,
            (this.authenticationToken != null) ? this.authenticationToken : customer.authenticationToken,
            (this.membershipFromDate != null) ? this.membershipFromDate : customer.membershipFromDate,
            (this.membershipToDate != null) ? this.membershipToDate : customer.membershipToDate,
            (this.enlistedBy != null) ? this.enlistedBy : customer.enlistedBy,
            (this.enlistedCustomers != null) ? this.enlistedCustomers : customer.enlistedCustomers
        );
        */
    }

    //@Override
    //public Customer clone() throws CloneNotSupportedException {
    //    throw new UnsupportedOperationException();
    //    //return (Customer) super.clone();
    //}
}
