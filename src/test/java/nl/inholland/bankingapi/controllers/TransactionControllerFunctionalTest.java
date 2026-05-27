package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // rolls back DB changes after each test
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

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

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
        User saved = userRepository.save(user);
        createProfile(saved, CustomerStatus.ACTIVE);
        return saved;
    }

    private CustomerProfile createProfile(User user, CustomerStatus status) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setBsn(String.format("%09d", user.getId()));
        profile.setPhoneNumber("0612345678");
        profile.setStatus(status);
        return customerProfileRepository.save(profile);
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
        return createAccount(owner, iban, AccountType.CHECKING);
    }

    private Account createAccount(User owner, String iban, AccountType type) {
        Account account = new Account();
        account.setUser(owner);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("-500.00"));
        account.setDailyTransferLimit(new BigDecimal("5000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private Transaction createTransaction(User initiatedBy, String fromIban, String toIban,
                                          TransactionType type, BigDecimal amount) {
        return createTransaction(initiatedBy, fromIban, toIban, type, amount, LocalDateTime.now());
    }

    private Transaction createTransaction(User initiatedBy, String fromIban, String toIban,
                                          TransactionType type, BigDecimal amount,
                                          LocalDateTime timestamp) {
        Transaction transaction = new Transaction();
        transaction.setInitiatedBy(initiatedBy);
        transaction.setFromIban(fromIban);
        transaction.setToIban(toIban);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setTimestamp(timestamp);
        return transactionRepository.save(transaction);
    }

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
        // no type field — should fail validation

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_pendingCustomerGetsForbidden() throws Exception {
        User customer = createCustomer("ft-pending@example.com");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.PENDING);
        customerProfileRepository.save(profile);
        Account toAccount = createAccount(customer, "FT-IBAN-PENDING-01");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", toAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "DEPOSIT");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_closedCustomerGetsForbidden() throws Exception {
        User customer = createCustomer("ft-closed@example.com");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.CLOSED);
        customerProfileRepository.save(profile);
        Account toAccount = createAccount(customer, "FT-IBAN-CLOSED-01");

        Map<String, Object> request = new HashMap<>();
        request.put("toIban", toAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "DEPOSIT");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTransaction_customerExternalTransferToSavingsAccountReturns400() throws Exception {
        User sender = createCustomer("ft-external-sender@example.com");
        User recipient = createCustomer("ft-external-recipient@example.com");
        Account fromAccount = createAccount(sender, "FT-IBAN-EXT-FROM");
        Account toSavingsAccount = createAccount(recipient, "FT-IBAN-EXT-SAV", AccountType.SAVINGS);

        Map<String, Object> request = new HashMap<>();
        request.put("fromIban", fromAccount.getIban());
        request.put("toIban", toSavingsAccount.getIban());
        request.put("amount", "100.00");
        request.put("type", "TRANSFER");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearerToken(sender))
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

        // no token = 401
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

    @Test
    void getTransactionById_pendingCustomerGetsForbidden() throws Exception {
        User customer = createCustomer("ft-detail-pending@example.com");
        Transaction transaction = createTransaction(
                customer, null, "FT-IBAN-PENDING-DETAIL", TransactionType.DEPOSIT, new BigDecimal("60.00"));
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.PENDING);
        customerProfileRepository.save(profile);

        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionById_closedCustomerGetsForbidden() throws Exception {
        User customer = createCustomer("ft-detail-closed@example.com");
        Transaction transaction = createTransaction(
                customer, null, "FT-IBAN-CLOSED-DETAIL", TransactionType.DEPOSIT, new BigDecimal("60.00"));
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.CLOSED);
        customerProfileRepository.save(profile);

        mockMvc.perform(get("/transactions/{id}", transaction.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    // --- GET /transactions ---

    @Test
    void getTransactions_customerOnlySeesOwnTransactions() throws Exception {
        User customer1 = createCustomer("ft-filter-c1@example.com");
        User customer2 = createCustomer("ft-filter-c2@example.com");

        createTransaction(customer1, null, "FT-IBAN-FC1", TransactionType.DEPOSIT, new BigDecimal("100.00"));
        createTransaction(customer2, null, "FT-IBAN-FC2", TransactionType.DEPOSIT, new BigDecimal("200.00"));

        // customers can only see their own transactions
        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].initiatedByUserId",
                        everyItem(is(customer1.getId()))));
    }

    @Test
    void getTransactions_filtersByInclusiveDateRange() throws Exception {
        User customer = createCustomer("ft-date-filter@example.com");
        Transaction before = createTransaction(customer, null, "FT-IBAN-DATE-OLD",
                TransactionType.DEPOSIT, new BigDecimal("100.00"),
                LocalDateTime.of(2026, 5, 1, 23, 59));
        Transaction firstDay = createTransaction(customer, null, "FT-IBAN-DATE-START",
                TransactionType.DEPOSIT, new BigDecimal("200.00"),
                LocalDateTime.of(2026, 5, 2, 0, 0));
        Transaction lastDay = createTransaction(customer, null, "FT-IBAN-DATE-END",
                TransactionType.DEPOSIT, new BigDecimal("300.00"),
                LocalDateTime.of(2026, 5, 3, 23, 59));
        Transaction after = createTransaction(customer, null, "FT-IBAN-DATE-NEW",
                TransactionType.DEPOSIT, new BigDecimal("400.00"),
                LocalDateTime.of(2026, 5, 4, 0, 0));

        mockMvc.perform(get("/transactions")
                        .param("startDate", "2026-05-02")
                        .param("endDate", "2026-05-03")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + firstDay.getId() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + lastDay.getId() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + before.getId() + ")]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == " + after.getId() + ")]").doesNotExist());
    }

    @Test
    void getTransactions_withStartDateAfterEndDateReturns400() throws Exception {
        User customer = createCustomer("ft-date-filter-invalid@example.com");

        mockMvc.perform(get("/transactions")
                        .param("startDate", "2026-05-04")
                        .param("endDate", "2026-05-03")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactions_pendingCustomerGetsForbidden() throws Exception {
        User customer = createCustomer("ft-history-pending@example.com");
        CustomerProfile profile = customerProfileRepository.findByUser_Id(customer.getId());
        profile.setStatus(CustomerStatus.PENDING);
        customerProfileRepository.save(profile);

        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }
}
