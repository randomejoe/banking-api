package nl.inholland.bankingapi.domain.policy;

import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.exceptions.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Component
public class TransactionPolicy {

    // --- main validation methods (called by TransactionService) ---

    public void enforceTransferPolicy(TransactionCreateRequest request,
                                      Account fromAccount, Account toAccount,
                                      User initiatedBy, BigDecimal outgoingToday) {
        enforceTransferIbansPresent(request.fromIban(), request.toIban());
        enforceNotSameAccount(request.fromIban(), request.toIban());
        enforceAccountIsActive(fromAccount, "Source");
        enforceAccountIsActive(toAccount, "Destination");
        enforceSourceAccountOwner(fromAccount, initiatedBy);
        enforceExternalCustomerTransferTargetsChecking(fromAccount, toAccount, initiatedBy);
        enforceAbsoluteTransferLimit(fromAccount, request.amount());
        enforceDailyTransferLimit(fromAccount, outgoingToday, request.amount());
    }

    public void enforceDepositPolicy(TransactionCreateRequest request, Account toAccount) {
        enforceDepositIbanPresent(request.toIban());
        enforceAccountIsActive(toAccount, "Destination");
    }

    public void enforceWithdrawalPolicy(TransactionCreateRequest request,
                                        Account fromAccount, User initiatedBy,
                                        BigDecimal outgoingToday) {
        enforceWithdrawalIbanPresent(request.fromIban());
        enforceAccountIsActive(fromAccount, "Source");
        enforceSourceAccountOwner(fromAccount, initiatedBy);
        enforceAbsoluteTransferLimit(fromAccount, request.amount());
        enforceDailyTransferLimit(fromAccount, outgoingToday, request.amount());
    }

    // --- individual rules (also tested directly) ---

    public void enforceTransferIbansPresent(String fromIban, String toIban) {
        if (fromIban == null || fromIban.isBlank() || toIban == null || toIban.isBlank()) {
            throw new BadRequestException("TRANSFER requires both fromIban and toIban");
        }
    }

    public void enforceNotSameAccount(String fromIban, String toIban) {
        if (fromIban.equals(toIban)) {
            throw new BadRequestException("Cannot transfer to the same account");
        }
    }

    public void enforceDepositIbanPresent(String toIban) {
        if (toIban == null || toIban.isBlank()) {
            throw new BadRequestException("DEPOSIT requires toIban");
        }
    }

    public void enforceWithdrawalIbanPresent(String fromIban) {
        if (fromIban == null || fromIban.isBlank()) {
            throw new BadRequestException("WITHDRAWAL requires fromIban");
        }
    }

    public void enforceAccountIsActive(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException(label + " account is not active: " + account.getIban());
        }
    }

    public void enforceSourceAccountOwner(Account account, User initiatedBy) {
        if (initiatedBy.getRole() == UserRole.EMPLOYEE) {
            return;
        }
        if (account.getUser().getId() != initiatedBy.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot use another customer's source account");
        }
    }

    public void enforceExternalCustomerTransferTargetsChecking(Account fromAccount, Account toAccount, User initiatedBy) {
        if (initiatedBy.getRole() == UserRole.EMPLOYEE) {
            return;
        }
        if (fromAccount.getUser().getId() == toAccount.getUser().getId()) {
            return;
        }
        if (toAccount.getType() != AccountType.CHECKING) {
            throw new BadRequestException("External transfers must target a checking account");
        }
    }

    public void enforceAbsoluteTransferLimit(Account from, BigDecimal amount) {
        BigDecimal balanceAfter = from.getBalance().subtract(amount);
        if (balanceAfter.compareTo(from.getAbsoluteTransferLimit()) < 0) {
            throw new BadRequestException(
                    "Transfer would breach the absolute transfer limit for account: " + from.getIban());
        }
    }

    /**
     * Pure math check: outgoingToday is pre-computed by TransactionService
     * from the repository so this class stays free of database dependencies.
     */
    public void enforceDailyTransferLimit(Account from, BigDecimal outgoingToday, BigDecimal amount) {
        if (outgoingToday.add(amount).compareTo(from.getDailyTransferLimit()) > 0) {
            throw new BadRequestException(
                    "Transaction would exceed the daily transfer limit for account: " + from.getIban());
        }
    }

    public void enforceUnsupportedType(nl.inholland.bankingapi.entities.enums.TransactionType type) {
        throw new BadRequestException("Unsupported transaction type: " + type);
    }
}
