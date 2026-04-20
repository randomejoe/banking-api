package com.example.demo.controllers;

import com.example.demo.models.AccountModel;
import com.example.demo.models.CustomerProfileModel;
import com.example.demo.models.TransactionModel;
import com.example.demo.models.UserModel;
import com.example.demo.models.enums.CustomerStatus;
import com.example.demo.models.enums.TransactionType;
import com.example.demo.services.AccountService;
import com.example.demo.services.AuthService;
import com.example.demo.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("customers")
public class CustomerController {

    final private AuthService authService;
    final private AccountService accountService;
    final private TransactionService transactionService;

    public CustomerController(AuthService authService, AccountService accountService, TransactionService transactionService) {
        this.authService = authService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("")
    List<UserModel> getAll(@RequestParam(required = false) CustomerStatus status,
                           @RequestParam(required = false) String search) {
        return authService.getAllCustomers(status, search);
    }

    @GetMapping("/{id}")
    Map<String, Object> getById(@PathVariable int id) {
        UserModel user = authService.getUserById(id);
        if (user == null) return null;
        CustomerProfileModel profile = authService.getProfileByUserId(id);
        List<AccountModel> accounts = accountService.getByUserId(id);
        BigDecimal totalBalance = accounts.stream()
                .map(AccountModel::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("bsn", profile != null ? profile.getBsn() : null);
        response.put("phoneNumber", profile != null ? profile.getPhoneNumber() : null);
        response.put("status", profile != null ? profile.getStatus() : null);
        response.put("createdAt", user.getCreatedAt());
        response.put("totalBalance", totalBalance);
        response.put("accounts", accounts);
        return response;
    }

    @PatchMapping("/{id}")
    CustomerProfileModel update(@PathVariable int id, @RequestBody Map<String, String> body) {
        CustomerStatus status = body.get("status") != null ? CustomerStatus.valueOf(body.get("status")) : null;
        CustomerProfileModel profile = authService.updateCustomer(id, status, body.get("firstName"), body.get("lastName"), body.get("phoneNumber"));
        if (status == CustomerStatus.ACTIVE && accountService.getByUserId(id).isEmpty()) {
            accountService.createAccountsForUser(id);
        }
        return profile;
    }

    @GetMapping("/{id}/transactions")
    List<TransactionModel> getTransactions(@PathVariable int id,
                                           @RequestParam(required = false) TransactionType type,
                                           @RequestParam(required = false) BigDecimal minAmount,
                                           @RequestParam(required = false) BigDecimal maxAmount) {
        List<String> ibans = accountService.getByUserId(id).stream()
                .map(AccountModel::getIban)
                .collect(Collectors.toList());
        return transactionService.getByIbans(ibans);
    }
}