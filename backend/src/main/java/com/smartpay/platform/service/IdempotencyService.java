package com.smartpay.platform.service;

import com.smartpay.platform.entity.Transaction;
import com.smartpay.platform.entity.TransactionType;
import com.smartpay.platform.exception.DuplicateTransactionException;
import com.smartpay.platform.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotency is scoped per sender: the pair (sender_id, idempotency_key) is UNIQUE in the database.
 * The lookup here is the fast path; the unique constraint is the guarantee when two identical requests race.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Optional<Transaction> findExisting(Long senderId, String idempotencyKey) {
        return transactionRepository.findWithPartiesBySenderIdAndIdempotencyKey(senderId, idempotencyKey);
    }

    /** Reusing a key for a different payment is a client bug, so it is rejected instead of silently replayed. */
    public void assertSamePayload(Transaction existing, Long receiverId, BigDecimal amount, TransactionType type) {
        boolean same = existing.getTransactionType() == type
                && existing.getReceiver().getId().equals(receiverId)
                && existing.getAmount().compareTo(amount) == 0;
        if (!same) {
            throw new DuplicateTransactionException(
                    "Idempotency-Key was already used for a different payment request",
                    existing.getTransactionReference());
        }
    }
}
