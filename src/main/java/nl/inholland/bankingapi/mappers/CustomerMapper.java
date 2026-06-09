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

@Mapper(componentModel = "spring", uses = {AccountMapper.class}, imports = {BigDecimal.class, Account.class})
public interface CustomerMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "customerProfile.status", target = "status")
    @Mapping(source = "createdAt", target = "createdAt")
    CustomerSummaryResponse toSummary(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "customerProfile.bsn", target = "bsn")
    @Mapping(source = "customerProfile.phoneNumber", target = "phoneNumber")
    @Mapping(source = "customerProfile.status", target = "status")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "accounts", target = "accounts")
    @Mapping(target = "totalBalance", expression = "java(user.getAccounts() == null ? BigDecimal.ZERO : user.getAccounts().stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add))")
    CustomerDetailResponse toDetail(User user);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "customerProfile.status", target = "status")
    @Mapping(source = "customerProfile.bsn", target = "bsn")
    @Mapping(source = "customerProfile.phoneNumber", target = "phoneNumber")
    CurrentUserResponse toCurrentUser(User user);

    CustomerProfileResponse toProfile(CustomerProfile profile);
}
