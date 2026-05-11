package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionFilterParams;
import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService,
                                 TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("")
    Page<TransactionResponse> getAll(@ModelAttribute TransactionFilterParams filters,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return transactionService.getAll(filters, pageable)
                .map(transactionMapper::toResponse);
    }

    //added missing get
    @GetMapping("/{id}")
    TransactionResponse getById(@PathVariable int id) {
        return transactionMapper.toResponse(transactionService.getById(id));
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse create(@RequestBody @Valid TransactionCreateRequest request) {
        return transactionMapper.toResponse(transactionService.create(request));
    }
}
