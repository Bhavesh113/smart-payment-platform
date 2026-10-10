package com.smartpay.platform.service.fraud;

import com.smartpay.platform.config.PaymentProperties;
import com.smartpay.platform.entity.RiskLevel;
import com.smartpay.platform.entity.TransactionStatus;
import com.smartpay.platform.entity.TransactionType;
import com.smartpay.platform.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyLimitRule implements FraudRule {

    private final TransactionRepository transactionRepository;
    private final PaymentProperties properties;
    private final Clock clock;

    @Override
    public Optional<RuleResult> evaluate(RiskContext ctx) {
        ZoneId zone = ZoneId.of(properties.timezone());
        Instant startOfDay = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
        BigDecimal alreadySent = transactionRepository.sumBySenderAndStatusSince(
                ctx.sender().getId(), TransactionType.TRANSFER, TransactionStatus.SUCCESS, startOfDay);
        if (alreadySent == null) {
            alreadySent = BigDecimal.ZERO;
        }
        if (alreadySent.add(ctx.amount()).compareTo(properties.dailyLimit()) > 0) {
            return Optional.of(new RuleResult("DAILY_LIMIT_EXCEEDED", RiskLevel.BLOCKED,
                    "Daily transfer limit of " + properties.dailyLimit().toPlainString() + " would be exceeded"));
        }
        return Optional.empty();
    }
}
