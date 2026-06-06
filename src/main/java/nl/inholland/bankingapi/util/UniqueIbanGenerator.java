package nl.inholland.bankingapi.util;

import nl.inholland.bankingapi.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

@Component
public class UniqueIbanGenerator implements IbanGenerator {

    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 100;

    private final AccountRepository accountRepository;
    private final LongSupplier numberSource;

    @Autowired
    public UniqueIbanGenerator(AccountRepository accountRepository) {
        this(accountRepository, () -> ThreadLocalRandom.current().nextLong(1_000_000_000L));
    }

    public UniqueIbanGenerator(AccountRepository accountRepository, LongSupplier numberSource) {
        this.accountRepository = accountRepository;
        this.numberSource = numberSource;
    }

    @Override
    public String generate() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            long num = numberSource.getAsLong();
            long accountNumber = Math.floorMod(num, 1_000_000_000L);
            String iban = "NL" + String.format("%02d", (accountNumber % 99) + 1)
                    + "INHO0" + String.format("%09d", accountNumber);
            if (!accountRepository.existsByIban(iban)) {
                return iban;
            }
        }
        throw new IllegalStateException("Unable to generate a unique IBAN");
    }
}
