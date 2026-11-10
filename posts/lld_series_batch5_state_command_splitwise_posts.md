# LLD Series — Batch 5 (7 posts, State + Command + Splitwise + Mix)

**Pattern of this batch:** two new patterns (State in 2 posts, Command in 1), the start of your second full problem (Splitwise, 2 posts), and 2 mix posts for rhythm.
**Series tag:** only on technical posts: `LLD Series | Part X` (continues from Part 23).
**Image rule for technical posts:** one portrait card (1080×1350). Top = class diagram, bottom = code (max ~15 lines).
**Fill-ins:** anything in `[square brackets]` needs YOUR real detail. Replace or delete the line. Don't post a made-up story.

**⚠️ About the next problem:** my last batch ended with a poll (Splitwise / BookMyShow / Elevator). I can't see the result, so I wrote **Splitwise**. If another one won, tell me and I'll write that one instead. Keep Posts 6 and 7 as drafts until the poll closes.

---

## What makes this batch stand out

**Post 1 (State, 1/2)** — Starts from the if-else-on-status mess every backend dev has in an `Order` class, then shows the same lifecycle as small state classes. Ends with a clear "State vs Strategy" answer, a common interview question.

**Post 2 (Mix: the substitute teacher)** — Third parable. Teaches Liskov Substitution, the hardest SOLID letter, through a story anyone understands.

**Post 3 (State, 2/2)** — The vending machine, the classic LLD problem, with practical tips on saving state to a database.

**Post 4 (Command)** — Undo/redo with a short code sample, plus a link to something you already use: a message on a RabbitMQ queue is basically a command.

