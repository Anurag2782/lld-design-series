# LLD Series — Batch 8 (7 posts: Template Method, Facade, ATM + Mix)

**Pattern of this batch:** two new patterns (Template Method in 2 posts, Facade in 1), a classic interview problem (ATM, 2 posts), and 2 mix posts.
**Series tags use topic names, not part numbers** (`LLD Series | Template Method (1/2)`), so the tags stay correct however many posts you've actually published.
**Code:** every example in these posts exists, runnable and tested, in the repo in Java, C#, Python and TypeScript:
`patterns/templatemethod`, `patterns/facade`, `problems/atm`.
**Image rule:** one portrait card (1080×1350). Top = diagram, bottom = code (max ~15 lines). The code below is trimmed from the repo versions.

---

## What makes this batch stand out

**Post 1–3 (Template Method)** — Part 1 teaches the skeleton idea. Part 2 shows the callback version, which is what Spring's `JdbcTemplate` really is. Between them, a mix post that gives readers something to bookmark.

**Post 2 (Mix: interview Q&A)** — Five questions with two-line answers. This is the most "save for later" mix post you can write.

**Post 4 (Facade)** — Includes the compensation step (release the inventory if payment fails), which most Facade tutorials skip.

**Post 5 (Mix: explaining code like a story)** — Ties your Hindi stories channel to the way you teach, and gives readers a reusable template.

**Posts 6–7 (ATM)** — Two earlier patterns working together (State for the session, Chain of Responsibility for the notes), plus an honest "known limitation" that makes you look careful rather than sloppy.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Tue 15 Dec | Template Method (1/2) | 9am IST |
| Wed 16 Dec | MIX: 5 LLD interview questions, short answers | 9am IST |
| Thu 17 Dec | Template Method (2/2): the callback version | 9am IST |
| Fri 18 Dec | Facade | 9am IST |
| Mon 21 Dec | MIX: Why I explain code like a story | 9am IST |
| Tue 22 Dec | ATM (1/2): requirements + design | 9am IST |
| Wed 23 Dec | ATM (2/2): the code | 9am IST |

---

# POST 1 — Template Method (1/2)

**Image:** Left = a flow diagram: `run()` as a vertical list `parse → validate → save → afterSave`, with `parse` and `save` colored "you MUST write this", `validate` "default, can override", `afterSave` "hook, optional". Right = two subclasses (`CsvImporter`, `PipeImporter`) hanging off `DataImporter`.

**Image code:**

```java
abstract class DataImporter {
    public final ImportResult run(List<String> lines) {   // the template
        List<Row> parsed = parse(lines);
        List<Row> valid  = validate(parsed);
        save(valid);
        afterSave(valid);
        return new ImportResult(valid.size(), parsed.size() - valid.size());
    }
    protected abstract List<Row> parse(List<String> lines);  // required
    protected abstract void save(List<Row> rows);            // required
    protected List<Row> validate(List<Row> rows) {           // default, overridable
        return rows.stream().filter(Row::isValid).toList();
    }
    protected void afterSave(List<Row> rows) { }             // hook
}
```

**Post text:**

LLD Series | Template Method (1/2)

You write a CSV importer. It: reads lines, parses them, validates, saves, logs.

Next month: a pipe-separated importer. Then JSON. Then Excel.

Each one copies the same skeleton:
`parse → validate → save → log`

Only the parsing differs. But now the "skeleton" lives in four copies, and when someone adds an audit step, they update three of them and forget the fourth. 🐛

Template Method:
👉 Put the skeleton of an algorithm in a base class. Let subclasses fill in only the steps that vary.

Look at the image:
• `run()` is the template. It's `final`, so no subclass can reorder the steps
• `parse()` and `save()` are abstract: you MUST provide them
• `validate()` has a sensible default: override it only if you need an extra rule
• `afterSave()` is a hook: empty by default, use it if you want

`PipeImporter` needs a different max quantity? It overrides `validate()` and calls `super.validate(...)` first. Nothing else changes. Adding "audit log" to ALL importers? One edit in `run()`.

It's called the Hollywood Principle: "Don't call us, we'll call you." The base class decides WHEN your code runs. You only decide WHAT it does.

