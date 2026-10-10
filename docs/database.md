# Database notes

Full reference DDL: [`../database/schema.sql`](../database/schema.sql). The app creates tables through Hibernate (`ddl-auto=update`).

| Table | Purpose | Notable constraints |
|---|---|---|
| `users` | accounts | `UNIQUE(email)`; role USER/ADMIN; status ACTIVE/INACTIVE/BLOCKED |
| `wallets` | one balance per user | `UNIQUE(user_id)`, `version` for optimistic locking, `CHECK(balance >= 0)` in reference DDL |
| `transactions` | every payment/top-up attempt, including failures | `UNIQUE(transaction_reference)`, **`UNIQUE(sender_id, idempotency_key)`** |
| `transaction_events` | audit trail / timeline | FK to `transactions` |
| `notifications` | user messages created by Kafka consumers | `UNIQUE(transaction_id, user_id, type)` (idempotent consumer) |

## Indexes and the queries they serve

| Index | Query |
|---|---|
| `(sender_id, created_at)` | history, velocity check, daily-limit sum |
| `(receiver_id, created_at)` | history for the receiving side |
| `status` | status filter, analytics `GROUP BY status` |
| `risk_level` | admin high-risk list and count |
| `created_at` | date filters, daily analytics window |
| `(transaction_id, created_at)` on events | timeline |
| `(user_id, created_at)` on notifications | dashboard feed |

## Modelling choices

- Money is `DECIMAL(19,2)` / `BigDecimal`.
- Timestamps are UTC (`Instant`, `datetime(6)`); "today" is computed in the configured business time zone.
- Top-up = `ADD_MONEY` transaction with `sender = receiver`, so it shares the idempotency constraint, history and analytics.
- `risk_level` is `NULL` when risk checks never ran (e.g. insufficient balance is detected first).
- Relationships are unidirectional and lazy; list endpoints use an entity graph to avoid N+1 queries.
- Sorting is whitelisted (`createdAt`, `amount`, `status`, `riskLevel`, `transactionReference`) to prevent arbitrary-property sorts.
