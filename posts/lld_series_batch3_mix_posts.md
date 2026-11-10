# LLD Series — Batch 3 (7 posts, Technical + Mix)

**Pattern of this batch:** 4 technical LLD posts + 3 mix posts (personal, opinion, story). Mix posts break the series rhythm so your feed doesn't feel like a textbook, and they bring in people who'd never click a "Singleton" post.
**Series tag:** only on technical posts: `LLD Series | Part X`. Mix posts have no tag.
**Image rule for technical posts:** one portrait card (1080×1350). Top = class diagram, bottom = code (max ~15 lines).
**For mix posts:** images are optional. A plain text post often performs better. If you want one, a simple quote card or a photo of your own setup works.

**⚠️ Fill-ins:** the mix posts are drafts built on what's true for you in general. Anything in `[square brackets]` is a spot where YOUR real detail makes the post 10x better. Replace those, or delete the line. Don't post a made-up story.

---

## What makes this batch stand out

**Post 1 (Builder 1)** — Starts from the ugly constructor call everyone has written, so the pattern feels like relief, not theory.

**Post 2 (Mix: learning LLD in public)** — Honest about being early-career and teaching anyway. This is the post that makes people follow YOU, not just the series.

**Post 3 (Builder 2)** — Lombok `@Builder` is what people actually use, with the three traps nobody warns you about.

**Post 4 (Singleton)** — Opinionated take: Spring made hand-written Singletons mostly unnecessary. Controversial enough to get comments, correct enough to be safe.

**Post 5 (Mix: things nobody tells you about learning patterns)** — The "things nobody posts about" format again, which was your most honest post last time.

**Post 6 (Decorator)** — Wrapper chain for logging + caching. The Java I/O stream example makes it click.

**Post 7 (Mix: parable)** — A short moral story that teaches Open/Closed. Ties directly to your Hindi stories channel and nobody else on LinkedIn is doing this.

---

## 7-day schedule

| Day | Post | Slot |
|---|---|---|
| Tue 27 Oct | Builder Part 1 | 9am IST |
| Wed 28 Oct | MIX: Learning LLD in public | 9am IST |
| Thu 29 Oct | Builder Part 2 (Lombok) | 9am IST |
| Fri 30 Oct | Singleton | 9am IST |
| Mon 2 Nov | MIX: Things nobody tells you about learning patterns | 9am IST |
| Tue 3 Nov | Decorator | 9am IST |
| Wed 4 Nov | MIX: The tailor's story (Open/Closed parable) | 9am IST |

---

# POST 1 — Builder Pattern, Part 1

**Image:** Left = the messy constructor call with arguments labeled in red as "which is which?". Right = the fluent builder call. Class diagram below: `Appointment` ← nested `Builder`.

**Image code:**

```java
// ❌ Which boolean is which?
new Appointment("P1", "D7", time, null, null, true, false);

// ✅ Reads like a sentence
Appointment a = Appointment.builder()
    .patientId("P1")
    .doctorId("D7")
    .time(time)
    .reminder(true)
    .build();
```

```java
class Appointment {
    private final String patientId, doctorId, notes;
    private final LocalDateTime time;
    private final boolean reminder;

    private Appointment(Builder b) {
        patientId = b.patientId; doctorId = b.doctorId;
        time = b.time; notes = b.notes; reminder = b.reminder;
    }
    static Builder builder() { return new Builder(); }

    static class Builder {
        private String patientId, doctorId, notes;
        private LocalDateTime time;
        private boolean reminder;

        Builder patientId(String v) { patientId = v; return this; }
        Builder doctorId(String v) { doctorId = v; return this; }
        Builder time(LocalDateTime v) { time = v; return this; }
        Builder reminder(boolean v) { reminder = v; return this; }

        Appointment build() {
            if (patientId == null || doctorId == null || time == null)
                throw new IllegalStateException("Missing required field");
            return new Appointment(this);
        }
    }
}
```

**Post text:**

LLD Series | Part 15 — Builder Pattern (1/2)

Quick quiz. What does this do?

`new Appointment("P1", "D7", time, null, null, true, false)`

Which `null` is the notes? Which `true` is the reminder? Which `false` is... something?

