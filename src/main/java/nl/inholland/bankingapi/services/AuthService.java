package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import nl.inholland.bankingapi.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountService accountService;

    public AuthService(UserRepository userRepository, CustomerProfileRepository customerProfileRepository, AccountService accountService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountService = accountService;
    }

    @Transactional
    public User register(String email, String password, String firstName, String lastName, String bsn, String phoneNumber) {
        User user = new User(0, email, password, firstName, lastName, UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(user);
        CustomerProfile profile = new CustomerProfile(0, user, bsn, phoneNumber, CustomerStatus.PENDING);
        customerProfileRepository.save(profile);
        return user;
    }

    public User login(String email, String password) {
        return userRepository.findByEmailAndPasswordHash(email, password);
    }

    public User getUserById(int id) {
        return userRepository.findById(id).orElse(null);
    }

    public CustomerProfile getProfileByUserId(int userId) {
        return customerProfileRepository.findByUser_Id(userId);
    }

    public List<User> getAllCustomers(CustomerStatus status, String search) {
        return userRepository.findAll().stream()
                .filter(user -> {
                    CustomerProfile profile = customerProfileRepository.findByUser_Id(user.getId());
                    if (profile == null) return false;
                    return status == null || profile.getStatus() == status;
                })
                .filter(user -> {
                    if (search == null) return true;
                    String s = search.toLowerCase(Locale.ROOT);
                    return user.getFirstName().toLowerCase(Locale.ROOT).contains(s)
                            || user.getLastName().toLowerCase(Locale.ROOT).contains(s)
                            || user.getEmail().toLowerCase(Locale.ROOT).contains(s);
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
