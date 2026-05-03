package com.oshaklya.movie_booking;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Seat {
    private final char row;
    private final int number;
    private SeatStatus status;
    private Reservation reservation;
    private final Lock lock;
    private LocalDateTime lockExpirationTime;

    public Seat(char row, int number) {
        this.row = row;
        this.number = number;
        this.status = SeatStatus.AVAILABLE;
        this.reservation = null;
        this.lock = new ReentrantLock();
        this.lockExpirationTime = null;
    }

    public char getRow() {
        return row;
    }

    public int getNumber() {
        return number;
    }

    public String getId() {
        return String.valueOf(row) + number;
    }

    // Package-private accessors for Show to modify state
    Lock getLock() {
        return lock;
    }

    SeatStatus getStatus() {
        return status;
    }

    void setStatus(SeatStatus status) {
        this.status = status;
    }

    Reservation getReservation() {
        return reservation;
    }

    void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    LocalDateTime getLockExpirationTime() {
        return lockExpirationTime;
    }

    void setLockExpirationTime(LocalDateTime lockExpirationTime) {
        this.lockExpirationTime = lockExpirationTime;
    }

    @Override
    public String toString() {
        return String.valueOf(row) + number;
    }
}
