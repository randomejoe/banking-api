package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.entities.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionResponse toResponse(Transaction transaction);
}
