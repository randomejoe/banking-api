package nl.inholland.bankingapi.controller;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
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
class AccountControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    private Account createAccount(User owner, String iban) {
        Account account = new Account();
        account.setUser(owner);
        account.setIban(iban);
        account.setType(AccountType.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("0.00"));
        account.setDailyTransferLimit(new BigDecimal("5000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    // Generates a real Bearer token so the JwtAuthenticationFilter can authenticate the user.
    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    // --- GET /accounts ---

    @Test
    void getAccounts_customerOnlySeesOwnAccounts() throws Exception {
        User customer1 = createCustomer("ac-c1@example.com");
        User customer2 = createCustomer("ac-c2@example.com");

        createAccount(customer1, "FTACCTC101");
        createAccount(customer2, "FTACCTC201");

        // The controller sets userId = currentUser.getId() for non-employees,
        // so customer1's request is filtered to their own accounts only.
        mockMvc.perform(get("/accounts")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                // Every account in the page must belong to customer1.
                .andExpect(jsonPath("$.content[*].userId",
                        everyItem(is(customer1.getId()))));
    }

    @Test
    void getAccounts_employeeCanSeeAllAccounts() throws Exception {
        User customer1 = createCustomer("ac-emp-c1@example.com");
        User customer2 = createCustomer("ac-emp-c2@example.com");
        User employee  = createEmployee("ac-emp@example.com");

        createAccount(customer1, "FTACCTEMPC101");
        createAccount(customer2, "FTACCTEMPC201");

        // Employees call getAll(null, ...) which applies no userId filter.
        // size=100 ensures both test accounts appear on the first page.
        mockMvc.perform(get("/accounts?size=100")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                // Both test customers' accounts must appear in the unfiltered employee view.
                .andExpect(jsonPath("$.content[*].userId",
                        hasItems(customer1.getId(), customer2.getId())));
    }

    @Test
    void getAccounts_withoutAuthentication_returns401() throws Exception {
        // No Authorization header — Spring Security rejects the request before reaching the controller.
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isUnauthorized());
    }

    // --- PATCH /accounts/{iban} ---

    @Test
    void updateAccount_employeeCanUpdateLimitsAndStatus() throws Exception {
        User customer = createCustomer("ac-patch-customer@example.com");
        User employee = createEmployee("ac-patch-employee@example.com");
        Account account = createAccount(customer, "FTACCTPATCH01");

        Map<String, Object> request = new HashMap<>();
        request.put("absoluteTransferLimit", "200.00");
        request.put("dailyTransferLimit", "3000.00");
        request.put("status", "CLOSED");

        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(account.getIban()))
                .andExpect(jsonPath("$.absoluteTransferLimit").value(200.00))
                .andExpect(jsonPath("$.dailyTransferLimit").value(3000.00))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void updateAccount_customerGetsForbidden() throws Exception {
        User customer = createCustomer("ac-forbidden@example.com");
        Account account = createAccount(customer, "FTACCTFORBID01");

        Map<String, Object> request = new HashMap<>();
        request.put("status", "CLOSED");

        // @PreAuthorize("hasRole('EMPLOYEE')") on PATCH rejects customers with 403.
        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
