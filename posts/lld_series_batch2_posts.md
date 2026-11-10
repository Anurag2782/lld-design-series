# LLD Series — Batch 2 (7 posts, Design Patterns)

**Series arc:** SOLID answer → Strategy (2 posts) → Factory (2 posts) → Observer (2 posts).
**Series tag on every post:** `LLD Series | Part X/…` (continues from Part 7)
**Image rule (same as batch 1):** one portrait card (1080×1350). Top = class diagram, bottom = code (max ~15 lines, font 14+).
**Pattern posts follow one shape:** the pain → the pattern → the structure → when NOT to use it → question.

---

## What makes this batch stand out

**Post 1 (Challenge answers)** — Closes the loop on last week's challenge. People who commented get a reply, people who didn't get the answer key, and it reminds everyone the series exists.

**Posts 2–3 (Strategy)** — Part 1 teaches the pattern with a retry policy, something every backend dev has written badly. Part 2 shows the Spring Boot version (inject a `Map` of strategies), which is what people actually use at work.

**Posts 4–5 (Factory)** — Part 1 is the simple factory, which most people already write without a name. Part 2 clears up the Factory Method vs Abstract Factory confusion, the single most common LLD interview mix-up.

**Posts 6–7 (Observer)** — Part 1 builds it by hand so the idea is clear. Part 2 swaps to Spring events and covers the traps (synchronous by default, `AFTER_COMMIT`).

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Fri 16 Oct | SOLID challenge answers | 9am IST |
| Mon 19 Oct | Strategy Part 1 | 9am IST |
| Tue 20 Oct | Strategy Part 2 (Spring) | 9am IST |
| Wed 21 Oct | Factory Part 1 | 9am IST |
| Thu 22 Oct | Factory Part 2 | 9am IST |
| Fri 23 Oct | Observer Part 1 | 9am IST |
| Mon 26 Oct | Observer Part 2 (Spring events) | 9am IST |

---

# POST 1 — SOLID challenge: the answers

**Image:** Left = the original bad class with colored line highlights (one color per letter). Right = refactored class diagram: `OrderProcessor` → `OrderRepository`, `Notifier`, `InvoiceService`, `PaymentMethod` (← `CodPayment`, `CardPayment`).

**Image code (refactored):**

```java
interface PaymentMethod { void pay(Order o); }
interface OrderRepository { void save(Order o); }
interface Notifier { void orderPlaced(Order o); }
interface InvoiceService { void generate(Order o); }

class OrderProcessor {
    private final OrderRepository repo;
    private final Notifier notifier;
    private final InvoiceService invoices;

    OrderProcessor(OrderRepository repo, Notifier notifier,
                   InvoiceService invoices) { /* assign */ }

    void process(Order o, PaymentMethod payment) {
        payment.pay(o);
        repo.save(o);
        notifier.orderPlaced(o);
        invoices.generate(o);
    }
}
```

**Post text:**

LLD Series | Part 8 — Challenge answers 🧩

Yesterday's class had SOLID violations hiding in it. Here's the full answer key 👇

Line 2 → DIP
`new MySqlDatabase()` welds the processor to one database. Fix: depend on an `OrderRepository` interface.

Lines 5–6 → OCP
`if COD ... else if CARD`. Adding UPI means editing this method. Fix: a `PaymentMethod` interface, one class per type.

Lines 7–9 → SRP
Saving, emailing, and invoicing all inside one method. Four reasons to change, one class.

Lines 8–9 → DIP again
`new SmtpMailer()` and `new PdfInvoice()` hard-coded inside the method.

Line 14 → LSP
`GiftOrder` extends `Order` and throws on `getTotal()`. Anything holding an `Order` can crash.

ISP wasn't in this snippet, which was the trick. If you said ISP, tell me which interface you'd split 😄

The refactored version is in the image. `OrderProcessor` now only coordinates. Every collaborator can be swapped or faked in a test.

Notice something: fixing one violation usually fixed another. They're not 5 separate rules, they're 5 angles on the same goal: code that's cheap to change.

Starting next week: design patterns. Strategy first, in two parts.

Which one did you miss? 👇

#LLD #SOLID #Java #CleanCode #SoftwareEngineering

