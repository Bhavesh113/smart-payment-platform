package com.smartpay.platform.service;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.dto.AdminAnalyticsResponse;
import com.smartpay.platform.dto.AnalyticsResponse;
import com.smartpay.platform.dto.DailyStat;
import com.smartpay.platform.dto.PageResponse;
import com.smartpay.platform.dto.TransactionResponse;
import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.entity.UserStatus;
import com.smartpay.platform.repository.StatusStat;
import com.smartpay.platform.repository.TransactionRepository;
import com.smartpay.platform.repository.TxPoint;
import com.smartpay.platform.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final PaymentProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AnalyticsResponse forUser(Long userId, int days) {
        Window w = window(days);
        return build(transactionRepository.summarizeByStatusForUser(userId),
                transactionRepository.findPointsSinceForUser(w.start(), userId), w);
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse forAdmin(int days) {
        Window w = window(days);
        AnalyticsResponse overview = build(transactionRepository.summarizeByStatus(),
                transactionRepository.findPointsSince(w.start()), w);

        List<TransactionResponse> recent = transactionService.search(null, TransactionService.Filter.empty(),
                0, 8, "createdAt", "desc").content();
        PageResponse<TransactionResponse> suspicious = transactionService.search(null,
                new TransactionService.Filter(null, RiskLevel.HIGH, null, null, null), 0, 8, "createdAt", "desc");
        PageResponse<TransactionResponse> blocked = transactionService.search(null,
                new TransactionService.Filter(null, RiskLevel.BLOCKED, null, null, null), 0, 8, "createdAt", "desc");

        List<TransactionResponse> flagged = new ArrayList<>(suspicious.content());
        flagged.addAll(blocked.content());
        flagged.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));

        return new AdminAnalyticsResponse(overview, userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                transactionRepository.countByRiskLevelIn(List.of(RiskLevel.HIGH, RiskLevel.BLOCKED)),
                recent, flagged.stream().limit(8).toList());
    }

    private record Window(ZoneId zone, LocalDate today, Instant startOfToday, LocalDate firstDay, Instant start) {
    }

    private Window window(int days) {
        int span = Math.min(Math.max(days, 1), 90);
        ZoneId zone = ZoneId.of(properties.timezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate first = today.minusDays(span - 1L);
        return new Window(zone, today, today.atStartOfDay(zone).toInstant(), first,
                first.atStartOfDay(zone).toInstant());
    }

    private AnalyticsResponse build(List<StatusStat> stats, List<TxPoint> points, Window w) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TransactionStatus s : TransactionStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        BigDecimal successAmount = BigDecimal.ZERO;
        long total = 0;
        for (StatusStat s : stats) {
            byStatus.merge(s.status().name(), s.count(), Long::sum);
            total += s.count();
            if (s.status() == TransactionStatus.SUCCESS && s.totalAmount() != null) {
                successAmount = successAmount.add(s.totalAmount());
            }
        }
        long success = byStatus.get(TransactionStatus.SUCCESS.name());
        long failed = byStatus.get(TransactionStatus.FAILED.name());
        long pending = byStatus.get(TransactionStatus.PENDING.name()) + byStatus.get(TransactionStatus.PROCESSING.name());

        long todayCount = 0;
        BigDecimal todayAmount = BigDecimal.ZERO;
        Map<LocalDate, long[]> counts = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> amounts = new LinkedHashMap<>();
        for (LocalDate d = w.firstDay(); !d.isAfter(w.today()); d = d.plusDays(1)) {
            counts.put(d, new long[]{0});
            amounts.put(d, BigDecimal.ZERO);
        }
        for (TxPoint p : points) {
            LocalDate day = p.createdAt().atZone(w.zone()).toLocalDate();
            boolean ok = p.status() == TransactionStatus.SUCCESS;
            if (counts.containsKey(day)) {
                counts.get(day)[0]++;
                if (ok) {
                    amounts.merge(day, p.amount(), BigDecimal::add);
                }
            }
            if (!p.createdAt().isBefore(w.startOfToday())) {
                todayCount++;
                if (ok) {
                    todayAmount = todayAmount.add(p.amount());
                }
            }
        }
        List<DailyStat> byDate = counts.entrySet().stream()
                .map(e -> new DailyStat(e.getKey(), e.getValue()[0], amounts.get(e.getKey())))
                .toList();

        return new AnalyticsResponse(total, success, failed, pending, successAmount, todayCount, todayAmount,
                percentage(success, total), percentage(failed, total), byStatus, byDate);
    }

    private static double percentage(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
