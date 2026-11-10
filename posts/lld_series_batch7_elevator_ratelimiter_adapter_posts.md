# LLD Series — Batch 7 (7 posts, Elevator + Rate Limiter + Adapter + Mix)

**Pattern of this batch:** a fourth full problem (Elevator, 2 posts), a classic LLD component (Rate Limiter), one more pattern (Adapter), the last SOLID parable, a reflection post, and a cheat sheet v2 with the GitHub repo.
**Series tag:** only on technical posts: `LLD Series | Part X` (continues from Part 33).
**Image rule for technical posts:** one portrait card (1080×1350). Top = class diagram, bottom = code (max ~15 lines).
**Fill-ins:** anything in `[square brackets]` needs YOUR real detail. Replace or delete the line. Don't post a made-up story or invented numbers.

---

## What makes this batch stand out

**Posts 1–2 (Elevator)** — The "which elevator do I send?" question is a real design decision, and the SCAN idea (keep going in one direction, serve stops on the way) is something people remember. Also an honest "I used an enum here, not State classes, and here's why."

**Post 3 (Mix: the mill and the coupling)** — The final SOLID parable (Dependency Inversion). Completes the set of five.

**Post 4 (Rate limiter)** — A very common interview component. Token bucket in ~20 lines, plus what changes when you go distributed.

**Post 5 (Adapter)** — Wrapping a third-party payment SDK. Practical for any backend developer, and it pulls together DIP and Strategy from earlier.

**Post 6 (Mix: what I learned from the series)** — Reflection post. Needs YOUR real numbers and lessons.

**Post 7 (Cheat sheet v2 + GitHub)** — Everything on one image, with the repo link. A high-save, high-follow post.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Fri 4 Dec | Elevator Part 1: requirements + entities | 9am IST |
| Mon 7 Dec | Elevator Part 2: dispatch + movement | 9am IST |
| Tue 8 Dec | MIX: The mill and the coupling (DIP parable) | 9am IST |
| Wed 9 Dec | Rate limiter: token bucket | 9am IST |
| Thu 10 Dec | Adapter pattern | 9am IST |
| Fri 11 Dec | MIX: What I learned from this series | 9am IST |
| Mon 14 Dec | Cheat sheet v2 + GitHub repo | 9am IST |

---

# POST 1 — Elevator, Part 1: Requirements + entities

**Image:** Left = requirements (IN / OUT). Middle = two kinds of requests drawn as two button panels (hallway button vs inside-the-car buttons). Right = elevator states: `IDLE ⇄ MOVING_UP / MOVING_DOWN`.

**Image content:**

```
IN SCOPE
- N elevators, M floors
- Hall call: press UP/DOWN on a floor
- Car call: press a floor number inside the elevator
- System chooses which elevator answers a hall call
- Each elevator serves its stops efficiently

OUT OF SCOPE
- Door sensors, weight limits, emergency mode
- Real hardware control

TWO KINDS OF REQUESTS
Hall call  = (floor, direction)    "I'm on floor 5, going UP"
Car call   = (floor)               "Take me to floor 9"

ENTITIES                  VERBS
ElevatorSystem            requestElevator(floor, dir)
Elevator                  selectFloor(floor)
Direction (UP/DOWN/IDLE)  step()
Request (hall / car)      openDoor()
```

**Post text:**

LLD Series | Part 34 — Elevator System (1/2)

Fourth full problem: "Design an elevator system."

It looks simple. It isn't. Let's use the framework.

1️⃣ Requirements
• Several elevators, many floors
• Someone in the hallway presses UP or DOWN
• Someone inside presses a floor number
• The system decides which elevator comes
Out of scope: sensors, weight limits, emergency mode.

Ask the interviewer: "One elevator or many?" Many changes the problem completely, because now you need a DECISION.

2️⃣ The insight: there are two different requests
🔹 Hall call: "I'm on floor 5, going UP." The system chooses WHICH elevator
🔹 Car call: "Take me to floor 9." Already inside one elevator, no choice

Beginners treat them as the same thing. They're not. One needs a dispatcher, the other goes straight to a specific elevator.

3️⃣ Entities and verbs
`ElevatorSystem` (receives hall calls, picks an elevator), `Elevator` (holds its own stops, moves), `Direction` (UP / DOWN / IDLE).
Verbs: `requestElevator(floor, dir)`, `selectFloor(floor)`, `step()`, `openDoor()`.

