# Fault-Tolerant Payment Ledger

A robust, enterprise-grade backend service built with **Java and Spring Boot** that simulates high-stakes financial ledger logic. This project demonstrates strict **ACID transactions**, concurrent race-condition handling using **Optimistic Locking**, and robust API design mimicking Stripe's precise standards.

## Core Features
1. **Idempotency Keys**: Safely handles network retries by ensuring a transaction (e.g., money transfer) happens exactly once, avoiding the "Double Spending" problem.
2. **ACID Transactions**: Wrapped in `@Transactional` to ensure that either both accounts update simultaneously, or neither does.
3. **Deadlock Prevention**: Implements strict lock ordering on transfer accounts.
4. **Optimistic Locking**: Utilizes `@Version` tags to seamlessly handle concurrent modifications to an account's balance without brutal database locks.

## Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.x
- **Database:** H2 (In-Memory, configured in **PostgreSQL** compatibility mode)
- **Data Access:** Spring Data JPA / Hibernate

## How to Run

1. Navigate to the project directory.
2. Run the application using the Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
   The server will start on `http://localhost:8081`.

*Note: The database seeds automatically with two accounts:*
- *Account `AC100`: $5000.00*
- *Account `AC200`: $1000.00*

## Testing Idempotency (Simulation Screenshot)

Run the following command twice in your terminal to see idempotency in action:

**Command:**
```bash
curl -i -X POST -H "Content-Type: application/json" -H "Idempotency-Key: tx-uuid-999" -d '{"fromAccount":"AC100","toAccount":"AC200","amount":500}' http://localhost:8081/api/ledger/transfer
```

**First Execution:**
```
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 19

Transfer successful
```
*The ledger transfers $500 and saves the `tx-uuid-999` key.*

**Second Execution (Network Retry):**
```
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 19

Transfer successful
```
*The ledger detects `tx-uuid-999` and immediately returns the cached response without altering the database balance a second time.*
