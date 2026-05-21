package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.AccountPolicy;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountServiceTest {

    // Shared AccountPolicy instance (no dependencies, safe to reuse)
    private final AccountPolicy accountPolicy = new AccountPolicy();

    @Test
    void exhaustingIbanCandidatesDoesNotSavePartialAccounts() {
        // All IBAN lookups return "already exists" — service must exhaust all attempts and throw
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(new Account()));
        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, () -> 1L);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalStateException.class, () ->
                accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), BigDecimal.valueOf(500)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void existingIbanCandidatesAreRetriedBeforeSaving() {
        // NL95INHL0000000001 is generated for num=1 (verified via MOD-97 algorithm)
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> {
            if ("NL95INHL0000000001".equals(iban)) {
                return Optional.of(new Account());
            }
            return Optional.empty();
        });

        long[] values = {1L, 2L, 3L, 4L};
        int[] index = {0};
        LongSupplier source = () -> values[index[0]++];

        AccountService accountService = new AccountService(accountRepository.proxy(), accountPolicy, source);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        // num=1 is rejected (IBAN exists), num=2 is accepted for first account,
        // num=3 is accepted for second account — two saves total
        accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), BigDecimal.valueOf(500));
        assertEquals(2, accountRepository.saveCount());
    }

    private record TestAccountRepository(IbanLookup ibanLookup, int[] saves) {
        TestAccountRepository(IbanLookup ibanLookup) {
            this(ibanLookup, new int[1]);
        }

        int saveCount() {
            return saves[0];
        }

        @SuppressWarnings("unchecked")
        AccountRepository proxy() {
            return (AccountRepository) Proxy.newProxyInstance(
                    AccountRepository.class.getClassLoader(),
                    new Class<?>[]{AccountRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByIban" -> ibanLookup.find((String) args[0]);
                        case "save" -> {
                            saves[0]++;
                            yield args[0];
                        }
                        case "saveAll" -> {
                            // saveAll receives a List<Account>; count each element as one save
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
