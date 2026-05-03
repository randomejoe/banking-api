package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.AccountDetailResponse;
import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountSearchResponse;
import nl.inholland.bankingapi.dtos.AccountSummaryResponse;
import nl.inholland.bankingapi.dtos.OwnerSummaryResponse;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        if (account == null) return null;
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getIban(),
                account.getType(),
                account.getBalance(),
                account.getAbsoluteTransferLimit(),
                account.getDailyTransferLimit(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public AccountSummaryResponse toSummary(Account account) {
        if (account == null) return null;
        return new AccountSummaryResponse(
                account.getIban(),
                account.getType(),
                account.getBalance(),
                account.getStatus()
        );
    }

    public AccountDetailResponse toDetail(Account account, User owner) {
        if (account == null) return null;
        OwnerSummaryResponse ownerSummary = owner == null ? null : new OwnerSummaryResponse(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName()
        );
        return new AccountDetailResponse(
                account.getIban(),
                account.getType(),
                account.getBalance(),
                account.getStatus(),
                account.getAbsoluteTransferLimit(),
                account.getDailyTransferLimit(),
                account.getCreatedAt(),
                ownerSummary
        );
    }

    public AccountSearchResponse toSearchResponse(Account account, User owner) {
        if (account == null || owner == null) return null;
        return new AccountSearchResponse(
                account.getIban(),
                owner.getFirstName(),
                owner.getLastName()
        );
    }
}
