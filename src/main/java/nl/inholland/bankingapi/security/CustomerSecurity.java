package nl.inholland.bankingapi.security;

import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("customerSecurity")
public class CustomerSecurity {

    private final CustomerProfileRepository customerProfileRepository;

    public CustomerSecurity(CustomerProfileRepository customerProfileRepository) {
        this.customerProfileRepository = customerProfileRepository;
    }

    public boolean isActiveCustomer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }
        if (user.getRole() != UserRole.CUSTOMER) {
            return false;
        }

        CustomerProfile profile = customerProfileRepository.findByUser_Id(user.getId());
        return profile != null && profile.getStatus() == CustomerStatus.ACTIVE;
    }
}
