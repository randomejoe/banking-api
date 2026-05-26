package nl.inholland.bankingapi.domain.policy;

import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CustomerStatusPolicy {

    private final CustomerProfileRepository customerProfileRepository;

    public CustomerStatusPolicy(CustomerProfileRepository customerProfileRepository) {
        this.customerProfileRepository = customerProfileRepository;
    }

    public void enforceActiveCustomer(User user) {
        if (user.getRole() != UserRole.CUSTOMER) {
            return;
        }

        CustomerProfile profile = customerProfileRepository.findByUser_Id(user.getId());
        if (profile == null || profile.getStatus() != CustomerStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer is not active");
        }
    }
}
