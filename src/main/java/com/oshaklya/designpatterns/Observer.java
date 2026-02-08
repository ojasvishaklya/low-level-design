package com.oshaklya.designpatterns;

import java.util.ArrayList;
import java.util.List;

interface Subject {
    void attachObserver(_Observer observer);
    void detachObserver(_Observer observer);
}

interface _Observer {
    void update(String message);
}

class PriceDisplay implements _Observer {
    @Override
    public void update(String message) {
        System.out.println("Display updated: " + message);
    }
}

class PriceAlert implements _Observer {
    @Override
    public void update(String message) {
        System.out.println("Alert triggered: " + message);
    }
}

class Stock implements Subject {
    List<_Observer> observers = new ArrayList<>();

    @Override
    public void attachObserver(_Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detachObserver(_Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String update) {
        observers.forEach(observer -> {
            observer.update(update);
        });
    }
}

public class Observer {
    public static void main(String[] args) {
        Stock stock = new Stock();
        stock.attachObserver(new PriceAlert());
        stock.attachObserver(new PriceDisplay());

        stock.notifyObservers("Market Open");
    }
}
