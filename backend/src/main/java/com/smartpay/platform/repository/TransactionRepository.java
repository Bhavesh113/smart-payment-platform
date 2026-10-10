package com.smartpay.platform.repository;

import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.Transaction;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @EntityGraph(attributePaths = {"sender", "receiver"})
    Optional<Transaction> findWithPartiesById(Long id);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    Optional<Transaction> findWithPartiesByTransactionReference(String transactionReference);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    Optional<Transaction> findWithPartiesBySenderIdAndIdempotencyKey(Long senderId, String idempotencyKey);

    @Override
    @EntityGraph(attributePaths = {"sender", "receiver"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    /** Atomic compare-and-set on the status: only one caller can move a PENDING transaction forward. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Transaction t set t.status = :toStatus, t.updatedAt = :now where t.id = :id and t.status = :fromStatus")
    int transition(@Param("id") Long id,
                   @Param("fromStatus") TransactionStatus fromStatus,
                   @Param("toStatus") TransactionStatus toStatus,
                   @Param("now") Instant now);

    // ---- risk-engine queries ----

    @Query("""
            select count(t) from Transaction t
            where t.sender.id = :senderId and t.transactionType = :type
              and t.createdAt >= :since and t.id <> :excludeId
            """)
    long countRecentBySender(@Param("senderId") Long senderId, @Param("type") TransactionType type,
                             @Param("since") Instant since, @Param("excludeId") Long excludeId);

    @Query("""
            select count(t) from Transaction t
            where t.sender.id = :senderId and t.receiver.id = :receiverId and t.transactionType = :type
              and t.createdAt >= :since and t.id <> :excludeId
            """)
    long countRecentBetween(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId,
                            @Param("type") TransactionType type, @Param("since") Instant since,
                            @Param("excludeId") Long excludeId);

    /** Returns null when there are no rows; callers treat that as zero. */
    @Query("""
            select sum(t.amount) from Transaction t
            where t.sender.id = :senderId and t.transactionType = :type
              and t.status = :status and t.createdAt >= :since
            """)
    BigDecimal sumBySenderAndStatusSince(@Param("senderId") Long senderId, @Param("type") TransactionType type,
                                         @Param("status") TransactionStatus status, @Param("since") Instant since);

    // ---- analytics queries ----

    @Query("select new com.smartpay.platform.repository.StatusStat(t.status, count(t), sum(t.amount)) from Transaction t group by t.status")
    List<StatusStat> summarizeByStatus();

    @Query("""
            select new com.smartpay.platform.repository.StatusStat(t.status, count(t), sum(t.amount)) from Transaction t
            where t.sender.id = :userId or t.receiver.id = :userId group by t.status
            """)
    List<StatusStat> summarizeByStatusForUser(@Param("userId") Long userId);

    @Query("select new com.smartpay.platform.repository.TxPoint(t.createdAt, t.status, t.amount) from Transaction t where t.createdAt >= :since")
    List<TxPoint> findPointsSince(@Param("since") Instant since);

    @Query("""
            select new com.smartpay.platform.repository.TxPoint(t.createdAt, t.status, t.amount) from Transaction t
            where t.createdAt >= :since and (t.sender.id = :userId or t.receiver.id = :userId)
            """)
    List<TxPoint> findPointsSinceForUser(@Param("since") Instant since, @Param("userId") Long userId);

    long countByRiskLevelIn(Collection<RiskLevel> levels);
}
