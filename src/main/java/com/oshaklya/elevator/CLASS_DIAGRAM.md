# Elevator System - Class Diagram

```
┌─────────────────────┐                          ┌─────────────────────┐                          ┌─────────────────────┐
│ ElevatorController  │──composition (1:many)────│      Elevator       │──aggregation (1:many)────│  ElevatorRequest    │
├─────────────────────┤                          ├─────────────────────┤                          ├─────────────────────┤
│ -elevators: List    │                          │ -id: int            │                          │ -sourceFloor: int   │
├─────────────────────┤                          │ -floor: int         │                          │ -destFloor: int     │
│ +requestElevator()  │                          │ -direction          │                          │ -direction          │
│ +stepAhead(): void  │                          │ -requests: HashSet  │                          │ -type: RequestType  │
│ -getClosestByDir()  │                          ├─────────────────────┤                          └─────────────────────┘
└─────────────────────┘                          │ +getFloor(): int    │
                                                 │ +getDirection()     │
                                                 │ +addRequest()       │
                                                 │ +stepAhead(): void  │
                                                 │ -reqToBeServed()    │
                                                 │ -hasReqsInDir()     │
                                                 │ -openDoors(): void  │
                                                 └─────────────────────┘

┌─────────────────┐      ┌─────────────────┐
│ «enumeration»   │      │ «enumeration»   │
│   Direction     │      │  RequestType    │
├─────────────────┤      ├─────────────────┤
│ UP              │      │ HALLWAY         │
│ DOWN            │      │ INTERNAL        │
│ IDLE            │      └─────────────────┘
└─────────────────┘
```

## Relationships
- **ElevatorController → Elevator**: composition (1:many) - Elevators' lifecycle tied to controller
- **Elevator → ElevatorRequest**: aggregation (1:many) - Requests exist independently, can be created externally
- **Elevator → Direction**: uses (dependency) - Uses enum for current state
- **ElevatorRequest → Direction, RequestType**: uses (dependency) - Uses enums for request data

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
