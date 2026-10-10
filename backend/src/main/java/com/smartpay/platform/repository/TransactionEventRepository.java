package com.smartpay.platform.repository;

import com.smartpay.platform.entity.TransactionEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {

    List<TransactionEvent> findByTransactionIdOrderByCreatedAtAscIdAsc(Long transactionId);
}
