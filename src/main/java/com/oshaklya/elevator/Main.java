package com.oshaklya.elevator;

class Main {
    public static void main(String[] args) {
        ElevatorController elevatorController = new ElevatorController(5);
        System.out.println(elevatorController);
        System.out.println();
        Object[][] requestData = {
                {0, 5, Direction.UP, RequestType.HALLWAY},
                {10, 3, Direction.DOWN, RequestType.HALLWAY},
                {7, 15, Direction.UP, RequestType.HALLWAY},
                {12, 8, Direction.DOWN, RequestType.HALLWAY},
                {2, 9, Direction.UP, RequestType.INTERNAL}
        };

        for (int i = 0; i < requestData.length; i++) {
            Object[] data = requestData[i];
            ElevatorRequest request = new ElevatorRequest((int) data[0], (int) data[1], (Direction) data[2], (RequestType) data[3]);
            int assignedElevator = elevatorController.requestElevator(request);
            System.out.println("Request " + (i + 1) + ": " + request + " assigned to Elevator " + assignedElevator);
            elevatorController.stepAhead();
        }

        System.out.println();
        System.out.println(elevatorController);
    }
}