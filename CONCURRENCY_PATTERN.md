# Entity-Level Locking Pattern

## Pattern Overview
**Problem:** Multiple threads modifying shared state of independent entities (clients, seats, inventory, accounts)  
**Solution:** ConcurrentHashMap + per-entity ReentrantLock

## When to Use
✅ Multiple independent entities (different clients don't affect each other)  
✅ Need to modify entity state atomically (read-check-modify)  
✅ Want maximum concurrency (Entity A and Entity B operations don't block each other)

## Core Structure

```java
class EntityState {
    // Business state fields
    int field1;
    String field2;
    
    // Lock for THIS entity only
    Lock lock = new ReentrantLock();
}

class Service {
    // ConcurrentHashMap ensures atomic computeIfAbsent
    Map<EntityId, EntityState> entitiesById = new ConcurrentHashMap<>();
    
    public Response operation(EntityId id) {
        // Step 1: Get or create entity (atomic via computeIfAbsent)
        EntityState entity = entitiesById.computeIfAbsent(
            id, 
            k -> new EntityState()
        );
        
        // Step 2: Lock THIS entity only
        try {
            entity.lock.lock();
            
            // Step 3: Read-Check-Modify atomically
            // - Read current state
            // - Validate business rules
            // - Modify state
            // - Return result
            
        } finally {
            entity.lock.unlock();  // ALWAYS unlock
        }
    }
}
```

## Implementation Checklist

### 1. Entity State Class
```java
class EntityState {
    // Business fields
    Lock lock = new ReentrantLock();  // ✅ Add lock
}
```

### 2. Use ConcurrentHashMap
```java
// ❌ Wrong
Map<String, EntityState> map = new HashMap<>();

// ✅ Correct
Map<String, EntityState> map = new ConcurrentHashMap<>();
```

**Why ConcurrentHashMap even if map structure is immutable after construction?**

If you create all entities in the constructor and NEVER call `put`/`remove`/`clear` after construction:
```java
class Service {
    private final Map<String, EntityState> entities;
    
    Service() {
        this.entities = new ConcurrentHashMap<>();
        // All entities created HERE before publication
        for (...) entities.put(...);
    }
    
    void operation(String id) {
        EntityState entity = entities.get(id);  // Only reads, no structural changes
        entity.lock.lock();
        // modify entity state
    }
}
```

**Two valid options:**

```java
// Option 1: ConcurrentHashMap (Recommended)
// ✅ Explicit concurrency support
// ✅ Safe concurrent iteration (won't throw ConcurrentModificationException)
// ✅ Self-documenting - code signals "multiple threads access this"
// ✅ Future-proof if someone adds dynamic creation later
private final Map<String, EntityState> entities = new ConcurrentHashMap<>();

// Option 2: HashMap + unmodifiableMap (Also valid)
// ✅ Makes immutability explicit
// ✅ Prevents accidental modification
// ⚠️ Must ensure no concurrent iteration during construction
Map<String, EntityState> temp = new HashMap<>();
for (...) temp.put(...);
this.entities = Collections.unmodifiableMap(temp);
```

**Use ConcurrentHashMap when:**
- Entities created dynamically at runtime (need `computeIfAbsent`)
- Multiple threads iterate/read concurrently
- Want explicit concurrency intent

**Use HashMap + unmodifiableMap when:**
- All entities created in constructor (fixed set)
- Want to prevent accidental modification
- Immutability is a key design constraint

### 3. Atomic Entity Retrieval
```java
// ❌ Wrong: Race condition, multiple threads create different objects
EntityState entity = map.get(id);
if (entity == null) {
    entity = new EntityState();
    map.put(id, entity);
}

// ✅ Correct: Atomic, all threads get same object
EntityState entity = map.computeIfAbsent(id, k -> new EntityState());
```

### 4. Lock Per Entity
```java
// ❌ Wrong: All entities serialize through one lock
synchronized(map) {
    // modify entity
}

// ✅ Correct: Only same entity operations block each other
try {
    entity.lock.lock();
    // modify entity
} finally {
    entity.lock.unlock();
}
```

### 5. Try-Finally Pattern
```java
// ❌ Wrong: Lock not released if exception
entity.lock.lock();
// business logic (might throw exception)
entity.lock.unlock();

// ✅ Correct: Always unlocks
try {
    entity.lock.lock();
    // business logic
} finally {
    entity.lock.unlock();
}
```

## Common Mistakes

### ❌ Using Non-Thread-Safe Collections in EntityState
```java
class EntityState {
    Queue<Long> timestamps = new ArrayDeque<>();  // ❌ NOT thread-safe
    Lock lock = new ReentrantLock();
}
// ArrayDeque corrupts even WITH lock if accessed outside lock
```

### ❌ Forgetting to Lock
```java
EntityState entity = map.get(id);
entity.field1++;  // ❌ No lock, race condition!
```

### ❌ Check-Then-Act Without Lock
```java
if (entity.count < MAX) {  // ❌ Check outside lock
    entity.lock.lock();
    try {
        entity.count++;     // Another thread might have incremented
    } finally {
        entity.lock.unlock();
    }
}

// ✅ Correct: Check inside lock
entity.lock.lock();
try {
    if (entity.count < MAX) {
        entity.count++;
    }
} finally {
    entity.lock.unlock();
}
```

### ❌ Holding Lock During I/O
```java
entity.lock.lock();
try {
    entity.status = PROCESSING;
    httpClient.call(url);  // ❌ Network call holds lock too long!
} finally {
    entity.lock.unlock();
}
```

## Pattern Application Examples

| Domain | Entity | EntityId | What Lock Protects | Map Type |
|--------|--------|----------|-------------------|----------|
| **Rate Limiter** | Client bucket | clientId | Token count, refill timestamp | ConcurrentHashMap (dynamic creation) |
| **Movie Booking** | Seat | seatId (e.g., "A1") | Status (available/reserved/booked) | ConcurrentHashMap (fixed, but safe iteration) |
| **Inventory** | Product stock | productId+warehouseId | Available/reserved/shipped counts | ConcurrentHashMap (dynamic creation) |
| **Bank Account** | Account | accountId | Balance, transaction history | ConcurrentHashMap (dynamic creation) |
| **Parking Lot** | Parking spot | spotId | Occupied status, vehicle info | ConcurrentHashMap (fixed, but safe iteration) |
| **Elevator** | Elevator car | elevatorId | Current floor, direction, requests | ConcurrentHashMap (fixed, but safe iteration) |

## Real-World Examples

### Movie Booking System (Fixed Entity Set)
```java
class Seat {
    char row;
    int number;
    SeatStatus status;
    Reservation reservation;
    Lock lock = new ReentrantLock();
    
    Seat(char row, int number) {
        this.row = row;
        this.number = number;
        this.status = SeatStatus.AVAILABLE;
    }
    
    String getId() { return row + String.valueOf(number); }
}

class Show {
    // ConcurrentHashMap even though seats never added/removed after construction
    // Reason: Safe concurrent iteration, explicit threading intent
    private final Map<String, Seat> seats = new ConcurrentHashMap<>();
    
    Show(Screen screen, ...) {
        // All seats created in constructor
        for (Seat templateSeat : screen.getSeats()) {
            Seat seat = new Seat(templateSeat.getRow(), templateSeat.getNumber());
            seats.put(seat.getId(), seat);  // ← No put() after this
        }
    }
    
    Reservation bookSeats(List<String> seatIds) {
        // Get seats from map
        List<Seat> seatsToBook = new ArrayList<>();
        for (String seatId : seatIds) {
            Seat seat = seats.get(seatId);  // ← Only reads, never put()
            seatsToBook.add(seat);
        }
        
        // Sort and lock
        seatsToBook.sort(Comparator.comparing(Seat::getId));
        for (Seat seat : seatsToBook) {
            seat.lock.lock();
        }
        
        try {
            // Check and book atomically
            for (Seat seat : seatsToBook) {
                if (seat.status != SeatStatus.AVAILABLE) return null;
            }
            for (Seat seat : seatsToBook) {
                seat.status = SeatStatus.RESERVED;
                seat.reservation = new Reservation(...);
            }
            return reservation;
        } finally {
            for (Seat seat : seatsToBook) {
                seat.lock.unlock();
            }
        }
    }
}
```

**Key Points:**
- Map structure is immutable after construction (fixed seat layout)
- ConcurrentHashMap used for safe iteration and explicit intent
- Each Seat is an independent entity with its own lock
- Booking multiple seats uses ordered locking to prevent deadlock

## When NOT to Use

❌ **Operations span multiple entities** (transfer between accounts → need distributed lock or 2PC)  
❌ **Entities are NOT independent** (order of operations matters across entities)  
❌ **Single shared resource** (connection pool → use BlockingQueue or Semaphore)  
❌ **Read-heavy workload** (consider ReadWriteLock instead)  
❌ **Distributed system** (need distributed locks like Redis/Zookeeper)

## Quick Validation Questions

Ask these to determine if pattern applies:

1. **Can Entity A and Entity B be modified independently?** → If YES, pattern applies
2. **Do you need atomic compound operations on entity state?** → If YES, pattern applies
3. **Are entities short-lived or do they accumulate?** → If accumulate, add cleanup
4. **Do operations span multiple entities atomically?** → If YES, pattern doesn't apply (need different approach)
