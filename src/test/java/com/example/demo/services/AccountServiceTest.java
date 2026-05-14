package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.AccountRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountServiceTest {

    @Test
    void exhaustingIbanCandidatesDoesNotSavePartialAccounts() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> Optional.of(new Account()));
        AccountService accountService = new AccountService(accountRepository.proxy(), () -> 1L);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

        assertThrows(IllegalStateException.class, () -> accountService.createAccountsForUser(user, BigDecimal.valueOf(1000), BigDecimal.valueOf(500)));
        assertEquals(0, accountRepository.saveCount());
    }

    @Test
    void existingIbanCandidatesAreRetriedBeforeSaving() {
        TestAccountRepository accountRepository = new TestAccountRepository(iban -> {
            if ("NL02BANK0000000001".equals(iban)) {
                return Optional.of(new Account());
            }
            return Optional.empty();
        });

        long[] values = {1L, 2L, 3L, 4L};
        int[] index = {0};
        LongSupplier source = () -> values[index[0]++];

        AccountService accountService = new AccountService(accountRepository.proxy(), source);
        User user = new User(1, "user@example.com", "secret", "Test", "User", UserRole.CUSTOMER, LocalDateTime.now());

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
