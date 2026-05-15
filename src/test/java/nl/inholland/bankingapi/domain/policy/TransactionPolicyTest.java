package nl.inholland.bankingapi.domain.policy;

import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.exceptions.BadRequestException;
import nl.inholland.bankingapi.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// Mockito is needed because enforceDailyTransferLimit queries TransactionRepository.
@ExtendWith(MockitoExtension.class)
class TransactionPolicyTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionPolicy transactionPolicy;

    private Account activeFromAccount;
    private Account activeToAccount;
    private Account closedAccount;
    private User customerUser;
    private User employeeUser;
    private User otherCustomer;

    private static final String FROM_IBAN = "NL01BANK0000000001";
    private static final String TO_IBAN   = "NL02BANK0000000002";

    @BeforeEach
    void setUp() {
        transactionPolicy = new TransactionPolicy(transactionRepository);

        customerUser = new User();
        customerUser.setId(1);
        customerUser.setRole(UserRole.CUSTOMER);

        employeeUser = new User();
        employeeUser.setId(2);
        employeeUser.setRole(UserRole.EMPLOYEE);

        otherCustomer = new User();
        otherCustomer.setId(3);
        otherCustomer.setRole(UserRole.CUSTOMER);

        activeFromAccount = new Account();
        activeFromAccount.setIban(FROM_IBAN);
        activeFromAccount.setStatus(AccountStatus.ACTIVE);
        activeFromAccount.setBalance(new BigDecimal("1000.00"));
        activeFromAccount.setAbsoluteTransferLimit(new BigDecimal("-500.00"));
        activeFromAccount.setDailyTransferLimit(new BigDecimal("2000.00"));
        activeFromAccount.setUser(customerUser);

        activeToAccount = new Account();
        activeToAccount.setIban(TO_IBAN);
        activeToAccount.setStatus(AccountStatus.ACTIVE);
        activeToAccount.setUser(customerUser);

        closedAccount = new Account();
        closedAccount.setIban("NL03BANK0000000003");
        closedAccount.setStatus(AccountStatus.CLOSED);
        closedAccount.setUser(customerUser);
    }

    // --- enforceTransferPolicy (high-level) ---

    @Test
    void enforceTransferPolicy_happyPath_doesNotThrow() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);
        when(transactionRepository.findByFromIbanAndTypeAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), eq(TransactionType.TRANSFER), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertDoesNotThrow(
                () -> transactionPolicy.enforceTransferPolicy(request, activeFromAccount, activeToAccount, customerUser));
    }

    @Test
    void enforceTransferPolicy_throwsWhenFromIbanIsNull() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceTransferPolicy(request, activeFromAccount, activeToAccount, customerUser));
    }

    @Test
    void enforceTransferPolicy_throwsWhenSameAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, FROM_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceTransferPolicy(request, activeFromAccount, activeFromAccount, customerUser));
    }

    @Test
    void enforceTransferPolicy_throwsWhenSourceAccountIsClosed() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceTransferPolicy(request, closedAccount, activeToAccount, customerUser));
    }

    @Test
    void enforceTransferPolicy_throwsWhenCustomerUsesAnotherCustomersAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThrows(ResponseStatusException.class,
                () -> transactionPolicy.enforceTransferPolicy(request, activeFromAccount, activeToAccount, otherCustomer));
    }

    @Test
    void enforceTransferPolicy_throwsWhenAbsoluteLimitBreached() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("1600.00"), TransactionType.TRANSFER, null);

        // balance 1000 - 1600 = -600, below absoluteTransferLimit of -500
        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceTransferPolicy(request, activeFromAccount, activeToAccount, customerUser));
    }

    // --- enforceDepositPolicy (high-level) ---

    @Test
    void enforceDepositPolicy_happyPath_doesNotThrow() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.DEPOSIT, null);

        assertDoesNotThrow(() -> transactionPolicy.enforceDepositPolicy(request, activeToAccount));
    }

    @Test
    void enforceDepositPolicy_throwsWhenToIbanIsNull() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, null, null, new BigDecimal("100.00"), TransactionType.DEPOSIT, null);

        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceDepositPolicy(request, activeToAccount));
    }

    @Test
    void enforceDepositPolicy_throwsWhenAccountIsClosed() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.DEPOSIT, null);

        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceDepositPolicy(request, closedAccount));
    }

    // --- enforceWithdrawalPolicy (high-level) ---

    @Test
    void enforceWithdrawalPolicy_happyPath_doesNotThrow() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);
        when(transactionRepository.findByFromIbanAndTypeAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), eq(TransactionType.TRANSFER), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertDoesNotThrow(
                () -> transactionPolicy.enforceWithdrawalPolicy(request, activeFromAccount, customerUser));
    }

    @Test
    void enforceWithdrawalPolicy_throwsWhenFromIbanIsNull() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);

        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceWithdrawalPolicy(request, activeFromAccount, customerUser));
    }

    @Test
    void enforceWithdrawalPolicy_throwsWhenAccountIsClosed() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);

        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceWithdrawalPolicy(request, closedAccount, customerUser));
    }

    @Test
    void enforceWithdrawalPolicy_throwsWhenCustomerUsesAnotherCustomersAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("100.00"), TransactionType.WITHDRAWAL, null);

        assertThrows(ResponseStatusException.class,
                () -> transactionPolicy.enforceWithdrawalPolicy(request, activeFromAccount, otherCustomer));
    }

    // --- enforceSourceAccountOwner (individual rule — employee bypass is worth testing directly) ---

    @Test
    void enforceSourceAccountOwner_throwsWhenCustomerUsesAnotherCustomersAccount() {
        assertThrows(ResponseStatusException.class,
                () -> transactionPolicy.enforceSourceAccountOwner(activeFromAccount, otherCustomer));
    }

    @Test
    void enforceSourceAccountOwner_allowsCustomerUsingOwnAccount() {
        assertDoesNotThrow(() -> transactionPolicy.enforceSourceAccountOwner(activeFromAccount, customerUser));
    }

    @Test
    void enforceSourceAccountOwner_allowsEmployeeToUseAnyAccount() {
        assertDoesNotThrow(() -> transactionPolicy.enforceSourceAccountOwner(activeFromAccount, employeeUser));
    }

    // --- enforceAbsoluteTransferLimit (individual rule) ---

    @Test
    void enforceAbsoluteTransferLimit_throwsWhenAmountBreachesLimit() {
        // balance 1000 - 1600 = -600, below absoluteTransferLimit of -500
        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceAbsoluteTransferLimit(activeFromAccount, new BigDecimal("1600.00")));
    }

    @Test
    void enforceAbsoluteTransferLimit_allowsAmountWithinLimit() {
        // balance 1000 - 1400 = -400, above absoluteTransferLimit of -500
        assertDoesNotThrow(
                () -> transactionPolicy.enforceAbsoluteTransferLimit(activeFromAccount, new BigDecimal("1400.00")));
    }

    // --- enforceDailyTransferLimit (individual rule — repository interaction tested here) ---

    @Test
    void enforceDailyTransferLimit_throwsWhenDailyLimitExceeded() {
        Transaction existing = new Transaction();
        existing.setAmount(new BigDecimal("1500.00"));
        when(transactionRepository.findByFromIbanAndTypeAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), eq(TransactionType.TRANSFER), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        // 1500 already transferred today + 600 new = 2100 > dailyTransferLimit of 2000
        assertThrows(BadRequestException.class,
                () -> transactionPolicy.enforceDailyTransferLimit(activeFromAccount, new BigDecimal("600.00")));
    }

    @Test
    void enforceDailyTransferLimit_allowsWhenWithinDailyLimit() {
        when(transactionRepository.findByFromIbanAndTypeAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), eq(TransactionType.TRANSFER), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // 0 transferred today + 500 = 500 < dailyTransferLimit of 2000
        assertDoesNotThrow(
                () -> transactionPolicy.enforceDailyTransferLimit(activeFromAccount, new BigDecimal("500.00")));
    }
}
