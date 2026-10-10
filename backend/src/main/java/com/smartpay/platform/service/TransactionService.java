package com.smartpay.platform.service;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.dto.PageResponse;
import com.smartpay.platform.dto.TransactionDetailResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.Transaction;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.exception.ResourceNotFoundException;
import com.smartpay.platform.exception.UnauthorizedException;
import com.smartpay.platform.mapper.TransactionMapper;
import com.smartpay.platform.repository.TransactionEventRepository;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.util.TransactionSpecifications;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of transactions: filtered/sorted/paginated listing and detail lookup with ownership checks. */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "amount", "status", "riskLevel", "transactionReference");
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository eventRepository;
    private final PaymentProperties properties;

    public record Filter(TransactionStatus status, RiskLevel riskLevel, String search, LocalDate from, LocalDate to) {
        public static Filter empty() {
            return new Filter(null, null, null, null, null);
        }
    }

    /** Pass viewerId to restrict to that user's transactions, or null for the admin (all transactions) view. */
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(Long viewerId, Filter filter, int page, int size,
                                                    String sortBy, String direction) {
        Specification<Transaction> spec = TransactionSpecifications.any();
        if (viewerId != null) {
            spec = spec.and(TransactionSpecifications.involvingUser(viewerId));
        }
        if (filter.status() != null) {
            spec = spec.and(TransactionSpecifications.hasStatus(filter.status()));
        }
        if (filter.riskLevel() != null) {
            spec = spec.and(TransactionSpecifications.riskIn(List.of(filter.riskLevel())));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            spec = spec.and(TransactionSpecifications.matchesText(filter.search()));
        }
        ZoneId zone = ZoneId.of(properties.timezone());
        if (filter.from() != null) {
            spec = spec.and(TransactionSpecifications.createdFrom(filter.from().atStartOfDay(zone).toInstant()));
        }
        if (filter.to() != null) {
            spec = spec.and(TransactionSpecifications.createdBefore(filter.to().plusDays(1).atStartOfDay(zone).toInstant()));
        }

        String sortProperty = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // id is a tie-breaker so pagination stays stable when many rows share the same sort value
        Sort sort = Sort.by(dir, sortProperty).and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);

        return PageResponse.from(transactionRepository.findAll(spec, pageable)
                .map(t -> TransactionMapper.toResponse(t, viewerId)));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getResponse(Long id, Long viewerId) {
        return TransactionMapper.toResponse(load(transactionRepository.findWithPartiesById(id)), viewerId);
    }

    @Transactional(readOnly = true)
    public TransactionDetailResponse getDetailById(Long id, Long viewerId, boolean admin) {
        return detail(load(transactionRepository.findWithPartiesById(id)), viewerId, admin);
    }

    @Transactional(readOnly = true)
    public TransactionDetailResponse getDetailByReference(String reference, Long viewerId, boolean admin) {
        return detail(load(transactionRepository.findWithPartiesByTransactionReference(reference)), viewerId, admin);
    }

    private TransactionDetailResponse detail(Transaction tx, Long viewerId, boolean admin) {
        boolean participant = viewerId.equals(tx.getSender().getId()) || viewerId.equals(tx.getReceiver().getId());
        if (!participant && !admin) {
            throw new UnauthorizedException("You do not have access to this transaction");
        }
        var events = eventRepository.findByTransactionIdOrderByCreatedAtAscIdAsc(tx.getId()).stream()
                .map(TransactionMapper::toResponse).toList();
        return new TransactionDetailResponse(TransactionMapper.toResponse(tx, viewerId), events);
    }

    private Transaction load(java.util.Optional<Transaction> found) {
        return found.orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }
}
