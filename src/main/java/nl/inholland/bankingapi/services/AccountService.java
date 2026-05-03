package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

@Service
public class AccountService {

    private static final int MAX_IBAN_ATTEMPTS = 10;

    private final AccountRepository accountRepository;
    private final LongSupplier ibanNumberSource;

    @Autowired
    public AccountService(AccountRepository accountRepository) {
        this(accountRepository, () -> ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
    }

    AccountService(AccountRepository accountRepository, LongSupplier ibanNumberSource) {
        this.accountRepository = accountRepository;
        this.ibanNumberSource = ibanNumberSource;
    }

    @Transactional
    public List<Account> createAccountsForUser(User user) {
        Account checking = new Account(0, user, generateUniqueIban(), AccountType.CHECKING,
                BigDecimal.ZERO, new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now());
        Account savings = new Account(0, user, generateUniqueIban(), AccountType.SAVINGS,
                BigDecimal.ZERO, new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now());
        accountRepository.save(checking);
        accountRepository.save(savings);
        return List.of(checking, savings);
    }

    public List<Account> getAll(Integer userId, AccountType type, AccountStatus status) {
        return accountRepository.findAll().stream()
                .filter(a -> userId == null || a.getUserId() == userId)
                .filter(a -> type == null || a.getType() == type)
                .filter(a -> status == null || a.getStatus() == status)
                .toList();
    }

    public Account getByIban(String iban) {
        return accountRepository.findByIban(iban);
    }

    public List<Account> getByUserId(int userId) {
        return accountRepository.findByUser_Id(userId);
    }

    public Account updateLimits(String iban, BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit) {
        Account account = getByIban(iban);
        if (account == null) return null;
        if (absoluteTransferLimit != null) account.setAbsoluteTransferLimit(absoluteTransferLimit);
        if (dailyTransferLimit != null) account.setDailyTransferLimit(dailyTransferLimit);
        return accountRepository.save(account);
    }

    private String generateUniqueIban() {
        for (int attempt = 0; attempt < MAX_IBAN_ATTEMPTS; attempt++) {
            long num = ibanNumberSource.getAsLong();
            String iban = "NL" + String.format("%02d", (num % 99) + 1) + "BANK" + String.format("%010d", num);
            if (accountRepository.findByIban(iban) == null) return iban;
        }
        throw new IllegalStateException("Could not generate unique IBAN after " + MAX_IBAN_ATTEMPTS + " attempts");
    }
}
