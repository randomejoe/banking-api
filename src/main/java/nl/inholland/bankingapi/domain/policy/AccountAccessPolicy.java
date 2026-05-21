package nl.inholland.bankingapi.domain.policy;

import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class AccountAccessPolicy {

    public AccountQuery effectiveQueryFor(User currentUser, AccountQuery query) {
        AccountQuery effective = new AccountQuery();
        effective.setUserId(query.getUserId());
        effective.setType(query.getType());
        effective.setStatus(query.getStatus());
        effective.setIban(query.getIban());
        effective.setName(query.getName());

        if (currentUser.getRole() != UserRole.EMPLOYEE) {
            effective.setUserId(currentUser.getId());
            effective.setName(null);
        }

        return effective;
    }
}
