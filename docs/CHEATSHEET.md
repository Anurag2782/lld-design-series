# LLD cheat sheet

## The 6-step framework
| Step | Do this |
|---|---|
| 1. Requirements | Ask 4-5 questions. Say what is IN and OUT of scope. State assumptions out loud. |
| 2. Entities | The nouns. Not every noun becomes a class. |
| 3. Behaviors | The verbs. Put each method where the data lives. |
| 4. Relationships | Is-a vs has-a. Who owns whom. |
| 5. Patterns | Only where you can name the pain. |
| 6. Extension test | Throw ONE new requirement at the design. Count the classes that break. |

## SOLID
| | Principle | One-line test |
|---|---|---|
| S | Single Responsibility | How many reasons could this class need to change? |
| O | Open/Closed | Can I add behavior by adding a class instead of editing a method? |
| L | Liskov Substitution | Can a subclass do everything its parent promised? |
| I | Interface Segregation | Is any class forced to implement methods it never uses? |
| D | Dependency Inversion | Does my business logic depend on an interface, with the dependency passed in? |

## Patterns: use it when...
| Pattern | Use it when | Not when |
|---|---|---|
| Strategy | A growing if-else chooses between interchangeable algorithms | Two options that will never grow |
| Factory | `new X` is chosen from a string/type in many places | One concrete type, one place |
| Observer | One event triggers several independent reactions | The reaction is part of the core logic |
| Builder | 4+ fields, several optional, validation needed | 2-3 required fields |
| Singleton | One shared, stateless or thread-safe instance (config, pool) | Anything holding per-request/user data |
| Decorator | Add behavior (log, cache, retry) by wrapping, same interface | You need to control access instead |
| State | Behavior differs per lifecycle state, many operations | 2-3 states with simple rules (use an enum) |
| Command | A request must be stored, queued, undone or logged | A plain method call is enough |
| Chain of Responsibility | 4+ independent checks/handlers on one request | A couple of checks |
| Proxy | Control access: permissions, lazy loading, remote calls | You only add behavior (that's Decorator) |
| Adapter | A third-party API does not match your interface | You own both sides |

## Confusions, cleared
- **Strategy vs State:** caller picks the behavior (Strategy) vs the object changes its own behavior over time (State).
- **Decorator vs Proxy:** Decorator ADDS behavior and always calls through; Proxy CONTROLS access and may not call through.
- **Factory Method vs Abstract Factory:** one product decided by inheritance vs a FAMILY of related products.
- **Adapter vs Facade:** change an interface so it fits vs simplify a big subsystem.
- **DIP vs DI vs IoC:** the principle vs the technique (pass it in) vs the tool (container).

## Concurrency reflex
"Check and act must be ONE atomic step." Two threads, one resource, three costumes: parking slot, movie seat, rate-limit token.
Move to Redis / a database unique constraint when you have more than one server.

## Money
Integers (paise) or `BigDecimal` / `decimal` / `Decimal`. Never floating point. Shares must add up to the total exactly.
