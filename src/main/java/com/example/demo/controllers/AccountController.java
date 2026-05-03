package com.example.demo.controllers;

import com.example.demo.dtos.AccountDetailResponse;
import com.example.demo.dtos.AccountLimitUpdateRequest;
import com.example.demo.dtos.AccountResponse;
import com.example.demo.dtos.AccountSearchResponse;
import com.example.demo.entities.Account;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.AccountStatus;
import com.example.demo.entities.enums.AccountType;
import com.example.demo.mappers.AccountMapper;
import com.example.demo.services.AccountService;
import com.example.demo.services.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("accounts")
public class AccountController {

    final private AccountService accountService;
    final private AuthService authService;
    final private AccountMapper accountMapper;

    public AccountController(AccountService accountService, AuthService authService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.authService = authService;
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
        return authService.getAllCustomers(null, name).stream()
                .flatMap(user -> accountService.getByUserId(user.getId()).stream()
                        .map(account -> accountMapper.toSearchResponse(account, user)))
                .toList();
    }

    @GetMapping("/{iban}")
    AccountDetailResponse getByIban(@PathVariable String iban) {
        Account account = accountService.getByIban(iban);
        if (account == null) return null;
        User owner = authService.getUserById(account.getUserId());
        return accountMapper.toDetail(account, owner);
    }

    @PatchMapping("/{iban}/limits")
    AccountResponse updateLimits(@PathVariable String iban, @RequestBody AccountLimitUpdateRequest request) {
        Account account = accountService.updateLimits(iban, request.absoluteTransferLimit(), request.dailyTransferLimit());
        return accountMapper.toResponse(account);
    }
}
