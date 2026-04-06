// ═══════════════════════════════════════════════════════════════════════════
// REQUIREMENTS
// ═══════════════════════════════════════════════════════════════════════════

REQUIREMENTS:

1. Design a parking lot management system with one entry and one exit.
2. The parking lot can have multiple floors.
3. The parking lot has multiple types of parking spots and each vehicle type is mapped to one particular slot.
4. Upon entry a parking spot should be assigned to the vehicle based on the vehicle type, and a ticket is issued with a unique ID linking the vehicle to the assigned spot.
5. On exit the owner has to pay the fee based on the duration of parking, rounded up to the hour.
6. On exit the ticket is validated - if the ticket is invalid or already used, exit is rejected.
7. Spot is marked free after a successful exit.
8. System should automatically assign an available compatible spot at entry.
9. If no slots are present vehicles are denied entry.

Out of Scope:
1. Payment processing.
2. Physical gates or hardware.
3. Lost ticket handling.


// ═══════════════════════════════════════════════════════════════════════════
// ENTITIES & RELATIONSHIPS
// ═══════════════════════════════════════════════════════════════════════════

ENTITIES
1. class ParkingSpot
2. class ParkingLot
3. class ParkingLotController
4. class Ticket
5. class Vehicle
6. enum ParkingSpotType
7. enum VehicleType


// ═══════════════════════════════════════════════════════════════════════════
// CLASS DESIGN
// ═══════════════════════════════════════════════════════════════════════════

enum VehicleType [TWO_WHEELER, SMALL_VEHICLE, LARGE_VEHICLE]
enum ParkingSpotType [SMALL, MEDIUM, LARGE]

class Vehicle
- id: Int
- vehicleType: VehicleType

class Ticket
- id: String  // UUID
- spotId: Int
- vehicleId: Int
- entryTime: LocalDateTime
- isUsed: Boolean

class ParkingSpot
- id: Int
- parkingSpotType: ParkingSpotType
- isVacant: Boolean

class ParkingLot
- parkingSpotsMap: Map<Int, ParkingSpot>
- ticketsMap: Map<String, Ticket>  // ticketId (String/UUID) -> Ticket
- parkingSpotMapping: Map<VehicleType, ParkingSpotType>  // TWO_WHEELER→SMALL, SMALL_VEHICLE→MEDIUM, LARGE_VEHICLE→LARGE
+ ParkingLot(parkingSpotsMap: Map<Int, ParkingSpot>)
+ getParkingSpotType(vehicleType: VehicleType) -> ParkingSpotType
+ createTicket() -> Ticket
+ getAvailableParkingSpot(vehicle: Vehicle) -> Int  // returns spotId or -1
+ markSpotUsed(id: Int)
+ markSpotVacant(id: Int)
+ getTicketDetails(ticketId: String) -> Ticket

class ParkingLotController
- parkingLot: ParkingLot
+ assignSpot(vehicle: Vehicle) -> String  // returns ticketId (UUID String) or error message
+ exitVehicleAndCalculateFare(vehicle: Vehicle, ticketId: String) -> Int  // returns fare in dollars
+ calculateFare(startTime: LocalDateTime) -> Int  // $10/hour, rounded up


// ═══════════════════════════════════════════════════════════════════════════
// DESIGN NOTES
// ═══════════════════════════════════════════════════════════════════════════

KEY DESIGN DECISIONS:

1. Two-level validation on Ticket:
   - vehicleId match: ensures correct owner claims the ticket
   - isUsed flag: prevents ticket reuse (same ticket used twice)

2. Ticket ID is UUID String (not Int) for uniqueness across distributed systems

3. Vehicle-to-Spot Mapping (hardcoded in ParkingLot constructor):
   - TWO_WHEELER → SMALL
   - SMALL_VEHICLE → MEDIUM
   - LARGE_VEHICLE → LARGE

4. Fare Calculation:
   - Duration: difference between entryTime and exit time
   - Formula: Math.ceil(minutes/60) * $10

5. Spot Assignment Flow:
   - getAvailableParkingSpot() finds first vacant spot of correct type
   - Returns -1 if no spot available (Controller returns "No slot available")
   - createTicket() generates UUID and sets isUsed=false
   - markSpotUsed() marks spot as occupied

6. Exit Flow:
   - Validates ticket (isUsed + vehicleId)
   - Marks ticket as used (prevents reuse)
   - Frees the spot
   - Calculates and returns fare


// ═══════════════════════════════════════════════════════════════════════════
// CONCURRENCY HANDLING
// ═══════════════════════════════════════════════════════════════════════════

RACE CONDITIONS TO FIX:

1. assignSpot(): Race between getAvailableParkingSpot() and markSpotUsed()
   - Problem: Two threads can get same spot as available, both assign it
   - Fix: Make check-then-act atomic (synchronize spot assignment or use CAS)
```java
      boolean tryReserveSpot(int spotId) {                                                                                                                              
          ParkingSpot spot = parkingSpotsMap.get(spotId);                                                                                                               
          if (spot == null) return false;                                                                                                                               
                                                                                                                                                                        
          // CAS: compareAndSet(expectedValue, newValue)                                                                                                                
          // Returns true only if current value is 'true' and successfully sets to 'false'
          return spot.isVacant.compareAndSet(true, false);                                                                                                              
      } 
     class ParkingSpot {                                                                                                                                                   
         int id;                                               
         ParkingSpotType parkingSpotType;
         AtomicBoolean isVacant;  // ← Changed from Boolean to AtomicBoolean
      }
```

2. The simplest correct solution is to synchronize the entire assignSpot() method,
   - which serializes all entrance requests. This is sufficient for most parking lots.
   - If we needed higher concurrency, we could use atomic check-and-add on the parkingSpotsMap Set with retry logic.
   - For a parking lot with 3-5 entrances and typical traffic, method-level synchronization is the right choice—it's simple