package com.oshaklya.inventory_management;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface AlertListener {
    void notify(String message);
}

class EmailAlertListener implements AlertListener {
    @Override
    public void notify(String message) {
        System.out.println("Email Alert: " + message);
    }
}

class AlertConfig {
    int threshold;
    AlertListener alertListener;

    AlertConfig(int threshold, AlertListener alertListener) {
        this.threshold = threshold;
        this.alertListener = alertListener;
    }
}

class Warehouse {
    int id;
    Map<String, List<AlertConfig>> alertConfigs;
    Map<String, Integer> inventory;

    Warehouse(int id) {
        this.id = id;
        alertConfigs = new HashMap<>();
        inventory = new HashMap<>();
    }

    void addItem(String item, int quantity) {
        List<AlertConfig> alertConfigList;

        synchronized (inventory) {
            inventory.put(item, inventory.getOrDefault(item, 0) + quantity);
            alertConfigList = getAlertsToFire(item, quantity);
        }
        // doing this outside synchronized as firing alerts can take time
        if (alertConfigList != null)
            alertConfigList.forEach(config -> {
                if (config.threshold >= quantity) {
                    config.alertListener.notify("product quantity is below threshold" + config.threshold);
                }
            });
    }

    void removeItem(String item, int quantity) {
        List<AlertConfig> alertConfigList;
        synchronized (inventory) {
            if (inventory.containsKey(item) && inventory.get(item) >= quantity) {
                inventory.put(item, inventory.get(item) - quantity);

            } else {
                throw new IllegalArgumentException("Item does not exist");
            }
            alertConfigList = getAlertsToFire(item, quantity);
        }
        if (alertConfigList != null)
            alertConfigList.forEach(config -> {
                if (config.threshold >= quantity) {
                    config.alertListener.notify("product quantity is below threshold" + config.threshold);
                }
            });
    }

    int getQuantity(String item) {
        synchronized (inventory) {
            return inventory.getOrDefault(item, 0);
        }
    }

    boolean getAvailable(String item)  {
        synchronized (inventory) {
            return inventory.containsKey(item);
        }
    }

    void setLowStockAlert(String item, int quantity, AlertListener alertListener) {
        alertConfigs.getOrDefault(item, new ArrayList<>()).add(new AlertConfig(quantity, alertListener));
    }

    private List<AlertConfig> getAlertsToFire(String item, int quantity) {
        if (!alertConfigs.containsKey(item)) {
            return null;
        }
        return alertConfigs.get(item);
    }
}

class InventoryManager {
    Map<Integer, Warehouse> warehouses;

    InventoryManager() {
        warehouses = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            Warehouse warehouse = new Warehouse(i);
            warehouses.put(i, warehouse);
        }
    }

    void addItem(String item, int quantity, int warehouseId) {
        Warehouse warehouse = warehouses.get(warehouseId);
        warehouse.addItem(item, quantity);
    }

    void removeItem(String item, int quantity, int warehouseId) {
        Warehouse warehouse = warehouses.get(warehouseId);
        warehouse.removeItem(item, quantity);
    }

    int getStock(String item) {
        return warehouses.values().stream().filter(it -> it.getAvailable(item))
                .map(warehouse -> warehouse.getQuantity(item))
                .reduce(Integer::sum).orElse(0);
    }

    boolean checkAvailability(String item) {
        return warehouses.values().stream().anyMatch(it -> it.getAvailable(item));
    }

    void setLowStockAlert(int warehouseId, String item, int quantity, AlertListener alertListener) {
        if (warehouses.containsKey(warehouseId)) {
            Warehouse warehouse = warehouses.get(warehouseId);
            warehouse.setLowStockAlert(item, quantity, alertListener);
        } else throw new IllegalArgumentException("Warehouse does not exist");
    }

    void transferStock(String item, int quantity, int warehouseIdTo, int warehouseIdFrom) {
        Warehouse warehouseTo = warehouses.get(warehouseIdTo);
        Warehouse warehouseFrom = warehouses.get(warehouseIdFrom);
        if (warehouseFrom == null || warehouseTo == null) {
            throw new IllegalArgumentException("Warehouse does not exist");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        synchronized (warehouses.get(Math.max(warehouseIdTo, warehouseIdFrom))) {
            synchronized (warehouses.get(Math.min(warehouseIdTo, warehouseIdFrom))) {
                if (warehouseFrom.getQuantity(item) < quantity) {
                    throw new IllegalArgumentException("Item does not exist");
                }
            }
            warehouseTo.addItem(item, quantity);
            warehouseFrom.removeItem(item, quantity);
        }
    }
}

public class Main {
    public static void main(String[] args) {

    }
}
