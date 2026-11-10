# LLD Series — Batch 4 (7 posts, Parking Lot + Mix)

**Pattern of this batch:** your first full LLD problem, built across 4 technical posts, with 2 mix posts in between and a cheat-sheet recap at the end.
**Series tag:** only on technical posts: `LLD Series | Part X`. Mix posts have no tag.
**Image rule for technical posts:** one portrait card (1080×1350). Top = class diagram, bottom = code (max ~15 lines).
**Fill-ins:** anything in `[square brackets]` needs YOUR real detail. Replace or delete the line. Don't post a made-up story.

---

## What makes this batch stand out

**Post 1 (Requirements + entities)** — Applies the 6-step framework from Part 1 live. People finally see the framework used on a real problem, which is what interviewers want to see.

**Post 2 (Mix: what I got wrong)** — Mistakes posts do well because they're honest, and the list doubles as an interview checklist.

**Post 3 (Classes + code)** — Full working core flow, including the concurrency detail (two cars grabbing one slot) that most tutorials skip.

**Post 4 (Strategy + Factory)** — Brings back two patterns from earlier batches and shows them solving a real problem, so the series feels connected.

**Post 5 (Mix: the cook's story)** — Second parable in the series (SRP). Repeatable format that nobody else on LinkedIn is doing.

**Post 6 (Extension test: EV charging)** — The post that proves the design. Includes an honest "one small change needed" and an open question for comments.

**Post 7 (Cheat sheet)** — High-save post. Everything from the series on one image.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Thu 5 Nov | Parking Lot Part 1: requirements + entities | 9am IST |
| Fri 6 Nov | MIX: Mistakes in my first LLD attempts | 9am IST |
| Mon 9 Nov | Parking Lot Part 2: classes + code | 9am IST |
| Tue 10 Nov | Parking Lot Part 3: Strategy + Factory | 9am IST |
| Wed 11 Nov | MIX: The cook's story (SRP parable) | 9am IST |
| Thu 12 Nov | Parking Lot Part 4: EV charging extension | 9am IST |
| Fri 13 Nov | LLD cheat sheet (recap) | 9am IST |

---

# POST 1 — Parking Lot, Part 1: Requirements + entities

**Image:** Two columns. Left: "Requirements" (IN scope / OUT of scope). Right: "Nouns → classes, Verbs → methods" with arrows. Bottom: a rough class box diagram (`ParkingLot` → `ParkingFloor` → `ParkingSlot`, plus `Vehicle`, `Ticket`).

**Image content:**

```
IN SCOPE
- Multiple floors
- Vehicle types: BIKE, CAR, TRUCK
- Entry: issue ticket + assign a free slot
- Exit: free the slot (fee comes later)
- Multiple gates at the same time

OUT OF SCOPE
- Online reservations
- Real payment gateway
- Cameras / number plate reading

NOUNS            VERBS
Vehicle          park()
ParkingSlot      unpark()
ParkingFloor     findFreeSlot()
ParkingLot       calculateFee()
Ticket
```

**Post text:**

LLD Series | Part 19 — Parking Lot (1/4)

First full LLD problem. "Design a parking lot."

Remember Part 1 of this series: DON'T start coding. Follow the 6 steps. Today we do the first three.

1️⃣ Clarify requirements
I'd ask the interviewer:
• Multiple floors? → yes
• Which vehicle types? → bike, car, truck
• What happens on entry and exit? → ticket in, slot freed on exit
• Can two gates run at the same time? → yes (this matters later!)
• Reservations, payments, cameras? → out of scope

Saying what's OUT is as important as saying what's IN. It tells the interviewer you can scope a problem.

2️⃣ Find the entities (nouns)
`Vehicle`, `ParkingSlot`, `ParkingFloor`, `ParkingLot`, `Ticket`

3️⃣ Find the behaviors (verbs)
`park()`, `unpark()`, `findFreeSlot()`, `calculateFee()`

Now the important question: WHO owns each verb?
• "Can this slot fit this vehicle?" → the slot (it knows its own type)
• "Find a free slot on this floor" → the floor (it owns the slots)
• "Park / unpark" → the lot (it owns the floors and issues tickets)

Rule: put the method where the data lives.

I deliberately did NOT add: a `Gate` class, a `Payment` class, or any pattern. No pain yet, so no pattern.

Next post: relationships and the code.

What would you add to the requirements? Disabled slots? Valet? 👇

#LLD #SystemDesign #ParkingLot #Java #InterviewPrep

**First comment:**

"Spot the trap: 'two gates at the same time' sounds like a small detail but it's the whole concurrency question. If two cars arrive at once, what stops them getting the same slot? Part 3 answers it."

**Notes for you:**
- Interviewers often score the clarifying questions separately. Ask 4–5 before drawing anything.
- Not every noun becomes a class. `Gate` could be one, but with our scope it adds nothing. Justify your choices out loud.

---

# POST 2 — MIX: Mistakes in my first LLD attempts

**Image:** None.

**Post text:**

Mistakes I made (or almost made) in my first LLD attempts:

❌ I started coding in the first two minutes.
The interviewer hadn't even finished the sentence. I was already typing `class ParkingLot`.

❌ I never said my assumptions out loud.
I assumed "one gate". The interviewer was thinking "five gates". We were solving different problems for 20 minutes.

❌ I made every noun a class and every class a data holder.
Getters and setters everywhere, and all the logic in one giant `Manager` class. Objects with no behavior aren't design, they're structs.

❌ I used inheritance for everything.
`Car extends Vehicle`, `Bike extends Vehicle`, `Truck extends Vehicle`... when a simple enum would have done the job.

❌ I added patterns to look smart.
Factory here, Observer there, and when asked "why?" I had no pain to point to.

❌ I forgot about concurrency.
Two cars, one slot. My code let both in.

❌ I never tested my design with a change.
"What if we add EV charging?" I'd have rewritten half of it.

What fixed it:
• 6-step framework before any code
• saying assumptions out loud
• putting methods where the data lives
• asking "what pain does this pattern remove?"

[One real moment: a specific interview, mock, or practice problem where you made one of these mistakes. One sentence is enough.]

Which of these have you done? Be honest, I'll go first 👇

#InterviewPrep #LLD #SoftwareEngineering #LearningInPublic #CareerGrowth

**First comment:**

"If you only fix one: say your assumptions out loud. It costs 20 seconds, and it turns a silent guess into a shared requirement."

**Notes for you:**
- Keep only the mistakes that are true for you. A list of 4 real ones beats 7 invented ones.
- Add your own real moment at the bracket. That single line carries the post.
- Don't name companies or interviewers.

---

# POST 3 — Parking Lot, Part 2: Classes + code

**Image:** Class diagram on top: `ParkingLot` 1─* `ParkingFloor` 1─* `ParkingSlot` (abstract) ← `StandardSlot`; `Ticket` references `Vehicle` and `ParkingSlot`. Code card below (split into two images if too long).

**Image code:**

```java
enum VehicleType { BIKE, CAR, TRUCK }
record Vehicle(String plate, VehicleType type) {}
record Ticket(String id, Vehicle vehicle, ParkingSlot slot, Instant entry) {}

abstract class ParkingSlot {
    private final String id;
    private boolean occupied;
    ParkingSlot(String id) { this.id = id; }

    protected abstract boolean accepts(Vehicle v);

    synchronized boolean tryOccupy(Vehicle v) {   // atomic check + set
        if (occupied || !accepts(v)) return false;
        occupied = true;
        return true;
    }
    synchronized void free() { occupied = false; }
}

class StandardSlot extends ParkingSlot {
    private final VehicleType type;
    StandardSlot(String id, VehicleType type) { super(id); this.type = type; }
    protected boolean accepts(Vehicle v) { return v.type() == type; }
}
```

```java
class ParkingFloor {
    private final List<ParkingSlot> slots;
    ParkingFloor(List<ParkingSlot> slots) { this.slots = slots; }

    Optional<ParkingSlot> occupySlotFor(Vehicle v) {
        for (ParkingSlot s : slots)
            if (s.tryOccupy(v)) return Optional.of(s);
        return Optional.empty();
    }
}

class ParkingLot {
    private final List<ParkingFloor> floors;
    ParkingLot(List<ParkingFloor> floors) { this.floors = floors; }

    Ticket park(Vehicle v) {
        for (ParkingFloor f : floors) {
            Optional<ParkingSlot> s = f.occupySlotFor(v);
            if (s.isPresent())
                return new Ticket(UUID.randomUUID().toString(), v, s.get(), Instant.now());
        }
        throw new IllegalStateException("No slot available");
    }

    void unpark(Ticket t) { t.slot().free(); }
}
```

**Post text:**

LLD Series | Part 20 — Parking Lot (2/4)

Last post: requirements and entities. Today: relationships and code.

Relationships:
• `ParkingLot` HAS floors
• `ParkingFloor` HAS slots
• `Ticket` REFERENCES a vehicle and a slot
• `StandardSlot` IS-A `ParkingSlot`

Look at the image. Three decisions worth explaining:

1️⃣ Slots decide if a vehicle fits
`ParkingSlot` has one abstract method: `accepts(Vehicle)`. The slot knows its own rule. The floor and the lot never ask "is it a car?" They just ask the slot.

2️⃣ `Vehicle` and `Ticket` are records
They're pure data. Immutable, no setters, no bugs from someone changing a plate number mid-parking.

3️⃣ The concurrency fix (remember "two gates at once?")
The naive way: check `isFree()`, then `occupy()`. Two threads both see "free", both occupy. Same slot, two cars. 💥

The fix: `tryOccupy()` does the check AND the set as one `synchronized` step. Only one thread wins. The loser just tries the next slot.

Check and act must be one atomic step, never two.

What this version doesn't have yet: pricing. Next post adds it with Strategy and Factory.

⚠️ Known limit: scanning every slot is O(n) per car. Fine for an interview. At scale you'd keep a free-slot queue per type. Worth mentioning if asked.

Can you spot another place two threads could collide? 👇

#LLD #Java #ParkingLot #Concurrency #SystemDesign

**First comment:**

"The `synchronized` lock is per slot, not per lot. So two cars parking in DIFFERENT slots never block each other. If I'd put one lock on the whole `ParkingLot`, every gate would queue up behind one car."

**Notes for you:**
- Answer to "another collision": `unpark` called twice with the same ticket. Freeing an already-free slot is harmless here, but if the slot was re-assigned in between, a stale ticket would free someone else's car. A real design marks tickets as used.
- `record` needs Java 16+.
- Try it: run 1000 threads calling `park` on a 10-slot floor and count the tickets. It should never exceed 10.

---

# POST 4 — Parking Lot, Part 3: Strategy + Factory

**Image:** Diagram: `ParkingLot` → `PricingStrategy` (interface) ← `HourlyPricing`, `FlatPricing`. Separate box: `FloorFactory.create(...)` → `ParkingFloor` containing `StandardSlot`s. Code card below.

**Image code:**

```java
interface PricingStrategy {
    double calculate(Ticket t, Instant exit);
}

class HourlyPricing implements PricingStrategy {
    private final Map<VehicleType, Double> ratePerHour;
    HourlyPricing(Map<VehicleType, Double> rates) { this.ratePerHour = rates; }

    public double calculate(Ticket t, Instant exit) {
        long minutes = Duration.between(t.entry(), exit).toMinutes();
        long hours = Math.max(1, (long) Math.ceil(minutes / 60.0));  // min 1 hour
        return hours * ratePerHour.get(t.vehicle().type());
    }
}

class FloorFactory {
    static ParkingFloor create(int floorNo, int bikes, int cars, int trucks) {
        List<ParkingSlot> slots = new ArrayList<>();
        add(slots, floorNo, VehicleType.BIKE, bikes);
        add(slots, floorNo, VehicleType.CAR, cars);
        add(slots, floorNo, VehicleType.TRUCK, trucks);
        return new ParkingFloor(slots);
    }
    private static void add(List<ParkingSlot> list, int floor, VehicleType type, int n) {
        for (int i = 0; i < n; i++)
            list.add(new StandardSlot("F" + floor + "-" + type + "-" + i, type));
    }
}

// In ParkingLot
double unpark(Ticket t) {
    double fee = pricing.calculate(t, Instant.now());
    t.slot().free();
    return fee;
}
```

**Post text:**

LLD Series | Part 21 — Parking Lot (3/4)

Now the interviewer says: "Add pricing. Bikes ₹10/hour, cars ₹30, trucks ₹60. Oh, and weekends might be different later."

"Might be different later" = the pain that earns a pattern.

🔹 Strategy for pricing
`PricingStrategy` is an interface. `HourlyPricing` is one implementation. Tomorrow's `WeekendPricing` or `FlatPricing` is a new class, and `ParkingLot` doesn't change.

You've seen this in Part 9: a changing algorithm behind an interface.

`ParkingLot` receives its strategy through the constructor, so it never knows which one it's using. (Part 6 of this series: Dependency Inversion.)

🔹 Factory for building floors
Real lots have 5 floors with different slot counts. Writing `new StandardSlot(...)` 200 times in `main` is ugly. `FloorFactory.create(1, 20, 50, 5)` builds a whole floor in one line, and slot naming lives in one place.

That's Part 11: move construction into one place.

Two small details people miss:
• Round UP to the next hour. 61 minutes = 2 hours. `ceil`, not integer division.
• Minimum one hour, so a 5-minute stay isn't free.

⚠️ Money tip: in a real system, use `BigDecimal` or integer paise, never `double`. I used `double` to keep the code readable; say that in an interview and you've earned a point.

Notice what I did NOT use: Observer, Singleton, Builder. No pain, no pattern.

Which pricing rule would you add first? Peak hours? Monthly pass? 👇

#LLD #DesignPatterns #StrategyPattern #Java #ParkingLot

**First comment:**

"Why not just `if (type == CAR) rate = 30`? It works today. But the day you add weekend rates, monthly passes, and a festival discount, that method becomes the 14-branch monster from the Open/Closed post. Strategy is paying a small cost now to avoid that."

**Notes for you:**
- `Math.ceil(minutes / 60.0)`: the `60.0` matters. Integer division (`minutes / 60`) would round DOWN before `ceil` sees it.
- Spring tie-in: in Spring Boot, you'd make `HourlyPricing` a `@Component` and inject the interface.

---

# POST 5 — MIX: The cook's story (SRP parable)

**Image:** Optional. A simple illustration of a sweet shop, or a quote card with the moral.

**Post text:**

Another story 👇

एक गाँव में एक हलवाई था। (There was a sweet-maker in a village.)

His laddoos were famous. So his shop got busy. And he did everything himself:
• made the sweets
• served the customers
• counted the money
• cleaned the shop
• ordered the sugar

One festival day, the line was long. While he counted money for one customer, the sugar syrup on the stove burned. The sweets were ruined. While he cleaned the floor, a customer walked off without paying.

Every job interrupted another job. When one thing went wrong, everything went wrong.

An old shopkeeper watched and said:

"Your hands are good, but your shop has one person doing five jobs. When any one of them goes wrong, all of them suffer. Give each job its own person. When the counting is wrong, you fix the counting, not the sweets."

The halwai hired a helper for the counter and one for cleaning. He kept only the sweets.

Now when the syrup needed attention, nothing else stopped. And when the cash register was off, the sweets were still perfect.

🍬 The moral: One role, one reason to change.

That's the Single Responsibility Principle from Part 2.

The halwai = the `AppointmentService` that validated, saved, emailed, and generated invoices.
Each helper = a separate class with one job.

If your class has "and" in its job description, it's the halwai doing five jobs 😄

Which principle should I turn into a story next? LSP, ISP, or DIP? 👇

#SoftwareEngineering #SOLID #Storytelling #CleanCode #Java

**First comment:**

"Parable #1 was the tailor (Open/Closed). Next: Liskov Substitution. Vote with a comment: LSP, ISP, or DIP?"

**Notes for you:**
- Keep it short on LinkedIn; the "see more" cut is after ~3 lines, so your first lines must hook.
- If you posted the tailor story already, this first comment works. If not, remove the "Parable #1" line.
- If you want to mention your channel, do it in the first comment.

---

# POST 6 — Parking Lot, Part 4: The extension test (EV charging)

**Image:** Before/after diff card. Left: "Classes that changed" (only `Vehicle`), right: "Classes added" (`EvChargingSlot`, `EvSurcharge`). Below: unchanged classes greyed out (`ParkingLot`, `ParkingFloor`, `ParkingSlot`, `HourlyPricing`).

**Image code:**

```java
// 1 small change: Vehicle gets a flag
record Vehicle(String plate, VehicleType type, boolean electric) {}

// New class: only electric cars fit here
class EvChargingSlot extends ParkingSlot {
    EvChargingSlot(String id) { super(id); }
    protected boolean accepts(Vehicle v) {
        return v.electric() && v.type() == VehicleType.CAR;
    }
}

// New class: wraps any PricingStrategy (Decorator!)
class EvSurcharge implements PricingStrategy {
    private final PricingStrategy base;
    private final double chargingFee;
    EvSurcharge(PricingStrategy base, double fee) { this.base = base; this.chargingFee = fee; }

    public double calculate(Ticket t, Instant exit) {
        double fee = base.calculate(t, exit);
        return t.vehicle().electric() ? fee + chargingFee : fee;
    }
}

PricingStrategy pricing = new EvSurcharge(new HourlyPricing(rates), 50);
```

**Post text:**

LLD Series | Part 22 — Parking Lot (4/4): the extension test

Step 6 of the framework: throw ONE new requirement at the design.

"Add EV charging slots. Only electric cars can use them. Charging costs extra."

Let's see how much breaks 👇

✅ Unchanged:
• `ParkingLot`: still just loops floors
• `ParkingFloor`: still just asks slots to `tryOccupy`
• `ParkingSlot`: the base class
• `HourlyPricing`: still calculates hourly rates
• the concurrency logic

➕ Added:
• `EvChargingSlot`: a new slot with its own `accepts()` rule
• `EvSurcharge`: a Decorator that wraps ANY pricing strategy and adds the charging fee

✏️ Changed (honestly):
• `Vehicle` got one new field: `electric`

That's the real result. One small edit to a data record, two new classes, and the core flow untouched. If this had needed edits in `ParkingLot` and `ParkingFloor` and the pricing code, the design would have failed the test.

Why it worked:
• Part 2's `accepts()` rule (the slot decides)
• Part 3's `PricingStrategy` interface
• Part 18's Decorator (wrap, don't edit)

