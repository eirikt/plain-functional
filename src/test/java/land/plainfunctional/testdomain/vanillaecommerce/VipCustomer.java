package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.OffsetDateTime;

// TODO: Make immutable
public class VipCustomer extends Customer implements Cloneable {

    public OffsetDateTime vipCustomerSince;

    public boolean wasVipCustomerAt(OffsetDateTime date) {
        return date.equals(this.vipCustomerSince) || date.isBefore(this.vipCustomerSince);
    }

    public boolean hasAlwaysBeenVipCustomer() {
        return this.membershipDate.equals(this.vipCustomerSince);
    }

    public VipCustomer append(VipCustomer vipCustomer) {
        VipCustomer mergedVipCustomer = (VipCustomer) super.append(vipCustomer);

        mergedVipCustomer.vipCustomerSince = this.vipCustomerSince != null ? this.vipCustomerSince : vipCustomer.vipCustomerSince;

        return mergedVipCustomer;
    }

    @Override
    public VipCustomer clone() throws CloneNotSupportedException {
        return (VipCustomer) super.clone();
    }
}
