# Inventory Management - Class Diagram

```
┌─────────────────────────────────────┐
│      InventoryManager               │
├─────────────────────────────────────┤
│ -warehouses: Map<Integer,Warehouse> │
├─────────────────────────────────────┤
│ +InventoryManager()                 │
│ +addItem(item,qty,whId): void       │
│ +removeItem(item,qty,whId): void    │
│ +getStock(item): int                │
│ +checkAvailability(item): boolean   │
│ +setLowStockAlert(...): void        │
│ +transferStock(...): void           │
└─────────────────────────────────────┘
                 │
                 │ composition (1:many)
                 ▼
┌─────────────────────────────────────┐
│          Warehouse                  │
├─────────────────────────────────────┤
│ -id: int                            │
│ -inventory: Map<String,Product>     │
├─────────────────────────────────────┤
│ +Warehouse(id: int)                 │
│ +addItem(item,qty): void            │
│ +removeItem(item,qty): void         │
│ +getQuantity(item): int             │
│ +getAvailable(item): boolean        │
└─────────────────────────────────────┘
                 │
                 │ composition (1:many)
                 ▼
┌─────────────────────────────────────┐
│           Product                   │
├─────────────────────────────────────┤
│ -name: String                       │
│ -quantity: int                      │
│ -alertConfigs: List<AlertConfig>    │
│ -lock: Lock                         │
├─────────────────────────────────────┤
│ +Product(name: String)              │
└─────────────────────────────────────┘
                 │
                 │ composition (1:many)
                 ▼
┌─────────────────────────────────────┐          ┌─────────────────────────────────────┐
│         AlertConfig                 │          │       «interface»                   │
├─────────────────────────────────────┤          │      AlertListener                  │
│ -threshold: int                     │          ├─────────────────────────────────────┤
│ -alertListener: AlertListener       │──────────│ +notify(message: String): void      │
├─────────────────────────────────────┤   uses   └─────────────────────────────────────┘
│ +AlertConfig(thresh,listener)       │                          ▲
└─────────────────────────────────────┘                          │
                                                                 │ implements
                                                                 │
                                                  ┌──────────────────────────────────────┐
                                                  │      EmailAlertListener              │
                                                  ├──────────────────────────────────────┤
                                                  │ +notify(message: String): void       │
                                                  └──────────────────────────────────────┘
```

## Relationships

- **InventoryManager → Warehouse**: composition (1:many) - Warehouses are created and owned by InventoryManager; they are destroyed when InventoryManager is removed (100 warehouses lifecycle tied to manager)
- **Warehouse → Product**: composition (1:many) - Products are created via computeIfAbsent and owned by Warehouse; their lifecycle is tied to the warehouse's inventory map
- **Product → AlertConfig**: composition (1:many) - AlertConfigs are created and added to Product's list; they exist only within the product's context and are destroyed with the product
- **Product → Lock**: composition (1:1) - ReentrantLock is instantiated directly in Product; lock lifecycle is tied to product instance
- **AlertConfig → AlertListener**: uses - AlertConfig holds a reference to AlertListener interface to trigger notifications; AlertListener exists independently
- **EmailAlertListener → AlertListener**: implements - EmailAlertListener provides concrete implementation of the AlertListener interface contract

## Core Flow

1. InventoryManager.addItem(item, quantity, warehouseId) → delegates to target Warehouse
2. Warehouse.addItem(item, quantity) → acquires Product lock, updates quantity, copies alertConfigs
3. Product.lock.lock() → ensures thread-safe quantity modification
4. AlertConfig.alertListener.notify(message) → fires low-stock alerts outside lock if threshold breached
5. InventoryManager.transferStock(item, qty, toId, fromId) → acquires both Product locks in deterministic order (by identityHashCode) to prevent deadlock
6. Product locks released → ensures concurrent operations remain consistent

## Key Decisions

- **Entity-level locking**: ReentrantLock in Product class enables fine-grained concurrency control, avoiding warehouse-wide locking
- **Lock ordering in transferStock**: Uses System.identityHashCode() to acquire locks in deterministic order, preventing deadlock in concurrent transfers
- **Alert firing outside lock**: AlertConfigs list copied under lock, then notifications fired after unlock to prevent callback delays from blocking inventory operations
- **ConcurrentHashMap for inventory**: Allows concurrent reads without blocking, while computeIfAbsent handles atomic Product creation
