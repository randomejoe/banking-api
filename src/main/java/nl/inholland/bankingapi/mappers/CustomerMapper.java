package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.CurrentUserResponse;
import nl.inholland.bankingapi.dtos.CustomerDetailResponse;
import nl.inholland.bankingapi.dtos.CustomerProfileResponse;
import nl.inholland.bankingapi.dtos.CustomerSummaryResponse;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", uses = {AccountMapper.class}, imports = {BigDecimal.class, Account.class})
public interface CustomerMapper {

    @Mapping(source = "user.id", target = "id")
    CustomerSummaryResponse toSummary(User user, CustomerProfile profile);

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "accounts", target = "accounts")
    @Mapping(target = "totalBalance", expression = "java(accounts == null ? BigDecimal.ZERO : accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add))")
    CustomerDetailResponse toDetail(User user, CustomerProfile profile, List<Account> accounts);

    @Mapping(source = "user.id", target = "id")
    CurrentUserResponse toCurrentUser(User user, CustomerProfile profile);

    CustomerProfileResponse toProfile(CustomerProfile profile);
}
