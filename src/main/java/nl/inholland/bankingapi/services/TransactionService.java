package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionFilterParams;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.exceptions.BadRequestException;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerService customerService;
    private final TransactionMapper transactionMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CustomerService customerService,
                              TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.customerService = customerService;
        this.transactionMapper = transactionMapper;
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
    public Transaction create(TransactionCreateRequest request) {
        User initiatedBy = customerService.getUserById(request.initiatedByUserId());
        if (initiatedBy == null) {
            throw new ResourceNotFoundException("User not found: " + request.initiatedByUserId());
        }

        TransactionType type = request.type();

        if (type == TransactionType.TRANSFER) {
            if (request.fromIban() == null || request.toIban() == null) {
                throw new BadRequestException("TRANSFER requires both fromIban and toIban");
            }
            if (request.fromIban().equals(request.toIban())) {
                throw new BadRequestException("Cannot transfer to the same account");
            }
            Account fromAccount = requireActiveAccount(request.fromIban(), "Source");
            Account toAccount   = requireActiveAccount(request.toIban(),   "Destination");
            validateTransferLimits(fromAccount, request.amount());
            debit(fromAccount, request.amount());
            credit(toAccount, request.amount());

        } else if (type == TransactionType.DEPOSIT) {
            if (request.toIban() == null) {
                throw new BadRequestException("DEPOSIT requires toIban");
            }
            Account toAccount = requireActiveAccount(request.toIban(), "Destination");
            credit(toAccount, request.amount());

        } else if (type == TransactionType.WITHDRAWAL) {
            if (request.fromIban() == null) {
                throw new BadRequestException("WITHDRAWAL requires fromIban");
            }
            Account fromAccount = requireActiveAccount(request.fromIban(), "Source");
            validateTransferLimits(fromAccount, request.amount());
            debit(fromAccount, request.amount());

        } else {
            throw new BadRequestException("Unsupported transaction type: " + type);
        }

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private Account requireActiveAccount(String iban, String label) {
        if (iban == null || iban.isBlank()) {
            throw new BadRequestException(label + " IBAN is required");
        }
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new ResourceNotFoundException(label + " account not found: " + iban));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException(label + " account is not active: " + iban);
        }
        return account;
    }

    private void validateTransferLimits(Account from, BigDecimal amount) {
        BigDecimal balanceAfter = from.getBalance().subtract(amount);
        if (balanceAfter.compareTo(from.getAbsoluteTransferLimit()) < 0) {
            throw new BadRequestException(
                    "Transfer would breach the absolute transfer limit for account: " + from.getIban());
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal transferredToday = transactionRepository
                .findByFromIbanAndTypeAndTimestampGreaterThanEqual(from.getIban(), TransactionType.TRANSFER, startOfDay)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (transferredToday.add(amount).compareTo(from.getDailyTransferLimit()) > 0) {
            throw new BadRequestException(
                    "Transfer would exceed the daily transfer limit for account: " + from.getIban());
        }
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
