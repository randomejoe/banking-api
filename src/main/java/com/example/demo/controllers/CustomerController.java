package com.example.demo.controllers;

import com.example.demo.dtos.CustomerDetailResponse;
import com.example.demo.dtos.CustomerProfileResponse;
import com.example.demo.dtos.CustomerSummaryResponse;
import com.example.demo.dtos.CustomerUpdateRequest;
import com.example.demo.dtos.TransactionResponse;
import com.example.demo.entities.Account;
import com.example.demo.entities.CustomerProfile;
import com.example.demo.entities.Transaction;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.CustomerStatus;
import com.example.demo.entities.enums.TransactionType;
import com.example.demo.mappers.CustomerMapper;
import com.example.demo.mappers.TransactionMapper;
import com.example.demo.services.AccountService;
import com.example.demo.services.AuthService;
import com.example.demo.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("customers")
public class CustomerController {

    final private AuthService authService;
    final private AccountService accountService;
    final private TransactionService transactionService;
    final private CustomerMapper customerMapper;
    final private TransactionMapper transactionMapper;

    public CustomerController(AuthService authService, AccountService accountService, TransactionService transactionService, CustomerMapper customerMapper, TransactionMapper transactionMapper) {
        this.authService = authService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.customerMapper = customerMapper;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("")
    List<CustomerSummaryResponse> getAll(@RequestParam(required = false) CustomerStatus status,
                                         @RequestParam(required = false) String search) {
        return authService.getAllCustomers(status, search).stream()
                .map(user -> customerMapper.toSummary(user, authService.getProfileByUserId(user.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    CustomerDetailResponse getById(@PathVariable int id) {
        User user = authService.getUserById(id);
        if (user == null) return null;
        CustomerProfile profile = authService.getProfileByUserId(id);
        List<Account> accounts = accountService.getByUserId(id);
        return customerMapper.toDetail(user, profile, accounts);
    }

    @PatchMapping("/{id}")
    CustomerProfileResponse update(@PathVariable int id, @RequestBody CustomerUpdateRequest request) {
        CustomerStatus status = request.status() != null ? CustomerStatus.valueOf(request.status()) : null;
        CustomerProfile profile = authService.updateCustomer(id, status, request.firstName(), request.lastName(), request.phoneNumber());
        return customerMapper.toProfile(profile);
    }

    @GetMapping("/{id}/transactions")
    List<TransactionResponse> getTransactions(@PathVariable int id,
                                              @RequestParam(required = false) TransactionType type,
                                              @RequestParam(required = false) BigDecimal minAmount,
                                              @RequestParam(required = false) BigDecimal maxAmount) {
        List<String> ibans = accountService.getByUserId(id).stream()
                .map(Account::getIban)
                .toList();
        return transactionService.getByIbans(ibans, type, minAmount, maxAmount).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }
}
