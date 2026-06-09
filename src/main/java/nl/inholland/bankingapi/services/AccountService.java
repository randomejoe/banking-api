package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.AccountPolicy;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.util.IbanGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountPolicy accountPolicy;
    private final IbanGenerator ibanGenerator;

    @Autowired
    public AccountService(AccountRepository accountRepository, AccountPolicy accountPolicy,
                          IbanGenerator ibanGenerator) {
        this.accountRepository = accountRepository;
        this.accountPolicy = accountPolicy;
        this.ibanGenerator = ibanGenerator;
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

    public Page<Account> getOwnAccounts(int userId, Pageable pageable) {
        return accountRepository.findByUser_Id(userId, pageable);
    }

    public Page<Account> searchTransferTargets(int excludeUserId, String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return Page.empty(pageable);
        }
        return accountRepository.findTransferTargetsByCustomerName(excludeUserId, name.trim(), pageable);
    }

    public Account getByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + iban));
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
        return new Account(0, user, ibanGenerator.generate(), type,
                BigDecimal.ZERO, absoluteTransferLimit, dailyTransferLimit,
                AccountStatus.ACTIVE, LocalDateTime.now());
    }
}
