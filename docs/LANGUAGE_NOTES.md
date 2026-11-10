# Language notes: same design, idiomatic code

The designs are identical across languages; the code follows each language's habits.

| Idea | Java | C# | Python | TypeScript |
|---|---|---|---|---|
| Interface | `interface` | `interface` (prefixed `I`) | `Protocol` / `ABC` | `interface` |
| Immutable data | `record` | `record` | `@dataclass(frozen=True)` | `readonly` + `Object.freeze` |
| Atomic check-and-set | `synchronized` | `lock` | `threading.Lock` | none needed (single-threaded) unless you `await` in between |
| Strategy by key | `Map<String, Strategy>` (Spring injects bean maps) | dictionary or .NET 8 keyed services | `dict` | `Map` |
| Singleton | holder idiom / `enum` | `Lazy<T>` (or `AddSingleton`) | module-level object / double-checked lock | module-level object |
| Builder | nested `Builder` (or Lombok `@Builder`) | nested `Builder` (or records + `with`) | `dataclasses.replace` | object spread |
| Observer | interface + list | interface + list (also `event`) | list of callables | list of functions (also `EventEmitter`) |
| Chain of Responsibility | handler chain | handler chain (ASP.NET Core middleware) | handler chain | handler chain (Express/Koa middleware) |
| Money | `long` paise | `long` paise / `decimal` | `int` paise / `Decimal` | integer paise |

## Why these ports are not line-by-line translations
- Python and TypeScript often replace a pattern with a language feature (functions as strategies, spread instead of a builder). The examples still show the classic structure first, then mention the idiom.
- TypeScript examples avoid enums and constructor parameter properties so they run directly on Node's type stripping.
- C# examples use primary constructors (C# 12 / .NET 8).
