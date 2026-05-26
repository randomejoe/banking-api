package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.domain.policy.CustomerStatusPolicy;
import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountListItemResponse;
import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountSearchResponse;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.UserRole;
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
    private final CustomerStatusPolicy customerStatusPolicy;

    public AccountController(AccountService accountService, AccountMapper accountMapper,
                             CustomerStatusPolicy customerStatusPolicy) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.customerStatusPolicy = customerStatusPolicy;
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
    Page<AccountListItemResponse> getAll(@ModelAttribute AccountQuery query,
                                         @PageableDefault(size = 20) Pageable pageable) {
        User current = currentUser();
        customerStatusPolicy.enforceActiveCustomer(current);
        if (isCustomerPublicLookup(current, query)) {
            return accountService.searchTransferTargets(current, query.getName(), pageable)
                    .map(account -> (AccountListItemResponse) new AccountSearchResponse(
                            account.getIban(),
                            account.getUser().getFirstName(),
                            account.getUser().getLastName()));
        }
        return accountService.getAllForUser(current, query, pageable)
                .map(account -> (AccountListItemResponse) accountMapper.toResponse(account));
    }

    private boolean isCustomerPublicLookup(User current, AccountQuery query) {
        return current.getRole() == UserRole.CUSTOMER
                && query.getName() != null
                && !query.getName().isBlank();
    }

    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    AccountResponse updateAccount(@PathVariable String iban, @RequestBody @Valid AccountUpdateRequest request) {
        return accountMapper.toResponse(accountService.updateAccount(iban, request));
    }
}
