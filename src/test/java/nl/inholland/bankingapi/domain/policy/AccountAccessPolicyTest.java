package nl.inholland.bankingapi.domain.policy;

import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccountAccessPolicyTest {

    private final AccountAccessPolicy accountAccessPolicy = new AccountAccessPolicy();

    @Test
    void effectiveQueryFor_employeeKeepsRequestedFilters() {
        AccountQuery query = query();
        User employee = user(7, UserRole.EMPLOYEE);

        AccountQuery effective = accountAccessPolicy.effectiveQueryFor(employee, query);

        assertEquals(42, effective.getUserId());
        assertEquals(AccountType.CHECKING, effective.getType());
        assertEquals(AccountStatus.ACTIVE, effective.getStatus());
        assertEquals("NL02INHL0000000001", effective.getIban());
        assertEquals("Jane", effective.getName());
    }

    @Test
    void effectiveQueryFor_customerForcesOwnUserIdAndClearsNameSearch() {
        AccountQuery query = query();
        User customer = user(9, UserRole.CUSTOMER);

        AccountQuery effective = accountAccessPolicy.effectiveQueryFor(customer, query);

        assertEquals(9, effective.getUserId());
        assertEquals(AccountType.CHECKING, effective.getType());
        assertEquals(AccountStatus.ACTIVE, effective.getStatus());
        assertEquals("NL02INHL0000000001", effective.getIban());
        assertNull(effective.getName());
    }

    private AccountQuery query() {
        AccountQuery query = new AccountQuery();
        query.setUserId(42);
        query.setType(AccountType.CHECKING);
        query.setStatus(AccountStatus.ACTIVE);
        query.setIban("NL02INHL0000000001");
        query.setName("Jane");
        return query;
    }

    private User user(int id, UserRole role) {
        return new User(id, "user@example.com", "secret", "Test", "User", role, LocalDateTime.now());
    }
}