4️⃣ The movement idea, in one line
An elevator keeps going in its current direction, serving every stop on the way, and only reverses when nothing is left ahead.

It's the same idea as the "SCAN" disk scheduling algorithm, and it's how real elevators avoid zig-zagging.

Naive elevator: serve requests in the order they arrive. Press 9, then 2, then 8, and it goes 0 → 9 → 2 → 8. 🤦
Smart elevator: 0 → 2 → 8 → 9.

3 states: IDLE, MOVING_UP, MOVING_DOWN.

Next post: the code, and how we choose which elevator answers.

If 3 elevators are on floors 2, 7, and 12, and someone on floor 6 presses UP, which one should come? 👇

#LLD #SystemDesign #ElevatorSystem #Java #InterviewPrep

**First comment:**

"Spoiler for the question: it depends on direction. An elevator on floor 7 going DOWN is closer than one on floor 2, but it's heading the wrong way. 'Closest' isn't enough, direction matters. Part 2 turns this into a cost function."

**Notes for you:**
- Answer to the closing question (don't post): the elevator at floor 2 if it's idle or going UP, since it passes floor 6 on the way. The one at 7 going DOWN would have to finish and come back. A good discussion starter in comments.
- Keep the "3 states" line short. You can mention Part 24 (State) in a reply if someone asks why no State classes: next post explains it.

---

# POST 2 — Elevator, Part 2: Dispatch + movement

**Image:** Top = a small diagram showing 3 elevators with floors and directions, and a hall call at floor 6 with the cost numbers next to each. Below: class diagram: `ElevatorSystem` → `ElevatorSelectionStrategy` ← `NearestElevatorStrategy`; `ElevatorSystem` has many `Elevator`.

**Image code:**

```java
enum Direction { UP, DOWN, IDLE }

class Elevator {
    private int floor = 0;
    private Direction dir = Direction.IDLE;
    private final TreeSet<Integer> upStops = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>(Comparator.reverseOrder());

    synchronized void addStop(int target) {
        if (target == floor) { openDoor(); return; }
        if (target > floor) upStops.add(target); else downStops.add(target);
        if (dir == Direction.IDLE) dir = target > floor ? Direction.UP : Direction.DOWN;
    }

    synchronized void step() {                    // called once per "tick"
        if (dir == Direction.UP) {
            if (upStops.isEmpty()) { dir = downStops.isEmpty() ? Direction.IDLE : Direction.DOWN; return; }
            floor++;
            if (upStops.remove(floor)) openDoor();
        } else if (dir == Direction.DOWN) {
            if (downStops.isEmpty()) { dir = upStops.isEmpty() ? Direction.IDLE : Direction.UP; return; }
            floor--;
            if (downStops.remove(floor)) openDoor();
        }
    }

    synchronized int cost(int target, Direction want) {
        int distance = Math.abs(floor - target);
        if (dir == Direction.IDLE) return distance;
        boolean onTheWay = (dir == Direction.UP && want == Direction.UP && target >= floor)
                        || (dir == Direction.DOWN && want == Direction.DOWN && target <= floor);
        return onTheWay ? distance : distance + 100;   // heavy penalty: must finish and turn around
    }
}
```

```java
interface ElevatorSelectionStrategy {
    Elevator select(List<Elevator> elevators, int floor, Direction want);
}

class NearestElevatorStrategy implements ElevatorSelectionStrategy {
    public Elevator select(List<Elevator> elevators, int floor, Direction want) {
        return elevators.stream()
            .min(Comparator.comparingInt(e -> e.cost(floor, want)))
            .orElseThrow();
    }
}
```

**Post text:**

LLD Series | Part 35 — Elevator System (2/2)

Time to code. Two decisions to look at 👇

1️⃣ Each elevator keeps TWO sorted sets of stops
• `upStops`: floors above, ascending (a `TreeSet`)
• `downStops`: floors below, descending (a `TreeSet` with reversed order)

`step()` moves one floor per tick. Going UP, it serves the next stop in `upStops`. When `upStops` is empty, it checks `downStops` and turns around, or goes IDLE.

Press 9, then 2, then 8 while at floor 0? Stops: `upStops = {2, 8, 9}`. The elevator goes 0 → 2 → 8 → 9. No zig-zagging.

2️⃣ Choosing the elevator is a Strategy (Part 9)
`ElevatorSelectionStrategy` is an interface. `NearestElevatorStrategy` picks the lowest `cost`:
• idle elevator: cost = distance
• moving toward you, in the same direction: cost = distance
• moving away, or the wrong direction: cost = distance + a big penalty

Back to last post's question: elevators on floors 2, 7, 12. Press UP on floor 6.
• Elevator at 2 (idle): cost 4 ✅
• Elevator at 7 going DOWN: cost 1 + penalty = 101
• Elevator at 12 going DOWN: cost 6 + penalty = 106

The idle one wins even though it's not the closest. Direction matters.

Want an "energy-saving" strategy for night time, or a "VIP elevator" strategy? One new class, nothing else changes.

Why an enum and not State classes?
In Part 24 we used a class per state because each state had very different rules. Here we have 3 states and a few `if`s in one method. A `Direction` enum is simpler. Patterns need pain, remember?

⚠️ Honest limits of this version:
• the "+100" penalty is a heuristic, not optimal. Real systems consider load, and group elevators together
• one thread should call `step()` per elevator (a scheduler tick), while requests arrive on other threads, hence `synchronized`
• door timing, capacity, and floor limits are skipped

If you wanted to add "express elevator, only stops at floors 1, 10, 20", what would you change? 👇

#LLD #Java #ElevatorSystem #DesignPatterns #SystemDesign

**First comment:**

"A good follow-up design question: what happens to the stops when an elevator goes out of service? Its stops need to be handed back to the system and re-dispatched. That's a nice place to use the same strategy again."

**Notes for you:**
- Answer to the express question: it's a new rule for which floors an elevator accepts, like `ParkingSlot.accepts()` in the Parking Lot. Put an `accepts(floor)` check in `Elevator`, and let the strategy skip elevators that don't accept that floor. No change to `ElevatorSystem`.
- ⚠️ `Elevator.step()` calls `openDoor()`, which isn't shown. Mention it's trimmed.
- Try it: simulate 3 elevators for 30 ticks and print the floors. Seeing it run is the fastest way to understand it.
- The cost numbers in the example above: 2→6 distance is 4. 7→6 is 1. 12→6 is 6.

---

# POST 3 — MIX: The mill and the coupling (DIP parable)

**Image:** Optional. A simple illustration of a village flour mill, or a quote card with the moral.

**Post text:**

The last story in my SOLID set 👇

एक गाँव में एक चक्की थी। (There was a flour mill in a village.)

The mill was built right next to the stream. A water wheel turned the stone, and the wheel was fixed straight onto the grinding stone, permanently, with no way to separate them.

It worked beautifully, all through the rains.

Then summer came. The stream dried up. The wheel stopped. The stone stopped. The whole village had no flour.

They wanted to use bullocks instead. But the stone was welded to the wheel. To change the power, they had to break the mill and rebuild it.

An old miller visited. He said:

"Don't fix the stone to the wheel. Fix a standard coupling between them. The stone only needs to be turned. It doesn't care who turns it. Today it's water. Tomorrow a bullock. Next year, a motor."

They rebuilt the mill once, with a standard coupling.

After that, when the stream dried up, they simply attached the bullocks. The grinding stone never changed.

⚙️ The moral: Depend on the coupling, not on the source.

That's the Dependency Inversion Principle from Part 6:
👉 High-level code should depend on abstractions, not on low-level details.

The mill = your business logic
The water wheel = `MySqlDatabase`, `SmtpMailer`, a specific payment provider
The coupling = an interface (`MessageSender`, `OrderRepository`)

When the business logic only knows the interface, switching the database, the email provider, or the payment gateway doesn't mean rebuilding the mill.

That's 5 stories for 5 principles:
🧵 Tailor: Open/Closed
🍬 Halwai: Single Responsibility
📚 Teacher: Liskov Substitution
📜 Duty cards: Interface Segregation
⚙️ Mill: Dependency Inversion

Which story helped you the most? 👇

#SoftwareEngineering #SOLID #Storytelling #CleanCode #SpringBoot

**First comment:**

"If the stories helped, tell me which concept to turn into a story next. I'm thinking: Observer (a village bell), or Strategy (choosing a road)."

**Notes for you:**
- Only list the 5 stories as a recap if you posted all five. Otherwise trim the list.
- If you want to mention your Hindi stories channel, do it in the first comment, with a link.

---

# POST 4 — Rate Limiter

**Image:** Left = a bucket diagram. Drip into the bucket labeled "refill: 5 tokens/sec", bucket capacity "10", requests taking tokens out. Right = code.

**Image code:**

```java
interface RateLimiter {
    boolean allow(String key);          // key = userId or IP
}

class TokenBucketLimiter implements RateLimiter {
    private final long capacity;
    private final double refillPerSec;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    TokenBucketLimiter(long capacity, double refillPerSec) {
        this.capacity = capacity; this.refillPerSec = refillPerSec;
    }

    public boolean allow(String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(capacity))
                      .tryConsume(capacity, refillPerSec, System.nanoTime());
    }

    private static class Bucket {
        private double tokens;
        private long last;
        Bucket(long capacity) { tokens = capacity; last = System.nanoTime(); }

        synchronized boolean tryConsume(long cap, double rate, long now) {
            tokens = Math.min(cap, tokens + (now - last) / 1e9 * rate);   // refill
            last = now;
            if (tokens < 1) return false;
            tokens -= 1;
            return true;
        }
    }
}
```

**Post text:**

LLD Series | Part 36 — Rate Limiter

"Design a rate limiter" is one of the most common LLD and system design questions.

Goal: "Each user can make at most N requests per time window."

The most popular answer: the Token Bucket 🪣

How it works:
• Each user has a bucket with a maximum of, say, 10 tokens
• Every request takes 1 token
• Tokens refill at a steady rate, say 5 per second
• Empty bucket? Request rejected

Why it's loved: it allows short bursts (use all 10 tokens at once) but limits the long-term average (5 per second).

The nice trick in the code: you don't need a timer thread adding tokens every second. When a request arrives, calculate how many tokens SHOULD have been added since the last request:

`tokens = min(capacity, tokens + elapsedSeconds × rate)`

Lazy refill. No background thread. 🎯

The design:
• `RateLimiter` is an interface with `allow(key)`. Callers don't care which algorithm is behind it (Strategy, Part 9)
• `TokenBucketLimiter` keeps one `Bucket` per key
• Each bucket is `synchronized`, so two requests from the same user can't both take the last token (the same check-and-act rule as Parking Lot and BookMyShow)

Other strategies you could plug in:
• Fixed window: count per minute. Simple, but allows a burst at the window edge
• Sliding window: smoother, more memory
• Leaky bucket: smooths output to a constant rate

Where it plugs in: as a filter or interceptor in front of your API (Chain of Responsibility, Part 30). Rejected request → HTTP 429 with a `Retry-After` header.

⚠️ Two production gotchas:
• This map grows forever. Evict idle buckets, or you'll leak memory
• One server's memory isn't shared. With 5 servers, a user gets 5× the limit. Move the state to Redis and do the check-and-update atomically (a Lua script is the usual approach)

If a user sends 10 requests in the first millisecond, then waits 2 seconds, how many can they make next? (capacity 10, refill 5/sec) 👇

#LLD #RateLimiter #SystemDesign #Java #BackendEngineering

**First comment:**

"Answer: 10 tokens refilled in 2 seconds at 5 per second, but the cap is 10, so they have a full bucket again: 10 requests. The cap is what stops tokens from piling up forever."

**Notes for you:**
- `System.nanoTime()` is monotonic (good for measuring elapsed time) but isn't wall-clock time. Right choice for rate limiting.
- Try it: write a small test with a fake clock passed into `tryConsume`; the `now` parameter makes this easy.
- If someone asks why a fixed window is bad: a user can send N requests at 11:59:59 and N more at 12:00:00, so 2N in 2 seconds.

---

# POST 5 — Adapter Pattern

**Image:** Left = a diagram: `CheckoutService` → `PaymentGateway` (our interface) ← `AcmePayAdapter` → `AcmePayClient` (third-party, greyed out, "can't change"). Add a small travel-plug sketch if you like. Right = code.

**Image code:**

```java
// OUR interface: what our code wants
interface PaymentGateway {
    PaymentResult charge(String userId, long paise);
}

// THEIR SDK: we can't change it
class AcmePayClient {
    AcmeResponse makePayment(String customerRef, BigDecimal rupees, String currency) { ... }
}

// The adapter translates between the two
class AcmePayAdapter implements PaymentGateway {
    private final AcmePayClient client;
    AcmePayAdapter(AcmePayClient client) { this.client = client; }

    public PaymentResult charge(String userId, long paise) {
        BigDecimal rupees = BigDecimal.valueOf(paise, 2);          // paise -> rupees
        AcmeResponse r = client.makePayment(userId, rupees, "INR");
        return r.isOk() ? PaymentResult.success(r.txnId())
                        : PaymentResult.failed(r.errorCode());
    }
}
```

**Post text:**

LLD Series | Part 37 — Adapter Pattern

You're integrating a payment provider. Their SDK wants:
• amount as a `BigDecimal` in rupees
• a customer "ref" string
• a currency code
• and it returns a provider-specific response object

Your code works in paise (`long`) and expects your own `PaymentResult`.

Option A: use their SDK everywhere. Now `AcmeResponse` and rupee decimals are scattered across 40 files. Switching providers means touching all 40.

Option B: the Adapter pattern.
👉 Wrap a class with an incompatible interface so it fits the interface your code expects.

You define the interface YOU want: `PaymentGateway.charge(userId, paise)`. Then `AcmePayAdapter` translates: paise → rupees, calls their SDK, converts their response into your `PaymentResult`.

The rest of your code only sees `PaymentGateway`. It never imports the vendor's classes.

What you get:
• Switching vendors = one new adapter, nothing else changes
• Tests use a fake `PaymentGateway`, no real HTTP
• The vendor's quirks (rupees vs paise, weird error codes) stay in one file

This is Dependency Inversion (Part 6) in practice, and the adapter is one small class.

Adapter vs the patterns it gets confused with:
• Adapter: CHANGES the interface so it fits
• Decorator: keeps the same interface, ADDS behavior
• Proxy: keeps the same interface, CONTROLS access
• Facade: SIMPLIFIES a big subsystem into a small interface

Real examples:
• JDBC drivers adapt each database to one `Connection` interface
• `InputStreamReader` adapts bytes to characters
• Spring's `HandlerAdapter` adapts different controller types

⚠️ Money tip, again: store paise as `long`. `BigDecimal.valueOf(paise, 2)` makes ₹12.34 from 1234 without any floating-point surprises.

When did a vendor SDK last leak into your whole codebase? 👇

#LLD #DesignPatterns #AdapterPattern #Java #BackendEngineering

**First comment:**

"Bonus: this is also called an anti-corruption layer. The adapter stops a vendor's model from leaking into your domain. If their API changes, only the adapter breaks."

**Notes for you:**
- `AcmePayClient` is made up for the example. Say "a payment provider" in the post and don't use a real vendor's name or their real method names.
- `BigDecimal.valueOf(long, int)` is `unscaledValue` and `scale`, so `valueOf(1234, 2)` = 12.34.
- If you want one extra line: mention that in Spring, the adapter is a `@Component` that implements the interface, injected wherever `PaymentGateway` is needed.

---

# POST 6 — MIX: What I learned from this series

**Image:** None. (Or a screenshot of your post analytics, if you're comfortable showing it.)

**Post text:**

I've been posting this LLD series for a while now, and it has covered a lot of ground: principles, patterns, and several full design problems.

What I've learned so far:

1️⃣ Teaching is the fastest way to find your gaps.
You think you understand a concept until you try to explain it with a short example and working code. Writing the post is the real exam.

2️⃣ The same few ideas keep coming back.
Single responsibility, small interfaces, depending on abstractions, and "check and act must be one step". Almost every problem is a remix of these.

3️⃣ Patterns are tools, not trophies.
Most good designs in this series started with NO pattern. The pattern showed up when something started to hurt.

4️⃣ Concurrency is where designs break.
Parking Lot, BookMyShow, Rate Limiter: the same bug in three costumes. Two threads, one resource.

5️⃣ Writing for beginners is harder than writing for experts.
To explain something simply, you have to understand it properly.

6️⃣ A steady rhythm beats occasional bursts.
Small, regular posts forced me to learn continuously instead of in panicked weekends.

7️⃣ Questions from readers push you into edge cases.
"What about failures?" "What if there are many servers?" Those questions are where the real learning is.

One thing I'd say honestly: some posts get far more attention than others, and it's rarely the ones you'd predict. In general, posts with a concrete before/after seem to land better than pure definitions.

What's next:
→ more full problems
→ more patterns
→ more stories

If you've been following along, thank you. If you haven't, the whole series is on my profile, starting from Part 1.

What topic should I do next? 👇

#LearningInPublic #LLD #SoftwareEngineering #Java #CareerGrowth

**First comment:**

"The best part of doing this publicly is the discussion. Corrections, edge cases, and better ideas from readers make every post stronger, so please keep them coming."

**Notes for you:**
- This version has no personal numbers on purpose, so it's safe to post as-is.
- Items 2, 3 and 4 come directly from the series content. Items 1, 5, 6 and 7 are common experiences; if one doesn't match yours, swap it for your own.
- If you ever want to add a real moment (one comment that taught you something, one post that surprised you), put it right before "What's next". One real detail makes this kind of post much stronger.

---

# POST 7 — Cheat sheet v2 + GitHub repo

**Image:** One clean card with three strips. Keep large font and spacing.

**Strip 1 — SOLID (one line each):**

```
S  One class, one reason to change
O  Add new code, don't edit working code
L  A subclass keeps the parent's promises
I  Small role-based interfaces
D  Depend on interfaces; inject dependencies
```

**Strip 2 — Patterns ("use it when"):**

```
Strategy    Swappable algorithm, growing if-else
Factory     Creation logic in one place
Observer    One event, many reactions
Builder     Many fields, several optional
Singleton   One shared, thread-safe instance
Decorator   Add behavior by wrapping
State       Behavior changes with lifecycle
Command     Request as an object (undo, queue)
Chain       Pass through independent handlers
Proxy       Control access to an object
Adapter     Make an incompatible API fit
```

**Strip 3 — Problems solved:**

```
Parking Lot   Splitwise   BookMyShow
Elevator      Rate Limiter

Always: requirements → entities → behaviors
→ relationships → patterns (only if pain) → extension test
```

**Post text:**

LLD Series | The cheat sheet v2 + the code

We've covered a lot by now. Here's everything so far on ONE card 👇

📌 The SOLID principles
📌 The core design patterns, each with "use it when"
📌 Several full problems: Parking Lot, Splitwise, BookMyShow, Elevator, Rate Limiter
📌 The 6-step framework that works on all of them

Save it. Send it to someone with an LLD round coming up.

And one more thing: ALL the code is now in a GitHub repo. 🚀
One folder per topic, runnable examples, in Java, C#, Python and TypeScript.
Link in the first comment.

If you remember three things from this series:

1️⃣ Don't start coding. Clarify, then list entities and behaviors.
2️⃣ Put the method where the data lives.
3️⃣ Add a pattern only when you can name the pain.

And one test you can use forever:
"Throw ONE new requirement at your design. How many classes break?"

Coming up: more patterns (Template Method, Facade and others), more full problems (ATM, Library Management, Chess and others), and more interview-style Q&A.

Vote for the next problem 👇
1️⃣ ATM
2️⃣ Library Management
3️⃣ Chess

♻️ Repost if this helps someone preparing.

#LLD #SOLID #DesignPatterns #Java #SystemDesign #InterviewPrep

**First comment:**

"All the code is here: [your repo link]. Java, C#, Python and TypeScript versions of every example, with a README. Found a bug or a better approach? Open an issue or tell me here. Corrections are very welcome."

**Notes for you:**
- The only blank left is `[your repo link]`. Publish the repo first (it's in the zip file), then paste the real URL.
- No post counts or part numbers appear in this version, so it stays correct no matter how many posts you've actually published.
- The code in the posts is trimmed for images; the repo versions are complete and runnable, with checks that confirm the behavior.

---

## Next batch (preview)

1. Template Method (Part 1 + Part 2: with Spring's `JdbcTemplate` as the example)
2. MIX: LLD interview Q&A (5 questions you'll be asked, with 2-line answers)
3. Facade pattern
4. MIX: a post about your Hindi stories channel and what explaining stories taught you about explaining code (needs your real details)
5. ATM / Library Management / Chess, Part 1 (based on the vote)
6. Same problem, Part 2
7. "Common LLD mistakes and how to avoid them" (a high-save checklist post)
