package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.dtos.CustomerUpdateRequest;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import nl.inholland.bankingapi.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Registers Mockito with JUnit 5 so @Mock/@InjectMocks fields are created before each test.
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    // Creates a Mockito test double instead of using a real repository implementation.
    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    // Mocked so we can verify account creation is triggered at the right moment.
    @Mock
    private AccountService accountService;

    // Builds CustomerService and injects all @Mock fields into its constructor automatically.
    @InjectMocks
    private CustomerService customerService;

    private User customer;
    private CustomerProfile profile;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1);
        customer.setEmail("customer@example.com");
        customer.setFirstName("Alice");
        customer.setLastName("Smith");
        customer.setRole(UserRole.CUSTOMER);

        profile = new CustomerProfile();
        profile.setId(1);
        profile.setUser(customer);
        profile.setBsn("123456789");
        profile.setPhoneNumber("0612345678");
        profile.setStatus(CustomerStatus.PENDING);
    }

    // --- getUserById ---

    @Test
    void getUserById_returnsUserWhenFound() {
        // Stubbing: when this mock method is called, return predefined data.
        when(userRepository.findById(1)).thenReturn(Optional.of(customer));

        User result = customerService.getUserById(1);

        assertEquals(customer, result);
        // Verification: assert that collaboration with the mock happened as expected.
        verify(userRepository).findById(1);
    }

    @Test
    void getUserById_returnsNullWhenNotFound() {
        // Stub missing user to drive the orElse(null) branch.
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        User result = customerService.getUserById(99);

        assertNull(result);
        verify(userRepository).findById(99);
    }

    // --- getProfileByUserId ---

    @Test
    void getProfileByUserId_delegatesToRepository() {
        when(customerProfileRepository.findByUser_Id(1)).thenReturn(profile);

        CustomerProfile result = customerService.getProfileByUserId(1);

        assertEquals(profile, result);
        verify(customerProfileRepository).findByUser_Id(1);
    }

    // --- getAllCustomers ---

    @Test
    void getAllCustomers_delegatesToRepository() {
        Pageable pageable = Pageable.unpaged();
        Page<User> expected = new PageImpl<>(List.of(customer));
        when(userRepository.findCustomers(eq(CustomerStatus.PENDING), eq("Alice"), eq(pageable)))
                .thenReturn(expected);

        Page<User> result = customerService.getAllCustomers(CustomerStatus.PENDING, "Alice", pageable);

        assertEquals(expected, result);
        verify(userRepository).findCustomers(CustomerStatus.PENDING, "Alice", pageable);
    }

    // --- updateCustomer ---

    @Test
    void updateCustomer_happyPath_updatesNameAndPhoneAndSavesBoth() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerProfileRepository.findByUser_Id(1)).thenReturn(profile);
        when(userRepository.save(customer)).thenReturn(customer);
        when(customerProfileRepository.save(profile)).thenReturn(profile);

        CustomerUpdateRequest request = new CustomerUpdateRequest(null, "Bob", "Jones", "0698765432", null, null);
        CustomerProfile result = customerService.updateCustomer(1, request);

        assertEquals(profile, result);
        assertEquals("Bob", customer.getFirstName());
        assertEquals("Jones", customer.getLastName());
        assertEquals("0698765432", profile.getPhoneNumber());
        verify(userRepository).save(customer);
        verify(customerProfileRepository).save(profile);
        // Status did not change to ACTIVE, so no accounts should be created.
        verify(accountService, never()).createAccountsForUser(any(), any(), any());
    }

    @Test
    void updateCustomer_whenStatusChangesToActive_createsAccountsForUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerProfileRepository.findByUser_Id(1)).thenReturn(profile);
        when(userRepository.save(customer)).thenReturn(customer);
        when(customerProfileRepository.save(profile)).thenReturn(profile);

        // Activating a PENDING customer triggers account creation with the provided limits.
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "ACTIVE", null, null, null,
                new BigDecimal("1000.00"), new BigDecimal("5000.00"));

        customerService.updateCustomer(1, request);

        // Verify account creation was triggered with the exact limits from the request.
        verify(accountService).createAccountsForUser(
                eq(customer), eq(new BigDecimal("1000.00")), eq(new BigDecimal("5000.00")));
    }

    @Test
    void updateCustomer_alreadyActive_doesNotCreateAccountsAgain() {
        profile.setStatus(CustomerStatus.ACTIVE);
        when(userRepository.findById(1)).thenReturn(Optional.of(customer));
        when(customerProfileRepository.findByUser_Id(1)).thenReturn(profile);
        when(userRepository.save(customer)).thenReturn(customer);
        when(customerProfileRepository.save(profile)).thenReturn(profile);

        // Setting status to ACTIVE when already ACTIVE should not trigger account creation.
        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "ACTIVE", null, null, null, null, null);

        customerService.updateCustomer(1, request);

        verify(accountService, never()).createAccountsForUser(any(), any(), any());
    }

    @Test
    void updateCustomer_whenUserNotFound_returnsNull() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        // Both lookups run before the null check, so the profile lookup also occurs.
        when(customerProfileRepository.findByUser_Id(99)).thenReturn(null);

        CustomerUpdateRequest request = new CustomerUpdateRequest(null, "Bob", null, null, null, null);
        CustomerProfile result = customerService.updateCustomer(99, request);

        assertNull(result);
        // Verify no persistence occurred after the missing user check.
        verify(userRepository, never()).save(any());
        verify(customerProfileRepository, never()).save(any());
    }
}
