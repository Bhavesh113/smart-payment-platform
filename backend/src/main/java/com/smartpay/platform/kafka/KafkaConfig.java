package com.smartpay.platform.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KafkaConfig {

    private static final int PARTITIONS = 3;

    /** Created on startup by KafkaAdmin if they do not exist yet. Each topic has a matching dead-letter topic. */
    @Bean
    public KafkaAdmin.NewTopics paymentTopics() {
        return new KafkaAdmin.NewTopics(
                topic(KafkaTopics.PAYMENT_CREATED),
                topic(KafkaTopics.PAYMENT_COMPLETED),
                topic(KafkaTopics.PAYMENT_FAILED),
                topic(KafkaTopics.NOTIFICATION_EVENTS),
                topic(KafkaTopics.PAYMENT_CREATED + KafkaTopics.DLT_SUFFIX),
                topic(KafkaTopics.PAYMENT_COMPLETED + KafkaTopics.DLT_SUFFIX),
                topic(KafkaTopics.PAYMENT_FAILED + KafkaTopics.DLT_SUFFIX),
                topic(KafkaTopics.NOTIFICATION_EVENTS + KafkaTopics.DLT_SUFFIX));
    }

    /**
     * Retry policy for every @KafkaListener: a failing record is retried maxRetries times, retryIntervalMs apart
     * (so maxRetries + 1 attempts in total). After that it is published to "<topic>.DLT" and consumption moves on,
     * so one poison message can never block a partition forever.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template,
                                                @Value("${app.kafka.max-retries}") long maxRetries,
                                                @Value("${app.kafka.retry-interval-ms}") long retryIntervalMs) {
        DeadLetterPublishingRecoverer deadLetters = new DeadLetterPublishingRecoverer(template);
        DefaultErrorHandler handler = new DefaultErrorHandler((record, ex) -> {
            log.error("Giving up on record topic={} partition={} offset={} after {} retries; moving to {}{}",
                    record.topic(), record.partition(), record.offset(), maxRetries, record.topic(),
                    KafkaTopics.DLT_SUFFIX, ex);
            deadLetters.accept(record, ex);
        }, new FixedBackOff(retryIntervalMs, maxRetries));
        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Kafka retry attempt {} for topic={} offset={}: {}", attempt, record.topic(),
                        record.offset(), ex.getMessage()));
        return handler;
    }

    private static org.apache.kafka.clients.admin.NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(PARTITIONS).replicas(1).build();
    }
}
