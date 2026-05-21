package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import nl.inholland.bankingapi.repositories.UserRepository;
import nl.inholland.bankingapi.util.JwtUtil;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Boots the full Spring application context so this test exercises real wiring.
@SpringBootTest
// Exposes MockMvc from the context, allowing HTTP-style endpoint testing without a real server.
@AutoConfigureMockMvc
// Wraps each test in a transaction and rolls it back afterward to keep database state isolated.
@Transactional
class UserControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    // Used to generate real JWT tokens so the JwtAuthenticationFilter authenticates correctly.
    @Autowired
    private JwtUtil jwtUtil;

    // --- Helper methods ---

    private User createCustomer(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setRole(UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    private User createEmployee(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Employee");
        user.setRole(UserRole.EMPLOYEE);
        return userRepository.save(user);
    }

    private CustomerProfile createProfile(User user, String bsn) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setBsn(bsn);
        profile.setPhoneNumber("0612345678");
        profile.setStatus(CustomerStatus.PENDING);
        return customerProfileRepository.save(profile);
    }

    private CustomerProfile createActiveProfile(User user, String bsn) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setBsn(bsn);
        profile.setPhoneNumber("0612345678");
        profile.setStatus(CustomerStatus.ACTIVE);
        return customerProfileRepository.save(profile);
    }

    // Generates a real Bearer token so the JwtAuthenticationFilter can authenticate the user.
    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    // --- GET /users ---

    @Test
    void getAll_withoutAuthentication_returns401() throws Exception {
        // No Authorization header — Spring Security rejects the request before reaching the controller.
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_customerGetsForbidden() throws Exception {
        User customer = createCustomer("uc-getall-customer@example.com");
        createProfile(customer, "100000001");

        // @PreAuthorize("hasRole('EMPLOYEE')") on GET /users rejects customers with 403.
        mockMvc.perform(get("/users")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_employeeSeesPagedCustomerList() throws Exception {
        User customer = createCustomer("uc-listed@example.com");
        createProfile(customer, "100000002");
        User employee = createEmployee("uc-getall-employee@example.com");

        mockMvc.perform(get("/users")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAll_whenNoCustomersExist_returnsEmptyList() throws Exception {
        User employee = createEmployee("uc-empty-employee@example.com");

        // No customers created in this transaction — the DataLoader customers are isolated
        // to the outer context, so filtering by a nonexistent search returns empty content.
        mockMvc.perform(get("/users?search=nonexistent-name-xyz987")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // --- GET /users/{id} ---

    @Test
    void getById_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_customerGetsForbidden() throws Exception {
        User customer = createCustomer("uc-getbyid-customer@example.com");

        // Customers cannot look up user details — only employees can.
        mockMvc.perform(get("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_employeeGetsCustomerDetail() throws Exception {
        User customer = createCustomer("uc-detail-customer@example.com");
        createProfile(customer, "100000003");
        User employee = createEmployee("uc-detail-employee@example.com");

        mockMvc.perform(get("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId()))
                .andExpect(jsonPath("$.email").value(customer.getEmail()));
    }

    // --- PATCH /users/{id} ---

    @Test
    void updateUser_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUser_customerGetsForbidden() throws Exception {
        User customer = createCustomer("uc-patch-forbidden@example.com");

        // @PreAuthorize("hasRole('EMPLOYEE')") on PATCH rejects customers with 403.
        mockMvc.perform(patch("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_employeeCanUpdateAllFields() throws Exception {
        User customer = createCustomer("uc-patch-customer@example.com");
        createProfile(customer, "100000004");
        User employee = createEmployee("uc-patch-employee@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("firstName", "Updated");
        request.put("lastName", "Name");
        request.put("phoneNumber", "0698765432");

        mockMvc.perform(patch("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("0698765432"));
    }

    @Test
    void updateUser_approvingCustomer_changesStatusToActive() throws Exception {
        User customer = createCustomer("uc-approve-customer@example.com");
        createProfile(customer, "100000005");
        User employee = createEmployee("uc-approve-employee@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("status", "ACTIVE");
        request.put("absoluteTransferLimit", "1000.00");
        request.put("dailyTransferLimit", "500.00");

        mockMvc.perform(patch("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateUser_approvingCustomer_createsCheckingAndSavingsAccounts() throws Exception {
        User customer = createCustomer("uc-accounts-customer@example.com");
        createProfile(customer, "100000006");
        User employee = createEmployee("uc-accounts-employee@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("status", "ACTIVE");
        request.put("absoluteTransferLimit", "1000.00");
        request.put("dailyTransferLimit", "500.00");

        mockMvc.perform(patch("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Activation must create exactly one CHECKING and one SAVINGS account for the customer.
        java.util.List<Account> accounts = accountRepository.findByUser_Id(customer.getId());
        long checkingCount = accounts.stream().filter(a -> a.getType() == AccountType.CHECKING).count();
        long savingsCount  = accounts.stream().filter(a -> a.getType() == AccountType.SAVINGS).count();
        org.junit.jupiter.api.Assertions.assertEquals(1, checkingCount, "Expected 1 CHECKING account");
        org.junit.jupiter.api.Assertions.assertEquals(1, savingsCount,  "Expected 1 SAVINGS account");
        accounts.forEach(a -> org.junit.jupiter.api.Assertions.assertEquals(AccountStatus.ACTIVE, a.getStatus()));
    }

    @Test
    void updateUser_invalidPhoneNumber_returns400() throws Exception {
        User customer = createCustomer("uc-invalid-phone@example.com");
        createProfile(customer, "100000007");
        User employee = createEmployee("uc-invalid-phone-emp@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("phoneNumber", "not-a-phone");

        mockMvc.perform(patch("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_negativeLimitValue_returns400() throws Exception {
        User customer = createCustomer("uc-neg-limit@example.com");
        createProfile(customer, "100000008");
        User employee = createEmployee("uc-neg-limit-emp@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("absoluteTransferLimit", "-50.00");

        mockMvc.perform(patch("/users/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
