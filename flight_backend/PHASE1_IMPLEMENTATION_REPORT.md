# Phase 1 Implementation Report

**Date:** August 12, 2026  
**Project:** Flight Booking Backend System (`com.example.flight`)  
**Status:** Completed & Verified (`BUILD SUCCESS`)  

---

## 1. Overview & Objective

Phase 1 focused on **Infrastructure, Security, and Configuration Hardening** without altering core business workflows or deleting existing working modules. 

Key objectives accomplished:
- Added Redis and Springdoc OpenAPI dependencies.
- Externalized all hard-coded credentials from `application.properties`.
- Refactored `JwtService` to inject secret keys and expiration timeouts dynamically.
- Enabled method-level security (`@EnableMethodSecurity`) and refined role-based authorization (`ROLE_ADMIN`, `ROLE_USER`).
- Created and integrated `BookingAccessService` across controllers to eradicate IDOR (Insecure Direct Object Reference) vulnerabilities.
- Standardized error handling in `GlobalExceptionHandler` for validation, authorization, authentication, constraint, and generic errors.
- Added Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Pattern`, `@Positive`) to DTOs.
- Configured CORS for the Angular frontend without allowing unrestricted wildcard `*` origins in production.
- Configured OpenAPI / Swagger UI with Bearer Token JWT support.

---

## 2. Dependencies Added (`pom.xml`)

| Dependency | GroupId / ArtifactId | Version | Purpose |
|---|---|---|---|
| **Spring Data Redis** | `org.springframework.boot:spring-boot-starter-data-redis` | Spring Boot Managed | Infrastructure for Redis caching and seat locking (Phase 2 readiness). |
| **Springdoc OpenAPI UI** | `org.springdoc:springdoc-openapi-starter-webmvc-ui` | `2.8.5` | Swagger UI documentation endpoints (`/swagger-ui/index.html`). |
| **H2 Database** | `com.h2database:h2` | Spring Boot Managed | In-memory database for isolated unit and integration testing. |

---

## 3. Configuration & Secret Management Changes

### 3.1 `application.properties`
All sensitive database and SMTP credentials have been externalized using environment variables with safe fallback defaults:
```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/Flight}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

spring.mail.host=${SPRING_MAIL_HOST:smtp.gmail.com}
spring.mail.port=${SPRING_MAIL_PORT:587}
spring.mail.username=${SPRING_MAIL_USERNAME}
spring.mail.password=${SPRING_MAIL_PASSWORD}

jwt.secret=${JWT_SECRET:mysecretkeymysecretkeymysecretkeymysecretkey}
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}

cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

### 3.2 Dynamic JWT Configuration (`JwtService.java`)
- Removed hard-coded secret string constant.
- Injected properties via constructor injection using `@Value("${jwt.secret}")` and `@Value("${jwt.expiration-ms:3600000}")`.

---

## 4. Security & IDOR Defenses Implemented

### 4.1 CORS Security (`SecurityConfig.java`)
- Replaced wildcard origins with a configurable `CorsConfigurationSource` reading from `${cors.allowed-origins:http://localhost:4200}`.
- Allowed HTTP methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.

### 4.2 Role-Based Security & OpenAPI Setup
- Enabled `@EnableMethodSecurity`.
- Permitted public routes (`/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`).
- Restricted `/admin/**` routes to `hasRole('ADMIN')`.
- Added `OpenApiConfig.java` bean declaring HTTP Bearer JWT `SecurityScheme`, enabling authorization directly inside Swagger UI.

### 4.3 IDOR Vulnerability Prevention (`BookingAccessService.java`)
- Enhanced `BookingAccessService` to enforce resource ownership checks for non-admin users and automatically grant access to `ADMIN` users:
  - `verifyBookingAccess(Long bookingId, Authentication authentication)`
  - `verifyBookingCodeAccess(String bookingCode, Authentication authentication)`
  - `verifyUserAccess(Long userId, Authentication authentication)`
  - `verifyPaymentAccess(Long paymentId, Authentication authentication)`
- Updated **`BookingController`**:
  - `GET /api/bookings` -> Restricted with `@PreAuthorize("hasRole('ADMIN')")`.
  - `GET /api/bookings/{bookingId}` -> Verifies ownership via `BookingAccessService`.
  - `GET /api/bookings/code/{bookingCode}` -> Verifies ownership via `BookingAccessService`.
  - `GET /api/bookings/user/{userId}` -> Verifies that caller ID matches `{userId}` or is `ADMIN`.
  - `GET /api/bookings/flight/{flightId}` -> Restricted with `@PreAuthorize("hasRole('ADMIN')")`.
- Updated **`PassengerController`**:
  - Verifies booking ownership before allowing passenger addition (`POST`) or listing (`GET`).