Nobody knows without opening the class. And if someone swaps two arguments of the same type, it compiles fine and fails at 2 AM.

This is the "telescoping constructor" problem. Every new optional field means another constructor overload.

Builder pattern:
👉 Build a complex object step by step, with named steps, and create it only at the end.

Same object, now:

```
Appointment.builder()
    .patientId("P1")
    .doctorId("D7")
    .time(time)
    .reminder(true)
    .build();
```

It reads like a sentence. No mystery `null`s.

What you get:
• readable calls (names instead of positions)
• optional fields are truly optional
• validation lives in ONE place: `build()`
• the final object can be immutable (all fields `final`)

Structure: the class has a private constructor + a nested `Builder`. The builder collects values and hands them over in `build()`.

⚠️ When NOT to use it: for 2–3 required fields, a normal constructor is simpler. Builder earns its place at 4+ fields, or when several are optional.

Part 2: how to get this for free with Lombok (and the 3 traps).

When did you last stare at a constructor call and have to count arguments? 👇

#LLD #DesignPatterns #BuilderPattern #Java #CleanCode

**First comment:**

"The real win is `build()` as a gatekeeper. If an object can only be created through `build()`, then it's impossible to hold a half-valid Appointment anywhere else in the code. Invalid state can't exist, so you stop writing null checks everywhere."

**Notes for you:**
- Returning `this` from each setter is what enables chaining (a "fluent" interface).
- Private constructor on the outer class forces everyone through the builder.

---

# POST 2 — MIX: Learning LLD in public

**Image:** None, or a screenshot of your own notes/whiteboard if you have one.

**Post text:**

I'm [about 1.5 years] into my career and I'm teaching Low-Level Design on LinkedIn.

Some people will think that's backwards. "Shouldn't a senior be doing this?"

Here's why I'm doing it anyway:

1️⃣ Teaching exposes the gaps
You think you understand the Liskov Substitution Principle. Then you try to explain it in 150 words with an example and realize you've been nodding at a definition. Writing the post is the exam.

2️⃣ Beginners explain things better
I just finished learning this. I remember exactly which part was confusing. A person with 15 years has forgotten what it felt like.

3️⃣ I'm not claiming to know it all
Every post is "here's what I understood, with code." If I'm wrong, someone will tell me, and that's a free correction.

4️⃣ It forces consistency
Seven posts a week need seven ideas a week. That's a better study plan than anything I'd have made for myself.

What I've noticed so far:
• the posts I was most nervous about got the best conversations
• half the comments teach ME something
• I now read code at work differently. I find myself thinking "which principle is this breaking?"

If you're early in your career and you've been waiting until you "know enough" to share what you're learning, you won't feel ready. Nobody does.

Pick one thing you learned this week. Explain it like you're helping a friend. Post it.

Who else is learning in public right now? Drop your topic, I'd like to follow along 👇

#LearningInPublic #SoftwareEngineering #CareerGrowth #Java #LLD

**First comment:**

"If you're learning something right now and want an accountability partner, comment your topic. I'll check back in a month and ask how it went."

**Notes for you:**
- Replace `[about 1.5 years]` with how you want to say it (or remove the number).
- Add ONE real example from your own learning: a concept that finally clicked, or a comment that corrected you. One real detail beats three generic paragraphs.
- Don't claim "best conversations" unless it's true for you; change that line to match what's actually happened.

---

# POST 3 — Builder Pattern, Part 2 (Lombok)

**Image:** Left = the Lombok class (short). Right = a "traps" panel listing the three gotchas, each with a tiny code snippet. Diagram not needed; a clean two-column card works.

**Image code:**

```java
@Value                       // all fields final, getters, no setters
@Builder(toBuilder = true)
class Appointment {
    @NonNull String patientId;   // NPE at build() if null
    @NonNull String doctorId;
    @NonNull LocalDateTime time;
    String notes;
    @Builder.Default boolean reminder = true;   // default value
}

// Copy with one change
Appointment moved = a.toBuilder().time(newTime).build();
```

**Post text:**

LLD Series | Part 16 — Builder Pattern (2/2)

Last time you saw the Builder written by hand: ~30 lines of boilerplate.

In real projects, nobody writes that. You add one annotation:

`@Builder`

