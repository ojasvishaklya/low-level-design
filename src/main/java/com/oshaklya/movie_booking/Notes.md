// ═══════════════════════════════════════════════════════════════════════════
// REQUIREMENTS
// ═══════════════════════════════════════════════════════════════════════════

FUNCTIONAL REQUIREMENTS:
- Add and manage theaters with multiple screens
- Add and manage movies with title and duration
- Create shows linking movies to screens at specific times
- Search movies by title (case-insensitive partial match)
- View all shows for a specific movie
- Check seat availability for a show
- Book multiple seats atomically (all-or-nothing)
- Generate unique confirmation ID for each reservation
- Cancel reservations and release seats back to availability pool
- Prevent double-booking (two users booking same seat)
- Fast seat availability lookups
- System should scale to multiple theaters and screens

OUT OF SCOPE:
- Payment processing
- User authentication and profiles
- Pricing and dynamic pricing
- Seat hold/timeout mechanism
- Movie ratings and reviews
- Food/concession ordering
- Ticket refunds and modifications
- Email confirmations
- Show cancellation and rescheduling
- Seat categories (premium, regular, etc.)

// ═══════════════════════════════════════════════════════════════════════════
// ENTITIES & RELATIONSHIPS
// ═══════════════════════════════════════════════════════════════════════════

ENTITIES:

1. SeatStatus (enum)
   - AVAILABLE
   - RESERVED
   - HOLD (for future temporary reservations)

2. Seat (immutable value object)
   - char row (A-Z)
   - int number (0-20)
   - Implements equals() and hashCode() for use as Map key

3. Movie
   - String id (UUID)
   - String title
   - int durationMinutes

4. Screen
   - String id (UUID)
   - String name
   - List<Seat> seats (546 seats: 26 rows × 21 seats)
   - List<Showtime> shows

5. Theater
   - String id (UUID)
   - String name
   - List<Screen> screens

6. SeatState (inner class in Showtime)
   - SeatStatus status (current state of seat)
   - Reservation reservation (which reservation owns this seat)
   - Lock lock (ReentrantLock for future seat-level locking)
   - LocalDateTime lockExpirationTime (for future temporary holds)

7. Showtime (OWNS ALL STATE - SINGLE SOURCE OF TRUTH)
   - String id (UUID)
   - Movie movie
   - Screen screen
   - LocalDateTime startTime
   - Map<Seat, SeatState> seatStateMap (contains ALL state per seat)

8. Reservation (just a receipt/record)
   - String confirmationId (UUID)
   - Showtime show
   - List<Seat> seats

9. BookingSystem (facade with routing index only)
   - Map<String, Theater> theaters
   - Map<String, Movie> movies
   - Map<String, String> reservationToShowtimeId (routing index, NOT state)

RELATIONSHIPS:
- Theater (1) → (*) Screen
- Screen (1) → (*) Showtime
- Showtime (*) → (1) Movie
- Showtime (*) → (1) Screen
- Showtime (1) → (*) Seat reservations
- Reservation (*) → (1) Showtime
- Reservation (1) → (*) Seat

HIERARCHY:
Theater → Screen → Showtime → Seat Availability

// ═══════════════════════════════════════════════════════════════════════════
// CLASS DESIGN
// ═══════════════════════════════════════════════════════════════════════════

SEAT (Value Object Pattern)
- Immutable identifier for theater seats
- Overrides equals() and hashCode() for Map key usage
- toString() returns compact format: "A0", "B15"

MOVIE
- Simple entity with auto-generated UUID
- No behavior, just data holder
- Duration stored in minutes

SCREEN
- Generates 546 standard seats on construction (A-Z rows, 0-20 columns)
- Maintains list of shows
- Defensive copying on getSeats() and getShowtimes()

THEATER
- Container for screens
- No complex logic, simple aggregation
- Defensive copying on getScreens()

SEATSTATE (Inner class in Showtime)
- Encapsulates ALL state for a single seat in a show
- Contains: status, reservation reference, lock, lockExpirationTime
- Initialized as AVAILABLE with null reservation
- Lock field prepared for future seat-level locking strategy

WHY SEATSTATE APPROACH:
Instead of separate Map<Seat, SeatStatus> and Map<String, Reservation>, we use
Map<Seat, SeatState> as SINGLE SOURCE OF TRUTH. Each SeatState object contains:
- status field (instead of separate seatAvailability map)
- reservation field (instead of separate reservations map)
This eliminates synchronization bugs between multiple maps. To cancel, we scan
seatStateMap and clear matching seats - O(546) but acceptable for 546 seats.
Each SeatState also contains a lock for future seat-level locking.

