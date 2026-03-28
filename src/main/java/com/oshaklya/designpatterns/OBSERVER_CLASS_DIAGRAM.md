# Observer Pattern - Class Diagram

```
┌─────────────────────┐                          ┌─────────────────────┐
│   «interface»       │                          │   «interface»       │
│      Subject        │                          │     _Observer       │
├─────────────────────┤                          ├─────────────────────┤
│ +attachObserver()   │                          │ +update(): void     │
│ +detachObserver()   │                          └─────────────────────┘
└─────────────────────┘                                   ▲
         ▲                                                │ implements
         │ implements                        ┌────────────┴────────────┐
         │                                   │                         │
┌─────────────────────┐         ┌─────────────────────┐   ┌─────────────────────┐
│       Stock         │─────────│   PriceDisplay      │   │    PriceAlert       │
├─────────────────────┤  uses   ├─────────────────────┤   ├─────────────────────┤
│ -observers: List    │ (1:many)│ +update(): void     │   │ +update(): void     │
├─────────────────────┤         └─────────────────────┘   └─────────────────────┘
│ +attachObserver()   │
│ +detachObserver()   │
│ +notifyObservers()  │
└─────────────────────┘
```

## Relationships
- **Stock → Subject**: implements (interface) - Concrete subject that maintains observers
- **Stock → _Observer**: aggregation (1:many) - Observers exist independently, can be attached/detached
- **PriceDisplay → _Observer**: implements (interface) - Concrete observer for display updates
- **PriceAlert → _Observer**: implements (interface) - Concrete observer for alerts

## Core Flow
1. Stock.attachObserver(observer) → registers observer to notification list
2. Stock.notifyObservers(message) → iterates through all observers
3. Observer.update(message) → each observer receives notification
4. Observers react independently → decoupled update logic

## Key Decisions
- List<_Observer> for O(n) notify, O(1) add → simple ArrayList suitable for small observer counts
- Push model (message passed to observers) → observers don't query subject
- No synchronization → not thread-safe, suitable for single-threaded scenarios
- Interface prefix `_Observer` → avoids conflict with java.util.Observer (deprecated)
