package nl.inholland.bankingapi.util;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.repositories.AccountRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueIbanGeneratorTest {

    @Test
    void existingIbanCandidatesAreRetriedBeforeReturning() {
        AccountRepository accountRepository = repository(iban ->
                "NL02INHO0000000001".equals(iban) ? Optional.of(new Account()) : Optional.empty());
        long[] values = {1L, 2L};
        int[] index = {0};

        UniqueIbanGenerator generator = new UniqueIbanGenerator(accountRepository, next(values, index));

        String iban = generator.generate();

        assertEquals("NL03INHO0000000002", iban);
        assertEquals(2, index[0]);
        assertTrue(iban.matches("NL\\d{2}INHO0\\d{9}"));
    }

    @Test
    void exhaustingIbanCandidatesThrows() {
        AccountRepository accountRepository = repository(iban -> Optional.of(new Account()));
        UniqueIbanGenerator generator = new UniqueIbanGenerator(accountRepository, () -> 1L);

        assertThrows(IllegalStateException.class, generator::generate);
    }

    private LongSupplier next(long[] values, int[] index) {
        return () -> values[index[0]++];
    }

    private AccountRepository repository(IbanLookup ibanLookup) {
        return (AccountRepository) Proxy.newProxyInstance(
                AccountRepository.class.getClassLoader(),
                new Class<?>[]{AccountRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByIban" -> ibanLookup.find((String) args[0]).isPresent();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @FunctionalInterface
    private interface IbanLookup {
        Optional<Account> find(String iban);
    }
}
