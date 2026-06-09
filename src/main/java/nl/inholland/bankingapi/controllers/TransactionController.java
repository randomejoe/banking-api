package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionFilterParams;
import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("transactions")
public class TransactionController extends BaseController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService,
                                 TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('EMPLOYEE') or (hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication))")
    Page<TransactionResponse> getAll(@ModelAttribute TransactionFilterParams filters,
                                     @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = currentUser();
        if (user.getRole() != UserRole.EMPLOYEE) {
            // Lock the filter to the caller's own identity so customers cannot
            // manipulate the customerId parameter to fetch other people's transactions.
            filters.setCustomerId(user.getId());
        }
        return transactionService.getAll(filters, pageable)
                .map(transactionMapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') or (hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication))")
    TransactionResponse getById(@PathVariable int id) {
        User user = currentUser();
        Transaction transaction = transactionService.getById(id);
        if (user.getRole() != UserRole.EMPLOYEE) {
            // Allows viewing if the customer initiated the transaction OR owns the fromIban/toIban.
            transactionService.assertCustomerCanView(transaction, user);
        }
        return transactionMapper.toResponse(transaction);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('EMPLOYEE') or (hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication))")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse create(@RequestBody @Valid TransactionCreateRequest request) {
        return transactionMapper.toResponse(transactionService.create(request, currentUser()));
    }
}
