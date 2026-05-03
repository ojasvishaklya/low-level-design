package com.oshaklya.movie_booking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Screen {
    private final String id;
    private final String name;
    private final List<Seat> seats;
    private final List<Show> shows;

    public Screen(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.seats = generateStandardSeats();
        this.shows = new ArrayList<>();
    }

    private List<Seat> generateStandardSeats() {
        List<Seat> allSeats = new ArrayList<>();
        for (char row = 'A'; row <= 'Z'; row++) {
            for (int number = 0; number <= 20; number++) {
                allSeats.add(new Seat(row, number));
            }
        }
        return allSeats;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Seat> getSeats() {
        return new ArrayList<>(seats);
    }

    public void addShowtime(Show show) {
        shows.add(show);
    }

    public List<Show> getShowtimes() {
        return new ArrayList<>(shows);
    }
}
