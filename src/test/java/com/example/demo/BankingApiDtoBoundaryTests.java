package nl.inholland.bankingapi;

import nl.inholland.bankingapi.controllers.AccountController;
import nl.inholland.bankingapi.controllers.AuthController;
import nl.inholland.bankingapi.controllers.UserController;
import nl.inholland.bankingapi.controllers.TransactionController;
import nl.inholland.bankingapi.dtos.CurrentUserResponse;
import nl.inholland.bankingapi.dtos.CustomerDetailResponse;
import nl.inholland.bankingapi.dtos.CustomerProfileResponse;
import nl.inholland.bankingapi.dtos.CustomerSummaryResponse;
import nl.inholland.bankingapi.dtos.LoginResponse;
import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.dtos.UserResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankingApiDtoBoundaryTests {

    private static final Set<Class<?>> CONTROLLERS = Set.of(
            AuthController.class,
            UserController.class,
            AccountController.class,
            TransactionController.class
    );

    private static final Set<Class<?>> ENTITY_TYPES = Set.of(
            User.class,
            CustomerProfile.class,
            Account.class,
            Transaction.class
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void mappedControllerMethodsReturnDtoTypesOnly() {
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isMappedEndpoint(method)) continue;

                Type returnType = method.getGenericReturnType();
                assertFalse(usesForbiddenResponseType(returnType), method + " must not return entities or raw transport types");
                assertTrue(isAllowedDtoResponse(returnType), method + " must return DTOs or collections of DTOs");
            }
        }
    }

    @Test
    void authEndpointsReturnDtoJsonWithoutPasswordHash() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "password": "Secret!123",
                                  "firstName": "New",
                                  "lastName": "Customer",
                                  "bsn": "123456789",
                                  "phoneNumber": "0612345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(content().string(not(containsString("passwordHash"))));

        User user = userRepository.findByEmail("new@example.com").orElseThrow();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "password": "Secret!123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token.value").exists())
                .andExpect(content().string(not(containsString("passwordHash"))));

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void customerAccountAndTransactionEndpointsReturnDtoJsonWithoutPasswordHash() throws Exception {
        TestData data = createActiveCustomer();
        User employee = createEmployee();
        transactionRepository.save(new Transaction(0, data.checking().getIban(), data.savings().getIban(), data.user(),
                new BigDecimal("100.00"), TransactionType.TRANSFER, "transfer", LocalDateTime.now()));

        mockMvc.perform(get("/users")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(data.user().getEmail()))
                .andExpect(content().string(not(containsString("passwordHash"))));

        mockMvc.perform(get("/users/" + data.user().getId())
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].iban").exists())
                .andExpect(content().string(not(containsString("passwordHash"))));

        mockMvc.perform(get("/accounts")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iban").exists())
                .andExpect(content().string(not(containsString("passwordHash"))));

        mockMvc.perform(get("/accounts")
                        .param("iban", data.checking().getIban())
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iban").value(data.checking().getIban()))
                .andExpect(content().string(not(containsString("passwordHash"))));

        mockMvc.perform(get("/accounts/me")
                        .header("Authorization", bearerToken(data.user())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iban").exists())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(content().string(not(containsString("passwordHash"))));

        mockMvc.perform(patch("/accounts/" + data.checking().getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "absoluteTransferLimit": 1200.00,
                                  "dailyTransferLimit": 600.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(data.checking().getIban()))
                .andExpect(content().string(not(containsString("passwordHash"))));

        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].initiatedByUserId").value(data.user().getId()))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void customerTransactionsHonorFilters() throws Exception {
        TestData data = createActiveCustomer();
        transactionRepository.save(new Transaction(0, data.checking().getIban(), data.savings().getIban(), data.user(),
                new BigDecimal("100.00"), TransactionType.TRANSFER, "included", LocalDateTime.now()));
        transactionRepository.save(new Transaction(0, data.checking().getIban(), data.savings().getIban(), data.user(),
                new BigDecimal("20.00"), TransactionType.TRANSFER, "too small", LocalDateTime.now()));
        transactionRepository.save(new Transaction(0, data.checking().getIban(), data.savings().getIban(), data.user(),
                new BigDecimal("100.00"), TransactionType.DEPOSIT, "wrong type", LocalDateTime.now()));

        mockMvc.perform(get("/transactions")
                        .header("Authorization", bearerToken(createEmployee()))
                        .param("customerId", String.valueOf(data.user().getId()))
                        .param("type", "TRANSFER")
                        .param("minAmount", "50.00")
                        .param("maxAmount", "150.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("included"))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void customerSearchMatchesEmailAddresses() throws Exception {
        TestData data = createActiveCustomer();

        mockMvc.perform(get("/users")
                        .header("Authorization", bearerToken(createEmployee()))
                        .param("search", "customer@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value(data.user().getEmail()))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void accountSearchReturnsAllAccountsForMatchingCustomer() throws Exception {
        TestData data = createActiveCustomer();

        mockMvc.perform(get("/accounts")
                        .header("Authorization", bearerToken(createEmployee()))
                        .param("name", data.user().getFirstName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.iban == '" + data.checking().getIban() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.iban == '" + data.savings().getIban() + "')]").exists())
                .andExpect(content().string(not(containsString("passwordHash"))));
    }

    @Test
    void accountEndpointsRejectMissingAccountsAndNegativeLimits() throws Exception {
        TestData data = createActiveCustomer();
        User employee = createEmployee();

        mockMvc.perform(patch("/accounts/NL99BANK0000009999")
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "absoluteTransferLimit": 1000.00
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/accounts/" + data.checking().getIban())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "absoluteTransferLimit": -1.00
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activatingCustomerProvisionsAccountsThroughDtoEndpoint() throws Exception {
        User user = userRepository.save(new User(0, "pending@example.com", "secret", "Pending", "Customer", UserRole.CUSTOMER, LocalDateTime.now()));
        customerProfileRepository.save(new CustomerProfile(0, user, "987654321", "0698765432", CustomerStatus.PENDING));

        mockMvc.perform(patch("/users/" + user.getId())
                        .header("Authorization", bearerToken(createEmployee()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACTIVE",
                                  "firstName": "Active",
                                  "lastName": "Customer",
                                  "phoneNumber": "0600000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(content().string(not(containsString("passwordHash"))));

        List<Account> accounts = accountRepository.findByUser_Id(user.getId());
        assertTrue(accounts.stream().anyMatch(account -> account.getType() == AccountType.CHECKING));
        assertTrue(accounts.stream().anyMatch(account -> account.getType() == AccountType.SAVINGS));
    }

    private TestData createActiveCustomer() {
        User user = userRepository.save(new User(0, "customer@example.com", "secret", "Casey", "Customer", UserRole.CUSTOMER, LocalDateTime.now()));
        customerProfileRepository.save(new CustomerProfile(0, user, "111222333", "0611111111", CustomerStatus.ACTIVE));
        Account checking = accountRepository.save(new Account(0, user, "NL01BANK0000000001", AccountType.CHECKING,
                new BigDecimal("250.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now()));
        Account savings = accountRepository.save(new Account(0, user, "NL02BANK0000000002", AccountType.SAVINGS,
                new BigDecimal("750.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now()));
        return new TestData(user, checking, savings);
    }

    private User createEmployee() {
        return userRepository.save(new User(0, "employee-" + System.nanoTime() + "@example.com", "secret", "Erin", "Employee", UserRole.EMPLOYEE, LocalDateTime.now()));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    private static boolean isMappedEndpoint(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == RequestMapping.class ||
                    annotationType == GetMapping.class ||
                    annotationType == PostMapping.class ||
                    annotationType == PatchMapping.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean usesForbiddenResponseType(Type type) {
        if (type instanceof Class<?> clazz) {
            return ENTITY_TYPES.contains(clazz) || clazz == Map.class || clazz == Object.class;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType == Map.class) return true;
            for (Type argument : parameterizedType.getActualTypeArguments()) {
                if (usesForbiddenResponseType(argument)) return true;
            }
        }
        return false;
    }

    private static boolean isAllowedDtoResponse(Type type) {
        if (type instanceof Class<?> clazz) {
            return isDto(clazz);
        }
        if (type instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType() != List.class && parameterizedType.getRawType() != Page.class) return false;
            Type[] arguments = parameterizedType.getActualTypeArguments();
            return arguments.length == 1 && arguments[0] instanceof Class<?> clazz && isDto(clazz);
        }
        return false;
    }

    private static boolean isDto(Class<?> clazz) {
        return clazz.getPackageName().equals("nl.inholland.bankingapi.dtos");
    }

    private record TestData(User user, Account checking, Account savings) {}
}
