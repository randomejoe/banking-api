package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountSummaryResponse;
import nl.inholland.bankingapi.entities.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);

    AccountSummaryResponse toSummary(Account account);
}
