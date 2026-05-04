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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public AuthService(UserRepository userRepository, CustomerProfileRepository customerProfileRepository) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
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
}
