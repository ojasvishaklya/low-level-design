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

## Interview Tips

When approaching LLD problems in interviews:

1. **Clarify Requirements**: Ask questions about scale, constraints, and edge cases
2. **Identify Entities**: List main classes and their relationships
3. **Define Relationships**: Use appropriate associations (composition, aggregation, inheritance)
4. **Apply SOLID**: Ensure your design follows SOLID principles
5. **Consider Extensibility**: Design should be easy to extend
6. **Handle Edge Cases**: Think about null checks, thread safety, etc.
7. **Keep It Simple**: Don't over-engineer; start with a simple solution

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
