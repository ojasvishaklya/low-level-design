# Builder Pattern - Class Diagram

```
┌─────────────────────────────────────────┐
│          HttpRequest                    │
├─────────────────────────────────────────┤
│ -url: String                            │
│ -method: String                         │
│ -headers: Map<String, String>           │
│ -body: String                           │
├─────────────────────────────────────────┤
│ -HttpRequest()                          │  private constructor
└─────────────────────────────────────────┘
         ▲
         │ composition (1:1)
         │ (inner class)
         │
┌─────────────────────────────────────────┐
│      HttpRequest.Builder                │
├─────────────────────────────────────────┤
│ -request: HttpRequest                   │
├─────────────────────────────────────────┤
│ +url(url: String): Builder              │
│ +method(method: String): Builder        │
│ +header(key, value): Builder            │
│ +body(body: String): Builder            │
│ +build(): HttpRequest                   │
└─────────────────────────────────────────┘
```

## Relationships
- **HttpRequest.Builder → HttpRequest**: composition (1:1) - Builder creates and fully owns HttpRequest, lifecycle tied
- **HttpRequest.Builder (inner class)**: static nested class - Has access to private HttpRequest constructor

## Core Flow
1. new HttpRequest.Builder() → creates builder with empty HttpRequest
2. Builder.url().method().header().body() → fluent chaining sets fields
3. Each builder method returns this → enables method chaining
4. Builder.build() → validates required fields, returns HttpRequest
5. HttpRequest constructor is private → enforces builder usage

## Key Decisions
- Static inner Builder class → access to private constructor, logical grouping
- Fluent interface (returns this) → readable, chainable API
- Validation in build() → fail-fast for required fields (url mandatory)
- Immutable HttpRequest → no setters, fields set only during construction
- Lazy header map initialization → avoids empty Map allocation if no headers
