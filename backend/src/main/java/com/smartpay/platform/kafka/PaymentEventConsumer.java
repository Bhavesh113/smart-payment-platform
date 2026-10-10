package com.smartpay.platform.kafka;

import com.smartpay.platform.entity.NotificationType;
import com.smartpay.platform.entity.TransactionType;
import com.smartpay.platform.event.NotificationEvent;
import com.smartpay.platform.event.PaymentCompletedEvent;
import com.smartpay.platform.event.PaymentCreatedEvent;
import com.smartpay.platform.event.PaymentFailedEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to payment lifecycle events and turns them into NotificationEvents. If anything here throws, the container's
 * error handler retries (see KafkaConfig) and finally parks the record on the matching .DLT topic.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final EventPublisher publisher;

    @KafkaListener(topics = KafkaTopics.PAYMENT_CREATED, groupId = "payment-created-processors")
    public void onCreated(PaymentCreatedEvent event) {
        log.info("Kafka consumed payment-created ref={} type={} amount={}", event.transactionReference(),
                event.transactionType(), event.amount());
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "payment-completed-processors")
    public void onCompleted(PaymentCompletedEvent event) {
        log.info("Kafka consumed payment-completed ref={}", event.transactionReference());
        String amount = event.amount().toPlainString() + " " + event.currency();
        if (event.transactionType() == TransactionType.ADD_MONEY) {
            publishNotification(event.receiverId(), event.transactionId(), event.transactionReference(),
                    NotificationType.WALLET_TOPUP, amount + " was added to your wallet.");
            return;
        }
        publishNotification(event.senderId(), event.transactionId(), event.transactionReference(),
                NotificationType.PAYMENT_SENT, "You sent " + amount + ". Reference " + event.transactionReference() + ".");
        publishNotification(event.receiverId(), event.transactionId(), event.transactionReference(),
                NotificationType.PAYMENT_RECEIVED, "You received " + amount + ". Reference " + event.transactionReference() + ".");
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "payment-failed-processors")
    public void onFailed(PaymentFailedEvent event) {
        log.info("Kafka consumed payment-failed ref={}", event.transactionReference());
        publishNotification(event.senderId(), event.transactionId(), event.transactionReference(), NotificationType.PAYMENT_FAILED,
                "Your payment of " + event.amount().toPlainString() + " " + event.currency() + " failed: "
                        + event.failureReason());
    }

    private void publishNotification(Long userId, Long transactionId, String reference, NotificationType type, String message) {
        publisher.publish(KafkaTopics.NOTIFICATION_EVENTS, reference,
                new NotificationEvent(userId, transactionId, reference, type, message, Instant.now()));
    }
}
