package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import static java.lang.String.format;
import static java.time.OffsetDateTime.now;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class VipCustomer extends Customer {

    public final OffsetDateTime vipCustomerSince;

    /*
    public VipCustomer() {
        super(null);
        this.vipCustomerSince = null;
    }

    public VipCustomer(String name) {
        super(name);
        this.vipCustomerSince = null;
    }


    public VipCustomer append(VipCustomer vipCustomer) {
        VipCustomer mergedVipCustomer = (VipCustomer) super.append(vipCustomer);

        mergedVipCustomer.vipCustomerSince = this.vipCustomerSince != null ? this.vipCustomerSince : vipCustomer.vipCustomerSince;

        return mergedVipCustomer;
    }
    */

    private static final VipCustomer IDENTITY = new VipCustomer();

    public VipCustomer() {
        super(null, null, null, null, null, null, null, null, null, null, null, null);
        this.vipCustomerSince = now();
    }

    public VipCustomer(String name) {
        super(null, null, null, name, null, null, null, null, null, null, null, null);
        this.vipCustomerSince = now();
    }

    public VipCustomer(
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
        Set<Customer> enlistedCustomers,
        OffsetDateTime vipCustomerSince
    ) {
        super(entityId, entityCreateTime, entityLastModifyTime, name, gender, birthDate, customerId, authenticationToken, membershipFromDate, membershipToDate, enlistedBy, enlistedCustomers);
        this.vipCustomerSince = vipCustomerSince;
    }

    public VipCustomer append(VipCustomer vipCustomer) {
        if (!this.entityId.equals(vipCustomer.entityId)) {
            throw new IllegalArgumentException(format("Appending different entities ('entityId') is not allowed (%s <-> %s)", this.entityId, vipCustomer.entityId));
        }
        if (!this.entityCreateTime.equals(vipCustomer.entityCreateTime)) {
            throw new IllegalArgumentException(format("Appending different entities ('entityCreateTime') is not allowed (%s <-> %s)", this.entityId, vipCustomer.entityId));
        }
        return new VipCustomer(
            this.entityId.toString(),
            this.entityCreateTime,
            now(),
            isNotBlank(this.name) ? this.name : vipCustomer.name,
            (this.gender != null) ? this.gender : vipCustomer.gender,
            (this.birthDate != null) ? this.birthDate : vipCustomer.birthDate,
            (this.customerId != null) ? this.customerId : vipCustomer.customerId,
            (this.authenticationToken != null) ? this.authenticationToken : vipCustomer.authenticationToken,
            (this.membershipFromDate != null) ? this.membershipFromDate : vipCustomer.membershipFromDate,
            (this.membershipToDate != null) ? this.membershipToDate : vipCustomer.membershipToDate,
            (this.enlistedBy != null) ? this.enlistedBy : vipCustomer.enlistedBy,
            (this.enlistedCustomers != null) ? this.enlistedCustomers : vipCustomer.enlistedCustomers,
            (this.vipCustomerSince != null) ? this.vipCustomerSince : vipCustomer.vipCustomerSince
        );
    }

    //@Override
    //public VipCustomer clone() throws CloneNotSupportedException {
    //    throw new UnsupportedOperationException();
    //    //return (VipCustomer) super.clone();
    //}

    public boolean wasVipCustomerAt(OffsetDateTime date) {
        return date.equals(this.vipCustomerSince) || date.isBefore(this.vipCustomerSince);
    }

    public boolean hasAlwaysBeenVipCustomer() {
        return this.membershipFromDate.equals(this.vipCustomerSince);
    }
}
