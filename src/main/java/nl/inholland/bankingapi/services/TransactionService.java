package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAll(String iban, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionRepository.findAll().stream()
                .filter(t -> iban == null || iban.equals(t.getFromIban()) || iban.equals(t.getToIban()))
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> minAmount == null || t.getAmount().compareTo(minAmount) >= 0)
                .filter(t -> maxAmount == null || t.getAmount().compareTo(maxAmount) <= 0)
                .toList();
    }

    public List<Transaction> getByIbans(List<String> ibans, TransactionType type, BigDecimal minAmount, BigDecimal maxAmount) {
        if (ibans.isEmpty()) return List.of();
        return transactionRepository.findByFromIbanInOrToIbanIn(ibans, ibans).stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> minAmount == null || t.getAmount().compareTo(minAmount) >= 0)
                .filter(t -> maxAmount == null || t.getAmount().compareTo(maxAmount) <= 0)
                .toList();
    }

    public Transaction create(String fromIban, String toIban, User initiatedBy, BigDecimal amount, TransactionType type, String description) {
        Transaction transaction = new Transaction(0, fromIban, toIban, initiatedBy, amount, type, description, LocalDateTime.now());
        return transactionRepository.save(transaction);
    }
}