You've used this already:
• `HttpServlet.service()` calls your `doGet()` or `doPost()`
• JUnit calls your `setUp()`, then the test, then `tearDown()`

⚠️ Template Method vs Strategy
• Template Method: ONE skeleton, subclasses fill in steps. Uses inheritance.
• Strategy: swap the WHOLE algorithm. Uses composition.
If your subclasses are overriding almost everything, you probably wanted Strategy.

⚠️ Watch out: if the template has 10 abstract steps, nobody can remember what to implement. Keep required steps to 2 or 3.

Next: the callback version, which is what Spring's `JdbcTemplate` actually does.

Where do you have copy-pasted skeletons in your code? 👇

#LLD #DesignPatterns #TemplateMethod #Java #CleanCode

**First comment:**

"Why `final` on the template? Without it, a subclass can override `run()` and skip validation entirely. The whole point of the pattern is that the order of steps is guaranteed. Make the skeleton final, leave the steps overridable."

**Notes for you:**
- Full code in the repo: `patterns/templatemethod` (all four languages).
- In C#, the template method simply isn't `virtual`. In Python there is no `final`, so a comment ("don't override") plus convention does the job.
- `HttpServlet.service()` dispatching to `doGet`/`doPost` is a standard textbook example of this pattern.

---

# POST 2 — MIX: 5 LLD interview questions, short answers

**Image:** Optional. A clean text card: "5 LLD questions you'll be asked", numbered, one line each.

**Post text:**

5 LLD interview questions you WILL be asked, and short answers that work 👇

1️⃣ "Where do you start?"
→ Not with code. Clarify requirements, say what's out of scope, list entities (nouns) and behaviors (verbs), then draw relationships.

2️⃣ "Strategy vs State?"
→ Same structure, different intent. Strategy: the caller picks an interchangeable algorithm. State: the object changes its own behavior as it moves through its lifecycle.

3️⃣ "Why did you add this interface/pattern?"
→ Only answer with a pain: "Because this if-else will grow every time we add X." If you can't name the pain, remove the pattern.

4️⃣ "How would you make it thread-safe?"
→ Find the check-then-act step (is the slot free? then take it) and make it ONE atomic step with a lock on that object, not a lock on the whole system. With many servers, move the guard to the database or Redis.

5️⃣ "What if a new requirement comes in?"
→ This is the extension test. Show which classes change. The best answer is "one new class, nothing else."

Bonus: say your assumptions out loud. "I'm assuming one building and 3 elevators; tell me if that's wrong." It costs 10 seconds and prevents a 20-minute mismatch.

What other question would you add? 👇

#InterviewPrep #LLD #SoftwareEngineering #SystemDesign #CareerGrowth

**First comment:**

"If you only have time to practice one thing: question 5. Take any design you've done, add one new requirement, and count how many classes you had to touch. That number tells you how good the design is."

**Notes for you:**
- These are general interview habits, not personal claims, so the post is safe to publish as written.
- Keep each answer to two lines in the image; people save posts they can screenshot.

---

# POST 3 — Template Method (2/2): the callback version

**Image:** Left = a "sandwich" diagram: `open` (template) → `YOUR CODE` (callback, highlighted) → `close` (template, always runs). Right = code, plus a small Spring snippet.

**Image code:**

```java
class ConnectionTemplate {
    <T> T execute(Function<List<String>, T> callback) {
        events.add("open");
        try {
            return callback.apply(events);     // the only part that varies
        } finally {
            events.add("close");               // ALWAYS runs
        }
    }
}

// Spring does exactly this:
List<User> users = jdbcTemplate.query(
    "SELECT id, name FROM users",
    (rs, i) -> new User(rs.getLong("id"), rs.getString("name")));
```

**Post text:**

LLD Series | Template Method (2/2)

Yesterday's Template Method used inheritance. There's a second flavour you've definitely used, and it doesn't need subclasses.

Remember the old JDBC code?

```
Connection c = null;
try {
    c = ds.getConnection();
    ... your query ...
} catch (SQLException e) { ... }
finally { if (c != null) c.close(); }
```

Every. Single. Query. The boilerplate was 90% of the code. And someone always forgot the `finally`, which caused connection leaks. 🚰

