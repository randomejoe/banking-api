package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.services.CustomerService;
import nl.inholland.bankingapi.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("transactions")
public class TransactionController {

    final private TransactionService transactionService;
    final private CustomerService customerService;
    final private TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, CustomerService customerService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.customerService = customerService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("")
    List<TransactionResponse> getAll(@RequestParam(required = false) String iban,
                                     @RequestParam(required = false) TransactionType type,
                                     @RequestParam(required = false) BigDecimal minAmount,
                                     @RequestParam(required = false) BigDecimal maxAmount) {
        return transactionService.getAll(iban, type, minAmount, maxAmount).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @PostMapping("")
    TransactionResponse create(@RequestBody TransactionCreateRequest request) {
        User initiatedBy = customerService.getUserById(request.initiatedByUserId());
        TransactionType type = TransactionType.valueOf(request.type());
        Transaction transaction = transactionService.create(request.fromIban(), request.toIban(), initiatedBy, request.amount(), type, request.description());
        return transactionMapper.toResponse(transaction);
    }
}
