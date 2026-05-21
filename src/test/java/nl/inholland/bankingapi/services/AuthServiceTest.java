package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.dtos.RegisterRequest;
import nl.inholland.bankingapi.entities.CustomerProfile;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.repositories.CustomerProfileRepository;
import nl.inholland.bankingapi.repositories.UserRepository;
import nl.inholland.bankingapi.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Registers Mockito with JUnit 5 so @Mock/@InjectMocks fields are created before each test.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Creates a Mockito test double instead of using a real repository implementation.
    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    // Mocked so we can control encode/matches without running BCrypt.
    @Mock
    private PasswordEncoder passwordEncoder;

    // Mocked so we can verify token generation calls without needing a real secret key.
    @Mock
    private JwtUtil jwtUtil;

    // Builds AuthService and injects all @Mock fields into its constructor automatically.
    @InjectMocks
    private AuthService authService;

    private User customer;
    private static final String RAW_PASSWORD = "Password1!";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedHashForTesting";

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1);
        customer.setEmail("auth@example.com");
        customer.setPasswordHash(ENCODED_PASSWORD);
        customer.setFirstName("Alice");
        customer.setLastName("Smith");
        customer.setRole(UserRole.CUSTOMER);
    }

    // --- register ---

    @Test
    void register_happyPath_savesUserAndProfile() {
        RegisterRequest request = new RegisterRequest(
                "new@example.com", RAW_PASSWORD, "Alice", "Smith", "123456789", "0612345678");

        // Stub email check to simulate a free email address.
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        // Stub encoder so we don't run real BCrypt in a unit test.
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(customer);

        User result = authService.register(request);

        assertNotNull(result);
        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(any(User.class));
        // Customer profile must be persisted alongside the user during registration.
        verify(customerProfileRepository).save(any(CustomerProfile.class));
    }

    @Test
    void register_withDuplicateEmail_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest(
                "auth@example.com", RAW_PASSWORD, "Alice", "Smith", "123456789", "0612345678");

        // Stub email lookup to simulate an already-registered account.
        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(customer));

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        // Verify no user or profile was persisted after the duplicate check failed.
        verify(userRepository, never()).save(any());
        verify(customerProfileRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_happyPath_returnsUser() {
        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(customer));
        // Stub password check to simulate a matching password.
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        User result = authService.login("auth@example.com", RAW_PASSWORD);

        assertEquals(customer, result);
        verify(userRepository).findByEmail("auth@example.com");
        verify(passwordEncoder).matches(RAW_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(customer));
        // Stub password check to simulate a mismatched password.
        when(passwordEncoder.matches("WrongPassword1!", ENCODED_PASSWORD)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("auth@example.com", "WrongPassword1!"));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("nobody@example.com", RAW_PASSWORD));

        assertEquals(401, exception.getStatusCode().value());
        // Verify password was never checked when the user does not exist.
        verify(passwordEncoder, never()).matches(any(), any());
    }

    // --- generateToken ---

    @Test
    void generateToken_delegatesToJwtUtil() {
        // Stub mapper so the service receives a predictable token string.
        when(jwtUtil.generateToken(customer)).thenReturn("mocked.jwt.token");

        String token = authService.generateToken(customer);

        assertEquals("mocked.jwt.token", token);
        verify(jwtUtil).generateToken(customer);
    }

    // --- getTokenExpirationMs ---

    @Test
    void getTokenExpirationMs_delegatesToJwtUtil() {
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        long result = authService.getTokenExpirationMs();

        assertEquals(3600000L, result);
        verify(jwtUtil).getExpirationMs();
    }
}
