package com.oshaklya.movie_booking;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Movie Booking System - Single Threaded Version ===\n");

        BookingSystem bookingSystem = new BookingSystem();

        Movie movie1 = new Movie("Inception", 148);
        Movie movie2 = new Movie("The Dark Knight", 152);
        bookingSystem.addMovie(movie1);
        bookingSystem.addMovie(movie2);

        Theater theater1 = new Theater("PVR Cinemas");
        Screen screen1 = new Screen("Screen 1");
        Screen screen2 = new Screen("Screen 2");
        theater1.addScreen(screen1);
        theater1.addScreen(screen2);
        bookingSystem.addTheater(theater1);

        LocalDateTime now = LocalDateTime.now();
        Show show1 = new Show(movie1, screen1, now.plusHours(2));
        Show show2 = new Show(movie2, screen2, now.plusHours(3));
        screen1.addShowtime(show1);
        screen2.addShowtime(show2);

        List<Movie> searchResults = bookingSystem.searchMoviesByTitle("knight");
        System.out.println("Found: " + searchResults);

        List<Show> shows = bookingSystem.getShowtimesForMovie(movie1.getId());
        System.out.println("Showtimes: " + shows);

        List<String> seatsToBook = Arrays.asList("A0", "A1", "A2");
        Reservation reservation1 = bookingSystem.bookSeats(show1.getId(), seatsToBook);
        if (reservation1 != null) {
            System.out.println("Booking successful: " + reservation1.getConfirmationId());
            System.out.println("Seats: " + reservation1.getSeats());
        } else {
            System.out.println("Booking failed!");
        }

        Reservation reservation2 = bookingSystem.bookSeats(show1.getId(), seatsToBook);
        if (reservation2 != null) {
            System.out.println("Booking successful: " + reservation2.getConfirmationId());
        } else {
            System.out.println("Booking failed - seats already reserved!");
        }

    }
}
