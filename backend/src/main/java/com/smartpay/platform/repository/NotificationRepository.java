package com.smartpay.platform.repository;

import com.smartpay.platform.entity.Notification;
import com.smartpay.platform.entity.NotificationType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByTransactionIdAndUserIdAndType(Long transactionId, Long userId, NotificationType type);

    @EntityGraph(attributePaths = "transaction")
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
