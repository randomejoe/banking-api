package nl.inholland.bankingapi.services;

import nl.inholland.bankingapi.domain.policy.TransactionPolicy;
import nl.inholland.bankingapi.dtos.TransactionCreateRequest;
import nl.inholland.bankingapi.dtos.TransactionFilterParams;
import nl.inholland.bankingapi.entities.Account;
import nl.inholland.bankingapi.entities.Transaction;
import nl.inholland.bankingapi.entities.User;
import nl.inholland.bankingapi.entities.enums.AccountStatus;
import nl.inholland.bankingapi.entities.enums.TransactionType;
import nl.inholland.bankingapi.entities.enums.UserRole;
import nl.inholland.bankingapi.exceptions.BadRequestException;
import nl.inholland.bankingapi.exceptions.ResourceNotFoundException;
import nl.inholland.bankingapi.mappers.TransactionMapper;
import nl.inholland.bankingapi.repositories.AccountRepository;
import nl.inholland.bankingapi.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionPolicy transactionPolicy;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private User customerUser;
    private Transaction transaction;

    private static final String FROM_IBAN = "NL01BANK0000000001";
    private static final String TO_IBAN   = "NL02BANK0000000002";

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(1);
        customerUser.setRole(UserRole.CUSTOMER);

        fromAccount = new Account();
        fromAccount.setIban(FROM_IBAN);
        fromAccount.setStatus(AccountStatus.ACTIVE);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setAbsoluteTransferLimit(new BigDecimal("-500.00"));
        fromAccount.setDailyTransferLimit(new BigDecimal("2000.00"));
        fromAccount.setUser(customerUser);

        toAccount = new Account();
        toAccount.setIban(TO_IBAN);
        toAccount.setStatus(AccountStatus.ACTIVE);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setUser(customerUser);

        transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setType(TransactionType.TRANSFER);
        transaction.setFromIban(FROM_IBAN);
        transaction.setToIban(TO_IBAN);
    }

    // --- getAll ---

    @Test
    void getAll_delegatesToRepository() {
        TransactionFilterParams filters = new TransactionFilterParams();
        Pageable pageable = Pageable.unpaged();
        Page<Transaction> expected = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expected);

        Page<Transaction> result = transactionService.getAll(filters, pageable);

        assertEquals(expected, result);
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAll_withDateRange_delegatesToRepository() {
        TransactionFilterParams filters = new TransactionFilterParams();
        filters.setStartDate(LocalDate.of(2026, 5, 1));
        filters.setEndDate(LocalDate.of(2026, 5, 3));
        Pageable pageable = Pageable.unpaged();
        Page<Transaction> expected = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expected);

        Page<Transaction> result = transactionService.getAll(filters, pageable);

        assertEquals(expected, result);
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAll_throwsWhenStartDateIsAfterEndDate() {
        TransactionFilterParams filters = new TransactionFilterParams();
        filters.setStartDate(LocalDate.of(2026, 5, 4));
        filters.setEndDate(LocalDate.of(2026, 5, 3));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionService.getAll(filters, Pageable.unpaged()));

        assertEquals("startDate must be on or before endDate", exception.getMessage());
        verify(transactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    // --- getById ---

    @Test
    void getById_returnsTransactionWhenFound() {
        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));

        Transaction result = transactionService.getById(1);

        assertEquals(transaction, result);
    }

    @Test
    void getById_throwsWhenTransactionNotFound() {
        when(transactionRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getById(99));
    }

    // --- assertCustomerCanView ---

    @Test
    void assertCustomerCanView_allowsInitiator() {
        transaction.setInitiatedBy(customerUser);

        assertDoesNotThrow(() -> transactionService.assertCustomerCanView(transaction, customerUser));
    }

    @Test
    void assertCustomerCanView_allowsOwnerOfToIban() {
        User otherUser = new User();
        otherUser.setId(99);
        otherUser.setRole(UserRole.CUSTOMER);
        transaction.setInitiatedBy(otherUser);

        Account recipientAccount = new Account();
        recipientAccount.setUser(customerUser);
        // fromIban is not owned by customerUser; toIban is
        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.empty());
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(recipientAccount));

        assertDoesNotThrow(() -> transactionService.assertCustomerCanView(transaction, customerUser));
    }

    @Test
    void assertCustomerCanView_throwsForbiddenWhenUnrelated() {
        User otherUser = new User();
        otherUser.setId(99);
        otherUser.setRole(UserRole.CUSTOMER);
        transaction.setInitiatedBy(otherUser);

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.empty());
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> transactionService.assertCustomerCanView(transaction, customerUser));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- create: TRANSFER ---

    @Test
    void create_transfer_happyPath_debitsFromAndCreditsTo() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, "test");

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.findByFromIbanAndTypeInAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), any(), any())).thenReturn(List.of());
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction result = transactionService.create(request, customerUser);

        assertNotNull(result);
        verify(transactionPolicy).enforceTransferPolicy(
                eq(request), eq(fromAccount), eq(toAccount), eq(customerUser), any(BigDecimal.class));
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void create_transfer_passesComputedDailyTotalToPolicy() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        Transaction existingToday = new Transaction();
        existingToday.setAmount(new BigDecimal("500.00"));

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.findByFromIbanAndTypeInAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), any(), any())).thenReturn(List.of(existingToday));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        transactionService.create(request, customerUser);

        // service must pass the summed total (500.00) to the policy, not zero
        verify(transactionPolicy).enforceTransferPolicy(
                eq(request), eq(fromAccount), eq(toAccount), eq(customerUser), eq(new BigDecimal("500.00")));
    }

    @Test
    void create_transfer_throwsWhenFromAccountNotFound() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.create(request, customerUser));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_transfer_throwsWhenToAccountNotFound() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.create(request, customerUser));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_transfer_propagatesPolicyException() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.findByFromIbanAndTypeInAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), any(), any())).thenReturn(List.of());
        doThrow(new BadRequestException("Transfer would breach the absolute transfer limit for account: " + FROM_IBAN))
                .when(transactionPolicy).enforceTransferPolicy(any(), any(), any(), any(), any());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionService.create(request, customerUser));

        assertEquals("Transfer would breach the absolute transfer limit for account: " + FROM_IBAN,
                exception.getMessage());
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    // --- create: DEPOSIT ---

    @Test
    void create_deposit_happyPath_creditsToAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("200.00"), TransactionType.DEPOSIT, null);

        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(toAccount));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction result = transactionService.create(request, customerUser);

        assertNotNull(result);
        verify(transactionPolicy).enforceDepositPolicy(request, toAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionPolicy, never()).enforceTransferPolicy(any(), any(), any(), any(), any());
    }

    @Test
    void create_deposit_throwsWhenToAccountNotFound() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, TO_IBAN, null, new BigDecimal("200.00"), TransactionType.DEPOSIT, null);

        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.create(request, customerUser));

        verify(transactionRepository, never()).save(any());
    }

    // --- create: WITHDRAWAL ---

    @Test
    void create_withdrawal_happyPath_debitsFromAccount() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("150.00"), TransactionType.WITHDRAWAL, null);

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.of(fromAccount));
        when(transactionRepository.findByFromIbanAndTypeInAndTimestampGreaterThanEqual(
                eq(FROM_IBAN), any(), any())).thenReturn(List.of());
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction result = transactionService.create(request, customerUser);

        assertNotNull(result);
        verify(transactionPolicy).enforceWithdrawalPolicy(
                eq(request), eq(fromAccount), eq(customerUser), any(BigDecimal.class));
        verify(accountRepository).save(fromAccount);
        verify(transactionPolicy, never()).enforceTransferPolicy(any(), any(), any(), any(), any());
    }

    @Test
    void create_withdrawal_throwsWhenFromAccountNotFound() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, null, null, new BigDecimal("150.00"), TransactionType.WITHDRAWAL, null);

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.create(request, customerUser));

        verify(transactionRepository, never()).save(any());
    }

    // --- create: unsupported type ---

    @Test
    void create_unsupportedType_delegatesToPolicy() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, null, null, new BigDecimal("50.00"), null, null);

        doThrow(new BadRequestException("Unsupported transaction type: null"))
                .when(transactionPolicy).enforceUnsupportedType(null);

        assertThrows(BadRequestException.class,
                () -> transactionService.create(request, customerUser));

        verify(transactionPolicy).enforceUnsupportedType(null);
        verify(accountRepository, never()).findByIban(any());
        verify(transactionRepository, never()).save(any());
    }
}