**First comment:**

"If you only remember one thing from SOLID: depend on small interfaces, and hand dependencies in from outside. That one habit fixes most of what's in that class."

**Notes for you:**
- Reply to every commenter with their score. This is what makes challenge posts pay off in reach.
- Line 14 (LSP) is the one most people miss because it's in a different class from the rest.

---

# POST 2 — Strategy Pattern, Part 1

**Image:** Class diagram: `ApiClient` → `RetryPolicy` (interface) ← `FixedDelayRetry`, `ExponentialBackoffRetry`, `NoRetry`. Short code card below.

**Image code:**

```java
interface RetryPolicy {
    boolean shouldRetry(int attempt);
    long delayMs(int attempt);
}

class ExponentialBackoffRetry implements RetryPolicy {
    public boolean shouldRetry(int attempt) { return attempt < 5; }
    public long delayMs(int attempt) { return 100L * (1L << attempt); }
}

class ApiClient {
    private final RetryPolicy policy;
    ApiClient(RetryPolicy policy) { this.policy = policy; }

    String call(Request r) throws InterruptedException {
        for (int attempt = 0; ; attempt++) {
            try { return http.send(r); }
            catch (IOException e) {
                if (!policy.shouldRetry(attempt)) throw new RuntimeException(e);
                Thread.sleep(policy.delayMs(attempt));
            }
        }
    }
}
```

**Post text:**

LLD Series | Part 9 — Strategy Pattern (1/2)

Every backend dev has written this:

```
if (mode.equals("FIXED")) sleep(1000);
else if (mode.equals("EXPONENTIAL")) sleep(100 * 2^attempt);
else if (mode.equals("NONE")) throw e;
```

Then someone adds jitter. Then a per-service config. Then it's a 40-line method nobody wants to touch.

Strategy pattern, in one sentence:
👉 Pull a changing algorithm out into an interface, and let the caller pick which one to use.

