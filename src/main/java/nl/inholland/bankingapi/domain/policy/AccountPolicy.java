package nl.inholland.bankingapi.domain.policy;

import nl.inholland.bankingapi.entities.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountPolicy {

    public void enforceCanClose(Account account) {
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Cannot close account with non-zero balance");
        }
    }

    public void enforceValidLimits(BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit) {
        if (absoluteTransferLimit != null && absoluteTransferLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("absoluteTransferLimit must be >= 0");
        }
        if (dailyTransferLimit != null && dailyTransferLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("dailyTransferLimit must be >= 0");
        }
    }
}
