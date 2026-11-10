# LLD Series — Batch 9 (7 posts: Library, Composite, Chess + Mix)

**Pattern of this batch:** a full problem (Library Management, 2 posts), one pattern (Composite), a bigger problem (Chess, 2 posts), and 2 mix posts.
**Series tags use topic names, not part numbers.**
**Code:** every example exists, runnable and tested, in the repo in Java, C#, Python and TypeScript: `problems/library`, `patterns/composite`, `problems/chess`.
**Holiday note:** this batch lands around Christmas and New Year, when feeds are quieter and people have time to read. The two mix posts and the "mistakes checklist" are the shareable ones; schedule them on the days you expect the most attention.

---

## What makes this batch stand out

**Posts 1–2 (Library)** — Reuses the best idea from BookMyShow (the thing you borrow is a *copy*, and status belongs to the copy). Readers who followed along will recognize the pattern, which is exactly what you want.

**Post 3 (Mix: LLD mistakes checklist)** — A high-save checklist post.

**Post 4 (Composite)** — A file system is the clearest example. Includes the cycle bug that most tutorials ignore.

**Post 5 (Mix: the village bell, Observer parable)** — The sixth story in your parable series. Observer is a natural fit for a story.

**Posts 6–7 (Chess)** — The "big" problem of the series. The key design idea (each piece owns its own rule) is simple, and the Fool's Mate test gives readers a satisfying end-to-end proof.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Thu 24 Dec | Library (1/2): requirements + Book vs BookCopy | 9am IST |
| Mon 28 Dec | Library (2/2): the code | 9am IST |
| Tue 29 Dec | MIX: 10 LLD mistakes (checklist) | 9am IST |
| Wed 30 Dec | Composite | 9am IST |
| Thu 31 Dec | MIX: The village bell (Observer parable) | 9am IST |
| Mon 4 Jan | Chess (1/2): requirements + design | 9am IST |
| Tue 5 Jan | Chess (2/2): the code + Fool's Mate | 9am IST |

---

# POST 1 — Library Management (1/2)

**Image:** Left = requirements (IN / OUT). Right = a callout comparing `Book` (one per title) and `BookCopy` (one per physical copy, with a status). A small timeline for a loan: `issue → 14 days → return → fine?`.

**Image content:**

```
IN SCOPE
- Search books by title / author
- Issue a copy to a member (max 3 loans, 14 days)
- Return a copy, calculate the fine
- Tell someone when a copy comes back

OUT OF SCOPE
- Payments, memberships, e-books

ENTITIES
Book      one per TITLE   (isbn, title, author)
BookCopy  one per COPY    (barcode, available?)  <- the thing you borrow
Member, Loan, Catalog, LoanService

FINE (paise): late days - grace days, times rate per day
```

**Post text:**

LLD Series | Library Management (1/2)

New problem: "Design a library management system."

Before any code, one question decides the whole design 👇

"The library has 3 copies of Dune. If I borrow one, is Dune borrowed?"

No! Two copies are still on the shelf.

So there are TWO things:
📘 `Book`: the title. One record for Dune (ISBN, author).
📗 `BookCopy`: one physical copy. Has a barcode and a status: available or on loan.

The thing you BORROW is a copy. So the status lives on the copy, not on the book.

If you put `isBorrowed` on `Book`, a library with 3 copies of Dune can only lend one at a time, and the system says "unavailable" while two copies sit on the shelf.

Does this ring a bell? It's the same idea as `Seat` vs `ShowSeat` in the BookMyShow design: put state on the entity that owns the lifecycle.

Requirements I'd confirm:
• search by title or author
• a member can hold at most 3 books, for 14 days
• returning late costs a fine, after a short grace period
• if someone is waiting for a title, tell them when a copy returns

Entities: `Book`, `BookCopy`, `Member`, `Loan`, `Catalog`, and a `LoanService` that ties them together.

Two patterns waiting in the wings:
• fines depend on a rule that will change (late fee per day, weekend rules, member type) → Strategy
• "tell someone when a copy comes back" → Observer

Next post: the code.

If a member returns a book 5 days late, with a 2-day grace period and ₹2 per day, what's the fine? 👇

#LLD #SystemDesign #LibraryManagement #Java #InterviewPrep

