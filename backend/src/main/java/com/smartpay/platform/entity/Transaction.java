package com.smartpay.platform.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tx_reference", columnNames = "transaction_reference"),
                @UniqueConstraint(name = "uk_tx_sender_idempotency", columnNames = {"sender_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_tx_sender_created", columnList = "sender_id, created_at"),
                @Index(name = "idx_tx_receiver_created", columnList = "receiver_id, created_at"),
                @Index(name = "idx_tx_status", columnList = "status"),
                @Index(name = "idx_tx_risk_level", columnList = "risk_level"),
                @Index(name = "idx_tx_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false, length = 40)
    private String transactionReference;

    /** For ADD_MONEY the sender and receiver are the same user (wallet top-up). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tx_sender"))
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tx_receiver"))
    private User receiver;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @Column(name = "risk_reasons", length = 500)
    private String riskReasons;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(length = 255)
    private String description;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
}
