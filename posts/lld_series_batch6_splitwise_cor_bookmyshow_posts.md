# LLD Series — Batch 6 (7 posts, Splitwise 3 + Patterns + BookMyShow + Mix)

**Pattern of this batch:** finish Splitwise (simplify debts), two patterns (Chain of Responsibility, Proxy vs Decorator), start a third full problem (BookMyShow, with real concurrency), and 2 mix posts.
**Series tag:** only on technical posts: `LLD Series | Part X` (continues from Part 28).
**Image rule for technical posts:** one portrait card (1080×1350). Top = class diagram, bottom = code (max ~15 lines).
**Fill-ins:** anything in `[square brackets]` needs YOUR real detail. Replace or delete the line. Don't post a made-up story.

**⚠️ Choices I made without your input:**
- Posts 6–7 are **BookMyShow**. If your poll picked Elevator, tell me and I'll swap them.
- The parable in Post 2 is **Interface Segregation**. If your vote picked DIP, tell me and I'll rewrite it.

---

## What makes this batch stand out

**Post 1 (Splitwise 3)** — The "simplify debts" question is the most-searched part of the Splitwise problem. A small greedy algorithm, a clear example, and an honest note that the perfect answer is a hard problem.

**Post 2 (Mix: the fair duty cards)** — Fourth parable. Teaches Interface Segregation.

**Post 3 (Chain of Responsibility)** — Ties to things people already use: servlet filters, Spring Security's filter chain, and ASP.NET Core middleware.

