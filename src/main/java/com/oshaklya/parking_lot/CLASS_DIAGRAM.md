# Parking Lot System - Class Diagram

```
┌───────────────────────┐                          ┌───────────────────────┐                          ┌───────────────────────┐
│ ParkingLotController  │──composition (1:1)───────│     ParkingLot        │──aggregation (1:many)────│       Ticket          │
├───────────────────────┤                          ├───────────────────────┤                          ├───────────────────────┤
│ -parkingLot           │                          │ -parkingSpotsMap: Map │                          │ -id: String           │
├───────────────────────┤                          │ -ticketsMap: Map      │                          │ -spotId: int          │
│ +assignSpot()         │                          │ -parkingSpotMapping   │                          │ -vehicleId: int       │
│ +exitVehicleAndCalc() │                          ├───────────────────────┤                          │ -entryTime: LocalDT   │
│ -calculateFare()      │                          │ +createTicket()       │                          │ -isUsed: boolean      │
└───────────────────────┘                          │ +getAvailableSpot()   │                          └───────────────────────┘
                                                   │ +markSpotUsed()       │
                                                   │ +markSpotVacant()     │
                                                   │ +getTicketDetails()   │
                                                   │ +getParkingSpotType() │
                                                   └───────────────────────┘
                                                              │
                                                              │ composition (1:many)
                                                              │
                                                              ▼
                         ┌───────────────────────┐          ┌───────────────────────┐
                         │       Vehicle         │          │     ParkingSpot       │
                         ├───────────────────────┤          ├───────────────────────┤
                         │ -id: int              │          │ -id: int              │
                         │ -vehicleType          │          │ -parkingSpotType      │
                         └───────────────────────┘          │ -isVacant: Boolean    │
                                  │                         └───────────────────────┘
                                  │ uses                                │
                                  ▼                                     │ uses
                         ┌───────────────────────┐                      │
                         │   «enumeration»       │                      │
                         │    VehicleType        │                      │
                         ├───────────────────────┤                      │
                         │ TWO_WHEELER           │                      │
                         │ SMALL_VEHICLE         │                      │
                         │ LARGE_VEHICLE         │                      │
                         └───────────────────────┘                      │
                                                                        ▼
                                                             ┌───────────────────────┐
                                                             │   «enumeration»       │
                                                             │  ParkingSpotType      │
                                                             ├───────────────────────┤
                                                             │ SMALL                 │
                                                             │ MEDIUM                │
                                                             │ LARGE                 │
                                                             └───────────────────────┘
```

## Relationships
- **ParkingLotController → ParkingLot**: composition (1:1) - ParkingLot lifecycle tied to controller
- **ParkingLot → Ticket**: aggregation (1:many) - Tickets created by ParkingLot but exist independently after creation
- **ParkingLot → ParkingSpot**: composition (1:many) - ParkingSpots destroyed when ParkingLot removed
- **Vehicle → VehicleType**: uses - Vehicle references enum for type classification
- **ParkingSpot → ParkingSpotType**: uses - ParkingSpot references enum for size classification
- **ParkingLot → VehicleType, ParkingSpotType**: uses - Maintains mapping between vehicle and spot types

## Core Flow
1. ParkingLotController.assignSpot(vehicle) → requests available spot from ParkingLot
2. ParkingLot.getAvailableParkingSpot(vehicle) → filters vacant spots matching vehicle type
3. ParkingLot.createTicket() → generates ticket with UUID, marks as unused
4. ParkingLot.markSpotUsed(spotId) → sets isVacant to false
5. ParkingLotController.exitVehicleAndCalculateFare(vehicle, ticketId) → validates ticket, marks spot vacant, calculates fare
6. ParkingLotController.calculateFare(entryTime) → duration in hours rounded up, multiply by rate

## Key Decisions

### Design Choices
- **Map<Integer, ParkingSpot>** → O(1) spot lookup by ID
- **Map<String, Ticket>** → O(1) ticket validation by UUID string
- **Map<VehicleType, ParkingSpotType>** → direct mapping between vehicle and compatible spot type
- **Optional.findFirst()** → stream filtering for available spots, explicit no-spot-available handling
- **Ticket.isUsed flag** → prevents ticket reuse after exit
- **UUID for ticketId** → ensures globally unique ticket identification

### Validation Strategy
- **Two-level ticket validation** → checks both vehicle ownership (vehicleId match) and usage status (isUsed flag)
- **Return -1 for no spots** → explicit sentinel value indicating unavailable parking
- **IllegalStateException** → throws for invalid spot IDs and ticket IDs
- **Ceiling function** → Math.ceil ensures partial hours are rounded up for fair billing

### Fare Calculation
- **Duration.between()** → precise time calculation using LocalDateTime
- **Hourly rate: 10 units/hour** → simple flat rate billing
- **Minute-to-hour conversion** → toMinutes() / 60 with ceiling ensures no fractional hours
