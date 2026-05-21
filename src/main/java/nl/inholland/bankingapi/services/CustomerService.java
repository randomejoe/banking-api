package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import nl.inholland.bankingapi.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.inholland.bankingapi.dtos.CustomerUpdateRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountService accountService;

    @Value("${banking.defaults.absolute-transfer-limit}")
    private BigDecimal defaultAbsoluteTransferLimit;

    @Value("${banking.defaults.daily-transfer-limit}")
    private BigDecimal defaultDailyTransferLimit;

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

    public Page<User> getAllCustomers(CustomerStatus status, String search, Pageable pageable) {
        return userRepository.findCustomers(status, search, pageable);
    }

    public List<Integer> getCustomerIdsBySearch(String search) {
        return userRepository.findCustomerIdsBySearch(search);
    }

    @Transactional
    public CustomerProfile updateCustomer(int userId, CustomerUpdateRequest request) {
        User user = getUserById(userId);
        CustomerProfile profile = getProfileByUserId(userId);
        if (user == null || profile == null) return null;

        CustomerStatus previousStatus = profile.getStatus();
        CustomerStatus newStatus = request.status() != null ? CustomerStatus.valueOf(request.status()) : null;

        // Update via Optional.ifPresent — null fields mean "no change" (PATCH semantics)
        Optional.ofNullable(request.firstName()).ifPresent(user::setFirstName);
        Optional.ofNullable(request.lastName()).ifPresent(user::setLastName);
        Optional.ofNullable(newStatus).ifPresent(profile::setStatus);
        Optional.ofNullable(request.phoneNumber()).ifPresent(profile::setPhoneNumber);

        // Both entities are managed in this @Transactional scope —
        // a single flush at commit propagates all field changes to the database.
        customerProfileRepository.save(profile);

        if (newStatus == CustomerStatus.ACTIVE && previousStatus != CustomerStatus.ACTIVE) {
            BigDecimal absLimit   = request.absoluteTransferLimit() != null ? request.absoluteTransferLimit() : defaultAbsoluteTransferLimit;
            BigDecimal dailyLimit = request.dailyTransferLimit()    != null ? request.dailyTransferLimit()    : defaultDailyTransferLimit;
            accountService.createAccountsForUser(user, absLimit, dailyLimit);
        }
        return profile;
    }
}
