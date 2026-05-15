package nl.inholland.bankingapi.controller;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.TransactionRepository;
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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Boots the full Spring application context so this test exercises real wiring.
@SpringBootTest
// Exposes MockMvc from the context, allowing HTTP-style endpoint testing without a real server.
@AutoConfigureMockMvc
// Wraps each test in a transaction and rolls it back afterward to keep database state isolated.
@Transactional
class TransactionControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
        account.setAbsoluteTransferLimit(new BigDecimal("-500.00"));
        account.setDailyTransferLimit(new BigDecimal("5000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private Transaction createTransaction(User initiatedBy, String fromIban, String toIban,
                                          TransactionType type, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setInitiatedBy(initiatedBy);
        transaction.setFromIban(fromIban);
        transaction.setToIban(toIban);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    // Generates a real Bearer token so the JwtAuthenticationFilter can authenticate the user.
    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    // --- POST /transactions ---

    @Test
    void createTransaction_deposit_returns201WithTransactionBody() throws Exception {
        User customer = createCustomer("ft-deposit@example.com");
        Account toAccount = createAccount(customer, "FT-IBAN-DEPOSIT-01");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", toAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "DEPOSIT");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.toIban").value(toAccount.getIban()))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createTransaction_withMissingType_returns400() throws Exception {
        User customer = createCustomer("ft-badrequest@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", "FT-IBAN-ANY");
        request.put("amount", "50.00");
        // type is intentionally omitted — @NotNull on TransactionCreateRequest.type triggers 400

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_withoutAuthentication_returns401() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("toIban", "FT-IBAN-ANY");
        request.put("amount", "50.00");
        request.put("type", "DEPOSIT");

        // No Authorization header — should be rejected before reaching the controller.
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /transactions/{id} ---

    @Test
    void getTransactionById_returnsOwnTransaction() throws Exception {
        User customer = createCustomer("ft-owner@example.com");
        Transaction transaction = createTransaction(
                customer, null, "FT-IBAN-TO", TransactionType.DEPOSIT, new BigDecimal("75.00"));

        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(75.00));
    }

    @Test
    void getTransactionById_returnsForbiddenForAnotherCustomersTransaction() throws Exception {
        User customer1 = createCustomer("ft-customer1@example.com");
        User customer2 = createCustomer("ft-customer2@example.com");
        Transaction transaction = createTransaction(
                customer1, null, "FT-IBAN-C1", TransactionType.DEPOSIT, new BigDecimal("50.00"));

        // customer2 tries to view customer1's transaction — should be denied.
        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionById_allowsEmployeeToViewAnyTransaction() throws Exception {
        User customer = createCustomer("ft-customer-emp@example.com");
        User employee = createEmployee("ft-employee@example.com");
        Transaction transaction = createTransaction(
                customer, null, "FT-IBAN-EMP", TransactionType.DEPOSIT, new BigDecimal("60.00"));

        // Employees can view any transaction regardless of who initiated it.
        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()));
    }

    // --- GET /transactions ---

    @Test
    void getTransactions_customerOnlySeesOwnTransactions() throws Exception {
        User customer1 = createCustomer("ft-filter-c1@example.com");
        User customer2 = createCustomer("ft-filter-c2@example.com");

        // Each customer has one transaction.
        createTransaction(customer1, null, "FT-IBAN-FC1", TransactionType.DEPOSIT, new BigDecimal("100.00"));
        createTransaction(customer2, null, "FT-IBAN-FC2", TransactionType.DEPOSIT, new BigDecimal("200.00"));

        // The controller sets filters.customerId = currentUser.getId() for non-employees.
        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                // Every transaction in the response must belong to customer1.
                .andExpect(jsonPath("$.content[*].initiatedByUserId",
                        everyItem(is(customer1.getId()))));
    }
}
