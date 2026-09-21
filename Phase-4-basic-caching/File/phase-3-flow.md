# Phase 3 Flow (No Redis)

Simple view of everything we built in Phase 3.
Every request goes to PostgreSQL. There is no cache, no lock and no Redis yet.

---

## 1. The big picture

Every request follows the same path: Controller, then Service, then Repository, then the database.

```mermaid
flowchart LR
    C["Client (Postman)"] --> F["JWT filter (Phase 2)"]
    F --> CT["Controller"]
    CT --> S["Service"]
    S --> R["Repository"]
    R --> DB[("PostgreSQL")]

    CT -.-> CU["CurrentUser (finds user id)"]
    CU -.-> PR["PersonRepository"]
```

| Layer | Job |
|---|---|
| Controller | Takes the request, calls the service, returns the response |
| Service | The real logic (checks, rules, saving) |
| Repository | Talks to the database |
| CurrentUser | Reads the username from the token and finds the user id |

---

## 2. The three tables

```mermaid
erDiagram
    EVENTS ||--o{ SEATS : "has 100 seats"
    SEATS ||--o{ BOOKINGS : "can be booked"

    EVENTS {
        bigint id PK
        string name
        string venue
        datetime event_date
        int seat_count
    }
    SEATS {
        bigint id PK
        bigint event_id
        string label
        string status
        bigint held_by
        datetime hold_expires_at
    }
    BOOKINGS {
        bigint id PK
        bigint user_id
        bigint seat_id
        datetime created_at
        string status
    }
```

`event_id`, `seat_id` and `user_id` are plain numbers. There are no foreign keys, to keep it simple.

---

## 3. Seat status: how a seat changes

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE
    AVAILABLE --> HELD : user holds (5 minutes)
    HELD --> BOOKED : holder confirms
    HELD --> AVAILABLE : holder releases
    HELD --> AVAILABLE : 5 minutes pass (lazy check)
    BOOKED --> AVAILABLE : booking cancelled
```

"Lazy check" means nobody cleans up in the background. The hold is only seen as expired when someone reads or touches that seat.

---

## 4. Admin creates an event (3.3 and 3.4)

```mermaid
sequenceDiagram
    actor Admin
    participant AC as AdminEventController
    participant ES as EventService
    participant ER as EventRepository
    participant SR as SeatRepository
    participant DB as PostgreSQL

    Admin->>AC: POST /admin/events
    Note over AC: only ADMIN role is allowed
    AC->>ES: create(request)
    ES->>ER: save(event, seatCount = 100)
    ER->>DB: INSERT into events
    DB-->>ER: event with id
    ES->>SR: saveAll(100 seats A1 to J10, all AVAILABLE)
    SR->>DB: INSERT into seats x 100
    ES-->>AC: EventResponse
    AC-->>Admin: 201 Created
```

If a normal user calls this, Spring Security stops it with 403 before the controller code runs.

---

## 5. Update and delete an event (3.3)

```mermaid
sequenceDiagram
    actor Admin
    participant AC as AdminEventController
    participant ES as EventService
    participant DB as PostgreSQL

    Admin->>AC: PUT /admin/events/1
    AC->>ES: update(1, request)
    ES->>DB: find event 1
    ES->>DB: UPDATE events
    AC-->>Admin: 200 with new values

    Admin->>AC: DELETE /admin/events/1
    AC->>ES: delete(1)
    ES->>DB: find event 1
    ES->>DB: DELETE seats of event 1
    ES->>DB: DELETE event 1
    AC-->>Admin: 204 No Content
```

If the event does not exist, the service throws 404.

---

## 6. Read events (3.5)

```mermaid
sequenceDiagram
    actor User
    participant EC as EventController
    participant ES as EventService
    participant DB as PostgreSQL

    User->>EC: GET /events/1
    EC->>ES: findById(1)
    ES->>DB: SELECT from events
    DB-->>ES: event
    ES-->>EC: EventResponse
    EC-->>User: 200

    User->>EC: GET /events/1 (again)
    EC->>ES: findById(1)
    ES->>DB: SELECT from events (again)
    Note over DB: same query every time
```

Every read hits the database, even when the data did not change. This is the problem Phase 4 solves with Redis cache.

---

## 7. Seat map with lazy expiry (3.6)

```mermaid
sequenceDiagram
    actor User
    participant EC as EventController
    participant ES as EventService
    participant DB as PostgreSQL

    User->>EC: GET /events/1/seats
    EC->>ES: getSeats(1)
    ES->>DB: check event 1 exists
    ES->>DB: SELECT all seats of event 1
    DB-->>ES: 100 seats
    loop each seat
        Note over ES: is it HELD and holdExpiresAt is in the past?
        Note over ES: if yes, set AVAILABLE and clear heldBy
    end
    ES-->>EC: list of SeatResponse
    EC-->>User: 200 with 100 seats
```

This GET can also write to the database (when it clears an expired hold). That is a weak point we fix in Phase 8.

---

## 8. How the app knows who the user is

```mermaid
sequenceDiagram
    actor User
    participant F as JWT filter
    participant BC as BookingController
    participant CU as CurrentUser
    participant PR as PersonRepository

    User->>F: request with Bearer token
    F->>F: check token, save username in security context
    F->>BC: continue
    BC->>CU: getId()
    CU->>CU: read username from security context
    CU->>PR: findByUsername(username)
    PR-->>CU: person
    CU-->>BC: user id
