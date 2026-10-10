package com.smartpay.platform.util;

import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.Transaction;
import com.smartpay.platform.entity.TransactionStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the transaction list endpoints (avoids hand-written dynamic JPQL). */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> any() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Transaction> involvingUser(Long userId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("sender").get("id"), userId),
                cb.equal(root.get("receiver").get("id"), userId));
    }

    public static Specification<Transaction> hasStatus(TransactionStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Transaction> riskIn(Collection<RiskLevel> levels) {
        return (root, query, cb) -> root.get("riskLevel").in(levels);
    }

    public static Specification<Transaction> createdFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Transaction> createdBefore(Instant toExclusive) {
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), toExclusive);
    }

    public static Specification<Transaction> matchesText(String text) {
        String like = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<Object, Object> sender = root.join("sender", JoinType.LEFT);
            Join<Object, Object> receiver = root.join("receiver", JoinType.LEFT);
            Predicate p = cb.or(
                    cb.like(cb.lower(root.get("transactionReference")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(sender.get("email")), like),
                    cb.like(cb.lower(sender.get("name")), like),
                    cb.like(cb.lower(receiver.get("email")), like),
                    cb.like(cb.lower(receiver.get("name")), like));
            return p;
        };
    }
}
