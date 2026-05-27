package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.domain.policy.CustomerStatusPolicy;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final CustomerStatusPolicy customerStatusPolicy;

    public TransactionController(TransactionService transactionService,
                                 TransactionMapper transactionMapper,
                                 CustomerStatusPolicy customerStatusPolicy) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
        this.customerStatusPolicy = customerStatusPolicy;
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
    Page<TransactionResponse> getAll(@ModelAttribute TransactionFilterParams filters,
                                     @PageableDefault(size = 20) Pageable pageable) {
        User currentUser = currentUser();
        customerStatusPolicy.enforceActiveCustomer(currentUser);
        if (currentUser.getRole() != UserRole.EMPLOYEE) {
            filters.setCustomerId(currentUser.getId());
        }

        return transactionService.getAll(filters, pageable)
                .map(transactionMapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
    TransactionResponse getById(@PathVariable int id) {
        User currentUser = currentUser();
        customerStatusPolicy.enforceActiveCustomer(currentUser);
        Transaction transaction = transactionService.getById(id);
        if (currentUser.getRole() != UserRole.EMPLOYEE
                && transaction.getInitiatedBy().getId() != currentUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return transactionMapper.toResponse(transaction);
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse create(@RequestBody @Valid TransactionCreateRequest request) {
        User currentUser = currentUser();
        customerStatusPolicy.enforceActiveCustomer(currentUser);
        return transactionMapper.toResponse(transactionService.create(request, currentUser));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return user;
    }
}
