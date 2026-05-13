package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.mappers.AccountMapper;
import nl.inholland.bankingapi.services.AccountService;
import nl.inholland.bankingapi.services.CustomerService;
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

import java.util.List;

@RestController
@RequestMapping("accounts")
public class AccountController {

    private final AccountService accountService;
    private final CustomerService customerService;
    private final AccountMapper accountMapper;

    public AccountController(AccountService accountService, CustomerService customerService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.customerService = customerService;
        this.accountMapper = accountMapper;
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
    Page<AccountResponse> getAll(@RequestParam(required = false) Integer userId,
                                 @RequestParam(required = false) AccountType type,
                                 @RequestParam(required = false) AccountStatus status,
                                 @RequestParam(required = false) String iban,
                                 @RequestParam(required = false) String name,
                                 @PageableDefault(size = 20) Pageable pageable) {
        User currentUser = currentUser();
        if (currentUser.getRole() != UserRole.EMPLOYEE) {
            return accountService.getAll(currentUser.getId(), type, status, iban, pageable)
                    .map(accountMapper::toResponse);
        }

        if (name != null) {
            List<Integer> userIds = customerService.getAllCustomers(null, name).stream()
                    .map(u -> u.getId())
                    .toList();
            return accountService.getByUserIds(userIds, pageable).map(accountMapper::toResponse);
        }
        return accountService.getAll(userId, type, status, iban, pageable).map(accountMapper::toResponse);
    }

    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    AccountResponse updateAccount(@PathVariable String iban, @RequestBody @Valid AccountUpdateRequest request) {
        return accountMapper.toResponse(
                accountService.updateAccount(iban, request.absoluteTransferLimit(), request.dailyTransferLimit(), request.status())
        );
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return user;
    }
}
