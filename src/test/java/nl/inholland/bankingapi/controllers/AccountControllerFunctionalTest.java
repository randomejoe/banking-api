package nl.inholland.bankingapi.controllers;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back DB changes after each test
class AccountControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    private Account createAccount(User owner, String iban) {
        return createAccount(owner, iban, AccountType.CHECKING, AccountStatus.ACTIVE);
    }

    private Account createAccount(User owner, String iban, AccountType type, AccountStatus status) {
        Account account = new Account();
        account.setUser(owner);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("0.00"));
        account.setDailyTransferLimit(new BigDecimal("5000.00"));
        account.setStatus(status);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

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

        // customers are filtered to their own accounts
        mockMvc.perform(get("/accounts")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
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

        // size=100 so both accounts show up on the first page
        mockMvc.perform(get("/accounts?size=100")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].userId",
                        hasItems(customer1.getId(), customer2.getId())));
    }

    @Test
    void getAccounts_customerNameFilterCannotExposeOtherCustomerAccounts() throws Exception {
        User customer1 = createCustomer("ac-name-c1@example.com");
        User customer2 = createCustomer("ac-name-c2-target@example.com");

        createAccount(customer1, "FTACCTNAMEC101");
        createAccount(customer2, "FTACCTNAMEC201");

        mockMvc.perform(get("/accounts?name=target&size=100")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].userId",
                        everyItem(is(customer1.getId()))))
                .andExpect(jsonPath("$.content[*].iban",
                        not(hasItem("FTACCTNAMEC201"))));
    }

    @Test
    void getAccounts_employeeCanSearchAccountsByCustomerName() throws Exception {
        User matchingCustomer = createCustomer("ac-search-match@example.com");
        User otherCustomer = createCustomer("ac-search-other@example.com");
        User employee = createEmployee("ac-search-employee@example.com");

        matchingCustomer.setFirstName("Alicia");
        userRepository.save(matchingCustomer);

        createAccount(matchingCustomer, "FTACCTSEARCH01");
        createAccount(otherCustomer, "FTACCTSEARCH02");

        mockMvc.perform(get("/accounts?name=lici&size=100")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].iban",
                        hasItem("FTACCTSEARCH01")))
                .andExpect(jsonPath("$.content[*].iban",
                        not(hasItem("FTACCTSEARCH02"))));
    }

    @Test
    void getAccounts_employeeNameSearchComposesWithAccountType() throws Exception {
        User customer = createCustomer("ac-compose-match@example.com");
        User employee = createEmployee("ac-compose-employee@example.com");

        createAccount(customer, "FTACCTCOMPOSECHK", AccountType.CHECKING, AccountStatus.ACTIVE);
        createAccount(customer, "FTACCTCOMPOSESAV", AccountType.SAVINGS, AccountStatus.ACTIVE);

        mockMvc.perform(get("/accounts?name=compose&type=SAVINGS&size=100")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type",
                        everyItem(is("SAVINGS"))))
                .andExpect(jsonPath("$.content[*].iban",
                        hasItem("FTACCTCOMPOSESAV")))
                .andExpect(jsonPath("$.content[*].iban",
                        not(hasItem("FTACCTCOMPOSECHK"))));
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

        // must be zero before closing
        account.setBalance(BigDecimal.ZERO);
        accountRepository.save(account);

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

        // only employees can update accounts
        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAccount_emptyBody_returns400() throws Exception {
        User customer = createCustomer("ac-empty@example.com");
        User employee = createEmployee("ac-empty-emp@example.com");
        Account account = createAccount(customer, "FTACCTEMPTY01");

        // empty body should fail validation
        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAccount_negativeLimit_returns400() throws Exception {
        User customer = createCustomer("ac-neglimit@example.com");
        User employee = createEmployee("ac-neglimit-emp@example.com");
        Account account = createAccount(customer, "FTACCTNEGLIM01");

        Map<String, Object> request = new HashMap<>();
        request.put("absoluteTransferLimit", -1.00);

        // negative limits are not allowed
        mockMvc.perform(patch("/accounts/{iban}", account.getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAccount_nonExistentIban_returns404() throws Exception {
        User employee = createEmployee("ac-notfound-emp@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("absoluteTransferLimit", "500.00");

        // unknown IBAN should be 404
        mockMvc.perform(patch("/accounts/NL99XXXX0000000000")
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
