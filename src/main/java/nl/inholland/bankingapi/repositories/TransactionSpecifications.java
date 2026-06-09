package nl.inholland.bankingapi.repositories;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import nl.inholland.bankingapi.dtos.TransactionFilterParams;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSpecifications {

    private TransactionSpecifications() {}

    public static Specification<Transaction> fromFilters(TransactionFilterParams filters) {
        // Start with an always-true base so the result is never null when all filters are empty.
        Specification<Transaction> spec = (root, q, cb) -> cb.conjunction();
        spec = andIfPresent(spec, withIban(filters.getIban()));
        spec = andIfPresent(spec, withType(filters.getType()));
        spec = andIfPresent(spec, withMinAmount(filters.getMinAmount()));
        spec = andIfPresent(spec, withMaxAmount(filters.getMaxAmount()));
        spec = andIfPresent(spec, visibleToCustomer(filters.getCustomerId()));
        spec = andIfPresent(spec, withStartDate(filters.getStartDate()));
        spec = andIfPresent(spec, withEndDate(filters.getEndDate()));
        return spec;
    }

    private static Specification<Transaction> andIfPresent(
            Specification<Transaction> base, Specification<Transaction> clause) {
        return clause == null ? base : base.and(clause);
    }

    private static Specification<Transaction> withIban(String iban) {
        if (iban == null || iban.isBlank()) return null;
        return (root, q, cb) -> {
            String pattern = "%" + iban.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fromIban")), pattern),
                    cb.like(cb.lower(root.get("toIban")), pattern));
        };
    }

    private static Specification<Transaction> withType(TransactionType type) {
        if (type == null) return null;
        return (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> withMinAmount(BigDecimal min) {
        if (min == null) return null;
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    private static Specification<Transaction> withMaxAmount(BigDecimal max) {
        if (max == null) return null;
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    /**
     * Matches transactions the customer initiated OR where they own the fromIban/toIban account.
     * This fixes the bug where a recipient (e.g. Charlie) could not see an incoming transfer.
     */
    private static Specification<Transaction> visibleToCustomer(Integer customerId) {
        if (customerId == null) return null;
        return (root, query, cb) -> {
            Subquery<String> ownedIbans = query.subquery(String.class);
            Root<Account> acc = ownedIbans.from(Account.class);
            ownedIbans.select(acc.get("iban"))
                    .where(cb.equal(acc.get("user").get("id"), customerId));
            return cb.or(
                    cb.equal(root.get("initiatedBy").get("id"), customerId),
                    root.get("fromIban").in(ownedIbans),
                    root.get("toIban").in(ownedIbans));
        };
    }

    private static Specification<Transaction> withStartDate(java.time.LocalDate startDate) {
        if (startDate == null) return null;
        LocalDateTime start = startDate.atStartOfDay();
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), start);
    }

    private static Specification<Transaction> withEndDate(java.time.LocalDate endDate) {
        if (endDate == null) return null;
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        return (root, q, cb) -> cb.lessThan(root.get("timestamp"), endExclusive);
    }
}
