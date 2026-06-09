package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.CustomerDetailResponse;
import nl.inholland.bankingapi.dtos.CustomerProfileResponse;
import nl.inholland.bankingapi.dtos.CustomerSummaryResponse;
import nl.inholland.bankingapi.dtos.CustomerUpdateRequest;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.mappers.CustomerMapper;
import nl.inholland.bankingapi.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("users")
public class UserController extends BaseController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public UserController(CustomerService customerService,
                          CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('EMPLOYEE')")
    Page<CustomerSummaryResponse> getAll(@RequestParam(required = false) CustomerStatus status,
                                         @RequestParam(required = false) String search,
                                         @PageableDefault(size = 20) Pageable pageable) {
        Page<User> users = customerService.getAllCustomers(status, search, pageable);
        return users.map(customerMapper::toSummary);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    CustomerDetailResponse getById(@PathVariable int id) {
        User user = customerService.getCustomerUserById(id);
        return customerMapper.toDetail(user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    CustomerProfileResponse update(@PathVariable int id, @RequestBody @Valid CustomerUpdateRequest request) {
        CustomerProfile profile = customerService.updateCustomer(id, request);
        return customerMapper.toProfile(profile);
    }
}
