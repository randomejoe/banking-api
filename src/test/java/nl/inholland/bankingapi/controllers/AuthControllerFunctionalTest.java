package nl.inholland.bankingapi.controllers;

import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.CustomerStatus;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import nl.inholland.bankingapi.repositories.UserRepository;
import nl.inholland.bankingapi.util.JwtUtil;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Boots the full Spring application context so this test exercises real wiring.
@SpringBootTest
// Exposes MockMvc from the context, allowing HTTP-style endpoint testing without a real server.
@AutoConfigureMockMvc
// Wraps each test in a transaction and rolls it back afterward to keep database state isolated.
@Transactional
class AuthControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    // Used to BCrypt-encode passwords so login tests can verify against a real hash.
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Used to generate real JWT tokens so the JwtAuthenticationFilter authenticates correctly.
    @Autowired
    private JwtUtil jwtUtil;

    // --- Helper methods ---

    // Creates a user whose password is properly BCrypt-encoded so AuthService.login can verify it.
    private User createCustomerWithPassword(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    private CustomerProfile createProfile(User user, String bsn) {
        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setBsn(bsn);
        profile.setPhoneNumber("0612345678");
        profile.setStatus(CustomerStatus.PENDING);
        return customerProfileRepository.save(profile);
    }

    // Generates a real Bearer token so the JwtAuthenticationFilter can authenticate the user.
    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user);
    }

    // --- POST /auth/register ---

    @Test
    void register_happyPath_returnsUserResponse() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "auth-register@example.com");
        request.put("password", "Password1!");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "123456789");
        request.put("phoneNumber", "0612345678");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("auth-register@example.com"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void register_withMissingEmail_returns400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        // email intentionally omitted — @NotBlank on RegisterRequest.email triggers 400.
        request.put("password", "Password1!");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "123456789");
        request.put("phoneNumber", "0612345678");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withWeakPassword_returns400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "auth-weak@example.com");
        // Does not satisfy the complexity @Pattern (no uppercase, no special char).
        request.put("password", "weakpassword");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "123456789");
        request.put("phoneNumber", "0612345678");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withDuplicateEmail_throwsAndReturns500() throws Exception {
        // Seed an existing user with the same email.
        createCustomerWithPassword("auth-dup@example.com", "Password1!");

        Map<String, Object> request = new HashMap<>();
        request.put("email", "auth-dup@example.com");
        request.put("password", "Password1!");
        request.put("firstName", "Alice");
        request.put("lastName", "Smith");
        request.put("bsn", "999888777");
        request.put("phoneNumber", "0612345679");

        // AuthService throws IllegalArgumentException for duplicate email,
        // which the default Spring error handler maps to 500.
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    // --- POST /auth/login ---

    @Test
    void login_happyPath_returnsTokenAndUserInfo() throws Exception {
        User user = createCustomerWithPassword("auth-login@example.com", "Password1!");
        createProfile(user, "111222333");

        Map<String, Object> request = new HashMap<>();
        request.put("email", "auth-login@example.com");
        request.put("password", "Password1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token.value").isString())
                .andExpect(jsonPath("$.token.type").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("auth-login@example.com"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        createCustomerWithPassword("auth-wrongpass@example.com", "Password1!");

        Map<String, Object> request = new HashMap<>();
        request.put("email", "auth-wrongpass@example.com");
        // Correct format but does not match the stored hash.
        request.put("password", "WrongPassword1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", "nobody@example.com");
        request.put("password", "Password1!");

        // AuthService throws ResponseStatusException(UNAUTHORIZED) when email is not found.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /auth/me ---

    @Test
    void me_returnsCurrentUser_whenAuthenticated() throws Exception {
        User user = createCustomerWithPassword("auth-me@example.com", "Password1!");
        createProfile(user, "444555666");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("auth-me@example.com"))
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void me_withoutAuthentication_returns401() throws Exception {
        // No Authorization header — the JwtAuthenticationFilter rejects the request.
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