**Post 4 (Mix: what I'd tell myself a year ago)** — A reflection post. Needs your real lessons, with a few safe ones pre-filled.

**Post 5 (Proxy vs Decorator)** — A short, sharp comparison. This is a top interview confusion, so it should get saves.

**Posts 6–7 (BookMyShow)** — The best concurrency teaching problem in LLD. Includes the insight most candidates miss (seat vs show-seat) and connects to Redis and PostgreSQL for real systems.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Wed 25 Nov | Splitwise Part 3: simplify debts | 9am IST |
| Thu 26 Nov | MIX: The fair duty cards (ISP parable) | 9am IST |
| Fri 27 Nov | Chain of Responsibility | 9am IST |
| Mon 30 Nov | MIX: What I'd tell myself a year ago | 9am IST |
| Tue 1 Dec | Proxy vs Decorator | 9am IST |
| Wed 2 Dec | BookMyShow Part 1: requirements + entities | 9am IST |
| Thu 3 Dec | BookMyShow Part 2: seat locking | 9am IST |

---

# POST 1 — Splitwise, Part 3: Simplify debts

**Image:** Left = a before/after graph. Before: 3 arrows between A, B, C, D. After: 2 arrows. Right = the algorithm in 4 steps plus code.

**Image content (graph):**

```
BEFORE (3 payments)        AFTER (2 payments)
A owes B  ₹500             A pays C  ₹500
D owes B  ₹200             D pays C  ₹200
B owes C  ₹700

Net balances:
A = -500   (owes 500)
B =    0   (+500 +200 -700)
C = +700   (is owed 700)
D = -200   (owes 200)
```

B was only a middleman: money flowed through B and B ends at zero. Remove B and everyone is still settled.

**Image code:**

```java
record Bal(String id, long paise) {}
record Payment(String from, String to, long paise) {}

List<Payment> simplify(Map<String, Long> net) {   // +ve = is owed, -ve = owes
    PriorityQueue<Bal> creditors =
        new PriorityQueue<>(Comparator.comparingLong(Bal::paise).reversed());
    PriorityQueue<Bal> debtors =
        new PriorityQueue<>(Comparator.comparingLong(Bal::paise));  // most negative first
    net.forEach((id, amt) -> {
        if (amt > 0) creditors.add(new Bal(id, amt));
        else if (amt < 0) debtors.add(new Bal(id, amt));
    });

    List<Payment> out = new ArrayList<>();
    while (!creditors.isEmpty() && !debtors.isEmpty()) {
        Bal c = creditors.poll(), d = debtors.poll();
        long pay = Math.min(c.paise(), -d.paise());
        out.add(new Payment(d.id(), c.id(), pay));
        if (c.paise() > pay)  creditors.add(new Bal(c.id(), c.paise() - pay));
        if (-d.paise() > pay) debtors.add(new Bal(d.id(), d.paise() + pay));
    }
    return out;
}
```

**Post text:**

LLD Series | Part 29 — Splitwise (3/3): simplify debts

Last time I asked: "What's the smallest number of payments 5 friends need to settle up?"

Here's the idea that makes it easy 👇

Don't think about who owes whom. Think about each person's NET position.

For every person: (what others owe them) minus (what they owe others).

Example:
• A owes B ₹500
• B owes C ₹500

Who actually owes who? B is in the middle. B gets ₹500 and pays ₹500. Net = 0. B doesn't need to be involved at all.

Net balances: A = −500, B = 0, C = +500.
So: A pays C ₹500. One payment instead of two. ✅

The algorithm:
1️⃣ Compute net balance per person
2️⃣ Put people who are owed money in one max-heap, people who owe in another
3️⃣ Take the biggest creditor and the biggest debtor. The smaller of the two amounts is settled in one payment
4️⃣ Put back whoever still has a remainder. Repeat until both heaps are empty

Properties worth knowing:
• Sum of all nets is always 0 (everything owed = everything owing)
• At most (n − 1) payments for n people. So 5 friends never need more than 4 payments
• Each payment fully settles at least one person

⚠️ Honest note: this greedy approach is simple and good enough for any real group. But finding the absolute minimum number of payments in every case is a known hard problem (it's NP-hard). In an interview, say that and explain the greedy solution. That's a strong answer.

My answer to the question from last time: 4 payments is the maximum for 5 friends, and often it's fewer.

Did anyone get it right? 👇

#LLD #Splitwise #Algorithms #Java #InterviewPrep

**First comment:**

"Why a heap? Because each step needs 'the biggest remaining creditor/debtor.' Heap gives that in O(log n). For 5 friends, a sorted list is fine. For a 10,000-person group, the heap matters."

**Notes for you:**
- The image example is worked out: A=-500, B=0, C=+700, D=-200, so two payments (A→C 500, D→C 200) settle everyone. If you change the numbers, recompute the nets first.
- To build `net` from the Part 28 `owes` map: for each (debtor → creditor, amount), do `net[debtor] -= amount` and `net[creditor] += amount`.
- NP-hard claim: minimum-transaction settlement relates to partitioning the group into the maximum number of zero-sum subsets. You don't have to explain it, just mention it's hard in general.

---

# POST 2 — MIX: The fair duty cards (ISP parable)

**Image:** Optional. A simple illustration of a village fair, or a quote card with the moral.

**Post text:**

Another story 👇

गाँव में मेला लगना था। (A fair was coming to the village.)

The sarpanch wanted everything organized. So he made one rule:

"Every helper must sign ONE paper. On it: I will cook, I will serve, I will drive the bullock cart, I will keep accounts, I will sing on stage."

Everyone signed. They wanted the work.

On fair day:
• The cook was asked to drive the cart. He'd never touched a bullock. The cart went into a ditch.
• The cart driver was asked to keep accounts. He couldn't count beyond ten.
• The singer was asked to cook. The rice burned.

Each person had signed a promise they could never keep.

An old woman from the village said:

"Why one paper for everyone? Give each person a card with only THEIR work. The cook's card says cook. The driver's card says drive. Nobody signs for what they can't do."

The sarpanch made separate duty cards. Next year, the fair ran smoothly.

📜 The moral: Don't force anyone to promise what they'll never use.

That's the Interface Segregation Principle from Part 5:
👉 No class should be forced to implement methods it doesn't use.

The one big paper = the fat `StaffMember` interface with `treatPatient()`, `prescribe()`, `handleBilling()`.
The `Receptionist` forced to sign it = `prescribe()` throwing `UnsupportedOperationException`.
The duty cards = small interfaces: `Treats`, `Prescribes`, `HandlesBilling`.

Next time you add a method to an interface, ask: "Does EVERY class that implements this really need it?"

Next parable: Dependency Inversion. Should it be a story about a lamp, a rope, or a well? 😄 Tell me 👇

#SoftwareEngineering #SOLID #Storytelling #CleanCode #Java

**First comment:**

"So far: the tailor (Open/Closed), the halwai (Single Responsibility), the teacher (Liskov), the duty cards (Interface Segregation). Only Dependency Inversion left."

**Notes for you:**
- Only claim "the first comment" numbering if you posted the earlier parables.
- If your poll chose DIP instead, tell me and I'll swap this post.

---

# POST 3 — Chain of Responsibility

**Image:** Left = a horizontal chain: `Request → [WorkingHours] → [SlotFree] → [NoDuplicate] → ✅ Accepted`, with a red ✖ arrow dropping from any box to "Rejected." Right = code.

**Image code:**

```java
abstract class Validator {
    private Validator next;

    Validator then(Validator next) { this.next = next; return next; }

    final void validate(BookingRequest r) {
        check(r);                          // throws if invalid
        if (next != null) next.validate(r);
    }
    protected abstract void check(BookingRequest r);
}

class WorkingHoursCheck extends Validator {
    protected void check(BookingRequest r) {
        if (!r.doctor().worksAt(r.time()))
            throw new ValidationException("Doctor not available at this time");
    }
}
// SlotFreeCheck, DuplicateBookingCheck: same shape

Validator chain = new WorkingHoursCheck();
chain.then(new SlotFreeCheck()).then(new DuplicateBookingCheck());
chain.validate(request);
```

**Post text:**

LLD Series | Part 30 — Chain of Responsibility

Before an appointment is booked, you check:
• Is the doctor working at that time?
• Is the slot free?
• Does the patient already have a booking then?
• Is the patient allowed to book this doctor?

The naive version: one method with 4 `if` blocks. Then 6. Then 9. Different teams add their own checks. Nobody dares reorder them.

Chain of Responsibility:
👉 Link small handlers in a chain. Each one does its job, then passes the request to the next.

Look at the image. Each check is its own class with one job (Single Responsibility, Part 2). They're linked with `then(...)`.

```
chain.then(new SlotFreeCheck()).then(new DuplicateBookingCheck());
```

Why it's useful:
• add a new check = one new class, plug it in
• reorder checks = change the wiring, not the code
• remove a check for a certain flow (e.g., admin bookings) by building a shorter chain
• each check can be unit-tested alone

There are two flavors:
1️⃣ Pipeline: EVERY handler runs, and any can reject (like the example above)
2️⃣ Classic: the FIRST handler that can handle the request does it, and the rest are skipped (like approval flows: manager → director → VP)

You've already used this:
• Servlet filters and Spring Security's filter chain
• ASP.NET Core middleware (logging → auth → routing → your code)
• exception handlers that pass up the stack

⚠️ Watch out:
• order matters, so document it
• it's hard to debug "who handled this?" without logging
• a chain with no end handler can silently drop requests

Rule of thumb: if you have 4+ independent checks on the same request, a chain is easier to live with than a long method.

What's the longest validation method you've ever seen? 👇

#LLD #DesignPatterns #ChainOfResponsibility #Java #BackendEngineering

**First comment:**

"If you've written ASP.NET Core middleware or a servlet filter, you've written a Chain of Responsibility. The pattern is the same, the framework just hides the `next` call for you."

**Notes for you:**
- The `final void validate` calls `check` first, then `next`. Order is "check, then pass on."
- In Spring Boot, you can inject a `List<Validator>` sorted with `@Order`. That's a chain without the `then(...)` wiring.
- Don't confuse with Decorator: Decorator always calls the wrapped object and adds behavior. A chain handler may stop the request.

---

# POST 4 — MIX: What I'd tell myself a year ago

**Image:** None.

**Post text:**

What I'd tell myself [a year ago / when I started]:

1️⃣ Write the description before you write the code.
Say what you're changing and why in 3 sentences. If you can't, you're not ready to code.

2️⃣ Read more code than you write.
Open a well-known library. See how it's shaped. You'll learn patterns faster than from any tutorial.

3️⃣ Ask the "obvious" question early.
A question costs 2 minutes today. A wrong assumption costs 2 days later.

4️⃣ [Your real lesson, e.g., something about debugging, deployments, or working with teammates.]

5️⃣ Don't add a pattern until you can name the pain.
Cleverness isn't design.

6️⃣ [Your real lesson, e.g., something about time, burnout, or learning.]

7️⃣ Teach what you just learned.
Writing it down shows you what you don't actually understand yet.

The biggest change in the last [year/months]: [one honest sentence, e.g., "I now see production as a feature, not an afterthought."]

If you're early in your career: nobody has it figured out. The people who look like they do just got confused earlier than you.

What would you tell your younger self? One line is enough 👇

#CareerAdvice #SoftwareEngineering #LearningInPublic #BackendEngineering #Growth

**First comment:**

"If I had to pick one from the list: asking early. Almost every painful moment I've had in a project started with a question I was too shy to ask in the first meeting."

**Notes for you:**
- ⚠️ **Items 4, 6, and the "biggest change" line are blanks on purpose.** I can't write your real lessons. Items 1, 2, 3, 5, 7 are common lessons; keep only the ones that are genuinely true for you, and delete the rest.
- Item 1 matches something you've already said publicly in an earlier post, so it's safe to keep. Check the first comment: "Almost every painful moment..." must also be true for you, or change it.
- Don't invent a timeframe. Use your actual time in the industry.

---

# POST 5 — Proxy vs Decorator

**Image:** Side-by-side. Left panel "Decorator": a box wrapping a box with "+ adds behavior" (logging, caching). Right panel "Proxy": a gate in front of a box with "controls access" (permissions, lazy load). Same shape drawn identically, with different labels. Below: a tiny code snippet.

**Image code:**

```java
interface ReportService { Report generate(long id); }

// Proxy: decides WHETHER the real object is called
class SecureReportProxy implements ReportService {
    private final ReportService real;
    private final User user;
    SecureReportProxy(ReportService real, User user) { this.real = real; this.user = user; }

    public Report generate(long id) {
        if (!user.hasRole("ADMIN")) throw new AccessDeniedException();
        return real.generate(id);          // only reached if allowed
    }
}

// Decorator: ALWAYS calls the real object, adds something around it
class TimingReportService implements ReportService {
    private final ReportService inner;
    TimingReportService(ReportService inner) { this.inner = inner; }

    public Report generate(long id) {
        long t = System.nanoTime();
        try { return inner.generate(id); }
        finally { log.info("took {} µs", (System.nanoTime() - t) / 1000); }
    }
}
```

**Post text:**

LLD Series | Part 31 — Proxy vs Decorator

"What's the difference between Proxy and Decorator?" is a classic interview question, because the code looks almost identical.

Both:
• implement the same interface as the real object
• hold a reference to the real object
• forward calls to it

So how are they different? The INTENT.

🎁 Decorator: ADDS behavior
"Do what the real object does, plus something extra."
Logging, caching, retry, compression. The real call always happens.

🚪 Proxy: CONTROLS access
"Decide whether, when, or how the real object is used."
Permission checks, lazy-loading, remote calls, rate limits. The real call may never happen.

One-line test:
→ Does it make the real call ALWAYS, and add something around it? → Decorator
→ Does it decide IF the real call is made, or WHEN it's created? → Proxy

Real examples you use every day:
• Hibernate lazy loading: returns a proxy now, and hits the DB only when you touch a field (virtual proxy)
• Spring's `@Transactional` and `@Cacheable`: they wrap your bean in a proxy
• Java's `BufferedReader` over a reader: Decorator
• A permission wrapper over a service: protection proxy

A second difference:
• With a Decorator, YOU usually build the stack: `new Logging(new Caching(real))`
• With a Proxy, the caller often doesn't know it's there. The framework hands it to you

⚠️ The gray area: a caching wrapper could be either! If it adds caching as a feature, it's a Decorator. If it controls access to an expensive object, it's a proxy. Don't lose sleep over the label; explain the intent clearly.

Which one would you say a rate limiter in front of an API client is? 👇

#LLD #DesignPatterns #Proxy #Decorator #InterviewPrep

**First comment:**

"Interview shortcut: if they ask for the difference, say 'same structure, different intent: Decorator adds behavior, Proxy controls access,' then give one example of each. That's the whole answer."

**Notes for you:**
- Answer to the closing question: a rate limiter decides IF the call is made, so it's a Proxy (protection/control).
- This post links back to Part 18 (Decorator) and the self-invocation point from that post. Mention "Part 18" in a reply if someone asks about wrappers.

---

# POST 6 — BookMyShow, Part 1: Requirements + entities

**Image:** Left = requirements (IN / OUT). Middle = entities with a highlighted "Seat vs ShowSeat" callout. Right = the seat lifecycle: `AVAILABLE → LOCKED → BOOKED` with a timer on LOCKED.

**Image content:**

```
IN SCOPE
- Browse movies and shows by city
- Select seats for a show
- Hold seats while paying (e.g., 5 minutes)
- Confirm booking after payment
- Cancel booking

OUT OF SCOPE
- Real payment gateway
- Food ordering, offers
- Recommendations

ENTITIES
Movie, Theatre, Screen, Show
Seat      (physical seat in a screen)
ShowSeat  (that seat, for ONE show: has the status)
Booking, User

SEAT LIFECYCLE
AVAILABLE → LOCKED (timer) → BOOKED
              ↓ timeout / cancel
           AVAILABLE
```

**Post text:**

LLD Series | Part 32 — BookMyShow (1/2)

Third full problem: "Design a movie ticket booking system."

Same framework. Steps 1 to 3 today.

1️⃣ Requirements
• Search shows by city and movie
• Pick seats
• Hold them while paying (this is the interesting part)
• Confirm after payment, or cancel
Out of scope: real payments, food orders, offers.

2️⃣ Entities
`Movie`, `Theatre`, `Screen`, `Show`, `Seat`, `ShowSeat`, `Booking`, `User`

Wait. Why both `Seat` AND `ShowSeat`? 🤔

This is the insight most candidates miss.

`Seat` is physical: row C, number 7, in Screen 2. It exists once, forever.
`ShowSeat` is that seat FOR ONE SHOW: "C7, for the 7 PM show on Friday".

Who has the status "booked"? ShowSeat! C7 can be booked for 7 PM and free for 10 PM.

If you put `isBooked` on `Seat`, booking one show blocks every other show. Classic beginner bug.

Rule: put state on the entity that owns the lifecycle. A seat's availability belongs to the show, not the seat.

3️⃣ Behaviors
`searchShows()`, `lockSeats()`, `confirmBooking()`, `cancel()`, `releaseExpiredLocks()`

And the seat lifecycle has three states:
`AVAILABLE → LOCKED → BOOKED`

If you've read Part 24, you'll recognise this: it's a mini state machine.

Why LOCKED exists:
You pick seats. You're typing your card details. Meanwhile, 500 other people want the same seats. Without a temporary hold, you'd get all the way to payment and then hear "sorry, taken". With a timed lock, those seats are yours for 5 minutes, then released if you don't pay.

Next post: the code, and the concurrency problem hiding inside `lockSeats()`.

What would you do if two users click the same seat at the exact same millisecond? 👇

#LLD #SystemDesign #BookMyShow #Java #InterviewPrep

**First comment:**

"The interview tell for this problem: candidates who ask 'what happens when two users pick the same seat?' in the first 5 minutes usually go on to design it well. It's the heart of the problem."

**Notes for you:**
- `Show` links a `Movie` to a `Screen` at a time. `ShowSeat`s are created per show from the screen's `Seat`s.
- Keep the lock duration a config value (5 minutes is just an example).

---

# POST 7 — BookMyShow, Part 2: Seat locking

**Image:** Top = a timeline with two users clicking the same seat; User A gets `LOCKED`, User B gets "Seat taken." Below: code card (two panels if needed).

**Image code:**

```java
enum SeatStatus { AVAILABLE, LOCKED, BOOKED }

class ShowSeat {
    private SeatStatus status = SeatStatus.AVAILABLE;
    private String lockedBy;
    private Instant lockExpiry;

    synchronized boolean tryLock(String userId, Instant now, Duration hold) {
        if (status == SeatStatus.LOCKED && now.isAfter(lockExpiry))
            status = SeatStatus.AVAILABLE;                 // expired lock is free again
        if (status != SeatStatus.AVAILABLE) return false;
        status = SeatStatus.LOCKED;
        lockedBy = userId;
        lockExpiry = now.plus(hold);
        return true;
    }

    synchronized boolean confirm(String userId, Instant now) {
        if (status != SeatStatus.LOCKED || !userId.equals(lockedBy)
                || now.isAfter(lockExpiry)) return false;
        status = SeatStatus.BOOKED;
        return true;
    }

    synchronized void release(String userId) {
        if (status == SeatStatus.LOCKED && userId.equals(lockedBy))
            status = SeatStatus.AVAILABLE;
    }
}
```

```java
List<ShowSeat> lockSeats(Show show, List<String> seatIds, String userId) {
    List<ShowSeat> locked = new ArrayList<>();
    for (String id : seatIds) {
        ShowSeat s = show.seat(id);
        if (s.tryLock(userId, Instant.now(), HOLD)) locked.add(s);
        else {
            locked.forEach(l -> l.release(userId));        // all-or-nothing
            throw new SeatUnavailableException(id);
        }
    }
    return locked;
}
```

**Post text:**

LLD Series | Part 33 — BookMyShow (2/2): the seat lock

Question from last post: two users click the same seat at the same millisecond. What happens?

If your code is:
```
if (seat.isAvailable()) seat.lock(user);
```
Both threads see "available." Both lock it. Two people think they own seat C7. 💥

Same bug as the Parking Lot (Part 20): check and act must be ONE step.

The fix is `tryLock()`, `synchronized` per seat. It checks and sets atomically. Exactly one thread wins. The other gets `false` immediately and shows "Seat taken."

Three details that make this solid 👇

1️⃣ Locks expire
If the user closes the tab mid-payment, the seat must come back. `tryLock` treats an expired lock as free. No background job needed for correctness (you can still add one to tidy up).

2️⃣ Only the lock owner can confirm
`confirm(userId)` checks that YOU hold the lock and it hasn't expired. Otherwise someone could "confirm" seats they never locked.

3️⃣ Multi-seat booking is all-or-nothing
User wants 4 seats together. If seat 3 fails, release seats 1 and 2. Never leave someone with half a booking.

Why no deadlock? Each `tryLock` takes ONE seat's lock, returns, and releases it before the next call. We never hold two locks at once, and we never wait. We just try and report.

⚠️ This works in ONE process. In real life you run several servers, so an in-memory `synchronized` isn't enough.
• Redis: a lock with an expiry (`SET key value NX EX 300`) gives you the "hold for 5 minutes" behavior across servers
• PostgreSQL: a unique constraint on (show_id, seat_id) for confirmed bookings makes a double booking impossible at the database level

In an interview, build the in-memory version first. Then say "in production I'd move the lock to Redis and keep a unique constraint as the final safety net." That's a strong ending.

Would you choose Redis locks, database row locks, or optimistic locking for this? 👇

#LLD #Concurrency #BookMyShow #Java #SystemDesign

**First comment:**

"The unique constraint is the one I'd never skip. Locks can fail, TTLs can expire at the wrong moment, a bug can slip through. A database constraint is the last line of defense that makes a double booking impossible even when everything else goes wrong."

**Notes for you:**
- Lock-then-confirm uses wall-clock time passed in (`Instant now`). That makes it testable: pass a fixed time in unit tests.
- Test it yourself: start 100 threads calling `tryLock` on one seat. Exactly one should return `true`.
- `SET key value NX EX 300` means "set only if it doesn't exist, expire in 300 seconds." Worth trying in redis-cli.
- If someone asks about `releaseExpiredLocks()`: a scheduled job that scans LOCKED seats past expiry is optional cleanup, not required for correctness.

---

## Idea: a GitHub repo for the series

You've now got 5 problems' worth of code. Put every example in one repo (`lld-with-java`: folders per topic, a README with the cheat sheet) and add the link in the first comment of your next recap post. It gives readers something to star, gives recruiters something to open, and costs you an afternoon.

---

## Next batch (preview)

1. Elevator system, Part 1 (requirements + entities + State)
2. Elevator system, Part 2 (scheduling Strategy)
3. MIX: parable #5 (Dependency Inversion), completing the SOLID parable set
4. Rate limiter (LLD version: token bucket with Strategy)
5. Adapter pattern (wrapping a third-party payment SDK)
6. MIX: "What I learned from 30+ posts" (fill with your real stats and lessons)
7. LLD cheat sheet v2 + GitHub repo announcement
