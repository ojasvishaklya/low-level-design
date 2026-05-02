package com.oshaklya.parking_lot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

enum VehicleType {
    TWO_WHEELER,
    SMALL_VEHICLE,
    LARGE_VEHICLE
}

enum ParkingSpotType {
    SMALL,
    MEDIUM,
    LARGE
}

class Vehicle {
    int id;
    VehicleType vehicleType;
}

class ParkingSpot {
    int id;
    ParkingSpotType parkingSpotType;
    Boolean isVacant;
}

// we need 2 validations on ticket:
// 1. Validate using vehicle id to ensure right owner
// 2. Validate using isUsed to make sure same user doesn't use the same ticket twice.
class Ticket {
    String id;
    int spotId;
    int vehicleId;
    LocalDateTime entryTime;
    boolean isUsed;
}

class ParkingLot {
    Map<Integer, ParkingSpot> parkingSpotsMap;
    Map<String, Ticket> ticketsMap;
    Map<VehicleType, ParkingSpotType> parkingSpotMapping;

    ParkingLot(Map<Integer, ParkingSpot> parkingSpotsMap) {
        this.parkingSpotsMap = parkingSpotsMap;
        this.ticketsMap = new HashMap<>();
        this.parkingSpotMapping = new HashMap<>();
        parkingSpotMapping.put(VehicleType.TWO_WHEELER, ParkingSpotType.SMALL);
        parkingSpotMapping.put(VehicleType.SMALL_VEHICLE, ParkingSpotType.MEDIUM);
        parkingSpotMapping.put(VehicleType.LARGE_VEHICLE, ParkingSpotType.LARGE);

    }

    ParkingSpotType getParkingSpotType(VehicleType vehicleType) {
        if (!parkingSpotMapping.containsKey(vehicleType)) {
            throw new IllegalStateException("unsupported vehicle type");
        }
        return parkingSpotMapping.get(vehicleType);
    }

    Ticket createTicket() {
        Ticket ticket = new Ticket();
        ticket.id = UUID.randomUUID().toString();
        ticket.entryTime = LocalDateTime.now();
        ticket.isUsed = false;
        ticketsMap.put(ticket.id, ticket);
        return ticket;
    }

    int getAvailableParkingSpot(Vehicle vehicle) {
        Optional<ParkingSpot> availableSpot = this.parkingSpotsMap.values().stream()
                .filter(parkingSpot -> {
                    return parkingSpot.isVacant &&
                            parkingSpot.parkingSpotType.equals(getParkingSpotType(vehicle.vehicleType));
                }).findFirst();
        return availableSpot.map(parkingSpot -> parkingSpot.id).orElse(-1);
    }

    void markSpotUsed(int id) {
        if (!parkingSpotsMap.containsKey(id)) {
            throw new IllegalStateException("invalid parking spot id");
        }
        ParkingSpot spot = parkingSpotsMap.get(id);
        spot.isVacant = false;
    }

    void markSpotVacant(int id) {
        if (!parkingSpotsMap.containsKey(id)) {
            throw new IllegalStateException("invalid parking spot id");
        }
        ParkingSpot spot = parkingSpotsMap.get(id);
        spot.isVacant = true;
    }

    Ticket getTicketDetails(String ticketId) {
        if (!this.ticketsMap.containsKey(ticketId)) {
            throw new IllegalStateException("invalid ticket id");
        }
        return ticketsMap.get(ticketId);
    }
}

class ParkingLotController {
    ParkingLot parkingLot;

    String assignSpot(Vehicle vehicle) {
        int spotId = parkingLot.getAvailableParkingSpot(vehicle);
        if (spotId == -1) {
            return "No slot available";
        }
        Ticket ticket = parkingLot.createTicket();
        ticket.vehicleId = vehicle.id;
        ticket.spotId = spotId;
        parkingLot.markSpotUsed(spotId);
        return ticket.id;
    }

    int exitVehicleAndCalculateFare(Vehicle vehicle, String ticketId) {
        Ticket ticket = parkingLot.getTicketDetails(ticketId);
        if (ticket.isUsed || vehicle.id != ticket.vehicleId) {
            // ticket validation failed
            throw new IllegalStateException("invalid ticket entered");
        }
        ticket.isUsed = true;
        parkingLot.markSpotVacant(ticket.spotId);
        return calculateFare(ticket.entryTime);
    }
    int calculateFare(LocalDateTime startTime) {
        Duration duration = Duration.between(startTime, LocalDateTime.now());
        // ceil ensures partial hours are rounded up (e.g. 30 min -> 1 hour)
        long hours = (long) Math.ceil((double) duration.toMinutes() / 60);
        return (int) (hours * 10);
    }
}

public class Main {

    public static void main(String[] args) {

    }
}
