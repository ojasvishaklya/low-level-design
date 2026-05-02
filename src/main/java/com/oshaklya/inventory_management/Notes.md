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
