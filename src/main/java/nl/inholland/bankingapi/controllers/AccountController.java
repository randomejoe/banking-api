package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.mappers.AccountMapper;
import nl.inholland.bankingapi.services.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("accounts")
public class AccountController extends BaseController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    public AccountController(AccountService accountService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
    Page<AccountResponse> getAll(@ModelAttribute AccountQuery query,
                                 @PageableDefault(size = 20) Pageable pageable) {
        User current = currentUser();
        return accountService.getAllForUser(current, query, pageable).map(accountMapper::toResponse);
    }

    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    AccountResponse updateAccount(@PathVariable String iban, @RequestBody @Valid AccountUpdateRequest request) {
        return accountMapper.toResponse(accountService.updateAccount(iban, request));
    }
}
