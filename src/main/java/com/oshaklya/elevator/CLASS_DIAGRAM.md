# Elevator System - Class Diagram

```
┌─────────────────────┐                          ┌─────────────────────┐                          ┌─────────────────────┐
│ ElevatorController  │──composition (1:many)────│      Elevator       │──aggregation (1:many)────│  ElevatorRequest    │
├─────────────────────┤                          ├─────────────────────┤                          ├─────────────────────┤
│ -elevators: List    │                          │ -id: int            │                          │ -sourceFloor: int   │
├─────────────────────┤                          │ -floor: int         │                          │ -destFloor: int     │
│ +requestElevator()  │                          │ -direction          │                          │ -direction          │
│ +stepAhead(): void  │                          │ -requests: HashSet  │                          │ -type: RequestType  │
│ -getClosestByDir()  │                          │ -threadSafeReqs: LBD│                          └─────────────────────┘
└─────────────────────┘                          ├─────────────────────┤
                                                 │ +getFloor(): int    │
                                                 │ +getDirection()     │
                                                 │ +addRequest()       │
                                                 │ +threadSafeAdd()    │
                                                 │ +stepAhead(): void  │
                                                 │ +threadSafeStep()   │
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

### Single-Threaded (Basic)
1. ElevatorController.requestElevator(request) → validates bounds, finds closest
2. getClosestElevatorByDirection() → priority: same dir > idle > opposite dir
3. Elevator.addRequest(request) → sets direction if IDLE, adds to HashSet
4. Controller.stepAhead() → all elevators advance simultaneously (single thread)
5. Elevator.stepAhead() → check floor → serve request OR toggle direction → move

### Concurrent (Thread-Safe)
1. Multiple threads call Elevator.threadSafeAddRequest(request) → uses put() on LinkedBlockingDeque
2. Single controller thread calls threadSafeStepAhead() → drains queue to HashSet
3. threadSafeStepAhead() calls stepAhead() → processes HashSet (thread-confined)
4. Concurrent writes to queue, single reader processes → producer-consumer pattern

## Key Decisions

### Single-Threaded Design
- HashSet<ElevatorRequest> → O(1) ops, no duplicates
- Optional<T> returns → explicit null handling
- Stream API → filtering & min distance calculation
- Position validation → UP: elevator ≤ source, DOWN: elevator ≥ source

### Concurrent Extension (Interview Follow-Up)
- **LinkedBlockingDeque (capacity 1000)** → thread-safe queue for concurrent writes
- **put() method** → blocking write, waits if full (no dropped requests)
- **Thread confinement** → stepAhead() called by single controller thread only
- **Producer-consumer pattern** → multiple threads write to queue, single thread drains and processes
- **No synchronization needed** → requests HashSet only accessed by processing thread (thread-confined)
- **Assumption**: Controller is single-threaded, calls stepAhead() sequentially
