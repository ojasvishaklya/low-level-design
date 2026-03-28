package com.oshaklya.elevator;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingDeque;

class Elevator {
    int id;
    int floor;
    Direction direction;
    HashSet<ElevatorRequest> requests;
    LinkedBlockingDeque<ElevatorRequest> threadSafeRequests;

    Elevator(int id) {
        this.id = id;
        this.floor = 0;
        this.direction = Direction.IDLE;
        this.requests = new HashSet<>();
        this.threadSafeRequests = new LinkedBlockingDeque<>(1000);
    }

    int getFloor() {
        return floor;
    }

    Direction getDirection() {
        return direction;
    }

    boolean addRequest(ElevatorRequest elevatorRequest) {
        if (requests.isEmpty()) {
            // this is important to change the direction from IDLE on first added request
            this.direction = elevatorRequest.direction;
        }
        requests.add(elevatorRequest);
        return true;
    }

    boolean threadSafeAddRequest(ElevatorRequest elevatorRequest) {
        try {
            threadSafeRequests.put(elevatorRequest);  // Blocking - waits if full
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    void threadSafeStepAhead(){
        this.threadSafeRequests.drainTo(this.requests);
        stepAhead();
    }

    void stepAhead() {
        if (requests.isEmpty()) {
            this.direction = Direction.IDLE;
            return;
        }
        Optional<ElevatorRequest> elevatorRequest = requestToBeServedOnCurrentFloor();
        // should stop on this and open gate
        if (elevatorRequest.isPresent()) {
            openDoors();
            requests.remove(elevatorRequest.get());
            return; // returning as in one itteration we either stop or we move.
        }
        if (!hasRequestsInCurrentDirection()) {
            // toggle direction if we have no more requests in the same direction.
            this.direction = this.direction == Direction.UP ? Direction.DOWN : Direction.UP;
        }
        this.floor = this.direction == Direction.UP ? this.floor + 1 : this.floor - 1;
    }

    Optional<ElevatorRequest> requestToBeServedOnCurrentFloor() {
        // there would always be only one such request as the set guarantees uniqueness
        return requests.stream().filter(requests -> {
            // Drop passenger:  should stop if any request has current floor as destination floor
            if (this.floor == requests.destinationFloor) {
                return true;
            }
            // Pick passenger: (a) stop if you are going up and there is a request that has source floor as current floor and destination floor above you
            // Pick passenger: (b) stop if you are going down and there is a request that has source floor as current floor and destination floor below you
            // should stop if any request has current floor as source floor AND same direction
            return this.direction == requests.direction && this.floor == requests.sourceFloor;
        }).findFirst();
    }

    boolean hasRequestsInCurrentDirection() {
        return requests.stream().anyMatch(request -> {
            if (this.direction == Direction.UP && (request.sourceFloor >= this.floor || request.destinationFloor >= this.floor)) {
                return true;
            }
            return this.direction == Direction.DOWN && (request.sourceFloor <= this.floor || request.destinationFloor <= this.floor);
        });
    }

    private void openDoors() {
        System.out.println("elevator " + this.id + "door opened at floor: " + this.floor);
    }


    @Override
    public String toString() {
        return "Elevator{" +
                "floor=" + floor +
                ", direction=" + direction +
                ", requests=" + requests +
                '}';
    }
}
