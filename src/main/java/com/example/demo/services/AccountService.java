package com.example.demo.services;

import com.example.demo.models.AccountModel;
import com.example.demo.models.enums.AccountStatus;
import com.example.demo.models.enums.AccountType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private List<AccountModel> accounts = new ArrayList<>();
    private int currentId = 0;

    public List<AccountModel> createAccountsForUser(int userId) {
        currentId++;
        AccountModel checking = new AccountModel(currentId, userId, generateIban(), AccountType.CHECKING,
                BigDecimal.ZERO, new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now());
        accounts.add(checking);
        currentId++;
        AccountModel savings = new AccountModel(currentId, userId, generateIban(), AccountType.SAVINGS,
                BigDecimal.ZERO, new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now());
        accounts.add(savings);
        return List.of(checking, savings);
    }

    public List<AccountModel> getAll(Integer userId, AccountType type, AccountStatus status) {
        return accounts.stream()
                .filter(a -> userId == null || a.getUserId() == userId)
                .filter(a -> type == null || a.getType() == type)
                .filter(a -> status == null || a.getStatus() == status)
                .collect(Collectors.toList());
    }

    public AccountModel getByIban(String iban) {
        return accounts.stream().filter(a -> a.getIban().equals(iban)).findFirst().orElse(null);
    }

    public List<AccountModel> getByUserId(int userId) {
        return accounts.stream().filter(a -> a.getUserId() == userId).collect(Collectors.toList());
    }

    public AccountModel updateLimits(String iban, BigDecimal absoluteTransferLimit, BigDecimal dailyTransferLimit) {
        accounts = accounts.stream().map(a -> {
            if (a.getIban().equals(iban)) {
                if (absoluteTransferLimit != null) a.setAbsoluteTransferLimit(absoluteTransferLimit);
                if (dailyTransferLimit != null) a.setDailyTransferLimit(dailyTransferLimit);
            }
            return a;
        }).collect(Collectors.toList());
        return getByIban(iban);
    }

    private String generateIban() {
        return "NL" + String.format("%02d", (currentId % 99) + 1) + "BANK" + String.format("%010d", currentId);
    }
}