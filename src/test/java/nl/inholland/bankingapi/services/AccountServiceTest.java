package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.AccountPolicy;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.util.IbanGenerator;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccountServiceTest {

    private final AccountPolicy accountPolicy = new AccountPolicy();

    @Test
    void failingIbanGenerationDoesNotSavePartialAccounts() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(new Account()));
        AccountService accountService = accountService(accountRepository, () -> {
            throw new IllegalStateException("Unable to generate a unique IBAN");
        });
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalStateException.class, () ->
                accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), BigDecimal.valueOf(500)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void createAccountsForUserUsesGeneratedIbansBeforeSaving() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = accountService(accountRepository,
                sequenceGenerator("NL01INHO0000000001", "NL02INHO0000000002"));
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        List<Account> accounts = accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), BigDecimal.valueOf(500));
        assertEquals("NL01INHO0000000001", accounts.get(0).getIban());
        assertEquals("NL02INHO0000000002", accounts.get(1).getIban());
        assertEquals(2, accountRepository.saveCount());
    }

    @Test
    void createAccountsForUser_requiresAbsoluteTransferLimitBeforeSaving() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccountsForUser(user, null, BigDecimal.valueOf(500)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void createAccountsForUser_requiresDailyTransferLimitBeforeSaving() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () ->
                accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), null));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void updateAccountRejectsNegativeLimitsBeforeLookupOrSave() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));
        AccountUpdateRequest request = new AccountUpdateRequest(new BigDecimal("-1.00"), null, null);

        assertThrows(IllegalArgumentException.class, () -> accountService.updateAccount("NL02INHO0000000001", request));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void updateAccountCanPatchOnlyAbsoluteLimit() {
        Account account = editableAccount();
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(account));
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));

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
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));

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
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));

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
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));

        assertThrows(IllegalArgumentException.class, () ->
                accountService.updateAccount(account.getIban(),
                        new AccountUpdateRequest(null, null, AccountStatus.CLOSED)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void getAllDelegatesFilteredQueryToRepository() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));
        AccountQuery query = new AccountQuery();
        query.setUserId(42);
        query.setName("Jane");
        query.setType(AccountType.CHECKING);

        accountService.getAll(query, PageRequest.of(0, 20));

        assertEquals(42, accountRepository.filteredQuery().getUserId());
        assertEquals(AccountType.CHECKING, accountRepository.filteredQuery().getType());
        assertEquals("Jane", accountRepository.filteredQuery().getName());
    }

    @Test
    void getOwnAccountsUsesDedicatedUserIdLookup() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));

        accountService.getOwnAccounts(9, PageRequest.of(0, 20));

        assertEquals(9, accountRepository.ownAccountsUserId());
    }

    @Test
    void searchTransferTargetsUsesDedicatedPublicLookupQuery() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.empty());
        AccountService accountService = accountService(accountRepository, sequenceGenerator("NL01INHO0000000001"));

        accountService.searchTransferTargets(9, "  Jane  ", PageRequest.of(0, 20));

        assertEquals(9, accountRepository.transferTargetUserId());
        assertEquals("Jane", accountRepository.transferTargetName());
    }

    @Test
    void updateAccountIsTransactional() throws NoSuchMethodException {
        Method method = AccountService.class.getMethod("updateAccount", String.class, AccountUpdateRequest.class);

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private AccountService accountService(TestAccountRepository accountRepository, IbanGenerator ibanGenerator) {
        return new AccountService(accountRepository.proxy(), accountPolicy, ibanGenerator);
    }

    private IbanGenerator sequenceGenerator(String... ibans) {
        int[] index = {0};
        return () -> ibans[index[0]++];
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
                                         int[] ownAccountsUserIds, Object[] transferTargetArgs) {
        TestAccountRepository(IbanLookup ibanLookup) {
            this(ibanLookup, new int[1], new AccountQuery[1], new int[]{-1}, new Object[2]);
        }

        int saveCount() {
            return saves[0];
        }

        AccountQuery filteredQuery() {
            return filteredQueries[0];
        }

        int ownAccountsUserId() {
            return ownAccountsUserIds[0];
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
                        case "findByUser_Id" -> {
                            if (args.length == 2) {
                                ownAccountsUserIds[0] = (Integer) args[0];
                                yield Page.empty((Pageable) args[1]);
                            }
                            yield List.of();
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
