# SmartPay – Project Testing Report

**Project Name:** SmartPay – Smart Payment Transaction Platform  
**Testing Date:** 10 October 2026  
**Environment:** Local Development  
**Frontend:** React.js with Vite  
**Backend:** Java, Spring Boot, Spring Data JPA  
**Database:** MySQL  
**Application URL:** http://localhost:5173  
**Backend Port:** 8080

## 1. Project Overview

SmartPay is a payment transaction platform designed to manage wallet balances, money transfers, transaction records, and risk checks. The application provides a dashboard, wallet management, payment processing, transaction history, and transaction timelines.

## 2. Functional Testing Results

| Test case | Expected result | Observed result | Status |
|---|---|---|---|
| User login | User can sign in | Dashboard opened after login | Passed |
| Add money – ₹500 | Wallet top-up succeeds | Success confirmation displayed | Passed |
| Send money – ₹100 | Valid payment succeeds | Payment completed with Low risk | Passed |
| Insufficient balance – ₹1,000 | Payment is rejected | Insufficient wallet balance message displayed | Passed |
| ₹20,000 transfer | Risk is evaluated | Payment completed with Medium risk | Passed |
| ₹50,000 transfer | Risk rules evaluate transaction | Payment completed with High risk | Passed |
| ₹50,001 transfer | Amount limit is enforced | Payment blocked by risk checks | Passed |
| Transaction history | Transactions are recorded | Seven transactions were visible in the observed history | Passed |
| Transaction timeline | Processing stages are displayed | Creation, processing, risk evaluation and completion were shown | Passed |

*Note: Results are based on the observed application screens. Automated verification of every backend rule has not yet been completed.*

## 3. Risk Management Testing

The application displayed risk information for transactions:

- **Low risk:** A ₹100 transfer was completed.
- **Medium risk:** A ₹20,000 transfer was completed with a high-value transfer warning.
- **High risk:** A ₹50,000 transfer was completed with risk reasons displayed.
- **Limit enforcement:** A ₹50,001 transfer was blocked. The message cited the ₹50,000 per-transaction limit, the daily transfer limit, and repeated-payment activity.

This demonstrates that the application displays risk classifications and can block a transfer when its risk checks reject the request.

## 4. Database and Backend Verification

- The backend started successfully on port 8080.
- Spring Data JPA initialized the database connection.
- MySQL connection pool initialization completed successfully.
- The application reported that database seeding was skipped because users already existed.
- Transaction records were visible through the application interface.

## 5. Issues and Observations

1. Kafka connection warnings appeared because a Kafka broker was not available at `localhost:9092`.
2. The application continued running despite those warnings.
3. The ₹50,001 test displayed multiple risk reasons, so each rule should be verified independently if detailed test coverage is required.
4. Duplicate-payment protection and concurrent transaction handling have not been verified in this testing session.
5. Production security, deployment, and performance testing remain outstanding.

## 6. Conclusion

The observed tests indicate that the SmartPay application starts successfully, connects to MySQL, supports wallet top-ups and transfers, records transaction history, displays transaction timelines, and applies risk checks.

**Overall result:** Core functionality demonstrated successfully in the local development environment. Further automated and security testing is recommended before production use.

**Report status:** Initial functional testing report based on observed results.
