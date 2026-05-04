package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.CurrentUserResponse;
import nl.inholland.bankingapi.dtos.LoginRequest;
import nl.inholland.bankingapi.dtos.LoginResponse;
import nl.inholland.bankingapi.dtos.RegisterRequest;
import nl.inholland.bankingapi.dtos.UserResponse;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.mappers.CustomerMapper;
import nl.inholland.bankingapi.mappers.UserMapper;
import nl.inholland.bankingapi.services.AuthService;
import nl.inholland.bankingapi.services.CustomerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class  AuthController {

    final private AuthService authService;
    final private CustomerService customerService;
    final private UserMapper userMapper;
    final private CustomerMapper customerMapper;

    public AuthController(AuthService authService, CustomerService customerService, UserMapper userMapper, CustomerMapper customerMapper) {
        this.authService = authService;
        this.customerService = customerService;
        this.userMapper = userMapper;
        this.customerMapper = customerMapper;
    }

    @PostMapping("/register")
    UserResponse register(@RequestBody RegisterRequest request) {
        User user = authService.register(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.bsn(),
                request.phoneNumber()
        );
        return userMapper.toResponse(user);
    }

    @PostMapping("/login")
    LoginResponse login(@RequestBody LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        if (user == null) return null;
        CustomerProfile profile = customerService.getProfileByUserId(user.getId());
        return customerMapper.toLogin(user, profile);
    }

    @GetMapping("/me")
    CurrentUserResponse me(@RequestParam int userId) {
        User user = customerService.getUserById(userId);
        if (user == null) return null;
        CustomerProfile profile = customerService.getProfileByUserId(userId);
        return customerMapper.toCurrentUser(user, profile);
    }
}
