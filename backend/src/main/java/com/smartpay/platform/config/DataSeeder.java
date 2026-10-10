package com.smartpay.platform.config;

import com.smartpay.platform.entity.*;
import com.smartpay.platform.repository.TransactionEventRepository;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.repository.UserRepository;
import com.smartpay.platform.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo data: one admin, three active users, one inactive user, wallets, and a spread of historical transactions.
 * Runs only when app.seed.enabled=true and the database has no users yet. Wallet balances are the final
 * figures; the sample history is illustrative and is not replayed against the balances.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    static final String ADMIN_PASSWORD = "Admin@123";
    static final String USER_PASSWORD = "User@1234";

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Seed skipped: database already contains users");
            return;
        }
        transactionTemplate.executeWithoutResult(status -> seed());
        log.info("Seed data created. Admin: admin@smartpay.dev / {}  Users: alice|bob|charlie@example.com / {}",
                ADMIN_PASSWORD, USER_PASSWORD);
    }

    private void seed() {
        User admin = user("Platform Admin", "admin@smartpay.dev", "+919000000001", Role.ADMIN, UserStatus.ACTIVE, ADMIN_PASSWORD);
        User alice = user("Alice Sharma", "alice@example.com", "+919000000002", Role.USER, UserStatus.ACTIVE, USER_PASSWORD);
        User bob = user("Bob Verma", "bob@example.com", "+919000000003", Role.USER, UserStatus.ACTIVE, USER_PASSWORD);
        User charlie = user("Charlie Rao", "charlie@example.com", "+919000000004", Role.USER, UserStatus.ACTIVE, USER_PASSWORD);
        User dave = user("Dave Inactive", "dave.inactive@example.com", "+919000000005", Role.USER, UserStatus.INACTIVE, USER_PASSWORD);

        wallet(admin, "0.00");
        wallet(alice, "24250.00");
        wallet(bob, "11300.00");
        wallet(charlie, "5450.00");
        wallet(dave, "1000.00");

        // daysAgo, hoursAgo, sender, receiver, amount, type, status, risk, failure reason, description
        tx(9, 3, alice, alice, "30000", TransactionType.ADD_MONEY, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Wallet top-up");
        tx(8, 5, bob, bob, "15000", TransactionType.ADD_MONEY, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Wallet top-up");
        tx(7, 2, alice, bob, "1200", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Dinner split");
        tx(6, 6, bob, charlie, "800", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Movie tickets");
        tx(5, 1, charlie, charlie, "6000", TransactionType.ADD_MONEY, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Wallet top-up");
        tx(5, 4, alice, charlie, "2500", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Rent share");
        tx(4, 2, bob, alice, "400", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Coffee");
        tx(3, 7, charlie, bob, "9000", TransactionType.TRANSFER, TransactionStatus.FAILED, null, "Insufficient wallet balance", "Laptop advance");
        tx(3, 3, alice, bob, "42000", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.HIGH, "Amount is close to the per-transaction limit", "Car down payment");
        tx(2, 5, bob, alice, "75000", TransactionType.TRANSFER, TransactionStatus.FAILED, RiskLevel.BLOCKED, "Payment blocked by risk checks: Amount exceeds the per-transaction limit of 50000", "Large transfer");
        tx(2, 1, alice, charlie, "22000", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.MEDIUM, "High-value transfer", "Trip booking");
        tx(1, 4, charlie, alice, "300", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Lunch");
        tx(1, 2, bob, dave, "500", TransactionType.TRANSFER, TransactionStatus.FAILED, null, "Receiver account is inactive", "Gift");
        tx(0, 3, alice, bob, "1500", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Groceries");
        tx(0, 1, bob, charlie, "650", TransactionType.TRANSFER, TransactionStatus.SUCCESS, RiskLevel.LOW, null, "Books");
    }

    private User user(String name, String email, String phone, Role role, UserStatus status, String rawPassword) {
        return userRepository.save(User.builder().name(name).email(email).phone(phone).role(role).status(status)
                .password(passwordEncoder.encode(rawPassword)).build());
    }

    private void wallet(User user, String balance) {
        walletRepository.save(Wallet.builder().user(user).balance(new BigDecimal(balance))
                .currency(properties.currency()).status(WalletStatus.ACTIVE).build());
    }

    private void tx(int daysAgo, int hoursAgo, User sender, User receiver, String amount, TransactionType type,
                    TransactionStatus status, RiskLevel risk, String note, String description) {
        Transaction tx = transactionRepository.saveAndFlush(Transaction.builder()
                .transactionReference("TXN-SEED-" + String.format("%04d", transactionRepository.count() + 1))
                .sender(sender).receiver(receiver).amount(new BigDecimal(amount)).currency(properties.currency())
                .transactionType(type).status(status).riskLevel(risk)
                .riskReasons(status == TransactionStatus.SUCCESS ? note : null)
                .failureReason(status == TransactionStatus.FAILED ? note : null)
                .description(description).idempotencyKey("seed-" + java.util.UUID.randomUUID()).build());

        Instant created = Instant.now().minus(Duration.ofDays(daysAgo)).minus(Duration.ofHours(hoursAgo));
        // JPA auditing always stamps "now" on insert, so back-date the sample rows with plain SQL.
        jdbcTemplate.update("update transactions set created_at = ?, updated_at = ? where id = ?",
                utc(created), utc(created.plusSeconds(2)), tx.getId());

        event(tx, TransactionEventType.PAYMENT_CREATED, EventStatus.SUCCESS, type + " request accepted", created);
        event(tx, TransactionEventType.PROCESSING_STARTED, EventStatus.SUCCESS, null, created.plusMillis(300));
        if (status == TransactionStatus.SUCCESS) {
            event(tx, TransactionEventType.PAYMENT_COMPLETED, EventStatus.SUCCESS, "Amount settled", created.plusSeconds(1));
        } else {
            event(tx, TransactionEventType.PAYMENT_FAILED, EventStatus.FAILURE, note, created.plusSeconds(1));
        }
    }

    /** Hibernate stores timestamps as UTC wall-clock time; raw JDBC must do the same to stay consistent. */
    private static java.time.LocalDateTime utc(Instant instant) {
        return java.time.LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
    }

    private void event(Transaction tx, TransactionEventType type, EventStatus status, String details, Instant at) {
        TransactionEvent e = eventRepository.saveAndFlush(TransactionEvent.builder().transaction(tx).eventType(type)
                .eventStatus(status).details(details).build());
        jdbcTemplate.update("update transaction_events set created_at = ? where id = ?", utc(at), e.getId());
    }
}