SHOWTIME (OWNS ALL STATE - Core business logic)
- Initializes all seats to AVAILABLE SeatState on construction
- Single Map<Seat, SeatState> contains ALL state for all seats
- bookSeats(List<Seat>): Check-then-mark pattern, O(K) where K = seats requested
  * First loop: Validates ALL seats exist and are AVAILABLE
  * Second loop: Creates Reservation, updates each SeatState.status = RESERVED
  * Second loop: Sets SeatState.reservation = new Reservation
  * Returns Reservation or null (no partial booking)
- cancelReservation(confirmationId): O(546) scan of seatStateMap
  * Scans all 546 seats to find matching confirmationId
  * Clears status to AVAILABLE and reservation to null
  * Returns boolean success
- getReservation(confirmationId): O(546) scan to find reservation
- getAvailableSeats(): O(546) scan to collect available seats

WHY SHOWTIME OWNS ALL STATE:
Showtime is the authority for seat state because a reservation is NOT meaningful on its own.
It only matters relative to one specific showing - seat A5 at 7pm ≠ seat A5 at 10pm.
Keeping book() and cancel() on Showtime means ONE object owns the check AND the state change,
making concurrency much easier to reason about. Single owner = single lock point.

COMPLEXITY ANALYSIS:
- bookSeats(): O(K) where K = number of seats requested (typically 1-8)
- cancelReservation(): O(N) where N = 546 seats - acceptable for small N
- getReservation(): O(N) where N = 546 seats - used less frequently
- getAvailableSeats(): O(N) where N = 546 seats - read-heavy, acceptable
Trade-off: Cancel/lookup are O(N) but we gain single source of truth and no sync bugs

RESERVATION (just a receipt/record)
- Immutable record of "what was booked"
- NOT a decision-maker, just a data holder
- Auto-generated confirmation ID
- Stores reference to show and booked seats

