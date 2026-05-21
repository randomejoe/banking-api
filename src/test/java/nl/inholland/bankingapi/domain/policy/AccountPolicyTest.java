package nl.inholland.bankingapi.domain.policy;

import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountPolicyTest {

    private AccountPolicy accountPolicy;
    private Account account;

    @BeforeEach
    void setUp() {
        accountPolicy = new AccountPolicy();

        account = new Account();
        account.setIban("NL95INHL0000000001");
        account.setType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("100.00"));
        account.setAbsoluteTransferLimit(BigDecimal.ZERO);
        account.setDailyTransferLimit(new BigDecimal("500.00"));
    }

    // --- enforceCanClose ---

    @Test
    void enforceCanClose_zeroBalance_doesNotThrow() {
        account.setBalance(BigDecimal.ZERO);
        assertDoesNotThrow(() -> accountPolicy.enforceCanClose(account));
    }

    @Test
    void enforceCanClose_positiveBalance_throwsIllegalArgument() {
        account.setBalance(new BigDecimal("100.00"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountPolicy.enforceCanClose(account));
        assertTrue(ex.getMessage().contains("Cannot close account with non-zero balance"));
    }

    @Test
    void enforceCanClose_negativeBalance_throwsIllegalArgument() {
        account.setBalance(new BigDecimal("-50.00"));
        assertThrows(IllegalArgumentException.class, () -> accountPolicy.enforceCanClose(account));
    }

    // --- enforceValidLimits ---

    @Test
    void enforceValidLimits_bothNull_doesNotThrow() {
        // null means "don't change this field" which is fine for a PATCH
        assertDoesNotThrow(() -> accountPolicy.enforceValidLimits(null, null));
    }

    @Test
    void enforceValidLimits_bothZero_doesNotThrow() {
        assertDoesNotThrow(() -> accountPolicy.enforceValidLimits(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void enforceValidLimits_positiveValues_doesNotThrow() {
        assertDoesNotThrow(() -> accountPolicy.enforceValidLimits(
                new BigDecimal("500.00"), new BigDecimal("2000.00")));
    }

    @Test
    void enforceValidLimits_negativeAbsoluteLimit_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountPolicy.enforceValidLimits(new BigDecimal("-1.00"), null));
        assertTrue(ex.getMessage().contains("absoluteTransferLimit"));
    }

    @Test
    void enforceValidLimits_negativeDailyLimit_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountPolicy.enforceValidLimits(null, new BigDecimal("-0.01")));
        assertTrue(ex.getMessage().contains("dailyTransferLimit"));
    }

    @Test
    void enforceValidLimits_bothNegative_throwsOnFirstViolation() {
        // absolute limit is validated first, so that's the one that throws
        assertThrows(IllegalArgumentException.class,
                () -> accountPolicy.enforceValidLimits(new BigDecimal("-10"), new BigDecimal("-10")));
    }
}
