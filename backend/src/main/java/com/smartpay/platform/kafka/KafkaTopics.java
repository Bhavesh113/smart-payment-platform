package com.smartpay.platform.kafka;

public final class KafkaTopics {

    public static final String PAYMENT_CREATED = "payment-created";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";
    public static final String NOTIFICATION_EVENTS = "notification-events";

    public static final String DLT_SUFFIX = ".DLT";

    private KafkaTopics() {
    }
}