Four posts, three earlier ideas, all working together. That's what design is.

Now the open question, and I don't have a perfect answer 🤔
An EV car can park in a normal slot too. If it does, an EV slot stays empty while a petrol car can't use it. How would YOU allocate? Try EV slots first for EVs? Or leave them for later?

Comment your approach 👇

#LLD #ParkingLot #DesignPatterns #Java #SystemDesign

**First comment:**

"My leaning: try EV slots first for electric cars, and fall back to normal slots. That means the floor needs an ordering rule. It could be another Strategy: `SlotAllocationStrategy`. Curious if anyone would do it differently."

**Notes for you:**
- Be honest about the `Vehicle` change in the post. Admitting a small edit is more credible than claiming "zero changes."
- Adding `electric` to the record breaks every existing `new Vehicle(plate, type)` call. In a real codebase, add a secondary constructor or use a builder (Part 15).
- Good discussion: what if a motorbike can be electric too? The `type() == CAR` check then needs thought. Mention it if someone asks.

---

# POST 7 — LLD cheat sheet (recap)

**Image:** One dense but clean card, two sections.
**Top half, SOLID (one line each):**

```
S  One class, one reason to change
O  Add new code, don't edit working code
L  A subclass must keep the parent's promises
I  Small role-based interfaces, no dead methods
D  Depend on interfaces; inject dependencies
```

