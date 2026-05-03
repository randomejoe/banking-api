package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.entities.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) return null;
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromIban(),
                transaction.getToIban(),
                transaction.getInitiatedByUserId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getTimestamp()
        );
    }
}
