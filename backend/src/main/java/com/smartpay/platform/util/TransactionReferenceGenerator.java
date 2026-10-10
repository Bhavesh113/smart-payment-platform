package com.smartpay.platform.util;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Produces human-readable, hard-to-guess references such as TXN-20261007-8F3K2QZP. */
@Component
public class TransactionReferenceGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public TransactionReferenceGenerator(Clock clock) {
        this.clock = clock;
    }

    public String next() {
        StringBuilder sb = new StringBuilder("TXN-").append(LocalDate.now(clock).format(DATE)).append('-');
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