Lombok generates the entire builder at compile time. Your class shrinks to the fields and nothing else.

But Lombok has 3 traps that bite people 👇

🔸 Trap 1: Required fields aren't required
By default, `@Builder` lets you skip any field. It silently sets it to `null`.
Fix: mark must-have fields `@NonNull`. Now `build()` throws immediately if one is missing.

🔸 Trap 2: Defaults get ignored
Writing `boolean reminder = true;` in the class does NOT work with `@Builder`; the builder sets it to `false`.
Fix: `@Builder.Default boolean reminder = true;`

🔸 Trap 3: JPA entities
Hibernate needs a no-arg constructor. `@Builder` alone doesn't give you one.
Fix: add `@NoArgsConstructor` + `@AllArgsConstructor`, or better, keep builders on your DTOs and keep entities simple.

Bonus feature: `toBuilder = true`
It lets you copy an immutable object with one change:
`a.toBuilder().time(newTime).build()`
No setters. No mutation. The original stays untouched.

💡 Java records are great for simple immutable data, but they don't have a builder or default values. When you need those, Builder still wins.

Do you use Lombok on entities, DTOs, or both? 👇

#LLD #SpringBoot #Lombok #Java #BackendEngineering

**First comment:**

"Honest take: I like Lombok `@Builder` for request/response DTOs and test data. For domain objects with real business rules, I'd rather write `build()` by hand so the validation is visible and testable."

**Notes for you:**
- `@Value` makes the class final and fields private final. Good for immutable DTOs.
- Try the traps yourself: create the class, skip a field, print the object. Seeing the silent `null` is more convincing than reading about it.

---

# POST 4 — Singleton Pattern

**Image:** Left = three small code panels: "Naive (not thread-safe)", "Holder idiom ✅", "enum ✅". Right = a Spring box labeled "Container" with one bean inside and many arrows pointing to it ("one per container, not per JVM").

**Image code:**

```java
// ❌ Two threads can create two instances
class Config {
    private static Config instance;
    static Config get() {
        if (instance == null) instance = new Config();
        return instance;
    }
}

// ✅ Holder idiom: lazy + thread-safe, no locks
class Config {
    private Config() {}
    private static class Holder {
        static final Config INSTANCE = new Config();
    }
    static Config getInstance() { return Holder.INSTANCE; }
}

// ✅ Simplest and safe
enum Registry { INSTANCE; }
```

**Post text:**

LLD Series | Part 17 — Singleton Pattern

Singleton: only one instance of a class ever exists, with one global way to get it.

Everyone knows the idea. Most people write it wrong.

The classic version has a bug:

```
if (instance == null) instance = new Config();
```

Two threads hit that line at the same time → two instances. The "single" object isn't single.

Safe ways in Java:
✅ Holder idiom: a private static inner class holds the instance. The JVM guarantees class loading is thread-safe, so no locks needed. Lazy too.
✅ `enum` singleton: one-line, thread-safe, and safe against reflection and serialization tricks.
✅ Double-checked locking works, but only if the field is `volatile`. Easy to get wrong.

Now the opinionated part 🔥

If you're writing Spring Boot, you'll almost never hand-write a Singleton.

Spring beans are singleton-scoped by default. One instance per application context, injected wherever needed.

Two big differences:
• Spring's singleton is "one per container", not "one per JVM"
• It's injected, not fetched via a static method. So it can be mocked in tests.

Why people criticize the classic version:
• hidden dependency: any code can call `Config.getInstance()` without it showing up in a constructor
• hard to test, you can't swap it
• becomes global mutable state, and shared state is where concurrency bugs live

⚠️ The rule that matters: a singleton bean must be stateless or thread-safe. Putting a mutable `currentUser` field on a `@Service` is a classic production bug. Every request shares that one instance.

Use a Singleton for: config, connection pools, caches, registries.
Avoid it for: anything holding per-request or per-user data.

Do you think Singleton is a pattern or an anti-pattern? I want to see both camps 👇

#LLD #DesignPatterns #Singleton #SpringBoot #Java

**First comment:**

"Interview follow-up they love: 'How would you break a Singleton?' Answers: reflection (calling the private constructor), serialization (deserializing creates a new object), and multiple class loaders. The enum version defends against the first two."

