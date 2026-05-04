package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import nl.inholland.bankingapi.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountService accountService;

    public CustomerService(UserRepository userRepository, CustomerProfileRepository customerProfileRepository, AccountService accountService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountService = accountService;
    }

    public User getUserById(int id) {
        return userRepository.findById(id).orElse(null);
    }

    public CustomerProfile getProfileByUserId(int userId) {
        return customerProfileRepository.findByUser_Id(userId);
    }

    public List<User> getAllCustomers(CustomerStatus status, String search) {
        List<User> users = (search != null)
                ? userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, search)
                : userRepository.findAll();

        return users.stream()
                .filter(user -> {
                    CustomerProfile profile = customerProfileRepository.findByUser_Id(user.getId());
                    if (profile == null) return false;
                    return status == null || profile.getStatus() == status;
                })
                .toList();
    }

    @Transactional
    public CustomerProfile updateCustomer(int userId, CustomerStatus status, String firstName, String lastName, String phoneNumber) {
        User user = getUserById(userId);
        CustomerProfile profile = getProfileByUserId(userId);
        if (user == null || profile == null) return null;
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (status != null) profile.setStatus(status);
        if (phoneNumber != null) profile.setPhoneNumber(phoneNumber);
        userRepository.save(user);
        customerProfileRepository.save(profile);
        if (status == CustomerStatus.ACTIVE && accountService.getByUserId(userId).isEmpty()) {
            accountService.createAccountsForUser(user);
        }
        return profile;
    }
}
