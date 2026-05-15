package nl.inholland.bankingapi.services;

import jakarta.persistence.criteria.Predicate;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

@Service
public class
AccountService {

    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 100;

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
    public List<Account> createAccountsForUser(User user, BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit) {
        if (absoluteTransferLimit == null)
            throw new IllegalArgumentException("absoluteTransferLimit is required");
        if (dailyTransferLimit == null)
            throw new IllegalArgumentException("dailyTransferLimit is required");
        if (absoluteTransferLimit.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("absoluteTransferLimit must be >= 0");
        if (dailyTransferLimit.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("dailyTransferLimit must be >= 0");

        Account checking = new Account(0, user, generateIban(), AccountType.CHECKING,
                BigDecimal.ZERO, absoluteTransferLimit, dailyTransferLimit, AccountStatus.ACTIVE, LocalDateTime.now());
        Account savings = new Account(0, user, generateIban(), AccountType.SAVINGS,
                BigDecimal.ZERO, absoluteTransferLimit, dailyTransferLimit, AccountStatus.ACTIVE, LocalDateTime.now());
        accountRepository.save(checking);
        accountRepository.save(savings);
        return List.of(checking, savings);
    }

    public Page<Account> getAll(AccountQuery query, Pageable pageable) {
        return accountRepository.findAll(buildSpec(query), pageable);
    }

    private Specification<Account> buildSpec(AccountQuery query) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getUserId() != null)
                predicates.add(cb.equal(root.get("user").get("id"), query.getUserId()));
            if (query.getType() != null)
                predicates.add(cb.equal(root.get("type"), query.getType()));
            if (query.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            if (query.getIban() != null)
                predicates.add(cb.equal(root.get("iban"), query.getIban()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Account getByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));
    }

    public List<Account> getByUserId(int userId) {
        return accountRepository.findByUser_Id(userId);
    }

    public Page<Account> getByUserIds(List<Integer> userIds, Pageable pageable) {
        if (userIds.isEmpty()) return Page.empty(pageable);
        return accountRepository.findByUser_IdIn(userIds, pageable);
    }

    public Account updateAccount(String iban, BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit, AccountStatus status) {
        if (absoluteTransferLimit != null && absoluteTransferLimit.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("absoluteTransferLimit must be >= 0");
        if (dailyTransferLimit != null && dailyTransferLimit.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("dailyTransferLimit must be >= 0");

        Account account = getByIban(iban);
        if (status == AccountStatus.CLOSED && account.getBalance().compareTo(BigDecimal.ZERO) != 0)
            throw new IllegalArgumentException("Cannot close account with non-zero balance");
        if (absoluteTransferLimit != null) account.setAbsoluteTransferLimit(absoluteTransferLimit);
        if (dailyTransferLimit != null) account.setDailyTransferLimit(dailyTransferLimit);
        if (status != null) account.setStatus(status);
        return accountRepository.save(account);
    }

    private String generateIban() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            long num = ibanNumberSource.getAsLong();
            String iban = "NL" + String.format("%02d", (num % 99) + 1) + "BANK" + String.format("%010d", num);
            if (accountRepository.findByIban(iban).isEmpty()) {
                return iban;
            }
        }
        throw new IllegalStateException("Unable to generate a unique IBAN");
    }
}
