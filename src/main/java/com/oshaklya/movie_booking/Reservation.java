package com.oshaklya.movie_booking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Reservation {
    private final String confirmationId;
    private final Show show;
    private final List<Seat> seats;

    public Reservation(Show show, List<Seat> seats) {
        this.confirmationId = UUID.randomUUID().toString();
        this.show = show;
        this.seats = new ArrayList<>(seats);
    }

    public String getConfirmationId() {
        return confirmationId;
    }

    public Show getShowtime() {
        return show;
    }

    public List<Seat> getSeats() {
        return new ArrayList<>(seats);
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "confirmationId='" + confirmationId + '\'' +
                ", showtime=" + show +
                ", seats=" + seats +
                '}';
    }
}
