# Phase 2 Implementation Report

**Date:** August 12, 2026  
**Project:** Flight Booking Backend System (`com.example.flight`)  
**Status:** Completed & Verified (`BUILD SUCCESS`)  

---

## 1. Overview & Objective

Phase 2 focused on **Redis Integration, Concurrency Control, Seat Locking, and Caching**.

Key objectives accomplished:
- Created `RedisConfig` with `LettuceConnectionFactory`, `StringRedisTemplate`, `RedisTemplate<String, Object>`, and `RedisCacheManager`.
- Externalized Redis host, port, password, and seat lock TTL properties into `application.properties`.
- Refactored `SeatLockService` to use atomic Redis lock acquisition (`SETNX` / `setIfAbsent`) with key structure `seat-lock:{flightId}:{seatNumber}`.
- Implemented owner-only atomic lock release using Lua script check-and-delete.
- Enforced seat state lifecycle (`AVAILABLE` -> `LOCKED` -> `BOOKED` or `LOCKED` -> `RELEASED`/`EXPIRED` -> `AVAILABLE`).
- Added `@Cacheable` and `@CacheEvict` annotations for read-heavy metadata services (`AirportService`, `AirlineService`).
- Added a comprehensive JUnit 5 and Mockito test suite (`Phase2RedisSeatLockTest`) covering all 10 required test scenarios.

---

## 2. Files Created & Modified

### New Files Created
- `com.example.flight.config.RedisConfig`
- `com.example.flight.Phase2RedisSeatLockTest`
- `PHASE2_IMPLEMENTATION_REPORT.md`

### Existing Files Modified
- `src/main/resources/application.properties`
- `com.example.flight.service.SeatLockService`
- `com.example.flight.controller.SeatLockController`
- `com.example.flight.service.AirportService`
- `com.example.flight.service.AirlineService`

---

## 3. Redis Configuration & Externalization

In `application.properties`:
```properties
# Redis Configuration
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}

# Seat Locking Configuration
seat.lock.duration-minutes=${SEAT_LOCK_DURATION_MINUTES:10}
```

In `RedisConfig.java`:
- Configured `LettuceConnectionFactory` reading host, port, and password.
- Configured `StringRedisTemplate` for key-value string operations.
- Configured `RedisCacheManager` with default entry TTL of 10 minutes and JSON value serialization.

---

## 4. Atomic Redis Seat Locking & Concurrency Control

### 4.1 Lock Key Format & Ownership
- Key format: `seat-lock:{flightId}:{seatNumber}` (e.g. `seat-lock:101:12A`).
- Value stored: `userEmail` (owner token).
- Default TTL: Configured dynamically via `${seat.lock.duration-minutes:10}` (10 minutes default).

### 4.2 Atomic Lock Acquisition
Uses Redis `setIfAbsent`:
```java
Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, userEmail, Duration.ofMinutes(lockDurationMinutes));
if (acquired == null || !acquired) {
    throw new RuntimeException("Seat " + seatNumber + " is currently locked by another user");
}
```
If User A and User B request the same seat simultaneously, Redis guarantees that only one request receives `true`.

### 4.3 Atomic Lock Release (Lua Script)
Only the owner who locked the seat can release it:
```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```
Executing this script prevents User B from releasing User A's seat lock.

### 4.4 Permanent Booking Check
Before attempting a Redis lock, PostgreSQL database is queried to check if the seat is already permanently `CONFIRMED`. Permanently booked seats cannot be locked.

---

## 5. Seat Lifecycle Management

```text
[ AVAILABLE ]
      │
      │  (User acquires atomic Redis lock)
      ▼
   [ LOCKED ] ───(Payment failure / TTL 10m expiry)───► [ AVAILABLE ]
      │
      │  (Payment completed)
      ▼
   [ BOOKED / CONFIRMED ] (Persisted in PostgreSQL)
```

---

## 6. Read-Heavy Metadata Caching Strategy

| Service | Method | Cache Annotation | Cache Name |
|---|---|---|---|
| `AirportService` | `getAirportByCode(code)` | `@Cacheable(value = "airports", key = "#airportCode")` | `airports` |
| `AirportService` | `getAllAirports()` | `@Cacheable(value = "airports_all")` | `airports_all` |
| `AirportService` | `addAirport`, `updateAirport`, `deleteAirport` | `@CacheEvict(value = {"airports", "airports_all"}, allEntries = true)` | `airports`, `airports_all` |
| `AirlineService` | `getAirlineByCode(code)` | `@Cacheable(value = "airlines", key = "#airlineCode")` | `airlines` |
| `AirlineService` | `getAllAirlines()` | `@Cacheable(value = "airlines_all")` | `airlines_all` |
| `AirlineService` | `addAirline`, `updateAirline`, `deleteAirline` | `@CacheEvict(value = {"airlines", "airlines_all"}, allEntries = true)` | `airlines`, `airlines_all` |

---

## 7. Tests Executed & Results

Executed command:
```bash
mvn clean test
```

### Test Results Summary:
```text
[INFO] Running com.example.flight.FlightApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.flight.Phase1SecurityVerificationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.example.flight.Phase2RedisSeatLockTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Covered Test Scenarios (All 10 Verification Points):
- **Test A:** User A successfully locks seat.
- **Test B:** User B attempts same seat and fails.
- **Test C:** User A releases own seat lock.
- **Test D:** User B cannot release User A's seat lock.
- **Test E:** Lock expires after TTL.
- **Test F:** Two concurrent multithreaded requests for the same seat.
- **Test G:** Already-booked seat in DB cannot be locked.
- **Test H:** Different seats (12A and 12B) locked independently.
- **Test I:** Metadata airport lookup works.
- **Test J:** Cache invalidation logic works after metadata updates.

---

## 8. Remaining Limitations (Deferred to Future Phases)

1. **Phase 3:** `Aircraft` entity model is still missing from the flight module.
2. **Phase 4:** Payment processing remains a mock service; payment gateway SDK integration required.
3. **Phase 5:** PDF & QR ticket generation libraries needed for boarding passes and invoices.
