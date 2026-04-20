package com.example.demo.controllers;

import com.example.demo.models.AccountModel;
import com.example.demo.models.UserModel;
import com.example.demo.models.enums.AccountStatus;
import com.example.demo.models.enums.AccountType;
import com.example.demo.services.AccountService;
import com.example.demo.services.AuthService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("accounts")
public class AccountController {

    final private AccountService accountService;
    final private AuthService authService;

    public AccountController(AccountService accountService, AuthService authService) {
        this.accountService = accountService;
        this.authService = authService;
    }

    @GetMapping("")
    List<AccountModel> getAll(@RequestParam(required = false) Integer userId,
                              @RequestParam(required = false) AccountType type,
                              @RequestParam(required = false) AccountStatus status) {
        return accountService.getAll(userId, type, status);
    }

    // must be declared before /{iban} to avoid route collision
    @GetMapping("/search")
    List<Map<String, Object>> searchByName(@RequestParam String name) {
        return authService.getAllCustomers(null, name).stream()
                .flatMap(user -> accountService.getByUserId(user.getId()).stream()
                        .map(account -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("iban", account.getIban());
                            result.put("firstName", user.getFirstName());
                            result.put("lastName", user.getLastName());
                            return (Map<String, Object>) result;
                        }))
                .collect(Collectors.toList());
    }

    @GetMapping("/{iban}")
    Map<String, Object> getByIban(@PathVariable String iban) {
        AccountModel account = accountService.getByIban(iban);
        if (account == null) return null;
        UserModel owner = authService.getUserById(account.getUserId());
        Map<String, Object> ownerMap = new HashMap<>();
        if (owner != null) {
            ownerMap.put("id", owner.getId());
            ownerMap.put("firstName", owner.getFirstName());
            ownerMap.put("lastName", owner.getLastName());
        }
        Map<String, Object> response = new HashMap<>();
        response.put("iban", account.getIban());
        response.put("type", account.getType());
        response.put("balance", account.getBalance());
        response.put("status", account.getStatus());
        response.put("absoluteTransferLimit", account.getAbsoluteTransferLimit());
        response.put("dailyTransferLimit", account.getDailyTransferLimit());
        response.put("createdAt", account.getCreatedAt());
        response.put("owner", ownerMap);
        return response;
    }

    @PatchMapping("/{iban}/limits")
    AccountModel updateLimits(@PathVariable String iban, @RequestBody Map<String, BigDecimal> body) {
        return accountService.updateLimits(iban, body.get("absoluteTransferLimit"), body.get("dailyTransferLimit"));
    }
}