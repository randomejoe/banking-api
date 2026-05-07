package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.AccountDetailResponse;
import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountSearchResponse;
import nl.inholland.bankingapi.dtos.AccountSummaryResponse;
import nl.inholland.bankingapi.dtos.OwnerSummaryResponse;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);

    AccountSummaryResponse toSummary(Account account);

    @Mapping(source = "user", target = "owner")
    AccountDetailResponse toDetail(Account account);

    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    AccountSearchResponse toSearchResponse(Account account);

    OwnerSummaryResponse toOwnerSummary(User user);
}
