package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.AccountQuery;
import nl.inholland.bankingapi.dtos.AccountUpdateRequest;
import nl.inholland.bankingapi.dtos.EmployeeAccountResponse;
import nl.inholland.bankingapi.dtos.OwnAccountResponse;
import nl.inholland.bankingapi.dtos.TransferTargetResponse;
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
    @PreAuthorize("hasRole('EMPLOYEE')")
    Page<EmployeeAccountResponse> getAll(@ModelAttribute AccountQuery query,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return accountService.getAll(query, pageable)
                .map(accountMapper::toEmployeeResponse);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication)")
    Page<OwnAccountResponse> getOwnAccounts(@PageableDefault(size = 20) Pageable pageable) {
        User current = currentUser();
        return accountService.getOwnAccounts(current.getId(), pageable)
                .map(accountMapper::toOwnResponse);
    }

    @GetMapping("/transfer-targets")
    @PreAuthorize("hasRole('CUSTOMER') and @customerSecurity.isActiveCustomer(authentication)")
    Page<TransferTargetResponse> searchTransferTargets(@RequestParam String name,
                                                       @PageableDefault(size = 20) Pageable pageable) {
        User current = currentUser();
        return accountService.searchTransferTargets(current.getId(), name, pageable)
                .map(accountMapper::toTransferTargetResponse);
    }

    @PatchMapping("/{iban}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    EmployeeAccountResponse updateAccount(@PathVariable String iban, @RequestBody @Valid AccountUpdateRequest request) {
        return accountMapper.toEmployeeResponse(accountService.updateAccount(iban, request));
    }
}
