package com.smartpay.platform.kafka;

/** Port for publishing integration events, so payment code does not depend on Kafka classes. */
public interface EventPublisher {

    void publish(String topic, String key, Object payload);
}
