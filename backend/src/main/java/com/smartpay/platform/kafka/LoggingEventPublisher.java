package com.smartpay.platform.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Used only when app.kafka.enabled=false (tests / running without a broker): events are logged, not sent. */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    @Override
    public void publish(String topic, String key, Object payload) {
        log.info("[kafka disabled] would publish to {} key={} payload={}", topic, key, payload);
    }
}
