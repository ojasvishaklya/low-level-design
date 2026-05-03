// ═══════════════════════════════════════════════════════════════════════════
// REQUIREMENTS
// ═══════════════════════════════════════════════════════════════════════════

REQUIREMENTS:

1. Track inventory for products across multiple warehouses
2. Add stock to a specific warehouse (receiving shipments)
3. Remove stock from a specific warehouse (fulfilling orders)
4. Check availability: given a product and quantity, return which warehouses can fulfill it
5. Transfer stock between warehouses
6. Low-stock alerts
7. Reject operations that would result in negative inventory
8. System must be thread-safe to handle concurrent operations

Out of Scope:
- Product catalog management (products exist externally)
- Order processing / payment / serviceability
- Persistence

// ═══════════════════════════════════════════════════════════════════════════
// ENTITIES & RELATIONSHIPS
// ═══════════════════════════════════════════════════════════════════════════

1. class Product
2. class Warehouse
3. class InventoryManager
4. class AlertConfig: per product alerting thresholds
5. class AlertListener: interface to implement different alerting

// ═══════════════════════════════════════════════════════════════════════════
// CLASS DESIGN
// ═══════════════════════════════════════════════════════════════════════════

class InventoryManager:
- warehouses: Map<string, Warehouse>
+ InventoryManager(warehouseIds)
+ addStock(warehouseId, productId, quantity) -> void
+ removeStock(warehouseId, productId, quantity) -> boolean
+ transfer(productId, fromWarehouseId, toWarehouseId, quantity) -> boolean
+ getWarehousesWithAvailability(productId, quantity) -> List<string>
+ setLowStockAlert(warehouseId, productId, threshold, listener) -> void

class Warehouse:
- id: string
- inventory: Map<string, int> // productId → quantity
- alertConfigs: Map<string, List<AlertConfig>> // productId → alert
+ Warehouse(id)
+ addStock(productId, quantity) -> void
+ removeStock(productId, quantity) -> boolean
+ getStock(productId) -> int
+ checkAvailability(productId, quantity) -> boolean
+ setLowStockAlert(productId, threshold, listener, warehouseId) -> void
- alertIfThresholdCrossed(productId, previousQty, newQty) -> List<AlertListener>

- class AlertConfig:
- threshold: int
- listener: AlertListener
+ AlertConfig(threshold, listener)
+ getThreshold() -> int
+ getListener() -> AlertListener

class AlertListener:
+ onLowStock(warehouseId, productId, currentQuantity) -> void

// ═══════════════════════════════════════════════════════════════════════════
// DESIGN DECISIONS
// ═══════════════════════════════════════════════════════════════════════════

PATTERN: Entity-Level Locking (Product-Level)
- Each Product has its own ReentrantLock
- Only locks the specific product being modified
- Why: Maximum concurrency - operations on different products don't contend
- Alternative rejected: Warehouse-level lock would serialize all operations in same warehouse

PATTERN: Observer Pattern (Alert System)
- AlertListener interface for notification callbacks
- Warehouse/Product maintains list of AlertConfig (threshold + listener)
- Alerts fired when quantity drops below threshold
- Decouples inventory logic from alert handling

CONCURRENCY: Ordered Lock Acquisition
- transferStock locks two products to move stock atomically
- Locks acquired in consistent order using System.identityHashCode()
- Prevents deadlock when concurrent transfers involve same products in reverse order
- Pattern: if (hashCode1 < hashCode2) lock first1 then first2, else first2 then first1

CONCURRENCY: Alert Firing Outside Locks
- Alert configs copied to local list before firing
- Alerts fired after releasing product lock
- Why: Avoid holding lock during I/O or external callbacks
- Prevents deadlock if alert listener tries to query inventory

THREAD-SAFETY: ConcurrentHashMap + computeIfAbsent
- Warehouse uses ConcurrentHashMap<String, Product>
- computeIfAbsent() atomically creates products on first access
- No explicit locking needed for product creation
- Why: Prevents duplicate product creation under concurrent access

LOCK DISCIPLINE: try-finally Pattern
- Every lock.lock() paired with try-finally-unlock
- Ensures lock released even if exception thrown
- Critical for preventing permanent lock acquisition

// ═══════════════════════════════════════════════════════════════════════════
// KEY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

Product.addQuantity(quantity):
1. Acquire product lock
2. Add quantity to current quantity
3. Release lock
- Note: No alert check on add (only on remove)

Product.removeQuantity(quantity):
1. Acquire product lock
2. Check if sufficient quantity available
3. Subtract quantity from current quantity
4. Copy alertConfigs to local list
5. Release lock
6. Fire alerts for each config where quantity < threshold (outside lock)

Warehouse.getOrCreateProduct(productName):
- Use computeIfAbsent(productName, name -> new Product(name))
- Atomic operation: only one thread creates product
- Returns existing product if already present

InventoryManager.transferStock(fromWarehouseId, toWarehouseId, productName, quantity):
1. Get source and target products (create if needed)
2. Compute identityHashCode for each product
3. Acquire locks in consistent order (lower hash first)
4. Check source has sufficient quantity
5. Remove quantity from source
6. Add quantity to target
7. Copy source alertConfigs
8. Release both locks (reverse order)
9. Fire alerts for source product

Warehouse.checkAvailability(productId, quantity):
- Look up product in inventory map
- Compare current stock with requested quantity
- Return boolean (no lock needed if using atomic read)

