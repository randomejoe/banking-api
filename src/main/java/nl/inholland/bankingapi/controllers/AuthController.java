package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.CurrentUserResponse;
import nl.inholland.bankingapi.dtos.LoginRequest;
import nl.inholland.bankingapi.dtos.LoginResponse;
import nl.inholland.bankingapi.dtos.RegisterRequest;
import nl.inholland.bankingapi.dtos.TokenResponse;
import nl.inholland.bankingapi.dtos.UserResponse;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.mappers.CustomerMapper;
import nl.inholland.bankingapi.mappers.UserMapper;
import nl.inholland.bankingapi.services.AuthService;
import nl.inholland.bankingapi.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("auth")
public class AuthController {

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
    UserResponse register(@RequestBody @Valid RegisterRequest request) {
        User user = authService.register(request);
        return userMapper.toResponse(user);
    }

    @PostMapping("/login")
    LoginResponse login(@RequestBody @Valid LoginRequest request) {
        User user = authService.login(request.email(), request.password());
        CustomerProfile profile = customerService.getProfileByUserId(user.getId());
        String accessToken = authService.generateToken(user);
        long expirationSeconds = authService.getTokenExpirationMs() / 1000;
        TokenResponse token = new TokenResponse(accessToken, expirationSeconds, "Bearer");
        CurrentUserResponse userInfo = customerMapper.toCurrentUser(user, profile);
        return new LoginResponse(token, userInfo);
    }

    @GetMapping("/me")
    CurrentUserResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        CustomerProfile profile = customerService.getProfileByUserId(user.getId());
        return customerMapper.toCurrentUser(user, profile);
    }
}
