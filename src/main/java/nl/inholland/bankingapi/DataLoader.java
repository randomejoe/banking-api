package nl.inholland.bankingapi;

import nl.inholland.bankingapi.entities.*;
import nl.inholland.bankingapi.entities.enums.*;
import nl.inholland.bankingapi.repositories.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements ApplicationRunner {

    // --- Seeded credentials (for developer reference) -------------------------
    // admin@bank.nl         / Admin123!
    // alice@example.nl      / Pass123!
    // bob@example.nl        / Pass123!
    // charlie@example.nl    / Charlie123!
    // daan.dejong@example.nl .. stijn.peeters@example.nl  / Pass123!
    // --------------------------------------------------------------------------

    private static final String ADMIN_PASSWORD    = "Admin123!";
    private static final String CUSTOMER_PASSWORD = "Pass123!";
    private static final String PENDING_PASSWORD  = "Charlie123!";

    // Columns: firstName, lastName, email, BSN, phone
    private static final String[][] MASS_CUSTOMER_DATA = {
        { "Daan",    "de Jong",      "daan.dejong@example.nl",      "200000001", "0611000001" },
        { "Emma",    "Visser",       "emma.visser@example.nl",       "200000002", "0611000002" },
        { "Levi",    "Smits",        "levi.smits@example.nl",        "200000003", "0611000003" },
        { "Sophie",  "Meijer",       "sophie.meijer@example.nl",     "200000004", "0611000004" },
        { "Noah",    "van den Berg", "noah.vandenberg@example.nl",   "200000005", "0611000005" },
        { "Mia",     "Mulder",       "mia.mulder@example.nl",        "200000006", "0611000006" },
        { "Julian",  "van Dijk",     "julian.vandijk@example.nl",    "200000007", "0611000007" },
        { "Olivia",  "Peters",       "olivia.peters@example.nl",     "200000008", "0611000008" },
        { "Finn",    "van Leeuwen",  "finn.vanleeuwen@example.nl",   "200000009", "0611000009" },
        { "Isabel",  "Bos",          "isabel.bos@example.nl",        "200000010", "0611000010" },
        { "Lars",    "de Wit",       "lars.dewit@example.nl",        "200000011", "0611000011" },
        { "Anna",    "Dekker",       "anna.dekker@example.nl",       "200000012", "0611000012" },
        { "Sander",  "Hendriks",     "sander.hendriks@example.nl",   "200000013", "0611000013" },
        { "Nora",    "Brouwer",      "nora.brouwer@example.nl",      "200000014", "0611000014" },
        { "Thijs",   "Jacobs",       "thijs.jacobs@example.nl",      "200000015", "0611000015" },
        { "Fleur",   "Vos",          "fleur.vos@example.nl",         "200000016", "0611000016" },
        { "Ruben",   "Bosman",       "ruben.bosman@example.nl",      "200000017", "0611000017" },
        { "Lisa",    "Koster",       "lisa.koster@example.nl",       "200000018", "0611000018" },
        { "Tim",     "Vermeulen",    "tim.vermeulen@example.nl",     "200000019", "0611000019" },
        { "Sarah",   "Mol",          "sarah.mol@example.nl",         "200000020", "0611000020" },
        { "Bram",    "Kok",          "bram.kok@example.nl",          "200000021", "0611000021" },
        { "Isa",     "Maas",         "isa.maas@example.nl",          "200000022", "0611000022" },
        { "Jesse",   "van Dam",      "jesse.vandam@example.nl",      "200000023", "0611000023" },
        { "Fenna",   "Linders",      "fenna.linders@example.nl",     "200000024", "0611000024" },
        { "Stijn",   "Peeters",      "stijn.peeters@example.nl",     "200000025", "0611000025" },
    };

    private final UserRepository            userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository         accountRepository;
    private final TransactionRepository     transactionRepository;
    private final PasswordEncoder           passwordEncoder;

    public DataLoader(UserRepository userRepository,
                      CustomerProfileRepository customerProfileRepository,
                      AccountRepository accountRepository,
                      TransactionRepository transactionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository            = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountRepository         = accountRepository;
        this.transactionRepository     = transactionRepository;
        this.passwordEncoder           = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        // ------------------------------------------------------------------ //
        //  1. EMPLOYEE                                                        //
        // ------------------------------------------------------------------ //
        User admin = new User(0, "admin@bank.nl",
                passwordEncoder.encode(ADMIN_PASSWORD),
                "Admin", "Bank", UserRole.EMPLOYEE, LocalDateTime.now());
        userRepository.save(admin);

        // ------------------------------------------------------------------ //
        //  2. ALICE  — active customer used as the bulk-transfer initiator    //
        // ------------------------------------------------------------------ //
        User alice = new User(0, "alice@example.nl",
                passwordEncoder.encode(CUSTOMER_PASSWORD),
                "Alice", "Jansen", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(alice);
        customerProfileRepository.save(
                new CustomerProfile(0, alice, "123456789", "0612345678", CustomerStatus.ACTIVE));

        // High balance and generous limits so Alice can initiate all 25 bulk transfers
        // through the API without hitting the absolute or daily limit.
        BigDecimal aliceCheckingBalance = new BigDecimal("50000.00");
        Account aliceChecking = accountRepository.save(new Account(0, alice,
                "NL01INHO0000000001", AccountType.CHECKING,
                aliceCheckingBalance,
                new BigDecimal("0.00"),
                aliceCheckingBalance.multiply(new BigDecimal("0.10")),
                AccountStatus.ACTIVE, LocalDateTime.now()));

        BigDecimal aliceSavingsBalance = new BigDecimal("3000.00");
        Account aliceSavings = accountRepository.save(new Account(0, alice,
                "NL02INHO0000000002", AccountType.SAVINGS,
                aliceSavingsBalance,
                new BigDecimal("0.00"),
                aliceSavingsBalance.multiply(new BigDecimal("0.10")),
                AccountStatus.ACTIVE, LocalDateTime.now()));

        // ------------------------------------------------------------------ //
        //  3. BOB  — active customer                                          //
        // ------------------------------------------------------------------ //
        User bob = new User(0, "bob@example.nl",
                passwordEncoder.encode(CUSTOMER_PASSWORD),
                "Bob", "de Vries", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(bob);
        customerProfileRepository.save(
                new CustomerProfile(0, bob, "987654321", "0687654321", CustomerStatus.ACTIVE));

        BigDecimal bobCheckingBalance = new BigDecimal("800.00");
        Account bobChecking = accountRepository.save(new Account(0, bob,
                "NL03INHO0000000003", AccountType.CHECKING,
                bobCheckingBalance,
                new BigDecimal("0.00"),
                bobCheckingBalance.multiply(new BigDecimal("0.10")),
                AccountStatus.ACTIVE, LocalDateTime.now()));

        // ------------------------------------------------------------------ //
        //  4. CHARLIE  — pending customer (no account, cannot transact)       //
        // ------------------------------------------------------------------ //
        User charlie = new User(0, "charlie@example.nl",
                passwordEncoder.encode(PENDING_PASSWORD),
                "Charlie", "Bakker", UserRole.CUSTOMER, LocalDateTime.now());
        userRepository.save(charlie);
        customerProfileRepository.save(
                new CustomerProfile(0, charlie, "111222333", "0699999999", CustomerStatus.PENDING));

        // ------------------------------------------------------------------ //
        //  5. ORIGINAL SAMPLE TRANSACTIONS                                    //
        // ------------------------------------------------------------------ //
        transactionRepository.save(new Transaction(0,
                aliceChecking.getIban(), bobChecking.getIban(),
                alice, new BigDecimal("250.00"), TransactionType.TRANSFER,
                "Rent payment", LocalDateTime.now().minusHours(26)));

        transactionRepository.save(new Transaction(0,
                null, aliceSavings.getIban(),
                alice, new BigDecimal("500.00"), TransactionType.DEPOSIT,
                "Initial deposit", LocalDateTime.now().minusHours(27)));

        // ------------------------------------------------------------------ //
        //  6. 25 MASS CUSTOMERS — realistic Dutch names, unique IBans         //
        //                                                                     //
        //  IBAN numbering continues from 004 → 028.                           //
        //  Each customer receives one CHECKING account and one SAVINGS account.                //
        // ------------------------------------------------------------------ //
        List<Account> massAccounts = new ArrayList<>();

        for (int i = 0; i < MASS_CUSTOMER_DATA.length; i++) {
            String firstName = MASS_CUSTOMER_DATA[i][0];
            String lastName  = MASS_CUSTOMER_DATA[i][1];
            String email     = MASS_CUSTOMER_DATA[i][2];
            String bsn       = MASS_CUSTOMER_DATA[i][3];
            String phone     = MASS_CUSTOMER_DATA[i][4];

            // IBANs 004 – 028 (zero-padded to match the existing scheme)
            String iban = String.format("NL%02dINHO%010d", i + 4, i + 4);

            User user = new User(0, email,
                    passwordEncoder.encode(CUSTOMER_PASSWORD),
                    firstName, lastName, UserRole.CUSTOMER, LocalDateTime.now());
            userRepository.save(user);

            customerProfileRepository.save(
                    new CustomerProfile(0, user, bsn, phone, CustomerStatus.ACTIVE));

            BigDecimal massCheckingBalance = new BigDecimal("1000.00");
            Account account = accountRepository.save(new Account(0, user, iban,
                    AccountType.CHECKING,
                    massCheckingBalance,
                    new BigDecimal("0.00"),
                    massCheckingBalance.multiply(new BigDecimal("0.10")),
                    AccountStatus.ACTIVE, LocalDateTime.now()));

            massAccounts.add(account);

            String savingsIban = String.format("NL%02dINHO1%09d", i + 4, i + 4);
            BigDecimal massSavingsBalance = new BigDecimal("500.00");
            accountRepository.save(new Account(0, user, savingsIban,
                    AccountType.SAVINGS,
                    massSavingsBalance,
                    new BigDecimal("0.00"),
                    massSavingsBalance.multiply(new BigDecimal("0.10")),
                    AccountStatus.ACTIVE, LocalDateTime.now()));
        }

        // ------------------------------------------------------------------ //
        //  7. 25 TRANSFERS FROM ALICE → EACH MASS CUSTOMER                   //
        //                                                                     //
        //  - Alice is the initiator on every transaction.                     //
        //  - Timestamps are staggered 1 hour apart (most recent first)        //
        //    so they sort chronologically and span two pages at the           //
        //    default page size of 20.                                         //
        //  - Each mass customer will see exactly one incoming transaction     //
        //    in their history (toIban visibility), verifying the fix.         //
        // ------------------------------------------------------------------ //
        for (int i = 0; i < massAccounts.size(); i++) {
            transactionRepository.save(new Transaction(0,
                    aliceChecking.getIban(),
                    massAccounts.get(i).getIban(),
                    alice,
                    new BigDecimal("50.00"),
                    TransactionType.TRANSFER,
                    "Bulk transfer " + (i + 1) + " to " + MASS_CUSTOMER_DATA[i][0] + " " + MASS_CUSTOMER_DATA[i][1],
                    LocalDateTime.now().minusHours(i)));
        }
    }
}