BOOKINGSYSTEM (Facade with routing index only)
- Centralized orchestrator for all operations
- Does NOT own reservation state (that's in Showtime.seatStateMap)
- Maintains Map<String, String> reservationToShowtimeId as routing index ONLY
- bookSeats(): Delegates to Showtime.bookSeats(), then adds to routing index
- cancelReservation(): Looks up show via index, calls Showtime.cancelReservation(),
  then removes from index if successful
- Search operations traverse all theaters/screens/shows
- Index enables O(1) routing from confirmationId → show (vs O(T*S*N) scan)

KEY FUNCTIONS:

Showtime.bookSeats(List<Seat>):
- Input: List of seats to book
- Loop 1: Validates all seats exist in seatStateMap and status == AVAILABLE
- Loop 2: Creates Reservation, updates each SeatState.status = RESERVED
- Loop 2: Updates each SeatState.reservation = new Reservation
- Returns Reservation or null (no partial booking)
- Complexity: O(K) where K = seats requested

Showtime.cancelReservation(String confirmationId):
- Input: Confirmation ID
- Scans all entries in seatStateMap (546 seats)
- For matching seats: sets status = AVAILABLE, reservation = null
- Returns boolean success (false if not found)
- Complexity: O(546) - acceptable for this size

Showtime.getReservation(String confirmationId):
- Scans seatStateMap.values() to find matching reservation
- Returns first found Reservation or null
- Complexity: O(546)

BookingSystem.bookSeats(String showtimeId, List<Seat>):
- Finds show by ID (O(N) linear search)
- Calls Showtime.bookSeats()
- If successful, adds confirmationId → showtimeId to routing index
- Returns Reservation or null

BookingSystem.cancelReservation(String confirmationId):
- Looks up showtimeId via routing index
- Finds show and calls Showtime.cancelReservation()
- If successful, removes from routing index
- Returns boolean success

BookingSystem.searchMoviesByTitle(String):
- Case-insensitive partial match
- Returns all matching movies

BookingSystem.getShowtimesForMovie(String movieId):
- Traverses all theaters → screens → shows
- Returns all shows for given movie

// ═══════════════════════════════════════════════════════════════════════════
// CONCURRENCY HANDLING
// ═══════════════════════════════════════════════════════════════════════════

DESIGN PRINCIPLE - SINGLE SOURCE OF TRUTH:
All actual state changes (creating reservations, updating seat availability) happen inside
Showtime under synchronized blocks. BookingSystem just maintains a routing index afterward.
Showtime is the ONLY stateful object for bookings.

WHY THIS MATTERS:
If Reservation or BookingSystem also changed seat state directly, you would spread logic
across multiple places and make double-booking bugs more likely. By keeping book() and
cancel() on Showtime, ONE object owns the check AND the state change = single lock point.

CURRENT STATE: Single-threaded base implementation
All state lives in Showtime.seatStateMap. Each SeatState contains lock field (ReentrantLock)
prepared for future enhancements. Two concurrency strategies can be added on top:

APPROACH 1: SHOWTIME-LEVEL LOCKING (Coarse-grained)
Pattern: Synchronized methods
Target: Showtime.bookSeats() and Showtime.cancelReservation()
Fix: Add synchronized keyword to both methods
Trade-off: Simple (2-line change) but serializes all bookings for same show (low throughput)
When to use: Small theaters, infrequent bookings
How SeatState enables this: SeatState.lock field unused, just synchronize on Showtime instance

APPROACH 2: SEAT-LEVEL LOCKING (Fine-grained)
Pattern: Per-resource locks with sorted acquisition
Changes needed:
- Sort requested seats before locking (prevent deadlock A→B vs B→A)
- Lock each SeatState.lock in sorted order using tryLock with timeout
- Perform check-then-act under locks
- Release locks in reverse order
Trade-off: High throughput (parallel bookings) but complex deadlock prevention
When to use: Large theaters, high concurrent load
How SeatState enables this: Each SeatState has its own lock field, ready to use

APPROACH 3: OPTIMISTIC LOCKING
Pattern: Version-based concurrency control
Changes needed:
- Add version field to Showtime
- Compare-and-swap on reserveSeats()
- Retry on conflict
Trade-off: Best throughput under low contention, retry storms under high contention
When to use: Read-heavy workloads

COMPARISON DEMO: See movie_booking_claude/LockingComparison.md

WHY THIS DESIGN ENABLES BOTH LOCKING STRATEGIES:
1. Single source of truth (seatStateMap) means only one data structure to protect
2. SeatState.lock field ready for seat-level locking without refactoring
3. Showtime owns all mutations, giving clear lock boundaries
4. No distributed state - can switch strategies by changing Showtime.bookSeats() only

ADDITIONAL ENHANCEMENTS:
- Seat hold with timeout (use SeatState.lockExpirationTime field, add HOLD status)
- Seat pricing tiers (premium, regular, economy)
- Transaction log for audit trail
- Database persistence (replace in-memory maps)
- Distributed locking for multi-instance deployment
- Event-driven architecture (booking events, notifications)
- GraphQL/REST API layer
- Admin interface for theater/movie management

// ═══════════════════════════════════════════════════════════════════════════
// DESIGN PATTERNS USED
// ═══════════════════════════════════════════════════════════════════════════

1. VALUE OBJECT: Seat (immutable, equality by value)
2. FACADE: BookingSystem (simplifies subsystem interactions)
3. CHECK-THEN-ACT: Showtime.reserveSeats() (all-or-nothing validation)
4. DEFENSIVE COPYING: All getters return new ArrayList/HashMap

// ═══════════════════════════════════════════════════════════════════════════
// IMPLEMENTATION NOTES
// ═══════════════════════════════════════════════════════════════════════════

- Standard seat layout: 26 rows (A-Z) × 21 seats (0-20) = 546 seats per screen
- All IDs generated with UUID.randomUUID()
- Map-based storage for O(1) lookups by ID
- Linear search for shows (acceptable for single-instance, small scale)
- No input validation (assumes valid inputs)
- No null checks (fail-fast approach)
- Immutable collections via defensive copying
- LocalDateTime for show scheduling (no timezone handling)

TRADE-OFFS:
- Simplicity over scalability (in-memory storage)
- Single-instance design (no distributed concerns)
- No persistence layer (data lost on restart)
- Linear search acceptable (small dataset assumption)
- Cancel/lookup are O(546) but we gain single source of truth
- SeatState contains lock + lockExpirationTime fields (8-16 bytes overhead per seat)
  but eliminates need for separate data structures

KEY DESIGN DECISION - SeatState vs Separate Maps:
OLD APPROACH (complex):
- Map<Seat, SeatStatus> seatAvailability
- Map<String, Reservation> reservations
Problem: Must keep two maps in sync, risk of inconsistency

NEW APPROACH (simple):
- Map<Seat, SeatState> seatStateMap (single source of truth)
Benefits:
- Impossible to have status = AVAILABLE but reservation != null
- Cancel is O(N) scan but atomic - no multi-map coordination
- Lock field per seat enables fine-grained locking without refactoring
- lockExpirationTime field enables temporary holds without refactoring