**Bottom half, patterns ("use it when"):**

```
Strategy   Growing if-else choosing between behaviors
Factory    "new X" chosen from a string in many places
Observer   One action, many independent reactions
Builder    4+ fields, several optional
Singleton  One shared, stateless/thread-safe instance
Decorator  Add behavior by wrapping (log, cache, retry)
```

**Third strip: The 6-step framework in one line each:**

```
1 Requirements (IN and OUT)   4 Relationships
2 Entities (nouns)            5 Patterns only where it hurts
3 Behaviors (verbs)           6 Code + one extension test
```

**Post text:**

LLD Series | Part 23 — The cheat sheet

22 posts. SOLID, 6 patterns, one full problem.

Here's everything on ONE image. Save it. Screenshot it. Send it to the friend with an LLD round next week.

📌 SOLID: the 5 principles that keep code cheap to change
📌 Patterns: when each one is actually worth using
📌 Framework: the 6 steps to follow on ANY problem

If you remember just three things:

1️⃣ Don't start coding. Clarify requirements and say assumptions out loud.
2️⃣ Put methods where the data lives.
3️⃣ Add a pattern only when you can name the pain it removes.

What this series taught me:
• teaching is the fastest way to learn
• patterns are solutions to problems you've already felt
• every design should survive one new requirement