**First comment:**

"Answer: 5 late days minus 2 grace days = 3 billable days, times ₹2 = ₹6. In the code, that's 600 paise. Money in whole paise, never floating point."

**Notes for you:**
- Repo: `problems/library`.
- If you didn't publish BookMyShow with the `Seat`/`ShowSeat` explanation, delete the "bell" paragraph.

---

# POST 2 — Library Management (2/2): the code

**Image:** Class diagram: `LoanService` → `FinePolicy` (interface) ← `PerDayFine`; `LoanService` notifies `CopyReturnedListener`; `Member` has `Loan`s; `Loan` references `BookCopy`. Code card below.

**Image code:**

```java
class BookCopy {
    synchronized boolean tryCheckout() {
        if (!available) return false;     // check AND set, atomically
        available = false;
        return true;
    }
}

interface FinePolicy { long fine(Loan loan, LocalDate returned); }

class PerDayFine implements FinePolicy {
    public long fine(Loan loan, LocalDate returned) {
        long late = ChronoUnit.DAYS.between(loan.due(), returned);
        return Math.max(0, late - graceDays) * ratePerDay;
    }
}

class LoanService {
    Loan issue(Member m, BookCopy copy, LocalDate today) {
        if (m.activeLoans.size() >= MAX_LOANS) throw new IllegalStateException("Loan limit reached");
        if (!copy.tryCheckout()) throw new IllegalStateException("Copy already issued");
        Loan loan = new Loan(m, copy, today, today.plusDays(14));
        m.activeLoans.add(loan);
        return loan;
    }
}
```

**Post text:**

LLD Series | Library Management (2/2): the code

Four decisions worth looking at 👇

1️⃣ The copy guards itself
`tryCheckout()` checks "is it available?" AND sets "not available" as one atomic step. Two librarians pressing "issue" on the same copy at the same moment: exactly one wins. Same lesson as the Parking Lot and BookMyShow: check-and-act must be ONE step.

2️⃣ Fines are a Strategy
`FinePolicy` is an interface. `PerDayFine` charges per day after a grace period. Tomorrow's "students pay half" or "no fines on Sundays" is a new class, and `LoanService` never changes.

Math: late days − grace days (never below zero) × rate. Returned on the due date? ₹0. Five days late with two grace days at ₹2 a day? 3 × 200 paise = 600 paise.

3️⃣ Returns notify listeners (Observer)
When a copy comes back, `LoanService` tells everyone who registered: "a copy of Dune is back". Hook up a notifier for the first member waiting for that title, and `returnCopy` doesn't change.

4️⃣ Rules in one place
"Max 3 loans" and "14 days" are constants in `LoanService`. Business rules live where the loan is created, not scattered across the UI.

What I'd say honestly about limits:
• a reservation QUEUE (who is first in line) isn't built; the listener is the hook where it would go
• real systems also handle lost books, renewals, and member types
• dates are `LocalDate`: the system is day-based on purpose

Test I like: issue the SAME copy to two members. The second one fails with "Copy already issued". And a member trying a 4th loan is stopped at the service, not in the UI.

If a member could renew a loan once, which class changes? 👇

#LLD #Java #LibraryManagement #DesignPatterns #SystemDesign

**First comment:**

"Answer: `LoanService` gets a `renew(loan)` method, and `Loan` becomes something you can copy with a new due date (it's immutable, so you'd create a new Loan and replace the old one). `BookCopy` and `PerDayFine` don't change at all."

**Notes for you:**
- The repo test checks: due date, double-issue rejection, loan limit, on-time return (no fine), 5-days-late fine, and the return listener.
- In Python/TypeScript the "atomic" part is single-threaded-safe as long as there's no `await` between check and set; the repo code comments say so.

---

# POST 3 — MIX: 10 LLD mistakes (checklist)

**Image:** A clean checklist card, 10 lines, each with a ❌ and a one-word fix.

**Post text:**

10 LLD mistakes I see again and again (and the one-line fix) 👇

1️⃣ ❌ Starting to code in the first minute → ✅ Clarify requirements and list what's out of scope

2️⃣ ❌ Silent assumptions → ✅ Say them out loud: "I'm assuming one building, 3 elevators."

3️⃣ ❌ Every noun becomes a class → ✅ Keep only entities with their own behavior or lifecycle

