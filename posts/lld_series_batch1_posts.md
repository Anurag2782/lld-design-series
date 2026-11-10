# LLD Series — Batch 1 (7 posts, Foundations + SOLID)

**Series arc:** Framework → S → O → L → I → D → Challenge. Every post: one hook, one before/after, one image (code + structure), one question for comments.
**Series tag on every post:** `LLD Series | Part X/7`
**Image rule:** one portrait card (1080×1350). Top half = class diagram, bottom half = code (max ~15 lines, font 14+). Make code in ray.so / carbon.now.sh, diagram in Excalidraw.

---

## What makes this batch stand out

**Post 1 (The framework)** — Most LLD posts jump straight to patterns. This one gives people the 6-step process first, which is what interviewers actually score. Anyone preparing will save it as a checklist.

**Post 2 (SRP)** — Uses an `AppointmentService` that does everything. The "how many reasons to change?" test is easy to remember and easy to apply tomorrow.

**Post 3 (OCP)** — The if-else discount chain is a pain every engineer has met. The fix (Strategy) shows up naturally, so it also plants the seed for the design-pattern posts next week.

**Post 4 (LSP)** — The hardest SOLID letter, explained with a contract mindset instead of the textbook Square/Rectangle. The "if you need instanceof, the abstraction is lying" line is quotable.

**Post 5 (ISP)** — Healthcare roles (Doctor/Receptionist) make the fat-interface problem obvious without any theory.

**Post 6 (DIP)** — Connects SOLID to something Spring Boot developers already use daily (constructor injection) and shows the testing payoff.

**Post 7 (Challenge)** — Interactive: "find the 5 violations." Drives comments, and the answer post becomes next week's opener.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Wed 7 Oct | The LLD framework | 9am IST |
| Thu 8 Oct | SRP | 9am IST |
| Fri 9 Oct | OCP | 9am IST |
| Mon 12 Oct | LSP | 9am IST |
| Tue 13 Oct | ISP | 9am IST |
| Wed 14 Oct | DIP | 9am IST |
| Thu 15 Oct | SOLID challenge | 9am IST |

---

# POST 1 — The LLD framework

**Image:** Flow card. Six boxes top to bottom: `Requirements → Entities → Behaviors → Relationships → Patterns (only if pain) → Code + Extend`. Right side: a mini Parking Lot example (nouns: Vehicle, Slot, Ticket, Gate; verbs: park, unpark, pay).

**Post text:**

Most people fail LLD rounds in the first 2 minutes.

Not because they can't code. Because they start coding.

Someone says "design a parking lot" and the hands go straight to `class ParkingLot {`.

Here's the 6-step process I'm using for every LLD problem in this series 👇

1️⃣ Clarify requirements
Multiple floors? Vehicle types? Payment? Say out loud what's IN and what's OUT.

2️⃣ Find the entities (the nouns)
Vehicle, Slot, Ticket, Gate. If it's a noun in the requirements, it's a candidate class.

3️⃣ Find the behaviors (the verbs)
park(), unpark(), calculateFee(). Each verb goes to the class that owns the data it needs.

4️⃣ Draw relationships
Is-a or has-a? Who owns whom? Who just uses whom?

5️⃣ Add patterns ONLY where it hurts
A pattern with no pain is just decoration.

6️⃣ Code the core flow, then test it with one change
"What if we add EV charging slots?" If that needs edits in 6 places, the design is wrong.

Over the next weeks I'll go SOLID → design patterns → full problems (Parking Lot, Splitwise, BookMyShow, Elevator).

Every post has the code and the class structure as an image. Save this one, you'll need it.

Which LLD problem should I do first after SOLID? 👇

#LLD #SystemDesign #Java #SoftwareEngineering #InterviewPrep

**First comment (post within 30 min):**

"The step most people skip is #6. After you draw a design, throw one new requirement at it. If adding it means touching 5 existing classes, you found a bad design in 2 minutes instead of 2 weeks. I do this on every service I write at work now, not just in interviews."

**Notes for you (understanding):**
- Steps 2 and 3 are the heart of it. Nouns become classes, verbs become methods. Put the method where the data lives.
- Step 5 is why people over-engineer. Patterns solve a specific pain; if you can't name the pain, skip the pattern.

---

# POST 2 — Single Responsibility Principle

**Image:** Left = "Before" code card (`AppointmentService` doing 4 jobs). Right = "After" class diagram: `AppointmentService` → `AppointmentRepository`, `NotificationService`, `InvoiceGenerator`.

**Image code (Before):**

