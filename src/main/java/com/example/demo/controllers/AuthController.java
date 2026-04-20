package com.example.demo.controllers;

import com.example.demo.models.CustomerProfileModel;
import com.example.demo.models.UserModel;
import com.example.demo.services.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("auth")
public class AuthController {

    final private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    UserModel register(@RequestBody Map<String, String> body) {
        return authService.register(
                body.get("email"),
                body.get("password"),
                body.get("firstName"),
                body.get("lastName"),
                body.get("bsn"),
                body.get("phoneNumber")
        );
    }

    @PostMapping("/login")
    Map<String, Object> login(@RequestBody Map<String, String> body) {
        UserModel user = authService.login(body.get("email"), body.get("password"));
        if (user == null) return null;
        CustomerProfileModel profile = authService.getProfileByUserId(user.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", "demo-token-" + user.getId());
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600);
        response.put("role", user.getRole());
        response.put("status", profile != null ? profile.getStatus() : null);
        return response;
    }

    @GetMapping("/me")
    Map<String, Object> me(@RequestParam int userId) {
        UserModel user = authService.getUserById(userId);
        if (user == null) return null;
        CustomerProfileModel profile = authService.getProfileByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole());
        response.put("status", profile != null ? profile.getStatus() : null);
        response.put("bsn", profile != null ? profile.getBsn() : null);
        response.put("phoneNumber", profile != null ? profile.getPhoneNumber() : null);
        return response;
    }
}