InventoryManager.getWarehousesWithAvailability(productId, quantity):
- Iterate through all warehouses
- Check each warehouse's availability
- Return list of warehouse IDs that can fulfill request

// ═══════════════════════════════════════════════════════════════════════════
// CONCURRENCY HANDLING
// ═══════════════════════════════════════════════════════════════════════════

PATTERN: ConcurrentHashMap + Per-Entity Lock
- Warehouse inventory map is ConcurrentHashMap
- Each Product has its own ReentrantLock
- Map operations thread-safe, lock only for product state mutation
- Why: Operations on different products fully parallel

RACE CONDITIONS PREVENTED:

removeStock():
- Check-then-act: verify quantity sufficient, then decrement
- Without lock: Thread A checks quantity=10, Thread B checks quantity=10, both remove 8 → negative quantity
- With lock: check-decrement atomic per product

transferStock():
- Nested locks on two products
- Without ordering: Transfer A→B and Transfer B→A can deadlock
- With ordering: Both transfers acquire locks in same order (e.g., lower hash first) → no circular wait

Alert firing:
- alertConfigs copied before releasing lock
- Without copy: concurrent setLowStockAlert() could modify list during iteration → ConcurrentModificationException
- With copy: snapshot captured under lock, iteration safe after release

Warehouse.getOrCreateProduct():
- computeIfAbsent atomically creates product
- Without atomic operation: Two threads check product absent, both create new Product(name), one overwrites other → lost alert configs
- With computeIfAbsent: Only one thread creates, others receive existing instance

WHY NOT WAREHOUSE-LEVEL LOCK:
- Warehouse lock would serialize all operations in same warehouse
- Current design: 1000 products in warehouse can be modified in parallel
- Only contention: concurrent operations on same product

WHY NOT READ-WRITE LOCK:
- getStock() could use read lock, addStock/removeStock use write lock
- Trade-off: ReadWriteLock more complex, write starvation risk
- Current design simpler with ReentrantLock
- Could optimize for read-heavy workloads

DEADLOCK PREVENTION IN TRANSFERS:
- Problem: Transfer A→B acquires lockA then lockB, Transfer B→A acquires lockB then lockA → deadlock
- Solution: Total ordering using identityHashCode
- Both transfers acquire locks in same order regardless of source/target
- If hash collision (rare), still safe (one blocks on first lock)

// ═══════════════════════════════════════════════════════════════════════════
// INTERVIEW TALKING POINTS
// ═══════════════════════════════════════════════════════════════════════════

Q: Why product-level locking instead of warehouse-level?
A: Warehouse might have 10,000 products. Warehouse lock would serialize all operations. Product lock allows 10,000 parallel operations (one per product). Only same product operations contend.

Q: How does transferStock avoid deadlock?
A: Ordered lock acquisition using identityHashCode. If transfer A→B and B→A happen concurrently, both acquire locks in same order (e.g., lower hash first). Prevents circular wait condition required for deadlock.

Q: Why fire alerts outside the lock?
A: Alert listener might do I/O (log to file, send HTTP request). Holding lock during I/O blocks other operations on same product. Copy configs under lock, fire after release. Trade-off: alert sees snapshot, not live state.

Q: Why ConcurrentHashMap if you have product locks?
A: Map operations (get, computeIfAbsent) are thread-safe. Lock is only for product quantity mutation. Don't need to hold lock during map lookup. Avoids lock contention on unrelated products.

Q: What if computeIfAbsent creates product twice?
A: It won't. computeIfAbsent is atomic - only one thread executes the mapping function. Other threads wait and receive the created instance. Critical for preventing duplicate products with separate locks.

Q: What if alert listener calls back into inventory?
A: Safe because alert fired outside lock. Listener can call getStock() without deadlock. If listener called under lock and tried to acquire another product lock, could deadlock.

Q: Why copy alertConfigs before firing?
A: Prevents ConcurrentModificationException if another thread calls setLowStockAlert() while iterating. Snapshot consistency: all alerts see same quantity value.

Q: How would you add read-heavy optimization?
A: Replace ReentrantLock with ReadWriteLock. getStock() uses readLock(), mutations use writeLock(). Allows concurrent reads. Trade-off: more complex lock management, write starvation risk.

Q: What if identityHashCode collides?
A: Rare but possible. If hash1 == hash2, ordering falls back to arbitrary choice (e.g., source first). Still safe - one transfer blocks on first lock, no deadlock. Could use fallback tiebreaker like product name comparison.

Q: Why not AtomicInteger for quantity?
A: Need to atomically: check quantity >= removeAmount, decrement, copy alertConfigs, check thresholds. AtomicInteger only atomizes single operation. compareAndSet() insufficient for multi-step logic with alerts.

Q: How to prevent negative quantity?
A: removeStock() checks quantity >= removeAmount under lock before decrementing. Returns false if insufficient, caller must handle. Transaction rolls back (no partial state change).

Q: How would you add distributed inventory?
A: Replace in-memory maps with distributed cache (Redis/Hazelcast). Use distributed locks (Redlock) or optimistic concurrency (version numbers + CAS). Trade-off: network latency vs coordination across nodes.

Q: What about transaction support across warehouses?
A: Two-phase locking: acquire all locks, validate all operations, commit all, release all locks. Requires global ordering to prevent deadlock. Or use optimistic concurrency with version numbers and retry on conflict.