4️⃣ ❌ Classes that only hold getters and setters → ✅ Put methods where the data lives

5️⃣ ❌ Inheritance for everything → ✅ An enum or an interface is often enough

6️⃣ ❌ A pattern with no pain → ✅ Name the problem it solves, or delete it

7️⃣ ❌ State on the wrong object (e.g. "booked" on the physical seat) → ✅ Put state on the thing that owns the lifecycle

8️⃣ ❌ Check, then act, as two steps → ✅ Make it ONE atomic step

9️⃣ ❌ Money as `double` → ✅ Integers (paise) or a decimal type

🔟 ❌ Never testing a new requirement → ✅ Throw ONE change at your design and count what breaks

Bonus: ❌ Forgetting failures. What happens if payment fails halfway? Name it, even if you don't build it.

If you fix only three: #1 (clarify), #7 (state on the right object), #8 (atomic check-and-act).

Which one have you made? Be honest, I'll go first: #6 😄 👇

#InterviewPrep #LLD #SoftwareEngineering #CleanCode #SystemDesign

**First comment:**

"Every item here comes from a problem in this series: #7 is BookMyShow and Library, #8 is Parking Lot, #9 is Splitwise. The full code for all of them is in the repo."

**Notes for you:**
- "I'll go first: #6" is a light, self-aware line. Change the number to whichever mistake is truly yours, or delete the sentence if none feels honest.
- Every item maps to code in the repo, so you can answer "show me" with a link.

---

# POST 4 — Composite

**Image:** Left = a tree diagram: a folder containing files and another folder, with "size()" arrows showing the sum going up. Right = code, plus a red box "cycle!" showing a folder added into its own child.

**Image code:**

```java
interface FileSystemNode { String name(); long size(); }

class FileNode implements FileSystemNode {
    public long size() { return size; }
}

class DirectoryNode implements FileSystemNode {
    private final List<FileSystemNode> children = new ArrayList<>();

    public long size() {                           // sum of children
        return children.stream().mapToLong(FileSystemNode::size).sum();
    }

    void add(FileSystemNode node) {
        if (node == this || (node instanceof DirectoryNode d && d.contains(this)))
            throw new IllegalArgumentException("cycle");
        children.add(node);
    }
}
```

**Post text:**

LLD Series | Composite

"What's the size of this folder?"

A folder contains files. And other folders. Which contain files and more folders. How deep does it go? Nobody knows.

The naive code:

```
if (node is File) return size;
else for each child: if (child is File) ... else (recurse)
```
`instanceof` checks everywhere. Every new node type touches all of them.

Composite pattern:
👉 Treat a single object and a GROUP of objects through the same interface.

Both `FileNode` and `DirectoryNode` implement `size()`.
• A file returns its own size
• A directory returns the sum of its children's sizes

The caller just says `node.size()`. It never asks "are you a file or a folder?" Recursion does the work.

Where this shows up:
• file systems (obviously)
• UI trees: a panel contains buttons and other panels
• menus with sub-menus
• org charts, permission groups, product categories

The bug nobody warns you about 🐛
What if someone adds a folder INTO ITS OWN CHILD?
`size()` recurses forever. A stack overflow in production.

The fix is in `add()`: before adding a node, check that it doesn't already contain this folder. The test in the repo does exactly this: adding the root into one of its subfolders is rejected.

⚠️ When NOT to use it: if your structure is flat (just a list), a loop is simpler. Composite earns its place when things nest to unknown depth.

Where in your project does something contain "things like itself"? 👇

#LLD #DesignPatterns #CompositePattern #Java #CleanCode

**First comment:**

"Related: a Composite plus the Visitor pattern lets you add new operations (like 'find all .pdf files' or 'total lines of code') without touching the node classes. I'll cover Visitor another time."

**Notes for you:**
- Repo: `patterns/composite`; the cycle test is in all four languages.
- The Visitor mention is a pointer for the future; don't promise a date.
- A folder containing a copy of itself via two paths (shared subfolder) is a separate case (a DAG); the example guards only against true cycles.

---

# POST 5 — MIX: The village bell (Observer parable)

**Image:** Optional. A simple illustration of a village bell, or a quote card with the moral.

