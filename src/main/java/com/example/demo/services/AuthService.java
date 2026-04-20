package com.example.demo.services;

import com.example.demo.models.CustomerProfileModel;
import com.example.demo.models.UserModel;
import com.example.demo.models.enums.CustomerStatus;
import com.example.demo.models.enums.UserRole;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private List<UserModel> users = new ArrayList<>();
    private List<CustomerProfileModel> profiles = new ArrayList<>();
    private int currentUserId = 0;
    private int currentProfileId = 0;

    public UserModel register(String email, String password, String firstName, String lastName, String bsn, String phoneNumber) {
        currentUserId++;
        UserModel user = new UserModel(currentUserId, email, password, firstName, lastName, UserRole.CUSTOMER, LocalDateTime.now());
        users.add(user);
        currentProfileId++;
        CustomerProfileModel profile = new CustomerProfileModel(currentProfileId, currentUserId, bsn, phoneNumber, CustomerStatus.PENDING);
        profiles.add(profile);
        return user;
    }

    public UserModel login(String email, String password) {
        return users.stream()
                .filter(u -> u.getEmail().equals(email) && u.getPasswordHash().equals(password))
                .findFirst().orElse(null);
    }

    public UserModel getUserById(int id) {
        return users.stream().filter(u -> u.getId() == id).findFirst().orElse(null);
    }

    public CustomerProfileModel getProfileByUserId(int userId) {
        return profiles.stream().filter(p -> p.getUserId() == userId).findFirst().orElse(null);
    }

    public List<UserModel> getAllCustomers(CustomerStatus status, String search) {
        return users.stream()
                .filter(u -> {
                    CustomerProfileModel p = getProfileByUserId(u.getId());
                    return status == null || (p != null && p.getStatus() == status);
                })
                .filter(u -> search == null ||
                        u.getFirstName().toLowerCase().contains(search.toLowerCase()) ||
                        u.getLastName().toLowerCase().contains(search.toLowerCase()) ||
                        u.getEmail().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
    }

    public CustomerProfileModel updateCustomer(int userId, CustomerStatus status, String firstName, String lastName, String phoneNumber) {
        users = users.stream().map(u -> {
            if (u.getId() == userId) {
                if (firstName != null) u.setFirstName(firstName);
                if (lastName != null) u.setLastName(lastName);
            }
            return u;
        }).collect(Collectors.toList());
        profiles = profiles.stream().map(p -> {
            if (p.getUserId() == userId) {
                if (status != null) p.setStatus(status);
                if (phoneNumber != null) p.setPhoneNumber(phoneNumber);
            }
            return p;
        }).collect(Collectors.toList());
        return getProfileByUserId(userId);
    }
}