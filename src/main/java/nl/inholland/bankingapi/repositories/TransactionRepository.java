package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByFromIbanInOrToIbanIn(List<String> fromIbans, List<String> toIbans);

    List<Transaction> findByFromIbanAndTypeAndTimestampGreaterThanEqual(String iban, TransactionType type, LocalDateTime since);
}
