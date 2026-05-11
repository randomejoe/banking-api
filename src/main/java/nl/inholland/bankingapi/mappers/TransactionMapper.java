package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


//using mapstruct instead now
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "initiatedBy.id", target = "initiatedByUserId")
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "initiatedBy", ignore = true)
    @Mapping(target = "timestamp",   ignore = true)
    Transaction toEntity(TransactionCreateRequest request);
}
