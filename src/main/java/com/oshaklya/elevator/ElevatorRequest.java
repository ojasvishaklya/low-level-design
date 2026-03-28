package com.oshaklya.elevator;

class ElevatorRequest {
    int sourceFloor;
    int destinationFloor;
    Direction direction;
    RequestType requestType;

    ElevatorRequest(int sourceFloor, int destinationFloor, Direction direction, RequestType requestType) {
        this.sourceFloor = sourceFloor;
        this.destinationFloor = destinationFloor;
        this.direction = direction;
        this.requestType = requestType;
    }

    @Override
    public String toString() {
        return "ElevatorRequest{" +
                "sourceFloor=" + sourceFloor +
                ", destinationFloor=" + destinationFloor +
                ", direction=" + direction +
                ", requestType=" + requestType +
                '}';
    }
}