The fix: let a template own the boilerplate, and pass only the VARYING part as a callback:

`jdbcTemplate.query("SELECT ...", (rs, i) -> new User(...))`

The template opens the connection, handles exceptions, and ALWAYS closes. You write the mapping. That's it.

Same pattern, different mechanism:
• Classic: base class + subclasses override steps
• Callback: a class + a lambda passed in for the step

Where you've seen it:
• Spring: `JdbcTemplate`, `TransactionTemplate`, `RestTemplate`
• Python: a `with` block
• C#: a `using` block
• Java: try-with-resources

The test of a good template: the resource is released even when YOUR code throws. Look at the `finally` in the image. That's the guarantee.

⚠️ One thing people confuse: the callback version looks like Strategy (you pass behavior in). The difference is the intent. Here the template owns a fixed lifecycle (open, run, close) and you fill the middle. Strategy replaces the whole algorithm.

Which boilerplate in your code could become a template? Retry loops? Timing? Transactions? 👇

#LLD #DesignPatterns #TemplateMethod #SpringBoot #Java

**First comment:**

"A retry helper is a great example: `retry.execute(() -> callApi())`. The template owns the loop, the backoff, and the give-up rule. You only pass the call. Same pattern, nothing new to learn."

**Notes for you:**
- Spring's `JdbcTemplate.query(sql, RowMapper)` and `TransactionTemplate.execute(callback)` are real, standard APIs.
- Repo: `patterns/templatemethod` includes the callback template and a test that proves `close` still runs after an exception.

---

# POST 4 — Facade

**Image:** Left = a "before" picture with the client calling 4 boxes directly (arrows crossing), right = "after" with one `CheckoutFacade` box in front of the same 4. Code card below.

**Image code:**

```java
class CheckoutFacade {
    OrderConfirmation placeOrder(String item, long paise, String address, String email) {
        inventory.reserve(item);
        try {
            String receipt  = payment.charge(paise);
            String tracking = shipping.schedule(address);
            notifier.orderPlaced(email);
            return new OrderConfirmation(receipt, tracking);
        } catch (RuntimeException e) {
            inventory.release(item);      // compensate: undo what we already did
            throw e;
        }
    }
}
```

**Post text:**

LLD Series | Facade

Placing an order means:
1. reserve stock
2. charge the card
3. schedule shipping
4. send a notification
5. and if payment fails, release the stock

Now imagine every controller, every scheduled job, and every test doing those five steps by hand. Each one slightly different. One forgets to release the stock. Another sends the email before the payment succeeds. 😬

Facade pattern:
👉 Give a complicated subsystem ONE simple entry point.

`checkout.placeOrder(item, paise, address, email)`

Callers don't know there are four services behind it, or what order to call them in. The facade does.

Look at the image: the "after" diagram has one door instead of four.

The part I want you to notice in the code is the `catch`:
If the payment fails, the facade releases the reserved stock. That's a compensating action. The caller shouldn't have to remember it.

What you get:
• one place that knows the correct order
• callers stay simple
• you can change the subsystems underneath without touching callers

In Spring Boot, many `@Service` classes that coordinate other services are facades, even if nobody calls them that.

⚠️ Facade is NOT:
• Adapter: an Adapter changes an interface so it fits. A Facade simplifies a big one.
• A god class: a facade should delegate, not contain business logic. If it grows to 1000 lines, split it.
• A wall: advanced users should still be able to reach the subsystems if they need to.

⚠️ And one honest limit: this works inside one process. When the four steps are four separate services, a failure halfway needs something stronger, like a Saga or an outbox. Different post, same idea.

Where is your "five steps, always in this order" code? 👇

#LLD #DesignPatterns #FacadePattern #Java #BackendEngineering

**First comment:**

"Quick test for whether you need a Facade: if you've copy-pasted the same 3+ service calls in more than two places, you've already got an implicit facade. Make it explicit."

**Notes for you:**
- Repo: `patterns/facade`. The test checks both the happy path (exact call order) and the failure path (stock released, nothing shipped).
- The Saga/outbox note is a pointer, not a promise; only link to your own outbox post if you've written one.

---