**Post 5 (Mix: a decision I'd change)** — An honest work-lesson post, the same kind of post that worked in your earlier batches. Needs YOUR real example.

**Posts 6–7 (Splitwise)** — Money logic (rounding, splitting ₹100 three ways) is a perfect LLD teaching moment because there's a real bug hiding in the obvious answer.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Mon 16 Nov | State Part 1: Order lifecycle | 9am IST |
| Tue 17 Nov | MIX: The substitute teacher (LSP parable) | 9am IST |
| Wed 18 Nov | State Part 2: Vending machine | 9am IST |
| Thu 19 Nov | Command: undo/redo | 9am IST |
| Fri 20 Nov | MIX: A design decision I'd change | 9am IST |
| Mon 23 Nov | Splitwise Part 1: requirements + entities | 9am IST |
| Tue 24 Nov | Splitwise Part 2: classes + code | 9am IST |

---

# POST 1 — State Pattern, Part 1

**Image:** Left = a state diagram: `CREATED → PAID → SHIPPED`, with `CANCELLED` reachable from `CREATED` and `PAID`. Right = class diagram: `Order` holds `OrderState` (interface) ← `Created`, `Paid`, `Shipped`, `Cancelled`.

**Image code:**

```java
interface OrderState {
    default OrderState pay()    { throw bad("pay"); }
    default OrderState ship()   { throw bad("ship"); }
    default OrderState cancel() { throw bad("cancel"); }

    private IllegalStateException bad(String action) {
        return new IllegalStateException("Cannot " + action + " when " + getClass().getSimpleName());
    }
}

class Created implements OrderState {
    public OrderState pay()    { return new Paid(); }
    public OrderState cancel() { return new Cancelled(); }
}
class Paid implements OrderState {
    public OrderState ship()   { return new Shipped(); }
    public OrderState cancel() { return new Cancelled(); }   // refund here
}
class Shipped implements OrderState { }
class Cancelled implements OrderState { }

class Order {
    private OrderState state = new Created();
    void pay()    { state = state.pay(); }
    void ship()   { state = state.ship(); }
    void cancel() { state = state.cancel(); }
}
```

**Post text:**

LLD Series | Part 24 — State Pattern (1/2)

Every backend project has an `Order` class like this:

```
void ship() {
    if (status == CREATED) throw new Exception("Not paid");
    else if (status == PAID) status = SHIPPED;
    else if (status == SHIPPED) throw new Exception("Already shipped");
    else if (status == CANCELLED) throw new Exception("Cancelled");
}
```

And the same if-else chain again inside `pay()`. And `cancel()`. And `refund()`.

Add a new status, `RETURNED`, and you edit every method. Miss one, and you've got a production bug where a returned order gets shipped.

State pattern:
👉 Give each state its own class. The object delegates its behavior to the current state object.

Look at the image. `Created`, `Paid`, `Shipped`, `Cancelled` are all separate classes. Each one only describes what's allowed FROM that state:
• `Created` can be paid or cancelled
• `Paid` can be shipped or cancelled
• `Shipped` can't do anything (yet)

Everything else falls to the default: "Cannot ship when Created". Invalid transitions are impossible by construction, not guarded by if-statements.

Adding `RETURNED`? One new class. Make `Shipped.returnOrder()` return it. No edits to other states.

⚠️ State vs Strategy (very common interview question)
Same class diagram, different intent:
• Strategy: the CALLER picks the behavior, and it usually stays the same
• State: the OBJECT changes its own behavior as it moves through its life

⚠️ When NOT to use it: two or three states with simple rules? An `enum` and a `switch` are clearer. State earns its place when each state has its own rules for several operations.

Next post: the vending machine, the classic LLD problem.

How many `if (status == ...)` checks are in your codebase right now? 👇

#LLD #DesignPatterns #StatePattern #Java #BackendEngineering

**First comment:**

"Quick test for whether you need State: draw your object's lifecycle on paper. If it has 4+ boxes and arrows between them, and different actions are allowed in different boxes, you're hand-writing a state machine whether you know it or not."

**Notes for you:**
- Private methods inside interfaces need Java 9+.
- The state objects here hold no data, so you can reuse single instances (or use an enum with behavior) instead of `new` each time.
- Good follow-up for comments: how do you store state in a database? That's covered in Part 2.

---

# POST 2 — MIX: The substitute teacher (LSP parable)

**Image:** Optional. A simple illustration of a classroom, or a quote card with the moral.

**Post text:**

Another story 👇

एक स्कूल में गुरुजी बीमार पड़ गए। (The teacher at a village school fell ill.)

The headmaster found a substitute. The title on his paper said "Teacher". Everyone was relieved.

The next morning the parents sent their children as usual.

Maths class: the substitute said, "I don't teach maths. I only sing."
Science class: "I don't teach science either. Let's sing."
By the end of the week, the children had learned many songs, and nothing for the exam.

The parents were angry. "He's a teacher. Why can't he teach?"

The headmaster said, "He has the title. But he never promised the work."

An old schoolmaster said:

"A substitute isn't someone who has the same name. A substitute is someone who can do EVERYTHING the original was trusted to do, so nobody has to check first."

📚 The moral: A replacement must keep every promise the original made.

That's the Liskov Substitution Principle from Part 4 of this series:
👉 If `B` replaces `A`, nothing should break.

The substitute who "only sings" = the `FixedDepositAccount` that extends `Account` but throws an exception on `withdraw()`.

The title matched. The behavior didn't.

Next time you write `extends`, ask: "Can this class do everything its parent promised?"

If the answer is "except for..." you don't have a subclass. You have the substitute who only sings.

Which principle next? ISP or DIP? 👇

#SoftwareEngineering #SOLID #Storytelling #CleanCode #Java

**First comment:**

"Parable #1: the tailor (Open/Closed). #2: the halwai (Single Responsibility). #3: today's teacher (Liskov). Vote for the next: Interface Segregation or Dependency Inversion?"

**Notes for you:**
- If you haven't posted both earlier parables, delete the first comment's numbering and just ask for the vote.
- Check your Hindi line once for natural phrasing. Use your own words if you'd say it differently.

---

# POST 3 — State Pattern, Part 2 (Vending machine)

**Image:** State diagram on top: `Idle ⇄ HasMoney`, with arrows labeled `insert`, `select`, `refund`. Code card below.

**Image code:**

```java
interface VendingState {
    void insert(VendingMachine m, int coin);
    void select(VendingMachine m, Item item);
}

class Idle implements VendingState {
    public void insert(VendingMachine m, int coin) {
        m.addBalance(coin);
        m.setState(new HasMoney());
    }
    public void select(VendingMachine m, Item item) {
        System.out.println("Insert money first");
    }
}

class HasMoney implements VendingState {
    public void insert(VendingMachine m, int coin) { m.addBalance(coin); }

    public void select(VendingMachine m, Item item) {
        if (m.stockOf(item) == 0) { m.refundAll(); m.setState(new Idle()); return; }
        if (m.balance() < item.price()) {
            System.out.println("Need " + (item.price() - m.balance()) + " more");
            return;
        }
        m.dispense(item);          // reduces stock, returns change
        m.setState(new Idle());
    }
}
```

**Post text:**

LLD Series | Part 25 — State Pattern (2/2): the vending machine

Every LLD list has it: "Design a vending machine."

It's the best State pattern example because the machine behaves differently depending on where it is:

• No money inside? "Select" does nothing but ask for money.
• Money inside? "Select" checks stock and price, then dispenses.
• Insert a coin in either state? It adds to the balance, but only the first coin changes the state.

Same two buttons. Different behavior. That's State.

The design:
• `VendingMachine` holds the current `VendingState`, the balance, and stock
• `Idle` and `HasMoney` each implement `insert()` and `select()`
• States tell the machine WHEN to switch with `m.setState(...)`

Notice there's no `if (state == IDLE)` anywhere. Adding "OutOfService" or "Dispensing" (when hardware takes 3 seconds) means a new class, not edits to the others.

Two real-world tips 👇

1️⃣ Persisting state
Don't save state objects in a database. Save a simple enum or string (`"HAS_MONEY"`), and rebuild the state object when you load the machine. State classes are behavior, not data.

2️⃣ Don't hand-roll huge machines
For 4–6 states, plain classes are perfect. For 20 states with guards and events (workflows, payments), look at a library like Spring Statemachine instead of building your own.

⚠️ Interview extras worth mentioning:
• thread safety: two people pressing buttons, so `synchronized` or a lock on the machine
• integers (paise/coins), never `double`, for money

If you were adding a "Maintenance" state, which operations would it block? 👇

#LLD #DesignPatterns #VendingMachine #Java #InterviewPrep

**First comment:**

"The interview tell: when asked 'design a vending machine', candidates who start by drawing the state diagram almost always do better than those who start with classes. Draw the boxes first. The classes fall out of them."

**Notes for you:**
- `Item` is assumed to have `price()`. `VendingMachine` needs `addBalance`, `balance`, `stockOf`, `dispense`, `refundAll`, `setState`. Mention it's trimmed for the image.
- If someone asks "who calls `setState`?": the state itself does, which is a common variation. The alternative is the machine deciding, which keeps states passive.

---

# POST 4 — Command Pattern

**Image:** Left = flow diagram: `Button/Menu` → `CommandHistory.run(cmd)` → `Command.execute()` → `Document`. Two stacks drawn beside it labeled "undo" and "redo". Right = code.

**Image code:**

```java
interface Command { void execute(); void undo(); }

class AddText implements Command {
    private final Document doc; private final String text;
    AddText(Document doc, String text) { this.doc = doc; this.text = text; }
    public void execute() { doc.append(text); }
    public void undo()    { doc.deleteLast(text.length()); }
}

class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    void run(Command c) { c.execute(); undoStack.push(c); redoStack.clear(); }
    void undo() {
        if (undoStack.isEmpty()) return;
        Command c = undoStack.pop(); c.undo(); redoStack.push(c);
    }
    void redo() {
        if (redoStack.isEmpty()) return;
        Command c = redoStack.pop(); c.execute(); undoStack.push(c);
    }
}
```

**Post text:**

LLD Series | Part 26 — Command Pattern

How does Ctrl+Z work?

Not by magic. By treating every action as an OBJECT.

Command pattern:
👉 Wrap a request ("add this text") inside an object that knows how to do it AND undo it.

Because the action is now an object, you can:
• store it (a history stack)
• undo it (call `undo()`)
• redo it
• queue it for later
• send it somewhere else

Look at the image:
1️⃣ `Command` has `execute()` and `undo()`
2️⃣ `AddText` remembers WHAT text it added, so it knows how to remove it
3️⃣ `CommandHistory` runs commands and keeps two stacks, "undo" and "redo"

Run → push on undo stack. Undo → pop, undo it, push on redo stack. Redo → the reverse.

One detail people forget: running a NEW command clears the redo stack. If you undo twice, then type something new, the old "redo" future no longer exists.

Where you've already used Command (without the name):
• Ctrl+Z in any editor
• a message on a RabbitMQ queue: "send this email" is a command object, stored and executed later by a worker
• a job in a task scheduler
• database migrations (up/down)

⚠️ The hard part is `undo()`. Appending text is easy to reverse. "Delete a database row" isn't, unless the command also saved the data it deleted. Commands must remember enough to undo themselves.

Which action in your app would be the hardest to make undoable? 👇

#LLD #DesignPatterns #CommandPattern #Java #BackendEngineering

**First comment:**

"Bonus use: logging every command also gives you an audit trail for free. 'Who did what, in what order' is just the command history, saved."

**Notes for you:**
- Command vs Strategy confusion: Strategy = HOW to do one thing (swappable algorithm). Command = a request turned into an object (what to do, when, with undo).
- `Document` needs `append(String)` and `deleteLast(int)`. Trimmed for the image.
- Good interview extension: macro commands (a command that holds a list of commands). Mention if asked.

---

# POST 5 — MIX: A design decision I'd change

**Image:** None.

**Post text:**

A design decision I'd change:

[Describe ONE real decision from your work in one sentence. Example shape: "We stored X as Y." / "We put all of Z inside one service." / "We used a shared table for A and B."]

At the time, it made sense:
• [the real reason, e.g., deadline, small team, simpler to start]
• [another real reason, optional]

What I didn't see:
• [what went wrong or got harder later]
• [how it showed up: bugs, slow changes, hard-to-test code]

What I'd do now:
• [the better approach, in plain words]
• [which principle or pattern from this series applies, if any. Only mention one if it genuinely fits]

What I learned:
→ Most "bad" decisions were reasonable decisions with missing information.
→ The cost of a design choice shows up months later, not on the day you make it.
→ I now ask one question before committing: "What happens when this requirement changes?"

I used to think good engineers got designs right the first time.

Now I think good engineers notice sooner that a design is wrong, and aren't embarrassed to change it.

What's a decision you'd change? No names, no company, just the lesson 👇

#SoftwareEngineering #LearningInPublic #SystemDesign #CareerGrowth #BackendEngineering

**First comment:**

"If you want a simple rule for this: when you notice yourself defending a design instead of questioning it, stop and write down what would change your mind."

**Notes for you:**
- ⚠️ **This post needs a real example from you.** I can't invent one. The skeleton is built so you can drop in one true decision (even a small one) and it works.
- Don't name your employer, clients, or anything confidential (patient data, internal systems). Keep it general: "a booking service", "a reporting table".
- If you don't have a work example, use a project from your side builds (ApiFlow, or something you built while learning). Honesty beats drama.
- The three "what I learned" lines and the closing are general, so they stay even if the middle changes.

---

# POST 6 — Splitwise, Part 1: Requirements + entities

**Image:** Left = requirements (IN / OUT). Right = nouns and verbs. Bottom = the "100 ÷ 3" teaser box with the problem written out.

**Image content:**

```
IN SCOPE
- Users and groups
- Add an expense: who paid, total, who shares it
- Split types: EQUAL, EXACT, PERCENT
- Show balances: who owes whom, how much
- Settle up (record a payment)

OUT OF SCOPE
- Real money transfer
- Currencies / exchange rates
- Notifications, receipts, chat

NOUNS              VERBS
User               addExpense()
Group              split()
Expense            showBalances()
Split              settleUp()
BalanceSheet

⚠️ The trap: ₹100 split 3 ways
   33.33 + 33.33 + 33.33 = 99.99
   Where did 1 paisa go?
```

**Post text:**

LLD Series | Part 27 — Splitwise (1/2)

Second full LLD problem: "Design Splitwise."

Same framework as the Parking Lot. Today: steps 1 to 3.

1️⃣ Requirements
• Users can add expenses and share them with others
• Three ways to split: equally, exact amounts, percentages
• Show who owes whom
• Record a settlement
Out of scope: real payments, currencies, notifications.

2️⃣ Entities (nouns)
`User`, `Group`, `Expense`, `Split`, `BalanceSheet`

3️⃣ Behaviors (verbs)
`addExpense()`, `split()`, `showBalances()`, `settleUp()`

Ownership question:
• "Work out each person's share" → depends on the split type. This changes, so it's a Strategy (Part 9).
• "Who owes whom" → the `BalanceSheet` tracks it
• "Add expense" → a service that ties them together

Now the part that makes this problem interesting 👇

₹100 split between 3 friends.
100 ÷ 3 = 33.33...
33.33 × 3 = 99.99

You just lost a paisa. Do it on 10,000 expenses and your balances don't add up to zero.

Two rules I'll follow:
1️⃣ Store money as integers (paise), never `double`
2️⃣ When it doesn't divide evenly, give the leftover paise to the first few people, so shares always add up to exactly the total

100 rupees = 10000 paise → 3334 + 3333 + 3333 = 10000 ✅

Next post: the classes and the code.

Have you ever had a money bug from rounding? What happened? 👇

#LLD #SystemDesign #Splitwise #Java #InterviewPrep

**First comment:**

"The golden rule for any money system: the sum of the parts must equal the whole, always. If your shares don't add up to the total, you don't have a rounding error, you have a bug."

**Notes for you:**
- Splitwise's famous extra feature is "simplify debts" (fewer payments to settle everyone). That's a graph problem; save it as a bonus Part 3.
- Interviewers love the rounding question. Say it out loud before they ask.

---

# POST 7 — Splitwise, Part 2: Classes + code

**Image:** Class diagram: `ExpenseService` → `SplitStrategy` (interface) ← `EqualSplit`, `ExactSplit`, `PercentSplit`; `ExpenseService` → `BalanceSheet`. `Expense` contains `Split`s. Code card below.

**Image code:**

```java
record User(String id, String name) {}
record Split(User user, long paise) {}
record Expense(String id, User paidBy, long totalPaise, List<Split> splits) {}

interface SplitStrategy {
    List<Split> split(long totalPaise, List<User> users, List<Long> values);
}

class EqualSplit implements SplitStrategy {
    public List<Split> split(long total, List<User> users, List<Long> ignored) {
        long base = total / users.size();
        long extra = total % users.size();          // leftover paise
        List<Split> out = new ArrayList<>();
        for (int i = 0; i < users.size(); i++)
            out.add(new Split(users.get(i), base + (i < extra ? 1 : 0)));
        return out;
    }
}
```

```java
class BalanceSheet {
    // debtor id -> (creditor id -> paise owed)
    private final Map<String, Map<String, Long>> owes = new HashMap<>();

    void apply(Expense e) {
        for (Split s : e.splits()) {
            if (s.user().equals(e.paidBy())) continue;   // you don't owe yourself
            owes.computeIfAbsent(s.user().id(), k -> new HashMap<>())
                .merge(e.paidBy().id(), s.paise(), Long::sum);
        }
    }
}

class ExpenseService {
    private final Map<SplitType, SplitStrategy> strategies;
    private final BalanceSheet sheet;
    // constructor omitted

    Expense addExpense(User paidBy, long total, List<User> users,
                       SplitType type, List<Long> values) {
        List<Split> splits = strategies.get(type).split(total, users, values);
        Expense e = new Expense(UUID.randomUUID().toString(), paidBy, total, splits);
        sheet.apply(e);
        return e;
    }
}
```

**Post text:**

LLD Series | Part 28 — Splitwise (2/2)

Time for the code. Three decisions to look at 👇

1️⃣ Strategy for splitting (callback to Part 9)
`EqualSplit`, `ExactSplit`, `PercentSplit` all implement `SplitStrategy`. `ExpenseService` just calls `strategies.get(type).split(...)`. Adding "split by shares" (2 parts, 1 part, 1 part) = one new class, zero edits.

2️⃣ The rounding fix in `EqualSplit`
`total / n` gives the base share. `total % n` is the leftover paise. The first `extra` people pay 1 paisa more.

₹100 between 3 → 3334 + 3333 + 3333 = 10000 ✅
The shares ALWAYS sum to the total. No lost paisa.

3️⃣ `BalanceSheet` stores debts as a map
`debtor → creditor → amount`. When Priya pays ₹900 for dinner for herself, Amit and Ravi, the sheet records "Amit owes Priya ₹300" and "Ravi owes Priya ₹300". Priya's own share isn't a debt, so it's skipped.

What each class knows:
• `Split` strategies: only math, no balances
• `BalanceSheet`: only debts, no splitting rules
• `ExpenseService`: connects them (Single Responsibility, Part 2)

⚠️ What I left out on purpose:
• `ExactSplit` must check that the amounts add up to the total, or reject the expense
• `PercentSplit` must check that percentages add up to 100
• netting: if Amit owes Priya ₹300 and Priya owes Amit ₹100, show "Amit owes ₹200"
• thread safety on `BalanceSheet`

Netting is the next step, and "simplify debts" is the hard version of it. Part 3 if you want it.

Want a Part 3 on simplifying debts? Comment "yes" and tell me the smallest number of payments you think 5 friends need 👇

#LLD #Java #Splitwise #DesignPatterns #SystemDesign

**First comment:**

"Try to break it: what happens when `values` is null for `EqualSplit`? It's ignored, which works, but it's a design smell. A cleaner API would have separate request types per split kind. Anyone want to suggest how?"

**Notes for you:**
- `SplitType` is an enum: `EQUAL, EXACT, PERCENT`. Add to the image or mention in the first comment.
- In Spring Boot, inject the `Map<SplitType, SplitStrategy>` via a config bean, or use the bean-name map trick from Part 10.
- Question for your own understanding: why is `Map<String, Map<String, Long>>` keyed by user ID and not by `User`? (Answer: IDs are stable and cheap; objects can have `equals` pitfalls. Either works for a record.)

---

## Next batch (preview)

1. Splitwise Part 3: netting + "simplify debts" (if the "yes" comments come in)
2. MIX: parable #4 (ISP or DIP, based on the vote)
3. Chain of Responsibility (request validation or approval flow)
4. MIX: "What I'd tell myself 1 year ago" (fill with your real lessons)
5. Proxy vs Decorator (a short, sharp comparison post)
6. Next full problem: BookMyShow or Elevator (Part 1)
7. Same problem (Part 2) with concurrency (seat locking)
