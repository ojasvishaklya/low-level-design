package com.oshaklya.elevator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparingInt;

class ElevatorController {
    List<Elevator> elevators;

    ElevatorController(int elevatorCount) {
        elevators = new ArrayList<>();
        for (int i = 0; i < elevatorCount; i++) {
            elevators.add(new Elevator(i + 1));
        }
    }

    int requestElevator(ElevatorRequest elevatorRequest) {
        if (elevatorRequest.destinationFloor < 0 || elevatorRequest.destinationFloor > 9) {
            throw new IllegalStateException("Illegal destination request");
        }
        if (elevatorRequest.sourceFloor < 0 || elevatorRequest.sourceFloor > 9) {
            throw new IllegalStateException("Illegal source request");
        }
        Elevator assignedElevator = null;

        Optional<Elevator> closestInDirection = getClosestElevatorByDirection(elevatorRequest.sourceFloor, elevatorRequest.direction);
        Optional<Elevator> closestIdle = getClosestElevatorByDirection(elevatorRequest.sourceFloor, Direction.IDLE);
        Optional<Elevator> closestInOppositeDirection = getClosestElevatorByDirection
                (elevatorRequest.sourceFloor, elevatorRequest.direction == Direction.UP ? Direction.DOWN : Direction.UP);
        if (closestInDirection.isPresent()) {
            assignedElevator = closestInDirection.get();
        } else if (closestIdle.isPresent()) {
            assignedElevator = closestIdle.get();
        } else if (closestInOppositeDirection.isPresent()) {
            assignedElevator = closestInOppositeDirection.get();
        }
        if (assignedElevator == null) {
            throw new IllegalStateException("could not assign elevator");
        }
        assignedElevator.addRequest(elevatorRequest);
        return assignedElevator.id;
    }

    private Optional<Elevator> getClosestElevatorByDirection(int sourceFloor, Direction direction) {
        return elevators.stream()
                .filter(e -> e.direction == direction)
                .filter(e -> {
                    if (direction == Direction.IDLE) {
                        return true;
                    }
                    if (direction == Direction.UP) {
                        return e.floor <= sourceFloor;
                    }
                    if (direction == Direction.DOWN) {
                        return e.floor >= sourceFloor;
                    }
                    return false;
                })
                .min(comparingInt(e -> Math.abs(e.floor - sourceFloor)));
    }

    void stepAhead() {
        // in one step of time the elevator either moves on floor or it stops at a floor (to be idle or to open doors)
        elevators.forEach(Elevator::stepAhead);
    }

    @Override
    public String toString() {
        return "ElevatorController{" +
                "elevators=" + elevators +
                '}';
    }
}
