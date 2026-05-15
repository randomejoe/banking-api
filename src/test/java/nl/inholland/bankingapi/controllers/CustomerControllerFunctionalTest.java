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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Boots the full Spring application context so this test exercises real wiring.
@SpringBootTest
// Exposes MockMvc from the context, allowing HTTP-style endpoint testing without a real server.
@AutoConfigureMockMvc
// Wraps each test in a transaction and rolls it back afterward to keep database state isolated.
@Transactional
class CustomerControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    // Used to generate real JWT tokens so the JwtAuthenticationFilter authenticates correctly.
    @Autowired
    private JwtUtil jwtUtil;

    // --- Helper methods ---

    private User createCustomer(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setRole(UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    private User createEmployee(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("irrelevant-hash");
        user.setFirstName("Test");
        user.setLastName("Employee");
        user.setRole(UserRole.EMPLOYEE);
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

    // --- GET /customers ---

    @Test
    void getAll_withoutAuthentication_returns401() throws Exception {
        // No Authorization header — Spring Security rejects the request before reaching the controller.
        mockMvc.perform(get("/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_customerGetsForbidden() throws Exception {
        User customer = createCustomer("cc-getall-customer@example.com");
        createProfile(customer, "123456781");

        // @PreAuthorize("hasRole('EMPLOYEE')") on GET /customers rejects customers with 403.
        mockMvc.perform(get("/customers")
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_employeeSeesPagedCustomerList() throws Exception {
        User customer = createCustomer("cc-listed@example.com");
        createProfile(customer, "123456782");
        User employee = createEmployee("cc-getall-employee@example.com");

        mockMvc.perform(get("/customers")
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // --- GET /customers/{id} ---

    @Test
    void getById_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/customers/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_customerGetsForbidden() throws Exception {
        User customer = createCustomer("cc-getbyid-customer@example.com");

        // Customers cannot look up customer details — only employees can.
        mockMvc.perform(get("/customers/{id}", customer.getId())
                        .header("Authorization", bearerToken(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_employeeGetsCustomerDetail() throws Exception {
        User customer = createCustomer("cc-detail-customer@example.com");
        createProfile(customer, "123456783");
        User employee = createEmployee("cc-detail-employee@example.com");

        mockMvc.perform(get("/customers/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId()))
                .andExpect(jsonPath("$.email").value(customer.getEmail()));
    }

    // --- PATCH /customers/{id} ---

    @Test
    void updateCustomer_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(patch("/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCustomer_customerGetsForbidden() throws Exception {
        User customer = createCustomer("cc-patch-forbidden@example.com");

        // @PreAuthorize("hasRole('EMPLOYEE')") on PATCH rejects customers with 403.
        mockMvc.perform(patch("/customers/{id}", customer.getId())
                        .header("Authorization", bearerToken(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCustomer_employeeCanUpdateNameAndPhone() throws Exception {
        User customer = createCustomer("cc-patch-customer@example.com");
        createProfile(customer, "123456784");
        User employee = createEmployee("cc-patch-employee@example.com");

        Map<String, Object> request = new HashMap<>();
        request.put("firstName", "Updated");
        request.put("lastName", "Name");
        request.put("phoneNumber", "0698765432");

        mockMvc.perform(patch("/customers/{id}", customer.getId())
                        .header("Authorization", bearerToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