```

---

## 9. Hold a seat (3.8)

```mermaid
sequenceDiagram
    actor User
    participant BC as BookingController
    participant BS as BookingService
    participant SR as SeatRepository
    participant DB as PostgreSQL

    User->>BC: POST /events/1/seats/1/hold
    BC->>BS: hold(1, 1, userId)
    BS->>SR: findByIdAndEventId(1, 1)
    SR->>DB: SELECT seat
    DB-->>BS: seat (status AVAILABLE)
    Note over BS: if old hold expired, make it AVAILABLE
    Note over BS: CHECK: is status AVAILABLE?
    alt not AVAILABLE
        BS-->>User: 409 Seat is not available
    else AVAILABLE
        Note over BS: SAVE: HELD, heldBy = user, expires = now + 5 min
        BS->>DB: UPDATE seats
        BS-->>User: 200 with holdExpiresAt
    end
```

The rule is "check first, then save". These are two separate steps, and nothing stops another request from sneaking in between them (see section 13).

---

## 10. Confirm a booking (3.9)

```mermaid
sequenceDiagram
    actor User
    participant BC as BookingController
    participant BS as BookingService
    participant DB as PostgreSQL

    User->>BC: POST /events/1/seats/1/confirm
    BC->>BS: confirm(1, 1, userId)
    BS->>DB: SELECT seat
    alt seat is not HELD
        BS-->>User: 409 Seat is not held
    else held by someone else
        BS-->>User: 403 Held by someone else
    else hold time is over
        BS-->>User: 409 Hold has expired
    else all good
        BS->>DB: UPDATE seat to BOOKED, clear hold
        BS->>DB: INSERT booking (CONFIRMED)
        BS-->>User: 200 BookingResponse
    end
```

Payment is fake. Calling confirm means "payment done".

---

## 11. Release a hold (3.10)

```mermaid
sequenceDiagram
    actor User
    participant BC as BookingController
    participant BS as BookingService
    participant DB as PostgreSQL

    User->>BC: DELETE /events/1/seats/1/hold
    BC->>BS: release(1, 1, userId)
    BS->>DB: SELECT seat
    alt seat is not HELD
        BS-->>User: 409 Seat is not held
    else you are not the holder
        BS-->>User: 403 You do not hold this seat
    else you are the holder
        BS->>DB: UPDATE seat to AVAILABLE, clear hold
        BS-->>User: 204 No Content
    end
```

---

## 12. Cancel a booking and see my bookings (3.10)

```mermaid
sequenceDiagram
    actor User
    participant BC as BookingController
    participant BS as BookingService
    participant DB as PostgreSQL

    User->>BC: POST /bookings/1/cancel
    BC->>BS: cancel(1, userId)
    BS->>DB: SELECT booking
    alt booking not found
        BS-->>User: 404
    else not your booking
        BS-->>User: 403
    else already cancelled
        BS-->>User: 409
    else ok
        BS->>DB: UPDATE booking to CANCELLED
        BS->>DB: UPDATE seat to AVAILABLE
        BS-->>User: 200 CANCELLED
    end

    User->>BC: GET /bookings/me
    BC->>BS: myBookings(userId)
    BS->>DB: SELECT bookings of this user, newest first
    BS-->>User: 200 list
```

---

## 13. The bug we keep on purpose: two users hold the same seat

Both users hit hold on seat 5 at almost the same time.

```mermaid
sequenceDiagram
    actor U1 as User 1
    actor U2 as User 2
    participant App as Spring app
    participant DB as PostgreSQL

    U1->>App: hold seat 5
    U2->>App: hold seat 5
    App->>DB: read seat 5 for User 1
    DB-->>App: AVAILABLE
    App->>DB: read seat 5 for User 2
    DB-->>App: AVAILABLE
    Note over App: both checks passed
    App->>DB: UPDATE seat 5, heldBy = User 1
    App->>DB: UPDATE seat 5, heldBy = User 2
    App-->>U1: 200 you hold seat 5
    App-->>U2: 200 you hold seat 5
    Note over DB: seat 5 has heldBy = User 2 only. Last write wins.
```

**Result:** both users are told they hold the seat, but only User 2 really does. This is a race condition.

Why it happens:
- "Check then save" is not one atomic step.
- There is no `@Version` and no lock.
- `synchronized` would not help either, because with two app instances each has its own lock.

**Fixed in:** Phase 7 (distributed lock) and Phase 8 (Redis `SET NX EX`).

---

## 14. What we have and what is coming

| Problem in Phase 3 | Solved in |
|---|---|
| Every read hits the database | Phase 4 (cache) |
| Cache can show old seat status | Phase 5 (invalidation) |
| Two users can hold the same seat | Phase 7 (lock) and Phase 8 (`SET NX EX`) |
| Holds expire only when someone reads (lazy) | Phase 8 (Redis TTL) |
| Logout blacklist is in memory | Phase 10 (Redis) |