**Notes for you:**
- Say "Spring's default scope is singleton" only for beans; `@Scope("prototype")` and request scope exist for the cases where you need a new object each time.
- Test it yourself: put a mutable field on a `@RestController`, call it from two requests, and watch values leak between them.

---

# POST 5 — MIX: Things nobody tells you about learning design patterns

**Image:** None. This one works as pure text.

**Post text:**

Things nobody tells you about learning design patterns:

• You'll over-apply them for the first few months. Everything starts looking like a Factory. It's a phase, and it passes.

• Most pattern names are less important than the problem they solve. If you can explain the pain, you can usually reinvent the pattern. In an interview, that matters more than reciting the name.

• Real code mixes patterns without labels. Nobody on your team says "this is a Decorator wrapping an Observer." They say "the thing that logs and notifies."

• Frameworks have already done a lot of it for you. Spring's dependency injection, Java's I/O streams, event listeners. You use patterns daily before you can name them.

• The hardest skill isn't applying a pattern. It's deciding NOT to. Two `if` branches don't need a Strategy. A class with one implementation doesn't need an interface.

• Reading good code teaches more than reading pattern books. Open a well-known library on GitHub and look for the shape.

• You'll understand a pattern properly only after you've used it wrongly once.

• Diagrams look clean. Production code never does. It's okay if your version doesn't match the textbook.

• Nobody remembers all 23 GoF patterns. The people who seem to are just very familiar with the six they use constantly.

They just don't post about it.

What's one thing about design patterns you wish someone told you earlier? 👇

#SoftwareEngineering #DesignPatterns #LearningInPublic #CareerAdvice #Java

**First comment:**

"The one that changed how I write code: [your own lesson, e.g., a time you added a pattern too early and had to undo it, or a refactor that made code simpler]. Fill this with a real one-liner."

**Notes for you:**
- This mirrors your "things nobody posts about" post, which was the most honest in your earlier batch.
- The first comment is where you add a real moment. If you don't have one, delete it and reply to commenters instead.
- The line "They just don't post about it" echoes your earlier post on purpose, so regulars will notice the callback.

---

# POST 6 — Decorator Pattern

**Image:** Left = layered "onion" diagram: `LoggingRepository` wraps `CachingRepository` wraps `DbRepository`, all implementing `AppointmentRepository`. Right = code.

**Image code:**

```java
interface AppointmentRepository {
    Appointment findById(long id);
}

class DbRepository implements AppointmentRepository { /* real query */ }

class CachingRepository implements AppointmentRepository {
    private final AppointmentRepository inner;
    private final Map<Long, Appointment> cache = new ConcurrentHashMap<>();

    CachingRepository(AppointmentRepository inner) { this.inner = inner; }

    public Appointment findById(long id) {
        return cache.computeIfAbsent(id, inner::findById);
    }
}

class LoggingRepository implements AppointmentRepository {
    private final AppointmentRepository inner;
    LoggingRepository(AppointmentRepository inner) { this.inner = inner; }

    public Appointment findById(long id) {
        long t = System.nanoTime();
        try { return inner.findById(id); }
        finally { log.info("findById({}) took {} µs", id, (System.nanoTime() - t) / 1000); }
    }
}

AppointmentRepository repo =
    new LoggingRepository(new CachingRepository(new DbRepository()));
```

**Post text:**

LLD Series | Part 18 — Decorator Pattern

You want to add caching to your repository.

Then logging.
Then both.
Then logging + caching + retry.

If you solve this with inheritance, you get `CachingRepository`, `LoggingRepository`, `CachingLoggingRepository`, `LoggingRetryRepository`... The class count explodes with every combination.

Decorator pattern:
👉 Wrap an object in another object that has the SAME interface, and add behavior before or after delegating.

Look at the image. `DbRepository` does the real work. `CachingRepository` wraps it. `LoggingRepository` wraps that. All three implement `AppointmentRepository`, so the caller can't tell the difference.

```
new LoggingRepository(new CachingRepository(new DbRepository()))
```

Want caching without logging? Remove one wrapper. Want a different order? Rearrange. No new classes.

You've used this already:
```
new BufferedReader(new InputStreamReader(new FileInputStream(f)))
```
That's three decorators stacked in Java's own I/O library.

