package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.EmployeeAccountResponse;
import nl.inholland.bankingapi.dtos.OwnAccountResponse;
import nl.inholland.bankingapi.dtos.TransferTargetResponse;
import nl.inholland.bankingapi.entities.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    EmployeeAccountResponse toEmployeeResponse(Account account);

    OwnAccountResponse toOwnResponse(Account account);

    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    TransferTargetResponse toTransferTargetResponse(Account account);
}
