# Factory Pattern - Class Diagram

```
                                        ┌─────────────────────┐
                                        │   «interface»       │
                                        │    Notification     │
                                        ├─────────────────────┤
                                        │ +sendNotification() │
                                        └─────────────────────┘
                                                 ▲
                                                 │ implements
                                 ┌───────────────┴───────────────┐
                                 │                               │
                    ┌─────────────────────┐         ┌─────────────────────┐
                    │ EmailNotification   │         │  SMSNotification    │
                    ├─────────────────────┤         ├─────────────────────┤
                    │ +sendNotification() │         │ +sendNotification() │
                    └─────────────────────┘         └─────────────────────┘
                                 ▲                               ▲
                                 │ creates                       │ creates
                                 └───────────────┬───────────────┘
                                                 │
                                     ┌─────────────────────┐
                                     │ NotificationFactory │
                                     ├─────────────────────┤
                                     │ +create(): Notif    │
                                     └─────────────────────┘
```

## Relationships
- **NotificationFactory → EmailNotification**: uses (dependency) - Creates instances based on type
- **NotificationFactory → SMSNotification**: uses (dependency) - Creates instances based on type
- **EmailNotification → Notification**: implements (interface) - Concrete email implementation
- **SMSNotification → Notification**: implements (interface) - Concrete SMS implementation

## Core Flow
1. Client calls NotificationFactory.create(type) → validates type parameter
2. Factory.create() → instantiates appropriate Notification subclass
3. Returns Notification interface reference → client uses polymorphically
4. Client calls notification.sendNotification() → executes concrete implementation

## Key Decisions
- Factory method is static → no need for factory instantiation
- Returns interface type → loose coupling, client depends on abstraction
- Throws exception for invalid types → fail-fast validation
- Simple factory pattern → suitable for small number of types (not Abstract Factory)