```java
class AppointmentService {
    void book(Appointment a) {
        if (a.getSlot() == null) throw new IllegalArgumentException();
        db.save(a);                          // persistence
        emailClient.send(a.getPatient(),     // notification
            "Booked for " + a.getTime());
        pdf.generate(a);                     // invoice
    }
}
```

**Image code (After):**

```java
class AppointmentService {
    private final AppointmentRepository repo;
    private final NotificationService notifier;
    private final InvoiceGenerator invoices;

    void book(Appointment a) {
        validate(a);
        repo.save(a);
        notifier.confirm(a);
        invoices.generate(a);
    }
}
```

**Post text:**

LLD Series | Part 2/7 — Single Responsibility Principle

Quick test for any class you write:

"How many different reasons could this class need to change?"

Look at this appointment booking method 👇
(see image)

It changes when:
• validation rules change
• the database changes
• the email wording changes
• the invoice format changes

That's 4 reasons. 4 different people asking for changes. 4 chances to break the other 3 things.

SRP says: one class, one reason to change.

After the split, `AppointmentService` only coordinates. It doesn't know HOW an email is sent or HOW a PDF is built. Change the invoice layout and the booking logic isn't even touched.

Bonus: you can now test booking without a real email server.

⚠️ Don't over-split. A class with 3 tiny methods that all change together is fine. SRP is about reasons to change, not about line count.

Where have you seen a "god class" in a real codebase? Mine was always called something like `Manager` or `Helper` 😅

#LLD #SOLID #Java #CleanCode #SpringBoot

**First comment:**

"Easy way to spot SRP violations: try to name the class without using 'and'. 'AppointmentValidatorAndNotifierAndInvoicer' is not a name, it's a confession."

**Notes for you:**
- "Reason to change" = a stakeholder or a technology. Business rules, DB, email provider and PDF library are different axes of change.
- The coordinating class still has a job: orchestration. That is one responsibility.

---

# POST 3 — Open/Closed Principle

**Image:** Left = if-else `DiscountCalculator`. Right = class diagram: `DiscountStrategy` interface ← `RegularDiscount`, `PremiumDiscount`, `StudentDiscount`; `DiscountCalculator` uses `DiscountStrategy`.

**Image code (Before):**

```java
double discount(String type, double amount) {
    if (type.equals("REGULAR")) return amount * 0.05;
    else if (type.equals("PREMIUM")) return amount * 0.15;
    else if (type.equals("STUDENT")) return amount * 0.10;
    return 0;
}
```

**Image code (After):**

```java
interface DiscountStrategy {
    double apply(double amount);
}

class PremiumDiscount implements DiscountStrategy {
    public double apply(double amount) { return amount * 0.15; }
}

class Checkout {
    double total(double amount, DiscountStrategy s) {
        return amount - s.apply(amount);
    }
}
```

**Post text:**

LLD Series | Part 3/7 — Open/Closed Principle

Product manager: "Can we add a festival discount?"

Dev opens `discount()`. Adds another `else if`.

Next month: employee discount. Another `else if`.
Next quarter: it's 14 branches and nobody dares touch it.

Open/Closed Principle:
👉 Open for extension
👉 Closed for modification

Meaning: adding new behavior should mean ADDING code, not EDITING code that already works and is already tested.

