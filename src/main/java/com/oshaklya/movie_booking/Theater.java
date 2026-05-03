package com.oshaklya.movie_booking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Theater {
    private final String id;
    private final String name;
    private final List<Screen> screens;

    public Theater(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.screens = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addScreen(Screen screen) {
        screens.add(screen);
    }

    public List<Screen> getScreens() {
        return new ArrayList<>(screens);
    }

    @Override
    public String toString() {
        return "Theater{" +
                "name='" + name + '\'' +
                ", screens=" + screens.size() +
                '}';
    }
}
