package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.AccountDetailResponse;
import nl.inholland.bankingapi.dtos.AccountLimitUpdateRequest;
import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountSearchResponse;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.AccountType;
import nl.inholland.bankingapi.mappers.AccountMapper;
import nl.inholland.bankingapi.services.AccountService;
import nl.inholland.bankingapi.services.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("accounts")
public class AccountController {

    final private AccountService accountService;
    final private CustomerService customerService;
    final private AccountMapper accountMapper;

    public AccountController(AccountService accountService, CustomerService customerService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.customerService = customerService;
        this.accountMapper = accountMapper;
    }

    @GetMapping("")
    List<AccountResponse> getAll(@RequestParam(required = false) Integer userId,
                                 @RequestParam(required = false) AccountType type,
                                 @RequestParam(required = false) AccountStatus status) {
        return accountService.getAll(userId, type, status).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    // must be declared before /{iban} to avoid route collision
    @GetMapping("/search")
    List<AccountSearchResponse> searchByName(@RequestParam String name) {
        return customerService.getAllCustomers(null, name).stream()
                .flatMap(user -> accountService.getByUserId(user.getId()).stream()
                        .map(account -> accountMapper.toSearchResponse(account, user)))
                .toList();
    }

    @GetMapping("/{iban}")
    AccountDetailResponse getByIban(@PathVariable String iban) {
        Account account = accountService.getByIban(iban);
        if (account == null) return null;
        User owner = customerService.getUserById(account.getUserId());
        return accountMapper.toDetail(account, owner);
    }

    @PatchMapping("/{iban}/limits")
    AccountResponse updateLimits(@PathVariable String iban, @RequestBody AccountLimitUpdateRequest request) {
        Account account = accountService.updateLimits(iban, request.absoluteTransferLimit(), request.dailyTransferLimit());
        return accountMapper.toResponse(account);
    }
}
