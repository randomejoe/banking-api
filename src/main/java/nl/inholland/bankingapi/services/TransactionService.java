package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.TransactionPolicy;
import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionFilterParams;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.exceptions.BadRequestException;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.TransactionRepository;
import nl.inholland.bankingapi.repositories.TransactionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private static final List<TransactionType> OUTGOING_LIMIT_TYPES =
            List.of(TransactionType.TRANSFER, TransactionType.WITHDRAWAL);

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
        if (filters.getStartDate() != null && filters.getEndDate() != null
                && filters.getStartDate().isAfter(filters.getEndDate())) {
            throw new BadRequestException("startDate must be on or before endDate");
        }
        return transactionRepository.findAll(TransactionSpecifications.fromFilters(filters), pageable);
    }

    public Transaction getById(int id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    /**
     * Throws 403 if the customer is not the initiator and does not own the fromIban or toIban.
     * Called by the controller for non-employee users on GET /transactions/{id}.
     */
    public void assertCustomerCanView(Transaction transaction, User customer) {
        if (transaction.getInitiatedBy().getId() == customer.getId()) {
            return;
        }
        boolean ownsFromIban = transaction.getFromIban() != null
                && accountRepository.findByIban(transaction.getFromIban())
                        .map(a -> a.getUser().getId() == customer.getId())
                        .orElse(false);
        boolean ownsToIban = transaction.getToIban() != null
                && accountRepository.findByIban(transaction.getToIban())
                        .map(a -> a.getUser().getId() == customer.getId())
                        .orElse(false);
        if (!ownsFromIban && !ownsToIban) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    @Transactional
    public Transaction create(TransactionCreateRequest request, User initiatedBy) {
        TransactionType type = request.type();

        if (type == TransactionType.TRANSFER) {
            Account fromAccount = findAccount(request.fromIban(), "Source");
            Account toAccount = findAccount(request.toIban(), "Destination");
            BigDecimal outgoingToday = computeOutgoingToday(fromAccount.getIban());
            transactionPolicy.enforceTransferPolicy(request, fromAccount, toAccount, initiatedBy, outgoingToday);
            debit(fromAccount, request.amount());
            credit(toAccount, request.amount());

        } else if (type == TransactionType.DEPOSIT) {
            Account toAccount = findAccount(request.toIban(), "Destination");
            transactionPolicy.enforceDepositPolicy(request, toAccount);
            credit(toAccount, request.amount());

        } else if (type == TransactionType.WITHDRAWAL) {
            Account fromAccount = findAccount(request.fromIban(), "Source");
            BigDecimal outgoingToday = computeOutgoingToday(fromAccount.getIban());
            transactionPolicy.enforceWithdrawalPolicy(request, fromAccount, initiatedBy, outgoingToday);
            debit(fromAccount, request.amount());

        } else {
            transactionPolicy.enforceUnsupportedType(type);
        }

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private BigDecimal computeOutgoingToday(String iban) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return transactionRepository
                .findByFromIbanAndTypeInAndTimestampGreaterThanEqual(iban, OUTGOING_LIMIT_TYPES, startOfDay)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