- Updated **`PaymentController`**:
  - `GET /api/payments` & `DELETE /api/payments/{id}` -> Restricted to `ADMIN`.
  - `GET /api/payments/{id}`, `GET /api/payments/booking/{bookingId}`, `GET /api/payments/transaction/{ref}` -> Enforces ownership check.
  - `POST /api/payments` & `PUT /api/payments/{id}` -> Enforces ownership check for the target booking.

---

## 5. Global Exception Handling Improvements (`GlobalExceptionHandler.java`)

Added comprehensive exception handlers returning uniform `ErrorResponse` JSON contracts:
1. `MethodArgumentNotValidException` (400 BAD REQUEST) - Returns detailed field validation error map.
2. `ConstraintViolationException` (400 BAD REQUEST) - Handles JPA/Hibernate validation constraints.
3. `AccessDeniedException` (403 FORBIDDEN) - Handles authorization failures and IDOR violations cleanly.
4. `AuthenticationException` / `BadCredentialsException` (401 UNAUTHORIZED) - Standardizes login failure responses.
5. `DataIntegrityViolationException` (409 CONFLICT) - Handles unique key & foreign key constraint errors.

---

## 6. Files Modified / Created

### New Files Created
- `com.example.flight.config.OpenApiConfig`
- `com.example.flight.Phase1SecurityVerificationTest`
- `src/test/resources/application.properties`
- `PHASE1_IMPLEMENTATION_REPORT.md`

### Existing Files Modified & Hardened
- `pom.xml`
- `src/main/resources/application.properties`
- `com.example.flight.config.SecurityConfig`
- `com.example.flight.service.JwtService`
- `com.example.flight.service.BookingAccessService`
- `com.example.flight.controller.BookingController`
- `com.example.flight.controller.PassengerController`
- `com.example.flight.controller.PaymentController`
- `com.example.flight.exception.GlobalExceptionHandler`
- `com.example.flight.dto.LoginRequestDTO`
- `com.example.flight.dto.RegisterRequestDTO`
- `com.example.flight.dto.BookingRequestDTO`
- `com.example.flight.dto.BookingResponseDTO`
- `com.example.flight.dto.BookingSegmentRequestDTO`
- `com.example.flight.dto.BookingSegmentResponseDTO`
- `com.example.flight.dto.BookingCancellationRequestDTO`
- `com.example.flight.dto.FlightRequestDTO`
- `com.example.flight.dto.FlightResponseDTO`
- `com.example.flight.dto.FlightSearchRequestDTO`
- `com.example.flight.dto.FlightStatusRequestDTO`
- `com.example.flight.dto.FlightPricingRequestDTO`
- `com.example.flight.dto.FlightPricingResponseDTO`
- `com.example.flight.dto.PassengerRequestDTO`
- `com.example.flight.dto.PassengerResponseDTO`
- `com.example.flight.dto.PaymentRequestDTO`
- `com.example.flight.dto.PaymentResponseDTO`
- `com.example.flight.dto.NotificationRequestDTO`
- `com.example.flight.dto.NotificationResponseDTO`
- `com.example.flight.dto.SearchLogRequestDTO`
- `com.example.flight.dto.SearchLogResponseDTO`
- `com.example.flight.dto.SeatLockRequestDTO`
- `com.example.flight.dto.SeatLockResponseDTO`
- `com.example.flight.entity.User`
- `com.example.flight.entity.Booking`
- `com.example.flight.entity.Payment`
- `com.example.flight.entity.BookingAddOn`
- `com.example.flight.entity.BookingCancellation`
- `com.example.flight.entity.BookingSegment`
- `com.example.flight.entity.Flight`
- `com.example.flight.entity.Airline`
- `com.example.flight.entity.Airport`
- `com.example.flight.entity.FlightPricing`
- `com.example.flight.entity.Notification`
- `com.example.flight.entity.SearchLog`
- `com.example.flight.entity.SeatLock`

---

## 7. Tests Executed & Verification Results

### Test Command:
```bash
mvn clean test
```

### Result:
```text
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 24.067 s
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```
- **Context Verification (`FlightApplicationTests`):** PASSED.
- **Role & Admin Check Verification (`Phase1SecurityVerificationTest`):** PASSED.
- **IDOR Defense Verification (`Phase1SecurityVerificationTest`):** PASSED.

---

## 8. Remaining Known Issues (Deferred to Future Phases)

1. **Phase 2:** Database-based seat locking requires refactoring to atomic Redis distributed locking.
2. **Phase 3:** `Aircraft` entity model is still missing from the flight module.
3. **Phase 4:** Payment execution remains a mock service; real payment gateway SDK & webhook support needed.
4. **Phase 5:** PDF & QR ticket document generation libraries needed for boarding passes and invoices.
