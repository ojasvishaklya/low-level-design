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

| Domain | Entity | EntityId | What Lock Protects |
|--------|--------|----------|-------------------|
| **Rate Limiter** | Client bucket | clientId | Token count, refill timestamp |
| **Movie Booking** | Seat | seatNumber | Status (available/reserved/booked) |
| **Inventory** | Product stock | productId+warehouseId | Available/reserved/shipped counts |
| **Bank Account** | Account | accountId | Balance, transaction history |
| **Parking Lot** | Parking spot | spotId | Occupied status, vehicle info |
| **Elevator** | Elevator car | elevatorId | Current floor, direction, requests |

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