What's next:
→ State and Command patterns
→ Another full problem: [Splitwise / BookMyShow / Elevator, pick one in comments]
→ More stories (LSP, ISP, DIP)

Vote on the next problem 👇
1️⃣ Splitwise
2️⃣ BookMyShow
3️⃣ Elevator system

♻️ Repost if this helps someone you know who's preparing.

#LLD #SOLID #DesignPatterns #Java #InterviewPrep #SystemDesign

**First comment:**

"Want the full posts for any of these? Parts 1 to 22 are all on my profile. Tell me which topic you'd like me to go deeper on."

**Notes for you:**
- Make the image clean: 3 strips, big font, lots of spacing. If it's too dense nobody screenshots it.
- Add a poll instead of the numbered vote if you prefer. LinkedIn polls get good reach, but they limit your post text.
- Check the count before posting: Part 23 assumes you've published every part in order. Adjust the numbers if you skipped or merged any.

---

## Next batch (preview)

1. State pattern (Part 1: traffic light or order status), contrasted with Strategy
2. MIX: story post (LSP: "the sparrow who couldn't swim" or your own)
3. State pattern (Part 2: vending machine, the classic LLD problem)
4. Command pattern (undo/redo or a task queue)
5. MIX: one honest post about your work, e.g. a design decision you'd change (fill with real Sarvodaya or work lesson)
6. Next full problem, based on the vote: Part 1 (requirements + entities)
7. Next full problem, Part 2 (classes + code)