Three parts:
1️⃣ Strategy: `RetryPolicy` (the interface)
2️⃣ Concrete strategies: `FixedDelayRetry`, `ExponentialBackoffRetry`, `NoRetry`
3️⃣ Context: `ApiClient` (uses a policy, doesn't know which)

Now:
• payments API → `ExponentialBackoffRetry`
• internal health check → `NoRetry`
• new "retry with jitter" → one new class, nothing else changes

This is the Open/Closed Principle from Part 3 with a name. Same idea, now it's a pattern.

⚠️ When NOT to use it: if you have two options that will never grow, a simple `if` is clearer. Patterns need pain first.

The test: do you have a growing if-else choosing between interchangeable behaviors? That's Strategy.

Where have you seen an if-else chain that begged to be a Strategy? 👇

#LLD #DesignPatterns #StrategyPattern #Java #SystemDesign

**First comment:**

"Strategy vs State get confused a lot. Strategy: the CALLER chooses the behavior and it stays put. State: the OBJECT changes its own behavior as it moves through states. Same class diagram, different intent. I'll cover State later in the series."

**Notes for you:**
- The interface should describe WHAT (retry decision), not HOW. If your interface has a method named after one implementation, it's leaking.
- Strategies are usually stateless, which makes them safe to share as singletons.

---

# POST 3 — Strategy Pattern, Part 2 (Spring Boot)

**Image:** Diagram showing Spring injecting a `Map<String, PaymentStrategy>` into `PaymentService`, with three beans (`upi`, `card`, `cod`) and a request arrow `type=upi`. Code card below.

**Image code:**

```java
interface PaymentStrategy { void pay(Order o); }

@Component("upi")
class UpiPayment implements PaymentStrategy { /* ... */ }

@Component("card")
class CardPayment implements PaymentStrategy { /* ... */ }

@Service
class PaymentService {
    private final Map<String, PaymentStrategy> strategies;

    PaymentService(Map<String, PaymentStrategy> strategies) {
        this.strategies = strategies;   // Spring fills this: bean name -> bean
    }

    void pay(String type, Order o) {
        PaymentStrategy s = strategies.get(type);
        if (s == null) throw new IllegalArgumentException("Unknown: " + type);
        s.pay(o);
    }
}
```

**Post text:**

LLD Series | Part 10 — Strategy Pattern (2/2): the Spring Boot way

Yesterday: Strategy with a hand-picked policy.

Real apps have a different question: "The request says `type=upi`. How do I pick the right strategy without an if-else?"

Here's the trick Spring gives you for free 👇

If you inject a `Map<String, SomeInterface>`, Spring fills it with every bean of that type, keyed by bean name.

So:
• `@Component("upi")` → `UpiPayment`
• `@Component("card")` → `CardPayment`
• `PaymentService` receives a map with both

Lookup is one line: `strategies.get(type)`.

Adding "wallet" payments?
→ Create one class with `@Component("wallet")`.
→ Done. No edits to `PaymentService`. No new if-else.

That's OCP working inside a real framework.

⚠️ Things to handle:
• unknown key → fail clearly (don't NPE)
• don't pass raw user input as the key; validate or use an enum
• if you'd rather avoid bean-name keys, give the interface a `supports(type)` method and inject a `List<PaymentStrategy>`

Rule: the "which one?" decision still has to live somewhere. Strategy doesn't delete it, it moves it into ONE place instead of scattering if-else everywhere.

Do you use bean-name maps or a `supports()` method? 👇

#LLD #SpringBoot #DesignPatterns #Java #BackendEngineering

**First comment:**

"Gotcha: if you rename a bean or the class name changes without an explicit `@Component(\"upi\")`, the default bean name changes and your map key silently breaks. Always set the name explicitly, or use an enum key with a `supports()` method."

**Notes for you:**
- Default bean name = class name with lowercase first letter (`UpiPayment` → `upiPayment`). This is why explicit names matter.
- Try this yourself: build a tiny Spring Boot app with 3 strategies and print `strategies.keySet()` to see it.

---

# POST 4 — Factory Pattern, Part 1 (Simple Factory)

**Image:** Class diagram: `ReportService` → `ExporterFactory.create(type)` → returns `Exporter` (← `PdfExporter`, `CsvExporter`, `ExcelExporter`). Code card below.

**Image code:**

```java
interface Exporter { byte[] export(Report r); }

class ExporterFactory {
    static Exporter create(String type) {
        return switch (type.toUpperCase()) {
            case "PDF"   -> new PdfExporter();
            case "CSV"   -> new CsvExporter();
            case "EXCEL" -> new ExcelExporter();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}

// Caller doesn't know the concrete class
Exporter e = ExporterFactory.create(request.getFormat());
byte[] file = e.export(report);
```

**Post text:**

LLD Series | Part 11 — Factory Pattern (1/2)

You've seen this scattered across a codebase:

```
Exporter e;
if (fmt.equals("PDF")) e = new PdfExporter();
else if (fmt.equals("CSV")) e = new CsvExporter();
```

In the controller. In the scheduler. In the email job. Same chain, three copies.

Add "Excel" and you hunt for all three.

Factory pattern:
👉 Move the "which concrete class do I create?" decision into ONE place.

The caller says WHAT it wants (`"PDF"`). The factory decides HOW to build it. The caller only ever sees the `Exporter` interface.

Why it's worth it:
• one place to add a new type
• callers don't import concrete classes
• construction logic (config, dependencies) stays out of business code

Quick honesty: this is the "Simple Factory." It's not an official GoF pattern, but it's the version you'll use 90% of the time. The official ones come next post.

⚠️ Watch out: a factory with a giant switch is still an OCP violation. For a handful of types it's fine. When it keeps growing, combine it with the Spring map from the Strategy post.

Not every `new` needs a factory. If you create one concrete type in one place, just call `new`.

How many places in your code choose a class from a string? 👇

#LLD #DesignPatterns #FactoryPattern #Java #CleanCode

**First comment:**

"Test: if you grep for `new PdfExporter(` and find it in more than one or two places, you've got a factory waiting to be written."

**Notes for you:**
- Factory = creation logic in one place. Strategy = choice of behavior at runtime. They're often used together: a factory can return a strategy.
- The `switch` expression with `->` needs Java 14+. Change to classic `switch` if your project is on 11.

---

# POST 5 — Factory Pattern, Part 2 (Factory Method vs Abstract Factory)

**Image:** Two-panel diagram. Left: Factory Method (abstract `ReportJob` with `createExporter()`, subclasses `DailyJob`, `MonthlyJob`). Right: Abstract Factory (`InfraFactory` creating `Storage` + `MessageQueue`; `AwsFactory`, `LocalFactory`).

**Image code:**

```java
// Factory Method: subclass decides WHICH object
abstract class ReportJob {
    abstract Exporter createExporter();
    void run(Report r) { createExporter().export(r); }
}
class MonthlyJob extends ReportJob {
    Exporter createExporter() { return new PdfExporter(); }
}

// Abstract Factory: a FAMILY of related objects
interface InfraFactory {
    Storage storage();
    MessageQueue queue();
}
class AwsFactory implements InfraFactory { /* S3 + SQS */ }
class LocalFactory implements InfraFactory { /* disk + in-memory queue */ }
```

**Post text:**

LLD Series | Part 12 — Factory Pattern (2/2)

"Factory Method vs Abstract Factory" is one of the most-asked and most-confused LLD questions.

Here's the simplest way I can explain it 👇

🏭 Factory Method
One abstract method in a parent class: `createExporter()`.
Subclasses override it to decide which object gets created. The parent's logic (`run()`) uses whatever it's given.
→ Use when a class can't know in advance which object it needs, and subclasses should decide.

🏢 Abstract Factory
An interface that creates a FAMILY of related objects: `storage()` AND `queue()`.
`AwsFactory` gives you S3 + SQS. `LocalFactory` gives you disk + an in-memory queue.
→ Use when objects must work together and you want to swap the whole set at once.

Memory trick:
• Factory Method = one product, decided by inheritance
• Abstract Factory = many related products, decided by which factory you hold

The real-world example for backend devs: local dev vs production. Same service code, switch one factory, and every dependency changes together. No mixing S3 with an in-memory queue by accident.

⚠️ Abstract Factory is heavier. If you have only one product type, Simple Factory from yesterday is enough.

In Spring, `@Profile("local")` and `@Profile("prod")` configuration classes often play the Abstract Factory role.

Which one did you confuse before reading this? 👇

#LLD #DesignPatterns #FactoryPattern #Java #InterviewPrep

**First comment:**

"Interview answer in 15 seconds: 'Factory Method lets a subclass choose what to create. Abstract Factory creates families of related objects so they stay compatible.' Then give the local-vs-prod example."

**Notes for you:**
- Factory Method uses inheritance; Abstract Factory uses composition (you hold a factory object).
- Be ready for "which one does Spring's `BeanFactory` use?" Short answer: it's closer to a factory of factories; don't overthink it in interviews.

---

# POST 6 — Observer Pattern, Part 1

**Image:** Class diagram: `AppointmentService` (subject) holds `List<AppointmentObserver>`; observers: `SmsNotifier`, `CalendarSync`, `AnalyticsLogger`. Arrows labeled "register" and "notify". Code card below.

**Image code:**

```java
interface AppointmentObserver {
    void onBooked(Appointment a);
}

class AppointmentService {
    private final List<AppointmentObserver> observers = new ArrayList<>();

    void register(AppointmentObserver o) { observers.add(o); }

    void book(Appointment a) {
        repo.save(a);
        for (AppointmentObserver o : observers) o.onBooked(a);
    }
}

service.register(new SmsNotifier());
service.register(new CalendarSync());
```

**Post text:**

LLD Series | Part 13 — Observer Pattern (1/2)

Appointment booked. Now:
• send an SMS
• sync the calendar
• log analytics
• notify the doctor

The naive version: `book()` calls all four directly.

Every new requirement means editing `book()`. And the booking service now imports SMS, calendar, analytics... it knows about everything.

Observer pattern:
👉 One object (the subject) announces "something happened." Any number of observers react. The subject doesn't know or care who's listening.

Structure:
1️⃣ Observer interface: `AppointmentObserver.onBooked()`
2️⃣ Subject: `AppointmentService` keeps a list, loops through it after booking
3️⃣ Concrete observers: `SmsNotifier`, `CalendarSync`, `AnalyticsLogger`

Adding "notify the doctor"? Write a new observer, register it. `book()` is untouched.

This is the basis of event-driven design. Message queues, webhooks, UI listeners, all the same idea at different scales.

⚠️ Things to know before you use it:
• observers run in the order registered, one after another
• if one throws, the rest might never run (handle exceptions in the loop)
• a slow observer slows down the whole `book()` call

Use it when one action triggers several independent reactions. Don't use it when the "reaction" is actually part of the core logic.

Next post: how Spring does this with events, and the traps to avoid.

What's the longest chain of "and then also..." you've seen after a save? 👇

#LLD #DesignPatterns #ObserverPattern #Java #EventDriven

**First comment:**

"The sneaky cost of Observer: the flow becomes harder to read. 'What happens after booking?' is no longer answered in one method. Name your observers clearly, and keep the list short."

**Notes for you:**
- Subject = publisher, Observer = subscriber. Interview synonyms: Pub-Sub (but real pub-sub usually has a broker in between).
- In the hand-built version, the loop is synchronous. This matters for Part 2.

---

# POST 7 — Observer Pattern, Part 2 (Spring events)

**Image:** Flow diagram: `book()` → `publishEvent(AppointmentBooked)` → [`SmsListener`, `CalendarListener`] with a note "AFTER_COMMIT" on the DB commit step. Code card below.

**Image code:**

```java
record AppointmentBooked(Long appointmentId) {}

@Service
class AppointmentService {
    private final ApplicationEventPublisher publisher;
    // constructor omitted

    @Transactional
    void book(Appointment a) {
        repo.save(a);
        publisher.publishEvent(new AppointmentBooked(a.getId()));
    }
}

@Component
class SmsListener {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(AppointmentBooked e) { /* send SMS */ }
}
```

**Post text:**

LLD Series | Part 14 — Observer Pattern (2/2): Spring events

Yesterday: Observer built by hand.

In Spring Boot you don't write the registration list. You get it built in 👇

1️⃣ Publish an event: `publisher.publishEvent(new AppointmentBooked(id))`
2️⃣ Listen anywhere: `@EventListener` (or `@TransactionalEventListener`)

`AppointmentService` no longer knows ANY listeners exist. Perfect decoupling.

But three traps catch people 🚨

🔸 Trap 1: It's synchronous by default
A normal `@EventListener` runs on the same thread, inside the publisher's call. A slow listener slows `book()`. Add `@Async` (and enable it with `@EnableAsync`) if the reaction doesn't need to block.

🔸 Trap 2: The transaction
If a listener sends an SMS and then the booking transaction rolls back, you've texted someone about an appointment that doesn't exist.
Fix: `@TransactionalEventListener(phase = AFTER_COMMIT)`. It fires only if the commit succeeds.

🔸 Trap 3: Failures are silent when async
An exception in an `@Async` listener doesn't reach the caller. You need logging or an async exception handler, and for critical side effects (money, emails that MUST go out) an in-process event isn't durable enough. That's where the Outbox pattern comes in.

Rule I'd follow:
• in-process Spring events → light, internal, "nice to have" reactions
• outbox + queue → reactions that must never be lost

Which trap have you stepped in? 👇

#LLD #SpringBoot #EventDriven #Java #BackendEngineering

**First comment:**

"If a Spring event listener failing would cause a real business problem, it should not be a plain event. That's the exact line between Observer and the Outbox pattern. I wrote about the Outbox pattern earlier; link it here."

**Notes for you:**
- `AFTER_COMMIT` listeners run after the transaction is done; if you need to write to the DB inside such a listener you need a new transaction (`@Transactional(propagation = REQUIRES_NEW)`). Mention this if someone asks.
- This post links back to your existing Outbox content. Paste the link in the first comment.

---

## Next batch (preview)

1. Builder pattern (Part 1: telescoping constructors, Part 2: Lombok `@Builder` + validation)
2. Singleton (and why Spring beans make you rarely write one)
3. Decorator or Command (pick one; Decorator pairs well with a logging/caching wrapper)
4. First full problem: Parking Lot (Part 1: requirements + entities)
5. Parking Lot (Part 2: classes + code)
6. Parking Lot (Part 3: patterns applied)
7. Parking Lot extension test: add EV charging slots without touching old code
