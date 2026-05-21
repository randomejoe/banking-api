package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.AccountPolicy;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.AccountSpecification;
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
    private final LongSupplier ibanNumberSource;

    @Autowired
    public AccountService(AccountRepository accountRepository, AccountPolicy accountPolicy) {
        this(accountRepository, accountPolicy,
                () -> ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
    }

    public AccountService(AccountRepository accountRepository, AccountPolicy accountPolicy,
                          LongSupplier ibanNumberSource) {
        this.accountRepository = accountRepository;
        this.accountPolicy = accountPolicy;
        this.ibanNumberSource = ibanNumberSource;
    }

    @Transactional
    public List<Account> createAccountsForUser(User user, BigDecimal absoluteTransferLimit,
                                               BigDecimal dailyTransferLimit) {
        accountPolicy.enforceValidLimits(absoluteTransferLimit, dailyTransferLimit);
        if (absoluteTransferLimit == null)
            throw new IllegalArgumentException("absoluteTransferLimit is required");
        if (dailyTransferLimit == null)
            throw new IllegalArgumentException("dailyTransferLimit is required");

        Account checking = buildAccount(user, AccountType.CHECKING, absoluteTransferLimit, dailyTransferLimit);
        Account savings  = buildAccount(user, AccountType.SAVINGS,  absoluteTransferLimit, dailyTransferLimit);
        accountRepository.save(checking);
        accountRepository.save(savings);
        return List.of(checking, savings);
    }

    public Page<Account> getAll(AccountQuery query, Pageable pageable) {
        return accountRepository.findAll(AccountSpecification.fromQuery(query), pageable);
    }

    public Page<Account> getAllForUser(User currentUser, AccountQuery query, Pageable pageable) {
        return getAll(effectiveQueryFor(currentUser, query), pageable);
    }

    public Account getByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));
    }

    public List<Account> getByUserId(int userId) {
        return accountRepository.findByUser_Id(userId);
    }

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

    private AccountQuery effectiveQueryFor(User currentUser, AccountQuery query) {
        AccountQuery effective = new AccountQuery();
        effective.setUserId(query.getUserId());
        effective.setType(query.getType());
        effective.setStatus(query.getStatus());
        effective.setIban(query.getIban());
        effective.setName(query.getName());

        if (currentUser.getRole() != UserRole.EMPLOYEE) {
            effective.setUserId(currentUser.getId());
            effective.setName(null);
        }

        return effective;
    }

    private String generateIban() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            long num = ibanNumberSource.getAsLong();
            String accountNumber = String.format("%010d", num);
            String bban = "INHL" + accountNumber;
            String iban = "NL" + mod97CheckDigits(bban) + bban;
            if (accountRepository.findByIban(iban).isEmpty()) {
                return iban;
            }
        }
        throw new IllegalStateException("Unable to generate a unique IBAN");
    }

    private static String mod97CheckDigits(String bban) {
        // Per ISO 13616: rearrange as BBAN + country code + "00", replace letters with digits, compute 98 - (mod 97)
        String rearranged = bban + "NL00";
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(c - 'A' + 10);
            } else {
                numeric.append(c);
            }
        }
        int mod = 0;
        for (char digit : numeric.toString().toCharArray()) {
            mod = (mod * 10 + (digit - '0')) % 97;
        }
        return String.format("%02d", 98 - mod);
    }
}
