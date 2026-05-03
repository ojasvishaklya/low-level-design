package com.oshaklya.movie_booking;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Show {
    private final String id;
    private final Movie movie;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final Map<String, Seat> seats;

    public Show(Movie movie, Screen screen, LocalDateTime startTime) {
        this.id = UUID.randomUUID().toString();
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.seats = new ConcurrentHashMap<>();

        // Create new Seat instances for this show
        for (Seat templateSeat : screen.getSeats()) {
            Seat seat = new Seat(templateSeat.getRow(), templateSeat.getNumber());
            seats.put(seat.getId(), seat);
        }
    }

    public String getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Screen getScreen() {
        return screen;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Reservation bookSeats(List<String> seatIds) {
        // Step 1: Validate all seats exist and collect Seat objects
        List<Seat> seatsToBook = new ArrayList<>();
        for (String seatId : seatIds) {
            Seat seat = seats.get(seatId);
            if (seat == null) {
                throw new IllegalArgumentException("Seat " + seatId + " does not exist");
            }
            seatsToBook.add(seat);
        }

        // Step 2: Sort seats by ID to prevent deadlock
        seatsToBook.sort(Comparator.comparing(Seat::getId));

        // Step 3: Acquire all locks in sorted order
        for (Seat seat : seatsToBook) {
            seat.getLock().lock();
        }

        try {
            // Step 4: Check all seats are available
            for (Seat seat : seatsToBook) {
                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    return null;
                }
            }

            // Step 5: All seats available, create reservation and update state
            Reservation reservation = new Reservation(this, seatsToBook);
            for (Seat seat : seatsToBook) {
                seat.setStatus(SeatStatus.RESERVED);
                seat.setReservation(reservation);
            }
            return reservation;
        } finally {
            // Step 6: Always release all locks
            for (Seat seat : seatsToBook) {
                seat.getLock().unlock();
            }
        }
    }

    public boolean cancelReservation(String confirmationId) {
        // Step 1: Find all seats belonging to this reservation (no locking yet)
        List<Seat> seatsToCancel = new ArrayList<>();
        for (Seat seat : seats.values()) {
            if (seat.getReservation() != null &&
                seat.getReservation().getConfirmationId().equals(confirmationId)) {
                seatsToCancel.add(seat);
            }
        }

        if (seatsToCancel.isEmpty()) {
            return false;
        }

        // Step 2: Sort seats by ID to prevent deadlock
        seatsToCancel.sort(Comparator.comparing(Seat::getId));

        // Step 3: Acquire all locks in sorted order
        for (Seat seat : seatsToCancel) {
            seat.getLock().lock();
        }

        try {
            // Step 4: Double-check reservation still exists and cancel
            boolean found = false;
            for (Seat seat : seatsToCancel) {
                if (seat.getReservation() != null &&
                    seat.getReservation().getConfirmationId().equals(confirmationId)) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setReservation(null);
                    found = true;
                }
            }
            return found;
        } finally {
            // Step 5: Always release all locks
            for (Seat seat : seatsToCancel) {
                seat.getLock().unlock();
            }
        }
    }

    public Reservation getReservation(String confirmationId) {
        // Read-only operation: scan without locking
        // Safe because we're just reading a reference, not mutating
        for (Seat seat : seats.values()) {
            if (seat.getReservation() != null &&
                seat.getReservation().getConfirmationId().equals(confirmationId)) {
                return seat.getReservation();
            }
        }
        return null;
    }

    public List<Seat> getAvailableSeats() {
        return seats.values().stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public Map<String, SeatStatus> getSeatAvailability() {
        Map<String, SeatStatus> availability = new HashMap<>();
        for (Seat seat : seats.values()) {
            availability.put(seat.getId(), seat.getStatus());
        }
        return availability;
    }

    public Seat getSeat(String seatId) {
        return seats.get(seatId);
    }

    @Override
    public String toString() {
        return "Showtime{" +
                "movie=" + movie.getTitle() +
                ", screen=" + screen.getName() +
                ", time=" + startTime +
                '}';
    }
}