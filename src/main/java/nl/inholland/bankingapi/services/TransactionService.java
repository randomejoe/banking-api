package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.exceptions.TransactionException;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public List<Transaction> getAll(String iban, TransactionType type,
                                    BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findAll().stream()
                .filter(t -> iban == null || iban.equals(t.getFromIban()) || iban.equals(t.getToIban()))
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> minAmount == null || t.getAmount().compareTo(minAmount) >= 0)
                .filter(t -> maxAmount == null || t.getAmount().compareTo(maxAmount) <= 0)
                .toList();
    }

    public List<Transaction> getByIbans(List<String> ibans, TransactionType type,
                                        BigDecimal minAmount, BigDecimal maxAmount) {
        if (ibans.isEmpty()) return List.of();
        return transactionRepository.findByFromIbanInOrToIbanIn(ibans, ibans).stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> minAmount == null || t.getAmount().compareTo(minAmount) >= 0)
                .filter(t -> maxAmount == null || t.getAmount().compareTo(maxAmount) <= 0)
                .toList();
    }

    @Transactional
    public Transaction create(String fromIban, String toIban, User initiatedBy,
                              BigDecimal amount, TransactionType type, String description) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Amount must be greater than zero");
        }

        if (type == TransactionType.TRANSFER) {
            Account fromAccount = requireActiveAccount(fromIban, "Source");
            Account toAccount   = requireActiveAccount(toIban,   "Destination");
            validateTransferLimits(fromAccount, amount);
            debit(fromAccount, amount);
            credit(toAccount, amount);

        } else if (type == TransactionType.DEPOSIT) {
            Account toAccount = requireActiveAccount(toIban, "Destination");
            credit(toAccount, amount);

        } else if (type == TransactionType.WITHDRAWAL) {
            Account fromAccount = requireActiveAccount(fromIban, "Source");
            validateTransferLimits(fromAccount, amount);
            debit(fromAccount, amount);

        } else {
            throw new TransactionException("Unsupported transaction type: " + type);
        }

        Transaction transaction = new Transaction(
                0, fromIban, toIban, initiatedBy, amount, type, description, LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Account requireActiveAccount(String iban, String label) {
        if (iban == null || iban.isBlank()) {
            throw new TransactionException(label + " IBAN is required");
        }
        Account account = accountRepository.findByIban(iban);
        if (account == null) {
            throw new TransactionException(label + " account not found: " + iban);
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new TransactionException(label + " account is not active: " + iban);
        }
        return account;
    }

    private void validateTransferLimits(Account from, BigDecimal amount) {
        // absoluteTransferLimit: the balance must not drop below this value after the transfer
        BigDecimal balanceAfter = from.getBalance().subtract(amount);
        if (balanceAfter.compareTo(from.getAbsoluteTransferLimit()) < 0) {
            throw new TransactionException(
                    "Transfer would breach the absolute transfer limit for account: " + from.getIban());
        }

        // dailyTransferLimit: total outgoing transfers today must not exceed this value
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal transferredToday = transactionRepository
                .findByFromIbanAndTypeAndTimestampGreaterThanEqual(from.getIban(), TransactionType.TRANSFER, startOfDay)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (transferredToday.add(amount).compareTo(from.getDailyTransferLimit()) > 0) {
            throw new TransactionException(
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
