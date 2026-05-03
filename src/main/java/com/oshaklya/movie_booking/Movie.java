package com.oshaklya.movie_booking;

import java.util.UUID;

public class Movie {
    private final String id;
    private final String title;
    private final int durationMinutes;

    public Movie(String title, int durationMinutes) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +
                ", duration=" + durationMinutes + " mins" +
                '}';
    }
}