⚠️ Order matters: logging outside caching logs every call, including cache hits. Logging inside logs only real DB hits. Decide what you actually want to measure.

⚠️ Decorator vs Proxy: structure is nearly identical. Decorator ADDS behavior. Proxy CONTROLS access (lazy-loading, permissions). Same shape, different intent.

In Spring Boot, `@Cacheable` and `@Transactional` do this for you with proxies, which is why they stop working when you call a method from inside the same class. Now you know why 👇

Have you ever hit the "self-invocation" bug with `@Transactional` or `@Cacheable`?

#LLD #DesignPatterns #DecoratorPattern #SpringBoot #Java

**First comment:**

"The self-invocation bug is Decorator in disguise. Spring wraps your bean in a proxy. When `methodA()` calls `this.methodB()` directly, it skips the wrapper completely, so `@Transactional` on `methodB` never runs. Call it through another bean and it works."

**Notes for you:**
- Key rule: a decorator and the thing it wraps share the SAME interface. That's what makes them stackable.
- `computeIfAbsent` with a function that returns `null` stores nothing; fine for the demo, but mention "cache misses for missing rows" if someone asks.
- Try it: write the three classes and swap the wrapper order to see the log difference.

---

# POST 7 — MIX: The tailor's story (Open/Closed parable)

**Image:** Optional. A simple illustration or a quote card with the moral line. You can also add the Open/Closed code diagram from Part 3 as a second image.

**Post text:**

A story I'd like to tell 👇

एक गाँव में एक दर्ज़ी था। (There was a tailor in a village.)

He made a perfect shirt for a customer. The customer loved it. Word spread.

Next week, someone asked: "Can you add a pocket?"
The tailor didn't add a pocket. He tore the whole shirt apart and re-stitched it from scratch, with a pocket.

The pocket was done. But now the collar was crooked. He fixed the collar. The sleeve came loose.

Next month, someone asked for buttons. He tore it apart again.

By the end of the year, the tailor was exhausted. Every small request risked ruining something that already worked.

An old master tailor visited. He said:

"A good tailor makes the shirt so that new things can be ADDED, a pocket, a button, an embroidery, without opening the seams that already hold."

The tailor learned to build shirts with a clear pattern: collar, sleeves, body each separate. Now adding a pocket was just sewing one more piece.

🧵 The moral: Don't rewrite what already works. Build so you can add.

That story is the Open/Closed Principle from Part 3 of this series:
👉 Open for extension. Closed for modification.

The tailor tearing the shirt apart = the developer editing a tested `if-else` every time a new requirement arrives.
The master's way = an interface and one new class per feature.

Software principles are old wisdom in new clothes. Good design has always been about not breaking what works.

[If you want: I run a Hindi stories channel and love explaining ideas this way. Would you like more tech concepts as short stories?]

Which principle should I turn into a story next? 👇

#SoftwareEngineering #DesignPrinciples #Storytelling #SOLID #Java

**First comment:**

"If you'd like the full SOLID series I posted earlier, it's pinned on my profile. Next story: Single Responsibility, 'the cook who also ran the shop.'"

**Notes for you:**
- Change the bracketed line to how you want to mention your channel. A link in the FIRST COMMENT, not in the post body, protects reach.
- Keep the Hindi line or remove it; a bit of Hindi gives the post your personal voice. Keep whichever feels natural to you.
- Check the pinned-post claim before posting. Pin your SOLID post first, or remove that line.
- This format is repeatable: a story for each remaining principle (SRP, LSP, ISP, DIP) is four more posts you could do between technical ones.

---

## Next batch (preview)

1. Parking Lot, Part 1: requirements and entities (use the 6-step framework from Part 1 of the series)
2. MIX: What I got wrong in my first LLD attempt (fill with your real experience)
3. Parking Lot, Part 2: classes and code
4. Parking Lot, Part 3: apply Strategy (pricing) and Factory (vehicles/slots)
5. MIX: Story post (SRP: "the cook who ran the shop")
6. Parking Lot extension test: add EV charging slots without touching old code
7. Recap: LLD cheat sheet (all SOLID + 7 patterns on one image), a high-save post
