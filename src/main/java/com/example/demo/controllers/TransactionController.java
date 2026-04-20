package com.example.demo.controllers;

import com.example.demo.models.TransactionModel;
import com.example.demo.models.enums.TransactionType;
import com.example.demo.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("transactions")
public class TransactionController {

    final private TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("")
    List<TransactionModel> getAll(@RequestParam(required = false) String iban,
                                  @RequestParam(required = false) TransactionType type,
                                  @RequestParam(required = false) BigDecimal minAmount,
                                  @RequestParam(required = false) BigDecimal maxAmount) {
        return transactionService.getAll(iban, type, minAmount, maxAmount);
    }

    @PostMapping("")
    TransactionModel create(@RequestBody Map<String, Object> body) {
        String fromIban = (String) body.get("fromIban");
        String toIban = (String) body.get("toIban");
        int initiatedByUserId = (int) body.get("initiatedByUserId");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        TransactionType type = TransactionType.valueOf((String) body.get("type"));
        String description = (String) body.get("description");
        return transactionService.create(fromIban, toIban, initiatedByUserId, amount, type, description);
    }
}