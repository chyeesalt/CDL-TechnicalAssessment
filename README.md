Facility Booking Application

A simple REST API to book facilities.

## Stack
- Spring Boot 3.2 / Java 17
- Spring Data JPA + H2 (in-memory)
- JUnit 5 (integrated tests against real H2 — no mocks)

## Business Rules
- `startTime` and `endTime` are required
- Start time cannot be a past date
- End time must be after start time
- Maximum duration of a booking: 2 hours
- No overlapping bookings for the same facility at the same time slot

## Concurrency

If two requests arrive concurrently for the same facility:
1. One transaction gets locked, checks for conflicts while the other waits.
2. Once the lock is released it wwill re-check — and now sees the first booking, returning an error.

This makes the check-then-insert atomic at the database level without any application-level implementation for ease of the build in this particular instruction usecase.
