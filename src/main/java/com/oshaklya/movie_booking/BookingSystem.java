package com.oshaklya.movie_booking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingSystem {
    private final Map<String, Theater> theaters;
    private final Map<String, Movie> movies;
    private final Map<String, String> reservationToShowtimeId;

    public BookingSystem() {
        this.theaters = new HashMap<>();
        this.movies = new HashMap<>();
        this.reservationToShowtimeId = new HashMap<>();
    }

    public void addTheater(Theater theater) {
        theaters.put(theater.getId(), theater);
    }

    public void addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
    }

    public List<Movie> searchMoviesByTitle(String title) {
        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getTitle().toLowerCase().contains(title.toLowerCase())) {
                results.add(movie);
            }
        }
        return results;
    }

    public List<Show> getShowtimesForMovie(String movieId) {
        List<Show> results = new ArrayList<>();
        for (Theater theater : theaters.values()) {
            for (Screen screen : theater.getScreens()) {
                for (Show show : screen.getShowtimes()) {
                    if (show.getMovie().getId().equals(movieId)) {
                        results.add(show);
                    }
                }
            }
        }
        return results;
    }

    public List<Show> getAllShowtimes() {
        List<Show> results = new ArrayList<>();
        for (Theater theater : theaters.values()) {
            for (Screen screen : theater.getScreens()) {
                results.addAll(screen.getShowtimes());
            }
        }
        return results;
    }

    public Reservation bookSeats(String showtimeId, List<String> seatIds) {
        Show show = findShowtimeById(showtimeId);
        if (show == null) {
            return null;
        }

        Reservation reservation = show.bookSeats(seatIds);
        if (reservation != null) {
            reservationToShowtimeId.put(reservation.getConfirmationId(), showtimeId);
        }
        return reservation;
    }

    private Show findShowtimeById(String showtimeId) {
        for (Theater theater : theaters.values()) {
            for (Screen screen : theater.getScreens()) {
                for (Show show : screen.getShowtimes()) {
                    if (show.getId().equals(showtimeId)) {
                        return show;
                    }
                }
            }
        }
        return null;
    }

    public boolean cancelReservation(String confirmationId) {
        String showtimeId = reservationToShowtimeId.get(confirmationId);
        if (showtimeId == null) {
            return false;
        }

        Show show = findShowtimeById(showtimeId);
        if (show == null) {
            return false;
        }

        if (show.cancelReservation(confirmationId)) {
            reservationToShowtimeId.remove(confirmationId);
            return true;
        }
        return false;
    }

    public Reservation getReservation(String confirmationId) {
        String showtimeId = reservationToShowtimeId.get(confirmationId);
        if (showtimeId == null) {
            return null;
        }

        Show show = findShowtimeById(showtimeId);
        if (show == null) {
            return null;
        }

        return show.getReservation(confirmationId);
    }
}
