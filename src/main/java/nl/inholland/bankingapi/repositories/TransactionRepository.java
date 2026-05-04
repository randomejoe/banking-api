package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByFromIbanInOrToIbanIn(List<String> fromIbans, List<String> toIbans);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.fromIban = :iban AND t.type = 'TRANSFER' AND t.timestamp >= :since")
    BigDecimal sumOutgoingTransfersSince(@Param("iban") String iban, @Param("since") LocalDateTime since);
}