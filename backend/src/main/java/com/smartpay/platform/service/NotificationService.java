package com.smartpay.platform.service;

import com.smartpay.platform.dto.NotificationResponse;
import com.smartpay.platform.entity.EventStatus;
import com.smartpay.platform.entity.Notification;
import com.smartpay.platform.entity.NotificationStatus;
import com.smartpay.platform.entity.TransactionEventType;
import com.smartpay.platform.event.NotificationEvent;
import com.smartpay.platform.mapper.TransactionMapper;
import com.smartpay.platform.repository.NotificationRepository;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventService eventService;

    /**
     * Kafka delivers at-least-once, so the same event can arrive twice. The existence check (backed by a UNIQUE
     * constraint on transaction/user/type) makes this method idempotent.
     */
    @Transactional
    public void process(NotificationEvent event) {
        if (notificationRepository.existsByTransactionIdAndUserIdAndType(
                event.transactionId(), event.userId(), event.type())) {
            log.info("Notification already stored, skipping duplicate delivery tx={} user={} type={}",
                    event.transactionReference(), event.userId(), event.type());
            return;
        }
        notificationRepository.save(Notification.builder()
                .user(userRepository.getReferenceById(event.userId()))
                .transaction(transactionRepository.getReferenceById(event.transactionId()))
                .message(event.message())
                .type(event.type())
                .status(NotificationStatus.SENT) // delivery channel (email/SMS/push) is simulated by this log line
                .build());
        eventService.record(event.transactionId(), TransactionEventType.NOTIFICATION_SENT, EventStatus.SUCCESS,
                event.type() + " notification for user " + event.userId());
        log.info("Notification sent tx={} user={} type={}", event.transactionReference(), event.userId(),
                event.type());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> recentForUser(Long userId, int limit) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.min(limit, 50)))
                .stream().map(TransactionMapper::toResponse).toList();
    }
}
