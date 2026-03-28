# Strategy Pattern - Class Diagram

```
                                        ┌─────────────────────┐
                                        │   «interface»       │
                                        │  PaymentStrategy    │
                                        ├─────────────────────┤
                                        │ +makePayment()      │
                                        └─────────────────────┘
                                                 ▲
                                                 │ implements
                                 ┌───────────────┴───────────────┐
                                 │                               │
                    ┌─────────────────────┐         ┌─────────────────────┐
                    │ CashPaymentStrategy │         │ UPIPaymentStrategy  │
                    ├─────────────────────┤         ├─────────────────────┤
                    │ +makePayment()      │         │ +makePayment()      │
                    └─────────────────────┘         └─────────────────────┘
                                 ▲                               ▲
                                 │ uses                          │ uses
                                 └───────────────┬───────────────┘
                                                 │
                                     ┌─────────────────────┐
                                     │   ShoppingCart      │
                                     ├─────────────────────┤
                                     │ -paymentStrategy    │
                                     ├─────────────────────┤
                                     │ +setPaymentStrategy()│
                                     │ +checkout(): void   │
                                     └─────────────────────┘
```

## Relationships
- **ShoppingCart → PaymentStrategy**: aggregation (1:1) - Strategy can be swapped at runtime, exists independently
- **CashPaymentStrategy → PaymentStrategy**: implements (interface) - Concrete cash payment algorithm
- **UPIPaymentStrategy → PaymentStrategy**: implements (interface) - Concrete UPI payment algorithm

## Core Flow
1. ShoppingCart.setPaymentStrategy(strategy) → injects concrete strategy
2. ShoppingCart.checkout(amount) → delegates to current strategy
3. PaymentStrategy.makePayment(amount) → executes algorithm-specific logic
4. Client can swap strategies dynamically → behavior changes at runtime

## Key Decisions
- Strategy injected via setter → runtime flexibility, can change behavior dynamically
- Context (ShoppingCart) delegates to strategy → separates algorithm from context
- Open/Closed Principle → add new payment methods without modifying ShoppingCart
- Composition over inheritance → strategies are composed, not extended
