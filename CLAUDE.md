# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Distributed payment processing system with two Kotlin/Spring Boot 3.3.2 microservices:
- **paymentservice** (port 8080): Handles payment checkout and confirmation via Toss Payments PSP. Uses reactive stack (WebFlux, R2DBC).
- **walletservice** (port 8081): Processes seller wallet settlement from payment events. Uses traditional stack (Spring MVC, JPA/Hibernate).

Both use Java 17, Kotlin 1.9.24, MySQL 8, and communicate asynchronously via Kafka (Spring Cloud Stream).

## Build & Run Commands

Each service has its own Gradle wrapper. Run commands from within each service directory.

```bash
# paymentservice
cd paymentservice
./gradlew build          # Compile and run tests
./gradlew test           # Run tests (excludes "TooLongToRun" tagged tests)
./gradlew bootRun        # Start the service

# walletservice
cd walletservice
./gradlew build
./gradlew test
./gradlew bootRun
```

Run a single test class:
```bash
./gradlew test --tests "woo.paymentservice.payment.application.service.PaymentConfirmServiceTest"
```

## Architecture

Both services follow **Hexagonal Architecture (Ports & Adapters)**:

```
adapter/
  in/         # Inbound adapters (web controllers, Kafka consumers)
  out/        # Outbound adapters (persistence, external APIs, Kafka producers)
application/
  port/in/    # Inbound port interfaces (use cases)
  port/out/   # Outbound port interfaces
  service/    # Use case implementations
domain/       # Domain entities and enums
```

Custom stereotype annotations mark adapter roles: `@WebAdapter`, `@PersistentAdapter`, `@StreamAdapter`, `@UseCase`.

### Payment Flow

1. Checkout creates `PaymentEvent` + `PaymentOrder` records (status: `NOT_STARTED`)
2. `POST /v1/toss/confirm` triggers `PaymentConfirmService` which calls Toss PSP API
3. On success, payment status updates to `SUCCESS` and an outbox record is created
4. `PaymentEventMessageRelayService` reads the outbox and publishes to Kafka topic `payment`
5. walletservice consumes the event and credits seller wallets via `SettlementService`

### Key Patterns

- **Transactional Outbox**: Payment events are written to an `outboxes` table within the same transaction, then relayed to Kafka separately for at-least-once delivery.
- **Idempotency**: Both services use idempotency keys to prevent duplicate processing.
- **Reactive vs Traditional**: paymentservice uses R2DBC (non-blocking DB) and WebFlux; walletservice uses JPA/Hibernate and Spring MVC.

## Database

MySQL 8 on `localhost:3306/test`. Schema DDL files are in `paymentservice/schema/`:
- `payment_events.sql`, `payment_orders.sql`, `payment_order_histories.sql`
- `outboxes.sql`
- `wallets.sql`, `wallet_transactions.sql`

walletservice uses Hibernate `ddl-auto: update` for schema management.

## Testing

- **paymentservice**: JUnit 5, MockK for mocking, reactor-test for reactive streams. Tests tagged `TooLongToRun` are excluded by default.
- **walletservice**: JUnit 5 with standard Spring Boot test support.

## Key Entry Points

- `paymentservice`: `TossPaymentController` (`/v1/toss/confirm`), `CheckoutController` (view)
- `walletservice`: `PaymentEventMessageHandler` (Kafka consumer)
- External API client: `TossPaymentExecutor` (calls `api.tosspayments.com`)
