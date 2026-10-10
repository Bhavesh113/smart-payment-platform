package com.smartpay.platform.service;

import com.smartpay.platform.entity.EventStatus;
import com.smartpay.platform.entity.TransactionEvent;
import com.smartpay.platform.entity.TransactionEventType;
import com.smartpay.platform.repository.TransactionEventRepository;
import com.smartpay.platform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes the audit trail shown as the timeline on the transaction details page. */
@Service
@RequiredArgsConstructor
public class TransactionEventService {

    private final TransactionEventRepository eventRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void record(Long transactionId, TransactionEventType type, EventStatus status, String details) {
        eventRepository.save(TransactionEvent.builder()
                .transaction(transactionRepository.getReferenceById(transactionId))
                .eventType(type)
                .eventStatus(status)
                .details(details == null ? null : truncate(details, 500))
                .build());
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