**Post text:**

Another story 👇

एक गाँव में एक मुखिया था। (A village had a head man.)

Whenever news arrived, he walked from house to house to tell everyone.

"Rain is coming." He told the farmers. Then he walked to the shopkeepers. Then to the school. By the time he finished, the first farmers had already lost half a day.

When the village grew, it got worse. He spent his whole day walking, and still someone was always the last to hear.

One day a wise woman said:

"Why do you go to every house? Hang a bell in the square. Ring it once. Let everyone who cares about news decide to listen."

The mukhiya hung the bell. When rain came, he rang it once. The farmers ran to cover the crops. The shopkeepers pulled in their goods. The school sent the children home.

Later, the new health clinic wanted to hear the news too. The mukhiya didn't change a thing. The clinic just started listening for the bell.

🔔 The moral: Announce once. Let the interested ones listen.

That's the Observer pattern:
• the bell ringer = the subject: it announces "something happened"
• each person who listens = an observer
• the ringer doesn't know who is listening, and doesn't need to

If you add "notify the doctor" to appointment booking, you don't edit `book()`. You add one more listener.

But the story has a warning too:
If one listener takes forever to respond, in the simple version everyone behind them waits. And if one listener faints (throws an error), the others should still hear the bell. A good bell ringer makes sure of that.

Which story should be next? Strategy ("choosing the road") or State ("the river in four seasons")? 👇

#SoftwareEngineering #DesignPatterns #Storytelling #ObserverPattern #Java

**First comment:**

"Parables so far: the tailor (Open/Closed), the halwai (Single Responsibility), the teacher (Liskov), the duty cards (Interface Segregation), the mill (Dependency Inversion), and now the bell (Observer). Vote for the next one."

**Notes for you:**
- Hindi lines are short; adjust to how you'd naturally say them.
- The "warning" paragraph matches what the Observer code in the repo tests: a failing observer doesn't stop the others.
- Edit the first comment to list only the parables you've actually posted.

---

# POST 6 — Chess (1/2): requirements + design

**Image:** Left = requirements (IN / OUT). Middle = class diagram: `Piece` (abstract) ← `King`, `Queen`, `Rook`, `Bishop`, `Knight`, `Pawn`; `Board` has pieces; `Game` has `Board`. Right = a note: "move legal = piece rule + king not left in check".

**Image content:**

```
IN SCOPE
- 8 x 8 board, two players, turns
- Each piece's own movement rule
- Captures, blocked paths
- Check, checkmate, stalemate

OUT OF SCOPE
- Castling, en passant, promotion
- Clocks, draw by repetition, UI

DESIGN
Piece.canMove(board, from, to)   <- each piece owns its rule
Board.pathClear(from, to)        <- shared by rook/bishop/queen
Game.move(...)                   <- turn, own piece, not own target,
                                    piece rule, king safe afterwards
```

**Post text:**

LLD Series | Chess (1/2)

"Design a chess game." The one everyone fears. It's actually friendly if you split the problem right.

