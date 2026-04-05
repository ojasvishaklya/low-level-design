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
enum ParkingSpotType [COMPACT, MEDIUM, LARGE]

class Vehicle
- vehicleType: VehicleType
- id: Int

class Ticket
- ticketId: Int
- vehicleId: Int
- spotId: Int
- parkingSpotType: ParkingSpotType
- entryTime: Long
- isUsed: Boolean

class ParkingSpot
- id: Int
- parkingSpotType: ParkingSpotType
- isVacant: Boolean

class ParkingLot
- parkingSpots: Map<Int, ParkingSpot>
- activeTickets: Map<Int, Ticket>  // ticketId -> Ticket
+ markParkingSpotVacant(parkingSpotId: Int)
+ markParkingSpotFilled(parkingSpotId: Int)
+ getAvailableParkingSpots(vehicle: Vehicle) -> List<ParkingSpot>

class ParkingLotController
- parkingLot: ParkingLot
+ assignSpot(vehicle: Vehicle) -> Int // only returns ticketId so ticket cant be altered.
+ exitVehicle(vehicle: Vehicle, ticket: Ticket) -> Boolean