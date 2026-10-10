package com.smartpay.platform.kafka;

import com.smartpay.platform.event.NotificationEvent;
import com.smartpay.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS, groupId = "notification-processors")
    public void onNotification(NotificationEvent event) {
        notificationService.process(event);
    }
}
