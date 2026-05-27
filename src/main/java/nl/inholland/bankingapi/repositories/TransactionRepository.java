package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    // Used by the daily outgoing limit check in TransactionPolicy
    List<Transaction> findByFromIbanAndTypeInAndTimestampGreaterThanEqual(
            String iban, Collection<TransactionType> types, LocalDateTime since);

    // Used by GET /transactions — all filters are optional; null = no filter
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:iban IS NULL OR t.fromIban = :iban OR t.toIban = :iban) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:customerId IS NULL OR t.initiatedBy.id = :customerId) AND " +
           "(:startDateTime IS NULL OR t.timestamp >= :startDateTime) AND " +
           "(:endDateExclusive IS NULL OR t.timestamp < :endDateExclusive)")
    Page<Transaction> findAllFiltered(
            @Param("iban") String iban,
            @Param("type") TransactionType type,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("customerId") Integer customerId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateExclusive") LocalDateTime endDateExclusive,
            Pageable pageable
    );
}
