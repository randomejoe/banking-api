package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.AccountAccessPolicy;
import nl.inholland.bankingapi.domain.policy.AccountPolicy;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

@Service
public class AccountService {

    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 100;

    private final AccountRepository accountRepository;
    private final AccountPolicy accountPolicy;
    private final AccountAccessPolicy accountAccessPolicy;
    private final LongSupplier ibanNumberSource;

    @Autowired
    public AccountService(AccountRepository accountRepository, AccountPolicy accountPolicy,
                          AccountAccessPolicy accountAccessPolicy) {
        this(accountRepository, accountPolicy, accountAccessPolicy,
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000L));
    }

    public AccountService(AccountRepository accountRepository, AccountPolicy accountPolicy,
                          AccountAccessPolicy accountAccessPolicy, LongSupplier ibanNumberSource) {
        this.accountRepository = accountRepository;
        this.accountPolicy = accountPolicy;
        this.accountAccessPolicy = accountAccessPolicy;
        this.ibanNumberSource = ibanNumberSource;
    }

    @Transactional
    public List<Account> createAccountsForUser(User user, BigDecimal absoluteTransferLimit,
                                               BigDecimal dailyTransferLimit) {
        accountPolicy.enforceRequiredLimits(absoluteTransferLimit, dailyTransferLimit);

        Account checking = buildAccount(user, AccountType.CHECKING, absoluteTransferLimit, dailyTransferLimit);
        Account savings  = buildAccount(user, AccountType.SAVINGS,  absoluteTransferLimit, dailyTransferLimit);
        return accountRepository.saveAll(List.of(checking, savings));
    }

    public Page<Account> getAll(AccountQuery query, Pageable pageable) {
        return accountRepository.findAllFiltered(query, pageable);
    }

    public Page<Account> getAllForUser(User currentUser, AccountQuery query, Pageable pageable) {
        return getAll(accountAccessPolicy.effectiveQueryFor(currentUser, query), pageable);
    }

    public Page<Account> searchTransferTargets(User currentUser, String name, Pageable pageable) {
        return accountRepository.findTransferTargetsByCustomerName(currentUser.getId(), name.trim(), pageable);
    }

    public Account getByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));
    }

    public List<Account> getByUserId(int userId) {
        return accountRepository.findByUser_Id(userId);
    }

    @Transactional
    public Account updateAccount(String iban, AccountUpdateRequest request) {
        accountPolicy.enforceValidLimits(request.absoluteTransferLimit(), request.dailyTransferLimit());
        Account account = getByIban(iban);
        if (request.status() == AccountStatus.CLOSED) {
            accountPolicy.enforceCanClose(account);
        }
        if (request.absoluteTransferLimit() != null) account.setAbsoluteTransferLimit(request.absoluteTransferLimit());
        if (request.dailyTransferLimit() != null)    account.setDailyTransferLimit(request.dailyTransferLimit());
        if (request.status() != null)                account.setStatus(request.status());
        return accountRepository.save(account);
    }

    private Account buildAccount(User user, AccountType type,
                                 BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit) {
        return new Account(0, user, generateIban(), type,
                BigDecimal.ZERO, absoluteTransferLimit, dailyTransferLimit,
                AccountStatus.ACTIVE, LocalDateTime.now());
    }

    private String generateIban() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            long num = ibanNumberSource.getAsLong();
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
