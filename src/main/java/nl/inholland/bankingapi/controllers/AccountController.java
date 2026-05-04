package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.AccountDetailResponse;
import nl.inholland.bankingapi.dtos.AccountLimitUpdateRequest;
import nl.inholland.bankingapi.dtos.AccountResponse;
import nl.inholland.bankingapi.dtos.AccountSearchResponse;
import nl.inholland.bankingapi.entities.Account;
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
        List<Integer> userIds = customerService.getAllCustomers(null, name).stream()
                .map(u -> u.getId())
                .toList();
        return accountService.getByUserIds(userIds).stream()
                .map(account -> accountMapper.toSearchResponse(account, account.getUser()))
                .toList();
    }

    @GetMapping("/{iban}")
    AccountDetailResponse getByIban(@PathVariable String iban) {
        Account account = accountService.getByIban(iban);
        return accountMapper.toDetail(account, account.getUser());
    }

    @PatchMapping("/{iban}/limits")
    AccountResponse updateLimits(@PathVariable String iban, @RequestBody AccountLimitUpdateRequest request) {
        return accountMapper.toResponse(accountService.updateLimits(iban, request.absoluteTransferLimit(), request.dailyTransferLimit()));
    }
}