# POST 5 — MIX: Why I explain code like a story

**Image:** Optional. A simple card with the 4-step template below.

**Post text:**

I run a channel of Hindi moral stories. And I've noticed something: the way a good story works is exactly the way good technical explanations work.

A story has:
1️⃣ A character with a problem
2️⃣ A first attempt that fails
3️⃣ A turning point
4️⃣ A moral you can carry out of the room

Compare that with how most code explanations go: definition, diagram, "so that's the Observer pattern." Nothing to hold on to.

Now try this template for any concept:

📌 The Problem: "A developer has an if-else that grows every sprint."
📌 The Pain: "Every new feature breaks something old."
📌 The Turn: "She puts each rule behind one interface."
📌 The Moral: "Add code. Don't edit code that works."

That's the Open/Closed Principle, in four lines, and you won't forget it.

Why this helps beyond posts:
• In interviews, a story-shaped answer ("the pain was X, so I used Y, and it gave me Z") sounds far more convincing than a definition
• In code reviews, "here's the problem this solves" beats "this is the Strategy pattern"
• For yourself, if you can't tell the story, you probably don't understand the concept yet

Next time you learn something new, try writing it as: Problem → Pain → Turn → Moral.

What's a concept you understood only after you heard it as a story? 👇

#LearningInPublic #Storytelling #SoftwareEngineering #CareerGrowth #TeachingTech

**First comment:**

"If you'd like to see the parables I've done so far (the tailor, the halwai, the teacher, the duty cards, the mill), they're on my profile. And my Hindi stories channel is here: [channel link]."

**Notes for you:**
- The only blank is `[channel link]`. Put the real link in the first comment, not in the post body.
- The "story template" is a teaching technique, not a claim about results, so nothing in the post depends on numbers you don't have.
- If you haven't posted all five parables, change the list in the first comment to the ones you have.

---

# POST 6 — ATM (1/2): requirements + design

**Image:** Left = requirements (IN / OUT). Middle = state diagram `Idle → HasCard → Authenticated → Idle`, with a branch "3 wrong PINs → card retained → Idle". Right = a note-chain diagram `500 → 200 → 100`.

**Image content:**

```
IN SCOPE
- Insert card, enter PIN (3 attempts)
- Withdraw cash (multiples of 100)
- Notes: 500, 200, 100 with limited stock
- Eject card

OUT OF SCOPE
- Deposits, transfers, receipts
- Real bank API (we call an interface)
- Hardware faults

STATES
Idle -> HasCard -> Authenticated -> Idle
HasCard --3 wrong PINs--> card retained -> Idle

ENTITIES
Atm, BankService (interface), CashDispenser, NoteHandler(500/200/100)
```

**Post text:**

LLD Series | ATM (1/2)

"Design an ATM." A classic. Same framework, three decisions that matter.

1️⃣ Requirements
• Insert card, enter PIN, 3 attempts
• Withdraw cash (multiples of 100)
• Machine has limited notes: 500, 200, 100
• Out of scope: deposits, receipts, real bank integration

2️⃣ What can the user do RIGHT NOW? That depends on the state.
Before inserting a card: only "insert card".
After inserting: only "enter PIN".
After the PIN: "withdraw" or "eject".

Whenever behavior depends on where you are in a flow, that's the State pattern (Part on State earlier in this series).

No `if (state == AUTHENTICATED && ...)` anywhere. Each state class knows what it allows, and everything else throws a clear error.

3️⃣ How do we hand out notes?
Withdraw 1300 → 2 × 500, 1 × 200, 1 × 100.
Idea: a chain of handlers, one per note. Each takes as many notes of its value as it can, then passes the remainder down: 500 → 200 → 100.

That's the Chain of Responsibility, but used differently from validation: here each handler takes a PART of the work and passes the rest.

And the bank? We don't implement it. `Atm` depends on a `BankService` interface (Dependency Inversion), so tests use a fake bank and the design survives when the real bank changes.

Two rules I'm following:
• Three wrong PINs → the card is retained
• The order of a withdrawal is: CAN WE PAY IT? → take the money from the account → hand out the notes. Never the other way around.

Why that order? Next post: the code, and what happens if we get it wrong.

