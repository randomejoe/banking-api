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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // loads all profiles for the given user IDs in one query instead of one-by-one
    public Map<Integer, CustomerProfile> getProfileMapByUserIds(List<Integer> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return customerProfileRepository.findByUser_IdIn(userIds).stream()
                .collect(Collectors.toMap(CustomerProfile::getUserId, p -> p));
    }

    @Transactional
    public CustomerProfile updateCustomer(int userId, CustomerUpdateRequest request) {
        User user = getUserById(userId);
        CustomerProfile profile = getProfileByUserId(userId);
        if (user == null || profile == null) return null;

        CustomerStatus previousStatus = profile.getStatus();
        CustomerStatus newStatus = request.status();

        // only update fields that were actually sent — null means leave unchanged
        Optional.ofNullable(request.firstName()).ifPresent(user::setFirstName);
        Optional.ofNullable(request.lastName()).ifPresent(user::setLastName);
        Optional.ofNullable(newStatus).ifPresent(profile::setStatus);
        Optional.ofNullable(request.phoneNumber()).ifPresent(profile::setPhoneNumber);

        customerProfileRepository.save(profile);

        provisionAccountsIfActivated(user, previousStatus, newStatus, request);
        return profile;
    }

    // creates accounts when a customer is activated for the first time; does nothing otherwise
    private void provisionAccountsIfActivated(User user, CustomerStatus previousStatus,
                                               CustomerStatus newStatus, CustomerUpdateRequest request) {
        if (newStatus != CustomerStatus.ACTIVE || previousStatus == CustomerStatus.ACTIVE) return;
        BigDecimal absLimit   = request.absoluteTransferLimit()  != null
                ? request.absoluteTransferLimit()  : defaultAbsoluteTransferLimit;
        BigDecimal dailyLimit = request.dailyTransferLimit()     != null
                ? request.dailyTransferLimit()     : defaultDailyTransferLimit;
        accountService.createAccountsForUser(user, absLimit, dailyLimit);
    }
}
