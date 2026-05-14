package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.TransactionPolicy;
import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionFilterParams;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionPolicy transactionPolicy;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              TransactionMapper transactionMapper,
                              TransactionPolicy transactionPolicy) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionMapper = transactionMapper;
        this.transactionPolicy = transactionPolicy;
    }

    public Page<Transaction> getAll(TransactionFilterParams filters, Pageable pageable) {
        return transactionRepository.findAllFiltered(
                filters.getIban(),
                filters.getType(),
                filters.getMinAmount(),
                filters.getMaxAmount(),
                filters.getCustomerId(),
                pageable
        );
    }

    public Transaction getById(int id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    @Transactional
    public Transaction create(TransactionCreateRequest request, User initiatedBy) {
        TransactionType type = request.type();

        if (type == TransactionType.TRANSFER) {
            Account fromAccount = findAccount(request.fromIban(), "Source");
            Account toAccount = findAccount(request.toIban(), "Destination");
            transactionPolicy.enforceTransferPolicy(request, fromAccount, toAccount, initiatedBy);
            debit(fromAccount, request.amount());
            credit(toAccount, request.amount());

        } else if (type == TransactionType.DEPOSIT) {
            Account toAccount = findAccount(request.toIban(), "Destination");
            transactionPolicy.enforceDepositPolicy(request, toAccount);
            credit(toAccount, request.amount());

        } else if (type == TransactionType.WITHDRAWAL) {
            Account fromAccount = findAccount(request.fromIban(), "Source");
            transactionPolicy.enforceWithdrawalPolicy(request, fromAccount, initiatedBy);
            debit(fromAccount, request.amount());

        } else {
            transactionPolicy.enforceUnsupportedType(type);
        }

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private Account findAccount(String iban, String label) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException(label + " account not found: " + iban));
    }

    private void debit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    private void credit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }
}
