package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

// TODO: Make immutable
public class Customer extends Person implements Cloneable {

    public static final Customer IDENTITY = new Customer();

    public OffsetDateTime membershipDate;

    public String customerId;             // Username (identifies the user)
    // TODO: Move to the top
    public String sessionId;              // Identifies the computer session
    // TODO: Needed? Maybe the 'entityId' serves as authentication token?
    public String authenticationToken;    // Proof of user authentication

    public Customer enlistedBy;
    public Set<Customer> enlistedCustomers;

    public Customer append(Customer customer) {
        Customer mergedCustomer = (Customer) super.append(customer);

        mergedCustomer.membershipDate = this.membershipDate != null ? this.membershipDate : customer.membershipDate;
        mergedCustomer.customerId = isNotBlank(this.customerId) ? this.customerId : customer.customerId;
        mergedCustomer.sessionId = isNotBlank(this.sessionId) ? this.sessionId : customer.sessionId;
        mergedCustomer.authenticationToken = isNotBlank(this.authenticationToken) ? this.authenticationToken : customer.authenticationToken;
        mergedCustomer.enlistedBy = this.enlistedBy != null ? this.enlistedBy : customer.enlistedBy;
        mergedCustomer.enlistedCustomers = this.enlistedCustomers != null ? this.enlistedCustomers : customer.enlistedCustomers;

        return mergedCustomer;
    }

    @Override
    public Customer clone() throws CloneNotSupportedException {
        return (Customer) super.clone();
    }
}
