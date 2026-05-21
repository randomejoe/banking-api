package nl.inholland.bankingapi.repositories;

import nl.inholland.bankingapi.dtos.TransactionFilterParams;
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
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    // Used by the daily transfer limit check in TransactionPolicy
    List<Transaction> findByFromIbanAndTypeAndTimestampGreaterThanEqual(
            String iban, TransactionType type, LocalDateTime since);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:iban IS NULL OR t.fromIban = :iban OR t.toIban = :iban) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:customerId IS NULL OR t.initiatedBy.id = :customerId)")
    Page<Transaction> findAllFiltered(
            @Param("iban") String iban,
            @Param("type") TransactionType type,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("customerId") Integer customerId,
            Pageable pageable
    );

    default Page<Transaction> findAllFiltered(TransactionFilterParams filters, Pageable pageable) {
        return findAllFiltered(
                filters.getIban(),
                filters.getType(),
                filters.getMinAmount(),
                filters.getMaxAmount(),
                filters.getCustomerId(),
                pageable
        );
    }
}
