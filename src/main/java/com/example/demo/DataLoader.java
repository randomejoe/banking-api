package com.example.demo;

import com.example.demo.entities.*;
import com.example.demo.entities.enums.*;
import com.example.demo.repositories.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DataLoader(UserRepository userRepository, CustomerProfileRepository customerProfileRepository,
                      AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Employee account
        User admin = new User(0, "admin@bank.nl", "admin123", "Admin", "Bank", UserRole.EMPLOYEE, LocalDateTime.now());
        userRepository.save(admin);

        // Active customer - Alice
        User alice = new User(0, "alice@example.nl", "pass123", "Alice", "Jansen", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(alice);
        customerProfileRepository.save(new CustomerProfile(0, alice, "123456789", "0612345678", CustomerStatus.ACTIVE));
        Account aliceChecking = accountRepository.save(new Account(0, alice, "NL01BANK0000000001", AccountType.CHECKING,
                new BigDecimal("1500.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now()));
        Account aliceSavings = accountRepository.save(new Account(0, alice, "NL01BANK0000000002", AccountType.SAVINGS,
                new BigDecimal("3000.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now()));

        // Active customer - Bob
        User bob = new User(0, "bob@example.nl", "pass123", "Bob", "de Vries", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(bob);
        customerProfileRepository.save(new CustomerProfile(0, bob, "987654321", "0687654321", CustomerStatus.ACTIVE));
        Account bobChecking = accountRepository.save(new Account(0, bob, "NL01BANK0000000003", AccountType.CHECKING,
                new BigDecimal("800.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"), AccountStatus.ACTIVE, LocalDateTime.now()));

        // Pending customer - Charlie
        User charlie = new User(0, "charlie@example.nl", "pass123", "Charlie", "Bakker", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(charlie);
        customerProfileRepository.save(new CustomerProfile(0, charlie, "111222333", "0699999999", CustomerStatus.PENDING));

        // Sample transactions
        transactionRepository.save(new Transaction(0, aliceChecking.getIban(), bobChecking.getIban(),
                alice, new BigDecimal("250.00"), TransactionType.TRANSFER, "Rent payment", LocalDateTime.now()));
        transactionRepository.save(new Transaction(0, null, aliceSavings.getIban(),
                alice, new BigDecimal("500.00"), TransactionType.DEPOSIT, "Initial deposit", LocalDateTime.now()));
    }
}
