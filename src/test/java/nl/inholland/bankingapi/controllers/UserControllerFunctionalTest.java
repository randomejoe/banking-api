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
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back DB changes after each test
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

    @Autowired
    private JwtUtil jwtUtil;

    // --- helpers ---

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

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    // --- GET /users ---

    @Test
    void getAll_withoutAuthentication_returns401() throws Exception {
        // no token = 401
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_customerGetsForbidden() throws Exception {
        User customer = createCustomer("uc-getall-customer@example.com");
        createProfile(customer, "100000001");

        // customers can't access this endpoint
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

        // no customers in this test's transaction, so search should return nothing
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

    @Test
    void getById_missingCustomerReturns404() throws Exception {
        User employee = createEmployee("uc-missing-detail-employee@example.com");

        mockMvc.perform(get("/users/{id}", 999999)
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found: 999999"));
    }

    @Test
    void getById_employeeIdReturns404() throws Exception {
        User employee = createEmployee("uc-employee-detail-employee@example.com");

        mockMvc.perform(get("/users/{id}", employee.getId())
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found: " + employee.getId()));
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

        // customers can't update users
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
        List<Account> accounts = accountRepository.findByUser_Id(customer.getId(), Pageable.unpaged()).getContent();
        long checkingCount = accounts.stream().filter(a -> a.getType() == AccountType.CHECKING).count();
        long savingsCount  = accounts.stream().filter(a -> a.getType() == AccountType.SAVINGS).count();
        assertEquals(1, checkingCount, "Expected 1 CHECKING account");
        assertEquals(1, savingsCount,  "Expected 1 SAVINGS account");
        accounts.forEach(a -> assertEquals(AccountStatus.ACTIVE, a.getStatus()));
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
