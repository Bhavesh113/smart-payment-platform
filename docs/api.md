# API reference

Base URL `http://localhost:8080` (or `http://localhost:3000` through the nginx proxy). Interactive docs: `/swagger-ui.html`.
All endpoints except register/login need `Authorization: Bearer <jwt>`.

## Envelope

```json
{ "success": true, "message": "Payment processed successfully", "data": { }, "timestamp": "2026-10-07T10:15:30Z" }
```
```json
{ "success": false, "message": "Insufficient wallet balance", "timestamp": "2026-10-07T10:15:30Z",
  "path": "/api/payments", "transactionReference": "TXN-20261007-8F3K2QZP" }
```
Validation errors add `"errors": { "amount": "Amount must be at least 0.01" }`.

## Authentication

```http
POST /api/auth/register
{ "name": "Alice Sharma", "email": "alice@example.com", "password": "User@1234", "phone": "+919000000002" }
→ 201  data: UserResponse
```
```http
POST /api/auth/login
{ "email": "alice@example.com", "password": "User@1234" }
→ 200  data: { "token": "...", "tokenType": "Bearer", "expiresInSeconds": 3600, "user": { ... } }
```
Wrong credentials → 401. Blocked account → 403.
Password rules: 8–72 characters with at least one letter and one digit.

## Payments

```http
POST /api/payments
Idempotency-Key: TXN-123456
{ "receiverEmail": "bob@example.com", "amount": 250.00, "description": "Lunch" }
```

| Outcome | Status | Body |
|---|---|---|
| New payment succeeded | 201 | `data.transaction.status = SUCCESS`, `data.idempotentReplay = false` |
| Key seen before, same payload | 200 + `Idempotent-Replayed: true` | original transaction, `idempotentReplay = true` |
| Key seen before, different payload | 409 | error |
| Receiver not found | 404 | error |
| Insufficient balance | 422 | error + `transactionReference` (stored as FAILED) |
| Blocked by risk rules | 422 | error + `transactionReference` |
| Receiver/sender inactive, wallet frozen | 400 | error + `transactionReference` |
| Missing `Idempotency-Key` | 400 | error |

`POST /api/wallet/add-money` takes `{ "amount": 500.00 }` and the same header and responses (top-up limit `payment.max-add-money`).

## Reading transactions

`GET /api/transactions` (and `/api/payments/history`, `/api/admin/transactions`) parameters:

| Param | Example | Notes |
|---|---|---|
| `status` | `FAILED` | PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, REVERSED |
| `riskLevel` | `HIGH` | admin endpoint only |
| `search` | `bob` | reference, description, names and emails |
| `from`, `to` | `2026-10-01` | inclusive dates, interpreted in `APP_TIMEZONE` |
| `page`, `size` | `0`, `10` | size capped at 100 |
| `sortBy`, `direction` | `amount`, `asc` | createdAt, amount, status, riskLevel, transactionReference |

Page shape: `{ content: [...], page, size, totalElements, totalPages, last }`.
`direction` on each row is `DEBIT` or `CREDIT` relative to the caller.

`GET /api/transactions/{id}`, `/api/payments/{id}`, `/api/payments/reference/{reference}` return
`{ transaction, events: [{ eventType, eventStatus, details, createdAt }] }`. Only the sender, the receiver, or an admin may read a transaction (otherwise 403).

## Analytics

`GET /api/analytics/me?days=7` and `GET /api/admin/analytics?days=14`:

```json
{ "totalTransactions": 12, "successfulTransactions": 9, "failedTransactions": 2, "pendingTransactions": 1,
  "totalAmount": 5400.00, "todayTransactions": 3, "todayAmount": 750.00,
  "successRate": 75.0, "failureRate": 16.67,
  "transactionsByStatus": { "SUCCESS": 9, "FAILED": 2, "PENDING": 1, "PROCESSING": 0, "CANCELLED": 0, "REVERSED": 0 },
  "transactionsByDate": [ { "date": "2026-10-01", "count": 2, "amount": 400.00 } ] }
```
Rates are a percentage of all transactions in scope; amounts count successful transactions only. The admin response wraps this as
`overview` and adds `totalUsers`, `activeUsers`, `highRiskTransactions`, `recentTransactions`, `suspiciousTransactions`.
