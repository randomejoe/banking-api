package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.CustomerDetailResponse;
import nl.inholland.bankingapi.dtos.CustomerProfileResponse;
import nl.inholland.bankingapi.dtos.CustomerSummaryResponse;
import nl.inholland.bankingapi.dtos.CustomerUpdateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.mappers.CustomerMapper;
import nl.inholland.bankingapi.services.AccountService;
import nl.inholland.bankingapi.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;
    private final CustomerMapper customerMapper;

    public CustomerController(CustomerService customerService,
                              AccountService accountService,
                              CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.accountService = accountService;
        this.customerMapper = customerMapper;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('EMPLOYEE')")
    List<CustomerSummaryResponse> getAll(@RequestParam(required = false) CustomerStatus status,
                                         @RequestParam(required = false) String search) {
        return customerService.getAllCustomers(status, search).stream()
                .map(user -> customerMapper.toSummary(user, customerService.getProfileByUserId(user.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    CustomerDetailResponse getById(@PathVariable int id) {
        User user = customerService.getUserById(id);
        CustomerProfile profile = customerService.getProfileByUserId(id);
        List<Account> accounts = accountService.getByUserId(id);
        return customerMapper.toDetail(user, profile, accounts);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    CustomerProfileResponse update(@PathVariable int id, @RequestBody @Valid CustomerUpdateRequest request) {
        CustomerStatus status = request.status() != null ? CustomerStatus.valueOf(request.status()) : null;
        CustomerProfile profile = customerService.updateCustomer(id, status, request.firstName(), request.lastName(), request.phoneNumber(), request.absoluteTransferLimit(), request.dailyTransferLimit());
        return customerMapper.toProfile(profile);
    }
}
