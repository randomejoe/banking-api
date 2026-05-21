package nl.inholland.bankingapi.repositories;

import jakarta.persistence.criteria.Predicate;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.entities.Account;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AccountSpecification {

    private AccountSpecification() {}

    public static Specification<Account> fromQuery(AccountQuery query) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getUserId() != null)
                predicates.add(cb.equal(root.get("user").get("id"), query.getUserId()));
            if (query.getType() != null)
                predicates.add(cb.equal(root.get("type"), query.getType()));
            if (query.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            if (query.getIban() != null)
                predicates.add(cb.equal(root.get("iban"), query.getIban()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
