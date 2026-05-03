package com.example.demo.controllers;

import com.example.demo.dtos.CurrentUserResponse;
import com.example.demo.dtos.LoginRequest;
import com.example.demo.dtos.LoginResponse;
import com.example.demo.dtos.RegisterRequest;
import com.example.demo.dtos.UserResponse;
import com.example.demo.entities.CustomerProfile;
import com.example.demo.entities.User;
import com.example.demo.mappers.CustomerMapper;
import com.example.demo.mappers.UserMapper;
import com.example.demo.services.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class AuthController {

    final private AuthService authService;
    final private UserMapper userMapper;
    final private CustomerMapper customerMapper;

    public AuthController(AuthService authService, UserMapper userMapper, CustomerMapper customerMapper) {
        this.authService = authService;
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
        CustomerProfile profile = authService.getProfileByUserId(user.getId());
        return customerMapper.toLogin(user, profile);
    }

    @GetMapping("/me")
    CurrentUserResponse me(@RequestParam int userId) {
        User user = authService.getUserById(userId);
        if (user == null) return null;
        CustomerProfile profile = authService.getProfileByUserId(userId);
        return customerMapper.toCurrentUser(user, profile);
    }
}
