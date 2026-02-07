# Low-Level Design (LLD) Problems

A collection of popular Low-Level Design problems implemented in Java using Spring Boot. This repository serves as a comprehensive reference for interview preparation and revision.

## Overview

This project contains clean, well-documented implementations of common LLD interview questions. Each problem demonstrates object-oriented design principles, design patterns, and best practices in software engineering.

## Technologies Used

- Java
- Spring Boot
- Gradle
- JUnit (for testing)

## Project Structure

```
src/
├── main/
│   └── java/
│       └── com/
│           └── lld/
│               ├── parkinglot/
│               ├── elevatorsystem/
│               ├── librarymanagementsystem/
│               ├── snakeandladder/
│               ├── chessgame/
│               └── ...
└── test/
    └── java/
        └── com/
            └── lld/
                └── ...
```

## Problems Covered

### System Design
- [ ] Parking Lot System
- [ ] Elevator System
- [ ] Library Management System
- [ ] Hotel Management System
- [ ] Movie Ticket Booking System
- [ ] Online Shopping System (e.g., Amazon)
- [ ] Food Delivery System (e.g., Swiggy/Zomato)
- [ ] Cab Booking System (e.g., Uber/Ola)
- [ ] Splitwise (Expense Sharing)

### Games
- [ ] Snake and Ladder
- [ ] Chess Game
- [ ] Tic-Tac-Toe
- [ ] Car Racing Game
- [ ] Sudoku Solver

### Data Structures & Algorithms
- [ ] LRU Cache
- [ ] LFU Cache
- [ ] In-Memory Database
- [ ] Rate Limiter
- [ ] Pub-Sub System
- [ ] Task Scheduler

### Design Patterns
- [ ] Factory Pattern
- [ ] Builder Pattern
- [ ] Singleton Pattern
- [ ] Observer Pattern
- [ ] Strategy Pattern
- [ ] Decorator Pattern
- [ ] Adapter Pattern

## How to Run

### Prerequisites
- JDK 17 or higher
- Gradle

### Build the Project
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Run Specific Problem
```bash
./gradlew bootRun
```

## Problem Implementation Guidelines

Each problem implementation follows these principles:

1. **SOLID Principles**: Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
2. **Design Patterns**: Appropriate use of creational, structural, and behavioral patterns
3. **Clean Code**: Readable, maintainable, and well-documented code
4. **Test Coverage**: Unit tests for critical components
5. **Extensibility**: Easy to extend with new features

## Key Concepts Demonstrated

- Object-Oriented Programming (OOP)
- Abstraction and Encapsulation
- Inheritance and Polymorphism
- Design Patterns
- Exception Handling
- Multithreading (where applicable)
- Concurrency Control
- Database Design (where applicable)

## Approach to Solving LLD Problems

Follow this systematic approach when tackling any LLD problem:

### 1. Clarify Requirements
- Ask questions about functional requirements
- Understand scale, constraints, and assumptions
- Identify core use cases vs. nice-to-have features
- Clarify edge cases and error scenarios

### 2. Identify Core Entities/Classes
- List all nouns from requirements (User, Vehicle, Parking Spot, etc.)
- Determine if they should be concrete classes, abstract classes, or interfaces
- Identify enums for fixed sets of values (VehicleType, PaymentStatus, etc.)

### 3. Define Relationships & Cardinality
- **IS-A relationships**: Inheritance hierarchies (Car IS-A Vehicle)
- **HAS-A relationships**: Composition vs. Aggregation
  - Composition: Strong ownership (ParkingLot HAS-A Floor - floor can't exist without lot)
  - Aggregation: Weak ownership (Library HAS-A Book - book can exist independently)
- **Cardinality**: One-to-One, One-to-Many, Many-to-Many
- **Dependencies**: Which classes depend on which

### 4. Assign Responsibilities (Methods to Classes)
- Follow Single Responsibility Principle - each class should have one reason to change
- Ask: "Which class should own this behavior?"
- Avoid "god classes" or "orchestrator classes" that do everything
- Distribute responsibilities appropriately
  - Example: `ParkingSpot.isAvailable()` vs. `ParkingLotManager.isSpotAvailable(spot)`

### 5. Define State (Attributes)
- List attributes needed for each class
- Determine visibility (private/public/protected)
- Consider immutability where applicable (use `final` for fields that shouldn't change)
- Think about which fields are required vs. optional

### 6. Identify Design Patterns
- **Creational**: Factory, Builder, Singleton (use sparingly)
- **Structural**: Adapter, Decorator, Facade
- **Behavioral**: Strategy, Observer, Command, State
- Don't force patterns - use them when they solve a real problem

### 7. Apply SOLID Principles
- **S**ingle Responsibility: One class, one job
- **O**pen-Closed: Open for extension, closed for modification
- **L**iskov Substitution: Subtypes should be substitutable for base types
- **I**nterface Segregation: Many small interfaces > one large interface
- **D**ependency Inversion: Depend on abstractions, not concretions

### 8. Handle Edge Cases & Constraints
- Null checks and validation
- Thread safety (synchronized, locks, concurrent collections)
- Error handling and exception strategies
- Performance considerations (time/space complexity)

### 9. Keep It Simple
- Start with a simple, working solution
- Don't over-engineer or add unnecessary abstractions
- Iterate and refine based on new requirements
- Prefer composition over inheritance

## Interview Tips

- **Draw diagrams**: Sketch class diagrams to visualize relationships
- **Think aloud**: Explain your thought process as you design
- **Start small**: Begin with core functionality, then extend
- **Ask for feedback**: Check if the interviewer wants more detail on any area
- **Write clean code**: Use meaningful names, proper indentation, and comments where needed
- **Test your design**: Walk through use cases to verify it works

## Contributing

Feel free to add new problems or improve existing implementations. Each problem should include:

- Clear problem statement
- Class diagram (optional)
- Implementation
- Unit tests
- README explaining the approach

## License

This project is for educational purposes and interview preparation.

## Author

Created for personal interview preparation and revision.

---

**Note**: This repository is continuously updated with new problems and improvements.
