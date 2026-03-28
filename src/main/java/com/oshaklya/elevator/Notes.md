// ═══════════════════════════════════════════════════════════════════════════
// REQUIREMENTS
// ═══════════════════════════════════════════════════════════════════════════

REQUIREMENTS:
1. Design an elevator control system for building with 10 floors with 3 elevators.
2. The users can press the direction they want to go to using buttons in the hallway.
3. The system should assign one elevator to one user request.
4. Once the user is inside an elevator they can press which floor they want to go to.
5. We should invalidate incorrect direction and floors request.
6. There should be a ticker that simulates passage of time and movement of elevators.

Out of scope:
1. Things like weight limits, door mechanics, or emergency stops


// ═══════════════════════════════════════════════════════════════════════════
// ENTITIES & RELATIONSHIPS
// ═══════════════════════════════════════════════════════════════════════════

ENTITIES
1. class elevator
2. class elevator controller
3. class elevator request
4. enum direction [UP, DOWN, IDLE]
5. enum request type [HALLWAY, INTERNAL]


// ═══════════════════════════════════════════════════════════════════════════
// CLASS DESIGN
// ═══════════════════════════════════════════════════════════════════════════

class Elevator
- direction : Direction
- floor: int
- requests: Queue/ Set <ElevatorRequest>
+ getDirection() -> Direction
+ getFloor() -> int
+ addRequest(request: ElevatorRequest) -> bool
+ stepAhead()

class ElevatorController
- elevators: List<Elevator>
+ requestElevator(request: ElevatorRequest) -> int
+ stepAhead()

class ElevatorRequest
- sourceFloor: int
- destinationFloor: int
- direction: Direction
- requestType: RequestType


// ═══════════════════════════════════════════════════════════════════════════
// LLD IMPLEMENTATION NOTES
// ═══════════════════════════════════════════════════════════════════════════

KEY IMPLEMENTATION DETAILS:

1. Elevator.addRequest(ElevatorRequest)
   • Set direction from IDLE to request direction on first request
   • Use HashSet to prevent duplicate request
   • Return boolean for success/failure

2. Elevator.stepAhead()
   • Single step = move 1 floor OR stop at a floor
   • Check if current floor has request to serve → open doors & remove request
   • If no requests in current direction → toggle direction (UP ↔ DOWN)
   • Move floor: UP (floor+1) or DOWN (floor-1)
   • Set direction to IDLE if no requests remain

3. Elevator.requestToBeServedOnCurrentFloor()
   • Returns Optional<ElevatorRequest>
   • Drop passenger: destinationFloor == currentFloor
   • Pick passenger: sourceFloor == currentFloor AND same direction
   • Use stream().filter().findFirst() for matching request

4. Elevator.hasRequestsInCurrentDirection()
   • Check if any request source/destination is ahead in current direction
   • UP: request floor >= currentFloor
   • DOWN: request floor <= currentFloor
   • Determines if elevator should continue or toggle direction

5. ElevatorController.requestElevator(ElevatorRequest)
   • Validate floor bounds (0-9 for 10-floor building)
   • Priority order for assignment:
     a) Closest elevator moving in SAME direction (can pick up)
     b) Closest IDLE elevator
     c) Closest elevator in OPPOSITE direction (fallback)
   • Throw exception if no elevator can be assigned

6. ElevatorController.getClosestElevatorByDirection(sourceFloor, direction)
   • Returns Optional<Elevator>
   • Filter by direction
   • Position validation:
     - IDLE: any position valid
     - UP: elevator.floor <= sourceFloor (at or below pickup)
     - DOWN: elevator.floor >= sourceFloor (at or above pickup)
   • Find min by distance: Math.abs(elevator.floor - sourceFloor)

7. ElevatorController.stepAhead()
   • Simulate time passage for ALL elevators simultaneously
   • Call stepAhead() on each elevator
   • One iteration = one time unit for entire system

ALGORITHM FLOW:
┌─────────────────────────────────────────────────────────────┐
│ Request comes in → Controller assigns elevator              │
│ ↓                                                           │
│ Elevator adds request to its HashSet                        │
│ ↓                                                           │
│ System.stepAhead() called (time ticker)                     │
│ ↓                                                           │
│ Each elevator.stepAhead():                                  │
│   1. Check current floor for pickup/drop → open doors       │
│   2. If no requests in direction → toggle direction         │
│   3. Move 1 floor in current direction                      │
│   4. If no requests left → set to IDLE                      │
└─────────────────────────────────────────────────────────────┘

EDGE CASES HANDLED:
• Empty requests → elevator becomes IDLE
• Invalid floor numbers → IllegalStateException
• No available elevators → throw exception
• Elevator passed pickup floor → not assigned (position validation)
• Multiple requests same floor → HashSet prevents duplicates
• Direction toggle → checked after each step

DESIGN DECISIONS:
• HashSet<ElevatorRequest> → O(1) add/remove, no duplicates
• Optional<T> returns → explicit null handling
• Stream API → clean filtering and sorting
• Comparator.comparingInt() → find closest elevator by distance
• Single responsibility → each method has one clear purpose
• Concurrent request can be handled by using BlockingQueue, only addRequest function adds the queue, stepAhead only reads
  and in memory converts into HashSet, this way we can avoid concurrency issues with the HashSet and still have O(1) add/remove operations.
  clear separation - multiple writers to queue, single reader processes. No race conditions.



