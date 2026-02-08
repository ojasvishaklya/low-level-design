package com.oshaklya.designpatterns;

/*
Purpose: Ensure only one instance of a class exists and provide global access point to it

Common Use Cases:
- Database connections, connection pools
- Configuration managers
- Logging, caching
- Thread pools
- File managers

=== IMPLEMENTATION 1: DOUBLE-CHECKED LOCKING (DCL) ===

Key Components:
1. Private static VOLATILE instance
   - volatile prevents instruction reordering and ensures visibility across threads
   - Without volatile, another thread might see partially constructed object

2. Private constructor
   - Prevents external instantiation via 'new'

3. Double-checked locking in getInstance()
   - First check (line 57): Avoids synchronization overhead after initialization
   - Synchronized block: Ensures thread-safe initialization
   - Second check (line 59): Prevents multiple instances in race condition
     (Multiple threads could pass first check before any creates instance)

=== IMPLEMENTATION 2: BILL PUGH (RECOMMENDED) ===

Uses static inner helper class for lazy initialization

Thread Safety Explanation:
- JVM guarantees class initialization is thread-safe
- Static fields are initialized when class is first loaded
- Class loading uses ClassLoader lock internally
- The inner class SingletonHelper is loaded only when getInstance() is called (lazy)
- JVM's class initialization process provides happens-before guarantee
- No explicit synchronization needed - JVM handles it
*/

// Implementation 1: Double-Checked Locking
class DatabaseConnectionDCL {
    private static volatile DatabaseConnectionDCL instance = null;

    private DatabaseConnectionDCL() {
        // Private constructor prevents instantiation
    }

    public static DatabaseConnectionDCL getInstance() {
        if (instance == null) {  // First check (no locking)
            synchronized (DatabaseConnectionDCL.class) {
                if (instance == null) {  // Second check (with locking)
                    instance = new DatabaseConnectionDCL();
                }
            }
        }
        return instance;
    }

    public void query(String sql) {
        System.out.println("DCL: Executing query: " + sql);
    }
}
//  The key insight for Bill Pugh: The JVM's class loading mechanism is inherently thread-safe.
//  When a class is first referenced, the ClassLoader uses internal locks to ensure only one thread
//  initializes it, making explicit synchronization unnecessary.

// Implementation 2: Bill Pugh Singleton (Recommended for lazy initialization)
class DatabaseConnectionBillPugh {

    private DatabaseConnectionBillPugh() {}

    // Static inner class - loaded only when getInstance() is called
    // JVM guarantees thread-safe initialization of static fields during class loading
    private static class SingletonHelper {
        private static final DatabaseConnectionBillPugh INSTANCE = new DatabaseConnectionBillPugh();
    }

    public static DatabaseConnectionBillPugh getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public void query(String sql) {
        System.out.println("Bill Pugh: Executing query: " + sql);
    }
}

public class Singleton {
    public static void main(String[] args) {
        // Testing DCL implementation
        DatabaseConnectionDCL dcl1 = DatabaseConnectionDCL.getInstance();
        DatabaseConnectionDCL dcl2 = DatabaseConnectionDCL.getInstance();
        System.out.println("DCL: Same instance? " + (dcl1 == dcl2));
        dcl1.query("SELECT * FROM users");

        System.out.println();

        // Testing Bill Pugh implementation
        DatabaseConnectionBillPugh bp1 = DatabaseConnectionBillPugh.getInstance();
        DatabaseConnectionBillPugh bp2 = DatabaseConnectionBillPugh.getInstance();
        System.out.println("Bill Pugh: Same instance? " + (bp1 == bp2));
        bp1.query("SELECT * FROM orders");

        System.out.println();
    }
}
