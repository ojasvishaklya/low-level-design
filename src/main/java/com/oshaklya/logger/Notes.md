Requirements:
1. Five severity levels: DEBUG < INFO < WARN < ERROR < FATAL.
2. Each record carries timestamp, level, message, emitting thread name.
3. Logger writes each record to one or more destinations, set at startup.
4. Each destination has its own min-level threshold and its own format.
   Format and destination type vary independently.
5. Concurrent calls are safe. A record's bytes never interleave with
   another record's bytes on the same destination.


Out of scope:
- Hot-reloading config at runtime
- Async / buffered writes
- Remote / network destinations in v1 (design should accommodate)
- Hierarchical / named loggers (com.app.service inheriting from com.app)