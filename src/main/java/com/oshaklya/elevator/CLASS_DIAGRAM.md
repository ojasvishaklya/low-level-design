# Elevator System - Class Diagram

                             +1        *1                      +1        0..*
┌──────────────────────┐                ┌──────────────────────┐                ┌──────────────────────┐
│  ElevatorController  │────────────────│      Elevator        │────────────────│   ElevatorRequest    │
├──────────────────────┤                ├──────────────────────┤                ├──────────────────────┤
│ -elevators: List     │                │ -id: int             │                │ -sourceFloor: int    │
├──────────────────────┤                │ -floor: int          │                │ -destFloor: int      │
│ +requestElevator()   │                │ -direction           │                │ -direction           │
│ +stepAhead(): void   │                │ -requests: HashSet   │                │ -type: RequestType   │
│ -getClosestByDir()   │                ├──────────────────────┤                └──────────────────────┘
└──────────────────────┘                │ +getFloor(): int     │
                                        │ +getDirection()      │
                                        │ +addRequest()        │
                                        │ +stepAhead(): void   │
                                        │ -reqToBeServed()     │
                                        │ -hasReqsInDir()      │
                                        │ -openDoors(): void   │
                                        └──────────────────────┘

┌────────────────┐      ┌────────────────┐
│ «enumeration»  │      │ «enumeration»  │
│   Direction    │      │  RequestType   │
├────────────────┤      ├────────────────┤
│ UP             │      │ HALLWAY        │
│ DOWN           │      │ INTERNAL       │
│ IDLE           │      └────────────────┘
└────────────────┘

## Cardinality Legend
+1 or 1   = exactly one
*1 or *   = zero or many
0..*      = zero to many
1..*      = one to many
0..1      = optional (zero or one)

## Relationships
- ElevatorController HAS-A List<Elevator> (1-to-many composition)
- Elevator HAS-A HashSet<ElevatorRequest> (1-to-many aggregation)
- ElevatorRequest USES Direction, RequestType (dependency)

## Core Flow
1. ElevatorController.requestElevator(request) → validates bounds, finds closest
2. getClosestElevatorByDirection() → priority: same dir > idle > opposite dir
3. Elevator.addRequest(request) → sets direction if IDLE, adds to HashSet
4. Controller.stepAhead() → all elevators advance simultaneously
5. Elevator.stepAhead() → check floor → serve request OR toggle direction → move

## Key Decisions
- HashSet<ElevatorRequest> → O(1) ops, no duplicates
- Optional<T> returns → explicit null handling
- Stream API → filtering & min distance calculation
- Position validation → UP: elevator ≤ source, DOWN: elevator ≥ source
