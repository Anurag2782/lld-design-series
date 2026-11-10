# LLD Design Series

Low-Level Design, learned properly: SOLID, design patterns, and full interview problems, with **runnable code in four languages** (Java, C#, Python, TypeScript).

Every example is a small program that **checks its own behavior** (assertions print `ok: ...` lines and fail loudly if something is wrong), so you can read the code, run it, and change it with confidence.

This repo accompanies a LinkedIn series. The posts themselves are in [`posts/`](posts/); the plan for what comes next is in [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Run everything

```bash
./run-all.sh              # runs every language you have installed
```

Or one language at a time:

| Language | Needs | Run all | Run one example |
|---|---|---|---|
| Java | JDK 17+ | `./java/run-all.sh` | `javac -d out $(find src -name '*.java') && java -cp out lld.patterns.strategy.Demo` (from `java/`) |
| C# | .NET SDK 8+ | `cd dotnet && dotnet run` | `dotnet run -- strategy` |
| Python | Python 3.10+ | `cd python && python run_all.py` | `python run_all.py strategy` |
| TypeScript | Node 22.18+ | `cd typescript && npm start` | `node src/run_all.ts strategy` |

TypeScript runs directly on Node (built-in type stripping, no build step). `npm install && npm run typecheck` adds strict type checking.

## What's inside

### SOLID

| Topic | What the example shows | Code | Post batch |
|---|---|---|---|
| **Single Responsibility** | Appointment booking split into repo / notifier / invoices | [Java](java/src/lld/solid/srp/Demo.java) · [C#](dotnet/src/Solid/Srp.cs) · [Python](python/lld/solid/srp.py) · [TS](typescript/src/solid/srp.ts) | batch 1 |
| **Open/Closed** | Discounts as strategies, new one without edits | [Java](java/src/lld/solid/ocp/Demo.java) · [C#](dotnet/src/Solid/Ocp.cs) · [Python](python/lld/solid/ocp.py) · [TS](typescript/src/solid/ocp.ts) | batch 1 |
| **Liskov Substitution** | Honest account hierarchy, no throwing overrides | [Java](java/src/lld/solid/lsp/Demo.java) · [C#](dotnet/src/Solid/Lsp.cs) · [Python](python/lld/solid/lsp.py) · [TS](typescript/src/solid/lsp.ts) | batch 1 |
| **Interface Segregation** | Doctor vs Receptionist role interfaces | [Java](java/src/lld/solid/isp/Demo.java) · [C#](dotnet/src/Solid/Isp.cs) · [Python](python/lld/solid/isp.py) · [TS](typescript/src/solid/isp.ts) | batch 1 |
| **Dependency Inversion** | Inject a MessageSender, test with a fake | [Java](java/src/lld/solid/dip/Demo.java) · [C#](dotnet/src/Solid/Dip.cs) · [Python](python/lld/solid/dip.py) · [TS](typescript/src/solid/dip.ts) | batch 1 |

### Design patterns

| Topic | What the example shows | Code | Post batch |
|---|---|---|---|
| **Strategy** | Retry policies + payment strategy by key | [Java](java/src/lld/patterns/strategy/Demo.java) · [C#](dotnet/src/Patterns/Strategy.cs) · [Python](python/lld/patterns/strategy.py) · [TS](typescript/src/patterns/strategy.ts) | batch 2 |
| **Factory (simple, method, abstract)** | Exporters, report jobs, infra families | [Java](java/src/lld/patterns/factory/Demo.java) · [C#](dotnet/src/Patterns/Factory.cs) · [Python](python/lld/patterns/factory.py) · [TS](typescript/src/patterns/factory.ts) | batch 2 |
| **Observer** | Appointment events with isolated observer failures | [Java](java/src/lld/patterns/observer/Demo.java) · [C#](dotnet/src/Patterns/Observer.cs) · [Python](python/lld/patterns/observer.py) · [TS](typescript/src/patterns/observer.ts) | batch 2 |
| **Builder** | Validated immutable Appointment + copy-with-change | [Java](java/src/lld/patterns/builder/Demo.java) · [C#](dotnet/src/Patterns/Builder.cs) · [Python](python/lld/patterns/builder.py) · [TS](typescript/src/patterns/builder.ts) | batch 3 |
| **Singleton** | Thread-safe instance, tested under concurrency | [Java](java/src/lld/patterns/singleton/Demo.java) · [C#](dotnet/src/Patterns/Singleton.cs) · [Python](python/lld/patterns/singleton.py) · [TS](typescript/src/patterns/singleton.ts) | batch 3 |
| **Decorator** | Caching + logging wrappers, order matters | [Java](java/src/lld/patterns/decorator/Demo.java) · [C#](dotnet/src/Patterns/Decorator.cs) · [Python](python/lld/patterns/decorator.py) · [TS](typescript/src/patterns/decorator.ts) | batch 3 |
| **State** | Order lifecycle + vending machine | [Java](java/src/lld/patterns/state/Demo.java) · [C#](dotnet/src/Patterns/State.cs) · [Python](python/lld/patterns/state.py) · [TS](typescript/src/patterns/state.ts) | batch 5 |
| **Command** | Undo/redo with two stacks | [Java](java/src/lld/patterns/command/Demo.java) · [C#](dotnet/src/Patterns/Command.cs) · [Python](python/lld/patterns/command.py) · [TS](typescript/src/patterns/command.ts) | batch 5 |
| **Chain of Responsibility** | Booking validation pipeline | [Java](java/src/lld/patterns/chain/Demo.java) · [C#](dotnet/src/Patterns/Chain.cs) · [Python](python/lld/patterns/chain.py) · [TS](typescript/src/patterns/chain.ts) | batch 6 |
| **Proxy (vs Decorator)** | Protection proxy, lazy proxy, decorator contrast | [Java](java/src/lld/patterns/proxy/Demo.java) · [C#](dotnet/src/Patterns/Proxy.cs) · [Python](python/lld/patterns/proxy.py) · [TS](typescript/src/patterns/proxy.ts) | batch 6 |
| **Adapter** | Wrap a vendor payment SDK behind your interface | [Java](java/src/lld/patterns/adapter/Demo.java) · [C#](dotnet/src/Patterns/Adapter.cs) · [Python](python/lld/patterns/adapter.py) · [TS](typescript/src/patterns/adapter.ts) | batch 7 |
| **Template Method** | Importer skeleton + callback template (open/close guaranteed) | [Java](java/src/lld/patterns/templatemethod/Demo.java) · [C#](dotnet/src/Patterns/TemplateMethod.cs) · [Python](python/lld/patterns/templatemethod.py) · [TS](typescript/src/patterns/templatemethod.ts) | batch 8 |
| **Facade** | One checkout call over four services, with compensation | [Java](java/src/lld/patterns/facade/Demo.java) · [C#](dotnet/src/Patterns/Facade.cs) · [Python](python/lld/patterns/facade.py) · [TS](typescript/src/patterns/facade.ts) | batch 8 |
| **Composite** | File system tree with cycle protection | [Java](java/src/lld/patterns/composite/Demo.java) · [C#](dotnet/src/Patterns/Composite.cs) · [Python](python/lld/patterns/composite.py) · [TS](typescript/src/patterns/composite.ts) | batch 9 |

### Full problems

| Topic | What the example shows | Code | Post batch |
|---|---|---|---|
| **Parking Lot** | Atomic slot allocation, pricing strategy, EV extension | [Java](java/src/lld/problems/parkinglot/Demo.java) · [C#](dotnet/src/Problems/ParkingLot.cs) · [Python](python/lld/problems/parkinglot.py) · [TS](typescript/src/problems/parkinglot.ts) | batch 4 |
| **Splitwise** | Split strategies, exact paise, simplify debts | [Java](java/src/lld/problems/splitwise/Demo.java) · [C#](dotnet/src/Problems/Splitwise.cs) · [Python](python/lld/problems/splitwise.py) · [TS](typescript/src/problems/splitwise.ts) | batch 5-6 |
| **BookMyShow** | Timed seat locks, all-or-nothing, concurrency test | [Java](java/src/lld/problems/bookmyshow/Demo.java) · [C#](dotnet/src/Problems/BookMyShow.cs) · [Python](python/lld/problems/bookmyshow.py) · [TS](typescript/src/problems/bookmyshow.ts) | batch 6 |
| **Elevator** | SCAN movement + cost-based dispatch | [Java](java/src/lld/problems/elevator/Demo.java) · [C#](dotnet/src/Problems/Elevator.cs) · [Python](python/lld/problems/elevator.py) · [TS](typescript/src/problems/elevator.ts) | batch 7 |
| **Rate Limiter** | Token bucket with lazy refill | [Java](java/src/lld/problems/ratelimiter/Demo.java) · [C#](dotnet/src/Problems/RateLimiter.cs) · [Python](python/lld/problems/ratelimiter.py) · [TS](typescript/src/problems/ratelimiter.ts) | batch 7 |
| **ATM** | State for the session, note chain, plan-then-commit | [Java](java/src/lld/problems/atm/Demo.java) · [C#](dotnet/src/Problems/Atm.cs) · [Python](python/lld/problems/atm.py) · [TS](typescript/src/problems/atm.ts) | batch 8 |
| **Library Management** | Book vs BookCopy, fine strategy, return listener | [Java](java/src/lld/problems/library/Demo.java) · [C#](dotnet/src/Problems/Library.cs) · [Python](python/lld/problems/library.py) · [TS](typescript/src/problems/library.ts) | batch 9 |
| **Chess** | Piece-owned rules, check/checkmate, Fool's Mate test | [Java](java/src/lld/problems/chess/Demo.java) · [C#](dotnet/src/Problems/Chess.cs) · [Python](python/lld/problems/chess.py) · [TS](typescript/src/problems/chess.ts) | batch 9 |

## The framework used on every problem

1. Clarify requirements (and say what is OUT of scope)
2. Find the entities (nouns)
3. Find the behaviors (verbs) and put each method where the data lives
4. Draw relationships (is-a vs has-a)
5. Add a pattern **only where it hurts**
6. Code the core flow, then throw **one new requirement** at it (the extension test)

See the one-page [`docs/CHEATSHEET.md`](docs/CHEATSHEET.md).

## Repo layout

```
lld-design-series/
  java/         src/lld/<group>/<topic>/Demo.java   (one package per topic)
  dotnet/       src/<Group>/<Topic>.cs              (one namespace per topic)
  python/       lld/<group>/<topic>.py              (one module per topic)
  typescript/   src/<group>/<topic>.ts              (one module per topic)
  posts/        the LinkedIn posts (Markdown), batch by batch
  docs/         cheat sheet, roadmap, language notes
```

## Design choices worth knowing

- **Money is integers (paise), never floating point.** Parking fees and Splitwise shares are exact.
- **"Check and act" is one atomic step.** Parking slots and BookMyShow seats use per-object locks; the Java, C# and Python versions include a many-threads test that proves no double allocation. TypeScript is single-threaded, so the same code is atomic as long as there is no `await` between the check and the set (the code comments say so).
- **Time and clocks are injected** where it matters (seat-lock expiry, rate limiting, pricing), so tests are fast and deterministic.
- **Examples are trimmed on purpose.** They show the idea clearly rather than being production frameworks; each file lists what it leaves out.
- **Idiomatic per language.** Each port uses its language's natural idioms (see [`docs/LANGUAGE_NOTES.md`](docs/LANGUAGE_NOTES.md)) instead of a line-by-line translation.

## Contributing

Found a bug, a better approach, or want another language (Go, Kotlin, Rust)? Open an issue or a pull request. Keep each example self-checking and runnable with the commands above.

## License

MIT, see [LICENSE](LICENSE).
