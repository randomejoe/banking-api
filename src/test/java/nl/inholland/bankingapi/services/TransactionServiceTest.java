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
import nl.inholland.bankingapi.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Registers Mockito with JUnit 5 so @Mock/@InjectMocks fields are created before each test.
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    // Creates a Mockito test double instead of using a real repository implementation.
    @Mock
    private TransactionRepository transactionRepository;

    // Another mock dependency controlled entirely from the test.
    @Mock
    private AccountRepository accountRepository;

    // Mocked mapper so we control what entity the service receives from the DTO.
    @Mock
    private TransactionMapper transactionMapper;

    // Mocked policy lets us verify calls and force policy-related failures when needed.
    @Mock
    private TransactionPolicy transactionPolicy;

    // Builds TransactionService and injects all @Mock fields into its constructor automatically.
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
        // Stubbing: when this mock method is called, return predefined data.
        when(transactionRepository.findAllFiltered(any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(expected);

        Page<Transaction> result = transactionService.getAll(filters, pageable);

        assertEquals(expected, result);
        // Verification: assert that collaboration with the mock happened as expected.
        verify(transactionRepository).findAllFiltered(any(), any(), any(), any(), any(), eq(pageable));
    }

    // --- getById ---

    @Test
    void getById_returnsTransactionWhenFound() {
        // Stubbing a specific ID lookup on the repository mock.
        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));

        Transaction result = transactionService.getById(1);

        assertEquals(transaction, result);
        // Verify the service delegated exactly this lookup to the mock.
        verify(transactionRepository).findById(1);
    }

    @Test
    void getById_throwsWhenTransactionNotFound() {
        // Stub missing transaction to drive the exception branch.
        when(transactionRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getById(99));
    }

    // --- create: TRANSFER ---

    @Test
    void create_transfer_happyPath_debitsFromAndCreditsTo() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, "test");

        // Stubbing repository lookups to simulate existing accounts.
        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.of(fromAccount));
        // Separate stub for the destination account.
        when(accountRepository.findByIban(TO_IBAN)).thenReturn(Optional.of(toAccount));
        // Stub mapper so the service receives a predictable entity.
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        // Stub save result so the service returns a predictable object.
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction result = transactionService.create(request, customerUser);

        assertNotNull(result);
        // Verify the service delegated all validation to the single high-level policy method.
        verify(transactionPolicy).enforceTransferPolicy(request, fromAccount, toAccount, customerUser);
        // Verify both account balances were persisted after debit/credit.
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void create_transfer_throwsWhenFromAccountNotFound() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        // Stub missing source account to drive the exception branch.
        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.create(request, customerUser));

        // Verify no saves occurred after the missing account failure.
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_transfer_throwsWhenToAccountNotFound() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                FROM_IBAN, TO_IBAN, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        when(accountRepository.findByIban(FROM_IBAN)).thenReturn(Optional.of(fromAccount));
        // Destination account is missing in this scenario.
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
        // doThrow(...).when(mock) is the preferred style for forcing exceptions on void methods.
        doThrow(new BadRequestException("Transfer would breach the absolute transfer limit for account: " + FROM_IBAN))
                .when(transactionPolicy).enforceTransferPolicy(request, fromAccount, toAccount, customerUser);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> transactionService.create(request, customerUser));

        assertEquals("Transfer would breach the absolute transfer limit for account: " + FROM_IBAN,
                exception.getMessage());
        // Verify no persistence side effects after policy failure.
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
        // Verify the service delegated validation to the deposit policy method.
        verify(transactionPolicy).enforceDepositPolicy(request, toAccount);
        // Verify only the destination account balance was saved.
        verify(accountRepository).save(toAccount);
        // Verify the transfer policy was never called for a deposit.
        verify(transactionPolicy, never()).enforceTransferPolicy(any(), any(), any(), any());
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
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        Transaction result = transactionService.create(request, customerUser);

        assertNotNull(result);
        // Verify the service delegated validation to the withdrawal policy method.
        verify(transactionPolicy).enforceWithdrawalPolicy(request, fromAccount, customerUser);
        // Verify only the source account balance was saved.
        verify(accountRepository).save(fromAccount);
        // Verify the transfer policy was never called for a withdrawal.
        verify(transactionPolicy, never()).enforceTransferPolicy(any(), any(), any(), any());
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

        // Force policy to throw when given an unrecognised type.
        doThrow(new BadRequestException("Unsupported transaction type: null"))
                .when(transactionPolicy).enforceUnsupportedType(null);

        assertThrows(BadRequestException.class,
                () -> transactionService.create(request, customerUser));

        // Verify policy was consulted for the unsupported type path.
        verify(transactionPolicy).enforceUnsupportedType(null);
        // Verify no accounts were touched.
        verify(accountRepository, never()).findByIban(any());
        verify(transactionRepository, never()).save(any());
    }
}
