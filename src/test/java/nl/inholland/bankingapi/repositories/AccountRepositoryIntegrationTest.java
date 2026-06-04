package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AccountRepositoryIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void accountFieldsPersistAndLoadByIban() {
        User user = createCustomer("account-persist@example.com", "Persist", "Customer");
        Account account = createAccount(user, "NL01INHO0000000001", AccountType.CHECKING,
                AccountStatus.ACTIVE, new BigDecimal("123.45"));

        Account loaded = accountRepository.findByIban(account.getIban()).orElseThrow();

        assertEquals(user.getId(), loaded.getUserId());
        assertEquals(AccountType.CHECKING, loaded.getType());
        assertEquals(AccountStatus.ACTIVE, loaded.getStatus());
        assertEquals(new BigDecimal("123.45"), loaded.getBalance());
        assertEquals(new BigDecimal("1000.00"), loaded.getAbsoluteTransferLimit());
        assertEquals(new BigDecimal("500.00"), loaded.getDailyTransferLimit());
    }

    @Test
    void duplicateIbanViolatesUniqueConstraint() {
        User first = createCustomer("account-unique-1@example.com", "Unique", "One");
        User second = createCustomer("account-unique-2@example.com", "Unique", "Two");
        createAccount(first, "NL02INHO0000000002", AccountType.CHECKING,
                AccountStatus.ACTIVE, BigDecimal.ZERO);

        Account duplicate = account("NL02INHO0000000002", second, AccountType.SAVINGS,
                AccountStatus.ACTIVE, BigDecimal.ZERO);

        assertThrows(DataIntegrityViolationException.class,
                () -> accountRepository.saveAndFlush(duplicate));
    }

    @Test
    void findTransferTargetsByCustomerNameOnlyReturnsOtherActiveCheckingAccounts() {
        User searchingCustomer = createCustomer("account-target-searcher@example.com", "Search", "Customer");
        User matchingCustomer = createCustomer("account-target-match@example.com", "Recipient", "Target");
        User otherCustomer = createCustomer("account-target-other@example.com", "Other", "Customer");

        createAccount(searchingCustomer, "NL03INHO0000000003", AccountType.CHECKING,
                AccountStatus.ACTIVE, BigDecimal.ZERO);
        Account expected = createAccount(matchingCustomer, "NL04INHO0000000004", AccountType.CHECKING,
                AccountStatus.ACTIVE, BigDecimal.ZERO);
        createAccount(matchingCustomer, "NL05INHO0000000005", AccountType.SAVINGS,
                AccountStatus.ACTIVE, BigDecimal.ZERO);
        createAccount(matchingCustomer, "NL06INHO0000000006", AccountType.CHECKING,
                AccountStatus.CLOSED, BigDecimal.ZERO);
        createAccount(otherCustomer, "NL07INHO0000000007", AccountType.CHECKING,
                AccountStatus.ACTIVE, BigDecimal.ZERO);

        Page<Account> targets = accountRepository.findTransferTargetsByCustomerName(
                searchingCustomer.getId(), "recipient", PageRequest.of(0, 20));

        assertEquals(1, targets.getTotalElements());
        assertEquals(expected.getIban(), targets.getContent().getFirst().getIban());
    }

    @Test
    void findAllFilteredComposesNameTypeStatusAndIban() {
        User matchingCustomer = createCustomer("account-filter-match@example.com", "Filtered", "Customer");
        User otherCustomer = createCustomer("account-filter-other@example.com", "Filtered", "Other");
        Account expected = createAccount(matchingCustomer, "NL08INHO0000000008", AccountType.CHECKING,
                AccountStatus.CLOSED, BigDecimal.ZERO);
        createAccount(matchingCustomer, "NL09INHO0000000009", AccountType.CHECKING,
                AccountStatus.ACTIVE, BigDecimal.ZERO);
        createAccount(matchingCustomer, "NL10INHO0000000010", AccountType.SAVINGS,
                AccountStatus.CLOSED, BigDecimal.ZERO);
        createAccount(otherCustomer, "NL11INHO0000000011", AccountType.CHECKING,
                AccountStatus.CLOSED, BigDecimal.ZERO);

        AccountQuery query = new AccountQuery();
        query.setName("filter-match");
        query.setType(AccountType.CHECKING);
        query.setStatus(AccountStatus.CLOSED);
        query.setIban(expected.getIban());

        Page<Account> accounts = accountRepository.findAllFiltered(query, PageRequest.of(0, 20));

        assertEquals(1, accounts.getTotalElements());
        assertEquals(expected.getIban(), accounts.getContent().getFirst().getIban());
    }

    private User createCustomer(String email, String firstName, String lastName) {
        User user = userRepository.save(new User(0, email, "secret", firstName, lastName,
                UserRole.CUSTOMER, LocalDateTime.now()));
        customerProfileRepository.save(new CustomerProfile(0, user, uniqueBsn(user), "0612345678",
                CustomerStatus.ACTIVE));
        return user;
    }

    private String uniqueBsn(User user) {
        return String.format("%09d", user.getId());
    }

    private Account createAccount(User user, String iban, AccountType type, AccountStatus status,
                                  BigDecimal balance) {
        return accountRepository.save(account(iban, user, type, status, balance));
    }

    private Account account(String iban, User user, AccountType type, AccountStatus status,
                            BigDecimal balance) {
        return new Account(0, user, iban, type, balance,
                new BigDecimal("1000.00"), new BigDecimal("500.00"), status, LocalDateTime.now());
    }
}
