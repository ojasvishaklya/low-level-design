# Singleton Pattern - Class Diagram

```
IMPLEMENTATION 1: DOUBLE-CHECKED LOCKING (DCL)

┌─────────────────────────────────────┐
│     DatabaseConnectionDCL           │
├─────────────────────────────────────┤
│ -instance: volatile static DCL      │
├─────────────────────────────────────┤
│ -DatabaseConnectionDCL()            │  private constructor
│ +getInstance(): static DCL          │
│ +query(sql: String): void           │
└─────────────────────────────────────┘

IMPLEMENTATION 2: BILL PUGH (RECOMMENDED)

┌─────────────────────────────────────┐
│   DatabaseConnectionBillPugh        │
├─────────────────────────────────────┤
│ -DatabaseConnectionBillPugh()       │  private constructor
│ +getInstance(): static BillPugh     │
│ +query(sql: String): void           │
└─────────────────────────────────────┘
         │
         │ composition (1:1)
         │ (static inner class)
         ▼
┌─────────────────────────────────────┐
│      SingletonHelper                │
├─────────────────────────────────────┤
│ -INSTANCE: static final BillPugh    │
└─────────────────────────────────────┘
```

## Relationships
- **DatabaseConnectionBillPugh → SingletonHelper**: composition (1:1) - Inner class holds single instance, lifecycle managed by JVM

## Core Flow

### DCL Implementation:
1. Client calls getInstance() → first null check (no lock)
2. If null → synchronized block entered
3. Second null check inside lock → prevents race condition
4. Creates instance if still null → volatile ensures visibility
5. Returns singleton instance → same object always

### Bill Pugh Implementation:
1. Client calls getInstance() → triggers SingletonHelper class loading
2. JVM loads SingletonHelper → thread-safe by specification
3. Static INSTANCE initialized → happens exactly once
4. Returns singleton instance → lazy initialization without explicit locking

## Key Decisions

### DCL Approach:
- **volatile keyword** → prevents instruction reordering, ensures memory visibility across threads
- **Double-checked locking** → avoids synchronization overhead after initialization
- **First check (no lock)** → fast path for already-initialized case
- **Second check (with lock)** → prevents multiple instances in race condition

### Bill Pugh Approach (RECOMMENDED):
- **Static inner class** → loaded only when getInstance() called (lazy initialization)
- **JVM-level thread safety** → ClassLoader guarantees thread-safe class initialization
- **No explicit synchronization** → JVM handles locking during class loading
- **Simpler and cleaner** → no volatile, no explicit locks, relies on JVM guarantees

### Comparison:
- Bill Pugh is preferred → simpler, no volatile needed, JVM-guaranteed thread safety
- DCL useful when you need additional logic in getInstance() beyond returning instance
- Both prevent external instantiation via private constructor
