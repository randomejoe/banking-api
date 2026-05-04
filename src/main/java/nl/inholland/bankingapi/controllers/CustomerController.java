package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.CustomerDetailResponse;
import nl.inholland.bankingapi.dtos.CustomerProfileResponse;
import nl.inholland.bankingapi.dtos.CustomerSummaryResponse;
import nl.inholland.bankingapi.dtos.CustomerUpdateRequest;
import nl.inholland.bankingapi.dtos.TransactionResponse;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.mappers.CustomerMapper;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.services.AccountService;
import nl.inholland.bankingapi.services.CustomerService;
import nl.inholland.bankingapi.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("customers")
public class CustomerController {

    final private CustomerService customerService;
    final private AccountService accountService;
    final private TransactionService transactionService;
    final private CustomerMapper customerMapper;
    final private TransactionMapper transactionMapper;

    public CustomerController(CustomerService customerService, AccountService accountService, TransactionService transactionService, CustomerMapper customerMapper, TransactionMapper transactionMapper) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.customerMapper = customerMapper;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("")
    List<CustomerSummaryResponse> getAll(@RequestParam(required = false) CustomerStatus status,
                                         @RequestParam(required = false) String search) {
        return customerService.getAllCustomers(status, search).stream()
                .map(user -> customerMapper.toSummary(user, customerService.getProfileByUserId(user.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    CustomerDetailResponse getById(@PathVariable int id) {
        User user = customerService.getUserById(id);
        if (user == null) return null;
        CustomerProfile profile = customerService.getProfileByUserId(id);
        List<Account> accounts = accountService.getByUserId(id);
        return customerMapper.toDetail(user, profile, accounts);
    }

    @PatchMapping("/{id}")
    CustomerProfileResponse update(@PathVariable int id, @RequestBody CustomerUpdateRequest request) {
        CustomerStatus status = request.status() != null ? CustomerStatus.valueOf(request.status()) : null;
        CustomerProfile profile = customerService.updateCustomer(id, status, request.firstName(), request.lastName(), request.phoneNumber());
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
