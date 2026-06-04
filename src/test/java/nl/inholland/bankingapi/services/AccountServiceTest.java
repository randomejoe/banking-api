package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.AccountAccessPolicy;
import nl.inholland.bankingapi.domain.policy.AccountPolicy;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    private final AccountPolicy accountPolicy = new AccountPolicy();
    private final AccountAccessPolicy accountAccessPolicy = new AccountAccessPolicy();

    @Test
    void exhaustingIbanCandidatesDoesNotSavePartialAccounts() {
        // every IBAN is already taken, so the service runs out of candidates and throws
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(new Account()));
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalStateException.class, () ->
                accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), BigDecimal.valueOf(500)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void existingIbanCandidatesAreRetriedBeforeSaving() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> {
            if ("NL02INHO0000000001".equals(iban)) {
                return Optional.of(new Account());
            }
            return Optional.empty();
        });

        long[] values = {1L, 2L, 3L, 4L};
        int[] index = {0};
        LongSupplier source = () -> values[index[0]++];

        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, source);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        // num=1 is rejected (IBAN exists), num=2 is accepted for first account,
        // num=3 is accepted for second account — two saves total
        List<Account> accounts = accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), BigDecimal.valueOf(500));
        assertEquals("NL03INHO0000000002", accounts.get(0).getIban());
        assertEquals("NL04INHO0000000003", accounts.get(1).getIban());
        assertEquals(3, index[0]);
        assertEquals(2, accountRepository.saveCount());
        assertTrue(accounts.stream().allMatch(account -> account.getIban().matches("NL\\d{2}INHO0\\d{9}")));
    }

    @Test
    void createAccountsForUser_requiresAbsoluteTransferLimitBeforeSaving() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccountsForUser(user, null, BigDecimal.valueOf(500)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void createAccountsForUser_requiresDailyTransferLimitBeforeSaving() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), null));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void updateAccountRejectsNegativeLimitsBeforeLookupOrSave() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);
        AccountUpdateRequest request = new AccountUpdateRequest(new BigDecimal("-1.00"), null, null);

        assertThrows(IllegalArgumentException.class, () -> accountService.updateAccount("NL02INHO0000000001", request));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void updateAccountCanPatchOnlyAbsoluteLimit() {
        Account account = editableAccount();
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(account));
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);

        Account updated = accountService.updateAccount(account.getIban(),
                new AccountUpdateRequest(new BigDecimal("250.00"), null, null));

        assertEquals(new BigDecimal("250.00"), updated.getAbsoluteTransferLimit());
        assertEquals(new BigDecimal("500.00"), updated.getDailyTransferLimit());
        assertEquals(AccountStatus.ACTIVE, updated.getStatus());
        assertEquals(1, accountRepository.saveCount());
    }

    @Test
    void updateAccountCanPatchOnlyDailyLimit() {
        Account account = editableAccount();
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(account));
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);

        Account updated = accountService.updateAccount(account.getIban(),
                new AccountUpdateRequest(null, new BigDecimal("750.00"), null));

        assertEquals(new BigDecimal("100.00"), updated.getAbsoluteTransferLimit());
        assertEquals(new BigDecimal("750.00"), updated.getDailyTransferLimit());
        assertEquals(AccountStatus.ACTIVE, updated.getStatus());
        assertEquals(1, accountRepository.saveCount());
    }

    @Test
    void updateAccountCanPatchOnlyStatus() {
        Account account = editableAccount();
        account.setBalance(BigDecimal.ZERO);
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(account));
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);

        Account updated = accountService.updateAccount(account.getIban(),
                new AccountUpdateRequest(null, null, AccountStatus.CLOSED));

        assertEquals(new BigDecimal("100.00"), updated.getAbsoluteTransferLimit());
        assertEquals(new BigDecimal("500.00"), updated.getDailyTransferLimit());
        assertEquals(AccountStatus.CLOSED, updated.getStatus());
        assertEquals(1, accountRepository.saveCount());
    }

    @Test
    void updateAccountRejectsClosingAccountWithNonZeroBalance() {
        Account account = editableAccount();
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(account));
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);

        assertThrows(IllegalArgumentException.class, () ->
                accountService.updateAccount(account.getIban(),
                        new AccountUpdateRequest(null, null, AccountStatus.CLOSED)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void getAllForUserAppliesPrivateAccountAccessPolicyBeforeRepositoryLookup() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);
        User customer = new User(9, "customer@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());
        AccountQuery query = new AccountQuery();
        query.setUserId(42);
        query.setName("Jane");
        query.setType(AccountType.CHECKING);

        accountService.getAllForUser(customer, query, PageRequest.of(0, 20));

        assertEquals(9, accountRepository.filteredQuery().getUserId());
        assertEquals(AccountType.CHECKING, accountRepository.filteredQuery().getType());
        assertNull(accountRepository.filteredQuery().getName());
    }

    @Test
    void searchTransferTargetsUsesDedicatedPublicLookupQuery() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, accountAccessPolicy, () -> 1L);
        User customer = new User(9, "customer@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        accountService.searchTransferTargets(customer, "  Jane  ", PageRequest.of(0, 20));

        assertEquals(9, accountRepository.transferTargetUserId());
        assertEquals("Jane", accountRepository.transferTargetName());
    }

    @Test
    void updateAccountIsTransactional() throws NoSuchMethodException {
        Method method = AccountService.class.getMethod("updateAccount", String.class, AccountUpdateRequest.class);

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private Account editableAccount() {
        Account account = new Account();
        account.setIban("NL02INHO0000000001");
        account.setBalance(new BigDecimal("100.00"));
        account.setAbsoluteTransferLimit(new BigDecimal("100.00"));
        account.setDailyTransferLimit(new BigDecimal("500.00"));
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private record TestAccountRepository(IbanLookup ibanLookup, int[] saves, AccountQuery[] filteredQueries,
                                         Object[] transferTargetArgs) {
        TestAccountRepository(IbanLookup ibanLookup) {
            this(ibanLookup, new int[1], new AccountQuery[1], new Object[2]);
        }

        int saveCount() {
            return saves[0];
        }

        AccountQuery filteredQuery() {
            return filteredQueries[0];
        }

        int transferTargetUserId() {
            return (Integer) transferTargetArgs[0];
        }

        String transferTargetName() {
            return (String) transferTargetArgs[1];
        }

        @SuppressWarnings("unchecked")
        AccountRepository proxy() {
            return (AccountRepository) Proxy.newProxyInstance(
                    AccountRepository.class.getClassLoader(),
                    new Class<?>[]{AccountRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByIban" -> ibanLookup.find((String) args[0]);
                        case "existsByIban" -> ibanLookup.find((String) args[0]).isPresent();
                        case "findAllFiltered" -> {
                            filteredQueries[0] = (AccountQuery) args[0];
                            yield Page.empty((Pageable) args[1]);
                        }
                        case "findTransferTargetsByCustomerName" -> {
                            transferTargetArgs[0] = args[0];
                            transferTargetArgs[1] = args[1];
                            yield Page.empty((Pageable) args[2]);
                        }
                        case "save" -> {
                            saves[0]++;
                            yield args[0];
                        }
                        case "saveAll" -> {
                            // count each account in the list individually
                            List<?> accounts = (List<?>) args[0];
                            saves[0] += accounts.size();
                            yield accounts;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    @FunctionalInterface
    private interface IbanLookup {
        Optional<Account> find(String iban);
    }
}
