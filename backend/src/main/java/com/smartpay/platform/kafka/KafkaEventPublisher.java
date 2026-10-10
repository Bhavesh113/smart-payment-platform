package com.smartpay.platform.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Never throws: this runs after the database commit, so a Kafka outage must not turn an already-committed
     * payment into an HTTP 500. (A transactional outbox would make delivery guaranteed - see README.)
     */
    @Override
    public void publish(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} to topic {} key={}", payload.getClass().getSimpleName(), topic, key, ex);
                } else {
                    log.debug("Published {} to {}-{} offset={}", payload.getClass().getSimpleName(),
                            result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (RuntimeException ex) {
            log.error("Kafka unavailable, event {} for topic {} was not published", payload.getClass().getSimpleName(), topic, ex);
        }
    }
}
