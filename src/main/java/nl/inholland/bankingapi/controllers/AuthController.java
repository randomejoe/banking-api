package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.dtos.CurrentUserResponse;
import nl.inholland.bankingapi.dtos.LoginRequest;
import nl.inholland.bankingapi.dtos.LoginResponse;
import nl.inholland.bankingapi.dtos.RegisterRequest;
import nl.inholland.bankingapi.dtos.TokenResponse;
import nl.inholland.bankingapi.dtos.UserResponse;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.mappers.CustomerMapper;
import nl.inholland.bankingapi.mappers.UserMapper;
import nl.inholland.bankingapi.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class AuthController extends BaseController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;

    public AuthController(AuthService authService,
                          UserMapper userMapper, CustomerMapper customerMapper) {
        this.authService = authService;
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
        String accessToken = authService.generateToken(user);
        long expirationSeconds = authService.getTokenExpirationMs() / 1000;
        TokenResponse token = new TokenResponse(accessToken, expirationSeconds, "Bearer");
        CurrentUserResponse userInfo = customerMapper.toCurrentUser(user);
        return new LoginResponse(token, userInfo);
    }

    @GetMapping("/me")
    CurrentUserResponse me() {
        User user = currentUser();
        return customerMapper.toCurrentUser(user);
    }
}
