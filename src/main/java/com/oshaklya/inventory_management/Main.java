package com.oshaklya.inventory_management;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

class Product {
    String name;
    int quantity;
    List<AlertConfig> alertConfigs;
    Lock lock = new ReentrantLock();

    Product(String name) {
        this.name = name;
        this.quantity = 0;
        this.alertConfigs = new ArrayList<>();
    }
}

class Warehouse {
    int id;
    Map<String, Product> inventory;

    Warehouse(int id) {
        this.id = id;
        inventory = new ConcurrentHashMap<>();
    }

    void addItem(String item, int quantity) {
        Product product = inventory.computeIfAbsent(item, k -> new Product(item));

        List<AlertConfig> alertConfigList;
        int currentStock;

        try {
            product.lock.lock();
            product.quantity += quantity;
            currentStock = product.quantity;
            alertConfigList = new ArrayList<>(product.alertConfigs);  // ✅ Copy list
        } finally {
            product.lock.unlock();
        }

        // Fire alerts outside lock
        for (AlertConfig config : alertConfigList) {
            if (currentStock <= config.threshold) {
                config.alertListener.notify("Stock below threshold: " + currentStock + " <= " + config.threshold);
            }
        }
    }

    void removeItem(String item, int quantity) {
        Product product = inventory.get(item);
        if (product == null) {
            throw new IllegalArgumentException("Item does not exist");
        }

        List<AlertConfig> alertConfigList;
        int currentStock;

        try {
            product.lock.lock();
            if (product.quantity < quantity) {
                throw new IllegalArgumentException("Insufficient quantity");
            }
            product.quantity -= quantity;
            currentStock = product.quantity;
            alertConfigList = new ArrayList<>(product.alertConfigs);
        } finally {
            product.lock.unlock();
        }

        // Fire alerts outside lock
        for (AlertConfig config : alertConfigList) {
            if (currentStock <= config.threshold) {
                config.alertListener.notify("Stock below threshold: " + currentStock + " <= " + config.threshold);
            }
        }
    }

    int getQuantity(String item) {
        Product product = inventory.get(item);
        if (product == null) return 0;

        try {
            product.lock.lock();
            return product.quantity;
        } finally {
            product.lock.unlock();
        }
    }

    boolean getAvailable(String item) {
        return inventory.containsKey(item);
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
            Product product = warehouse.inventory.get(item);
            if (product == null) {
                throw new IllegalArgumentException("Item does not exist");
            }
            product.lock.lock();
            product.alertConfigs.add(new AlertConfig(quantity, alertListener));
            product.lock.unlock();
        } else throw new IllegalArgumentException("Warehouse does not exist");
    }

    void transferStock(String item, int quantity, int warehouseIdTo, int warehouseIdFrom) {
        Warehouse warehouseTo = warehouses.get(warehouseIdTo);
        Warehouse warehouseFrom = warehouses.get(warehouseIdFrom);
        if (warehouseFrom == null || warehouseTo == null) {
            throw new IllegalArgumentException("Warehouse does not exist");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        Product productFrom = warehouseFrom.inventory.get(item);
        if (productFrom == null) {
            throw new IllegalArgumentException("Item does not exist in source warehouse");
        }
        Product productTo = warehouseTo.inventory.computeIfAbsent(item, k -> new Product(item));
        Product first, second;
        if (System.identityHashCode(productFrom) < System.identityHashCode(productTo)) {
            first = productFrom;
            second = productTo;
        } else {
            first = productTo;
            second = productFrom;
        }

        try {
            first.lock.lock();
            try {
                second.lock.lock();

                if (productFrom.quantity < quantity) {
                    throw new IllegalArgumentException("Insufficient quantity in source warehouse");
                }

                productFrom.quantity -= quantity;
                productTo.quantity += quantity;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }
}

public class Main {
    public static void main(String[] args) {

    }
}
