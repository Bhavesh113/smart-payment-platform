package com.smartpay.platform.kafka;

import com.smartpay.platform.event.PaymentCompletedEvent;
import com.smartpay.platform.event.PaymentCreatedEvent;
import com.smartpay.platform.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges database transactions and Kafka: events are only sent AFTER the payment transaction has committed, so a
 * rolled-back payment can never produce a Kafka message. The record key is the transaction reference, so all events of
 * one payment land in the same partition and keep their order.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventDispatcher {

    private final EventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(PaymentCreatedEvent event) {
        publisher.publish(KafkaTopics.PAYMENT_CREATED, event.transactionReference(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(PaymentCompletedEvent event) {
        publisher.publish(KafkaTopics.PAYMENT_COMPLETED, event.transactionReference(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFailed(PaymentFailedEvent event) {
        publisher.publish(KafkaTopics.PAYMENT_FAILED, event.transactionReference(), event);
    }
}