Why do you think "debit first, dispense last" can still go wrong? 👇

#LLD #SystemDesign #ATM #Java #InterviewPrep

**First comment:**

"Spoiler: what if the account is debited and the machine jams before the notes come out? Real ATMs handle it with reconciliation and reversals. In an interview, mentioning that failure window is worth points even if you don't build it."

**Notes for you:**
- Repo: `problems/atm`.
- The notes 500/200/100 reflect what ATMs commonly dispense; the numbers are configurable in the code.
- The "failure window" note is a real engineering concern; you can say it as a general fact without claiming personal experience.

---

# POST 7 — ATM (2/2): the code

**Image:** Top = a small diagram: a `withdraw(1300)` call going `plan → debit → commit`. Below: code card (two panels if needed).

**Image code:**

```java
class NoteHandler {
    Map<Integer,Integer> plan(long amount, Map<Integer,Integer> out) {
        int use = (int) Math.min(available, amount / note);
        if (use > 0) out.put(note, use);
        long rest = amount - (long) use * note;
        if (rest == 0) return out;
        if (next == null) throw new CannotDispenseException("...");
        return next.plan(rest, out);          // pass the remainder down
    }
    void commit(Map<Integer,Integer> plan) { // only now do counts change
        available -= plan.getOrDefault(note, 0);
        if (next != null) next.commit(plan);
    }
}

class AuthenticatedState implements AtmState {
    public Map<Integer,Integer> withdraw(Atm a, long amount) {
        Map<Integer,Integer> plan = a.dispenser.plan(amount);  // 1. can we pay it?
        if (!a.bank.debit(a.card, amount)) throw new IllegalStateException();  // 2. take the money
        a.dispenser.commit(plan);                               // 3. hand out the notes
        return plan;
    }
}
```

**Post text:**

LLD Series | ATM (2/2): the code

Last time: the design. Now the three details that make it correct 👇

1️⃣ PLAN first, COMMIT later
`plan()` works out which notes we'd give, without touching the counts. `commit()` actually reduces the stock.

Why split them? Imagine we reduce counts as we go, and then fail halfway because we can't make the exact amount. The machine would have lost notes it never handed out. Planning first means a failed withdrawal changes nothing.

2️⃣ Debit BEFORE dispensing
If we dispensed first and the bank then declined, the customer walks away with free cash. Debit first: the worst case becomes "account debited, notes stuck", which can be reversed. Free cash can't.

3️⃣ Each state is tiny
`HasCardState` only knows about PINs: right PIN → Authenticated, wrong PIN → count it, third wrong PIN → keep the card, back to Idle. `AuthenticatedState` only knows about withdrawing.

Try withdrawing before entering the PIN: "Enter your PIN first". Not an `if`; the default method of the state.

⚠️ An honest limitation I put in the tests:
This chain is greedy. It never backtracks. Machine has one 500 note and three 200 notes. You ask for 600.
Greedy takes the 500, has 100 left, no 100 notes → fails.
But 3 × 200 = 600 works!

A real ATM would use a small search (dynamic programming or backtracking) to find a valid combination. Greedy is perfect when you have plenty of every note; it's wrong when stock is thin. Say this in an interview and you've shown you know where your design breaks.

The test that proves "plan doesn't consume notes": a failed 1000 withdrawal on a machine with one 500 note leaves BOTH the notes and the balance untouched.

If you were adding "daily withdrawal limit", which class would change? 👇

#LLD #Java #ATM #DesignPatterns #SystemDesign

**First comment:**

"Answer to the question: a daily limit is a business rule, so it goes in `AuthenticatedState.withdraw()` (or better, a small `LimitPolicy` interface it calls). The `CashDispenser` shouldn't know about limits, and the notes chain stays untouched."

**Notes for you:**
- Repo: `problems/atm` has the greedy-limitation test and the "failed plan changes nothing" test, in all four languages.
- Denominations are an assumption; say so in the post if someone asks (₹2000 notes are not a practical default today).
- Don't claim real ATMs use exactly this design; say "a real machine would..." only for the general points (reconciliation, search for combinations).

---

## Next batch: see `lld_series_batch9_library_composite_chess_posts.md`