The fix is an interface plus one class per rule (this is the Strategy pattern, we'll go deep on it next week).

Now "festival discount" = one new class. Zero changes to `Checkout`. Zero risk to existing discounts.

How to spot an OCP violation:
• a growing if-else or switch on a "type" field
• every new feature touches the same method
• you're afraid to edit a function

⚠️ Don't build extension points for things that will never change. Wait until the second `else if`, then refactor.

Do you prefer a switch statement or Strategy? Tell me when a switch is actually fine 👇

#LLD #SOLID #DesignPatterns #Java #SoftwareEngineering

**First comment:**

"Rule I follow: the first time, just write the if. The second time, notice. The third time, refactor to an interface. Designing for extension too early is how you end up with an interface and exactly one implementation forever."

**Notes for you:**
- OCP is almost always achieved with polymorphism: interface + multiple implementations, chosen at runtime.
- The "decision" about which strategy to use still lives somewhere (a factory or Spring bean map). OCP moves it to one place, it doesn't remove it.

---

# POST 4 — Liskov Substitution Principle

**Image:** Class diagram of the broken design (`Account` ← `SavingsAccount`, `FixedDepositAccount` with a red ✖ on `withdraw()`), next to the fixed design (`Account` ← `WithdrawableAccount` ← `SavingsAccount`; `FixedDepositAccount` separate).

**Image code:**

```java
// ❌ Breaks the contract
class FixedDepositAccount extends Account {
    @Override
    void withdraw(double amt) {
        throw new UnsupportedOperationException();
    }
}

// ✅ Honest hierarchy
interface Withdrawable { void withdraw(double amt); }
class SavingsAccount extends Account implements Withdrawable { ... }
class FixedDepositAccount extends Account { /* no withdraw */ }
```

**Post text:**

LLD Series | Part 4/7 — Liskov Substitution Principle

This is the SOLID letter people nod at and then break the same afternoon.

The rule: if `B` extends `A`, you should be able to use `B` anywhere `A` is expected without anything surprising happening.

Look at this 👇 (see image)

`Account` has `withdraw()`. `FixedDepositAccount` extends it... and throws an exception when you call it.

Now every piece of code that holds an `Account` is a trap:

```
for (Account a : accounts) a.withdraw(100); // 💥
```

To be safe you'd need `if (a instanceof FixedDepositAccount)`. And now the abstraction is lying to you.

Think of a parent class as a CONTRACT:
• subclasses can't demand stricter inputs
• subclasses can't give weaker outputs
• subclasses can't throw new surprises

The fix isn't a smarter subclass. It's a more honest hierarchy: only accounts that can withdraw implement `Withdrawable`.

🚩 Red flags you've broken LSP:
• `instanceof` checks on a parent type
• overridden methods that throw `UnsupportedOperationException`
• empty overrides that silently do nothing

"Is-a" in English isn't the same as "is-a" in code. A penguin is a bird, but it can't `fly()`.

Have you ever hit this in production? What was it?

#LLD #SOLID #Java #OOP #CleanCode

**First comment:**

"Quick test: if a subclass needs a comment like 'don't call this on X', the inheritance is wrong. Fix the hierarchy, not the documentation."

**Notes for you:**
- LSP is about behavior, not syntax. The code compiles either way; it breaks at runtime.
- Prefer composition and small interfaces when a subtype can't honor every parent behavior.

---

# POST 5 — Interface Segregation Principle

**Image:** Left = one fat `StaffMember` interface with 4 methods and `Receptionist` highlighted with 2 forced empty implementations. Right = three small interfaces: `Treats`, `Prescribes`, `HandlesBilling`, with `Doctor` implementing two and `Receptionist` one.

**Image code:**

```java
// ❌ Fat interface
interface StaffMember {
    void treatPatient();
    void prescribe();
    void handleBilling();
    void scheduleAppointment();
}
// Receptionist is forced to implement treatPatient() and prescribe()

// ✅ Segregated
interface Treats { void treatPatient(); }
interface Prescribes { void prescribe(); }
interface HandlesBilling { void handleBilling(); }

class Doctor implements Treats, Prescribes { ... }
class Receptionist implements HandlesBilling { ... }
```

**Post text:**

LLD Series | Part 5/7 — Interface Segregation Principle

Imagine you build a hospital system with one interface:

`StaffMember` → treat, prescribe, bill, schedule.

Then you add `Receptionist`.

A receptionist can't treat patients or prescribe medicine. But the interface forces her class to implement both.

So you write:

```
public void prescribe() {
    throw new UnsupportedOperationException();
}
```

Sound familiar? That's an LSP violation caused by an ISP violation. They travel together.

ISP says: no class should be forced to depend on methods it doesn't use.

Fix: break the fat interface into small, role-based ones. `Doctor` implements `Treats` + `Prescribes`. `Receptionist` implements only what she does.

Why it matters beyond cleanliness:
• changing `prescribe()` no longer forces recompiling or retesting `Receptionist`
• callers depend on the narrow role they need, not the whole object
• mocks in tests become tiny

💡 Java's own library does this: `Comparable`, `Runnable`, `Closeable` are all tiny on purpose.

Rule of thumb: if an interface has more than ~5 methods, ask who is being forced to carry dead weight.

What's the fattest interface you've ever had to implement? 👇

#LLD #SOLID #Java #SoftwareDesign #CleanArchitecture

**First comment:**

"Spring Data is a great example. `CrudRepository` is small; `JpaRepository` extends it with more. You pick the narrowest one your class actually needs. That's ISP working in a framework you already use."

**Notes for you:**
- Name interfaces after roles (what the client needs), not after the object (what the class is).
- Segregation is the opposite of one giant `IService` interface. Small and composable beats big and complete.

---

# POST 6 — Dependency Inversion Principle

**Image:** Class diagram with arrows. Left (bad): `NotificationService` → `EmailSender` (concrete). Right (good): `NotificationService` → `MessageSender` (interface) ← `EmailSender`, `SmsSender`, `FakeSender` (tests).

**Image code:**

```java
// ❌ High-level depends on low-level
class NotificationService {
    private EmailSender sender = new EmailSender();
}

// ✅ Both depend on the abstraction
interface MessageSender { void send(String to, String msg); }

@Service
class NotificationService {
    private final MessageSender sender;

    NotificationService(MessageSender sender) {  // injected
        this.sender = sender;
    }
}
```

**Post text:**

LLD Series | Part 6/7 — Dependency Inversion Principle

Your `NotificationService` has `new EmailSender()` inside it.

Now:
• switching to SMS means editing the service
• unit tests send REAL emails (or need a real SMTP server)
• the business logic is welded to one provider

DIP says two things:
1️⃣ High-level modules shouldn't depend on low-level modules. Both should depend on abstractions.
2️⃣ Abstractions shouldn't depend on details. Details depend on abstractions.

In plain words: your business logic should define WHAT it needs (`MessageSender`), and let the outside world decide WHO provides it.

Look at the image 👇 The arrow direction flips. That's the "inversion."

Spring Boot developers use this daily without naming it:
• `@Service` with constructor injection = DIP + Dependency Injection
• swap `EmailSender` for `SmsSender` by changing a bean, not business code
• in tests, pass a `FakeSender` that just records calls

⚠️ Quick distinction people mix up:
• DIP = the principle (depend on abstractions)
• DI = the technique (pass dependencies in)
• IoC container (Spring) = the tool that does the passing

You can follow DIP without Spring. Spring just makes it convenient.

Did you ever mock a class and realize you were really missing an interface? 👇

#LLD #SOLID #SpringBoot #Java #DependencyInjection

**First comment:**

"Test of whether you're really following DIP: can you unit test your service with zero real infrastructure (no DB, no SMTP, no network)? If not, something concrete is leaking into your business logic."

**Notes for you:**
- Constructor injection over field `@Autowired`: dependencies are explicit, final, and testable without Spring.
- Don't create an interface for every class. Create one where you have (or expect) more than one implementation, or where you need to isolate infrastructure in tests.

---

# POST 7 — SOLID Challenge (interactive)

**Image:** Code card of the "bad" class below with line numbers, so people can reference lines in comments.

**Image code:**

```java
 1  class OrderProcessor {
 2      private MySqlDatabase db = new MySqlDatabase();
 3
 4      void process(Order o, String type) {
 5          if (type.equals("COD")) { /* cash logic */ }
 6          else if (type.equals("CARD")) { /* card logic */ }
 7          db.save(o);
 8          new SmtpMailer().send(o.getEmail(), "Order placed");
 9          new PdfInvoice().generate(o);
10      }
11  }
12
13  class GiftOrder extends Order {
14      @Override double getTotal() { throw new UnsupportedOperationException(); }
15  }
```

**Post text:**

LLD Series | Part 7/7 — Challenge time 🧩

6 posts, 5 principles. Let's see if they stuck.

The class in the image has SOLID violations hiding in it.

Your task:
👉 Find as many as you can
👉 Comment the line number + which letter (S/O/L/I/D) it breaks
👉 Bonus: say how you'd fix it

Hint: there are at least 5.

I'll reply to every correct answer. Tomorrow I'll post the full refactored version with the class diagram.

And starting next week: Design Patterns, with the same format. Strategy, Factory, Observer, Builder, one at a time, with real code and structure.

If this series is helping, repost it for someone preparing for LLD rounds ♻️

Drop your answers 👇

#LLD #SOLID #Java #SoftwareEngineering #CodingChallenge

**First comment:**

"Spoiler-free hint: lines 2, 5-6, 7-9, and 14 are not all the same kind of problem. One line violates two principles at once."

**Notes for you (answer key, don't post):**
- Line 2: DIP (concrete `MySqlDatabase`).
- Lines 5-6: OCP (if-else on payment type; new type = edit this method).
- Lines 7-9: SRP (persistence, email, and PDF inside one method) and DIP (`new SmtpMailer()`, `new PdfInvoice()`).
- Line 14: LSP (subclass throws on an inherited method).
- ISP: not directly in this snippet. Point this out if someone says it is, and ask them which interface. Good conversation starter, and a nice reason to say "ISP was the trick one."

---

## Next batch (preview, for planning)

1. Strategy pattern (Part 1: concept + code, Part 2: Spring Boot bean-map version)
2. Factory pattern (Part 1: Simple Factory, Part 2: Factory Method vs Abstract Factory)
3. Observer pattern (Part 1: concept, Part 2: event-driven version with Spring events)
4. Builder pattern
5. Singleton (and why it's often a trap)
6. First full problem: Parking Lot (Part 1: requirements + entities, Part 2: code + extension test)
7. Parking Lot (Part 3: patterns applied + EV charging extension)
