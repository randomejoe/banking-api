package com.example.demo.mappers;

import com.example.demo.dtos.AccountSummaryResponse;
import com.example.demo.dtos.CurrentUserResponse;
import com.example.demo.dtos.CustomerDetailResponse;
import com.example.demo.dtos.CustomerProfileResponse;
import com.example.demo.dtos.CustomerSummaryResponse;
import com.example.demo.dtos.LoginResponse;
import com.example.demo.entities.Account;
import com.example.demo.entities.CustomerProfile;
import com.example.demo.entities.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CustomerMapper {

    private final AccountMapper accountMapper;

    public CustomerMapper(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    public CustomerSummaryResponse toSummary(User user, CustomerProfile profile) {
        if (user == null) return null;
        return new CustomerSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                profile == null ? null : profile.getStatus(),
                user.getCreatedAt()
        );
    }

    public CustomerDetailResponse toDetail(User user, CustomerProfile profile, List<Account> accounts) {
        if (user == null) return null;
        List<AccountSummaryResponse> accountResponses = accounts.stream()
                .map(accountMapper::toSummary)
                .toList();
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustomerDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                profile == null ? null : profile.getBsn(),
                profile == null ? null : profile.getPhoneNumber(),
                profile == null ? null : profile.getStatus(),
                user.getCreatedAt(),
                totalBalance,
                accountResponses
        );
    }

    public CurrentUserResponse toCurrentUser(User user, CustomerProfile profile) {
        if (user == null) return null;
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                profile == null ? null : profile.getStatus(),
                profile == null ? null : profile.getBsn(),
                profile == null ? null : profile.getPhoneNumber()
        );
    }

    public CustomerProfileResponse toProfile(CustomerProfile profile) {
        if (profile == null) return null;
        return new CustomerProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getBsn(),
                profile.getPhoneNumber(),
                profile.getStatus()
        );
    }

    public LoginResponse toLogin(User user, CustomerProfile profile) {
        if (user == null) return null;
        return new LoginResponse(
                "demo-token-" + user.getId(),
                "Bearer",
                3600,
                user.getRole(),
                profile == null ? null : profile.getStatus()
        );
    }
}