1️⃣ Requirements (what I'd say is in and out)
In: 8×8 board, two players, turns, each piece's movement, captures, check, checkmate, stalemate.
Out: castling, en passant, promotion, clocks, UI.
Saying what's OUT is how you keep this to 45 minutes.

2️⃣ The key design decision
Who knows how a rook moves?
❌ A giant `Game.isValid()` with `switch (pieceType)`. Every new piece variant edits that switch.
✅ Each piece knows its OWN rule: `canMove(board, from, to)`.

`Knight` knows the L-shape. `Rook` knows straight lines. Add a "fairy chess" piece? One new class.

3️⃣ Who knows about blocking?
The board does: `pathClear(from, to)`. Rook, Bishop, and Queen share it. Knights ignore it (they jump).

4️⃣ What makes a move LEGAL?
Not just the piece's rule. A move is legal only if:
✔ it's your turn and your piece
✔ the target isn't your own piece
✔ the piece's rule allows it
✔ after the move, YOUR king isn't in check

That last rule is the one beginners forget. And how do we check it? Simplest way: try the move, ask "is my king attacked now?", then undo it.

5️⃣ Check, checkmate, stalemate
• in check and no legal move → checkmate
• NOT in check and no legal move → stalemate (a draw)
• in check but you can escape → just "check"

Entities: `Game`, `Board`, `Piece` (6 subclasses), `Color`, `Status`.

Next post: the code, and a real game that ends in checkmate in 4 moves.

Which two moves does Black need to checkmate White the fastest? (Hint: it has a famous name.) 👇

#LLD #SystemDesign #Chess #Java #InterviewPrep

**First comment:**

"Answer: Fool's Mate. White plays f3 and g4 (both terrible moves), Black plays e5 and Qh4. It's the shortest possible checkmate, and it's the end-to-end test in the code."

**Notes for you:**
- Repo: `problems/chess`.
- The board uses row 0 = rank 8 (black's side) and row 7 = rank 1 (white's side). White's pawns move to smaller row numbers.
- Be careful to say "out of scope" for castling, en passant and promotion: they're real rules, and chess players will notice.

---

# POST 7 — Chess (2/2): the code + Fool's Mate

**Image:** Top = a small board showing the Fool's Mate final position (Black queen on h4, White king on e1). Below: code card.

**Image code:**

```java
abstract class Piece {
    abstract boolean canMove(Board b, int fr, int fc, int tr, int tc);
}
class Knight extends Piece {
    boolean canMove(Board b, int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
}
class Rook extends Piece {
    boolean canMove(Board b, int fr, int fc, int tr, int tc) {
        return (fr == tr || fc == tc) && b.pathClear(fr, fc, tr, tc);
    }
}

// In Game: is this move legal?
board.set(tr, tc, p); board.set(fr, fc, null);   // try it
boolean safe = !isInCheck(color);                // is my king attacked now?
board.set(fr, fc, p); board.set(tr, tc, target); // undo it
```

**Post text:**

LLD Series | Chess (2/2): the code

The whole design fits in a few ideas 👇

1️⃣ Each piece owns its rule
`Knight.canMove` is two lines. `Rook.canMove` is "same row or same column, and nothing in between". The `Game` never asks "what type is this piece?"

2️⃣ `pathClear()` is shared
Walk square by square from start to target. If anything is in the way, stop. Rook, Bishop and Queen all use it.

3️⃣ Legality = try it, then undo it
To check if your move leaves your king in check, play it on the board, ask `isInCheck(color)`, and put everything back. Simple, and impossible to forget a case, because it uses the same attack rules as real play.

4️⃣ Checkmate is just "no legal move while in check"
`status()` asks: am I in check? Do I have ANY legal move? Both yes/no answers give four outcomes: ongoing, check, checkmate, stalemate.

The test that ties it all together: Fool's Mate ♟️
1. f2-f3, e7-e5
2. g2-g4, Qd8-h4
After Qh4, White's king on e1 is attacked along the diagonal. Nothing can block, nothing can capture, the king can't move. `status()` returns CHECKMATE, and even a harmless pawn move is rejected.

Other tests in the repo:
• a pawn can't jump three squares
• a rook can't jump over its own pawn
• Black can't move on White's turn
• a piece must actually move

⚠️ Honest limits:
• `hasLegalMove` tries every from-square and to-square combination (about 4,000 per check). Fine for a demo; a real engine generates moves per piece instead
• castling, en passant, and promotion aren't there. Each is a rule that needs extra state (has the king moved? what was the last move?), so they'd be good extension exercises
• no move history, so no undo and no draw by repetition (that's the Command pattern's job)

Extension test: where would "pawn promotion" go? 👇

#LLD #Java #Chess #DesignPatterns #SystemDesign

**First comment:**

"Answer: `Game.move()` is where it goes. After a pawn reaches the last row, replace it with a Queen (or let the player choose). `Pawn`, `Board` and all other pieces stay the same. And castling would live in `King` plus a flag on `Game`."

**Notes for you:**
- Repo: `problems/chess`. Fool's Mate and the illegal-move checks are tested in Java, C#, Python and TypeScript.
- For the board image, draw only the final position (pieces that matter: black queen h4, white king e1, white pawns f3 and g4). A clean 8×8 grid with a few pieces beats a full board.
- The "about 4,000" figure is 64 × 64 combinations per color check; say "thousands" if you prefer a looser phrase.

---

## Next batches

See `docs/ROADMAP.md` in the repo for the plan beyond this batch.
