# Roadmap: how long the series runs and what comes next

## Where things stand

| Phase | What | Status |
|---|---|---|
| 1. Foundations | The 6-step framework, SOLID (5 principles), a challenge and its answers | written (batches 1-2) |
| 2. Core patterns | Strategy, Factory, Observer, Builder, Singleton, Decorator, State, Command, Chain of Responsibility, Proxy, Adapter | written (batches 2-7) |
| 3. Full problems | Parking Lot, Splitwise, BookMyShow, Elevator, Rate Limiter | written (batches 4-7) |
| 4. More patterns and problems | Template Method, Facade, Composite, ATM, Library Management, Chess | written (batches 8-9) |
| 5-10 | See the plan below | planned |

Everything marked "written" has runnable code in this repo in four languages.

## How long the series runs

- Planned length: **about 15 batches of 7 posts, roughly 105 posts.**
- At 5 posts a week (Mon-Fri) that is **about 21 weeks, a little under 5 months.**
- Already written: 9 batches (about 12-13 weeks of posting). Remaining: 6 batches (about 8-9 weeks).
- It is fine to stop early or stretch it. The batches are independent, so the series never ends mid-thought.

## The plan for batches 10 to 15

| Batch | Theme | Posts (sketch) |
|---|---|---|
| **10** | Games and small patterns | Tic-Tac-Toe (win-check as a Strategy, then N x N), Snake and Ladder, Memento (undo via snapshots), Mediator (chat room), 2 mix posts |
| **11** | Backend building blocks | LRU Cache (hash map + linked list, eviction as a Strategy, thread safety), Logger framework (Singleton + Chain + Strategy), Pub-Sub / message queue, 1 mix post |
| **12** | Concurrency in design | Producer-consumer and a bounded blocking queue, read-write locks, deadlock walk-through, idempotency, "one atomic step" recap, 1-2 mix posts |
| **13** | Patterns inside real frameworks | Where each pattern hides in Spring and ASP.NET Core, Hexagonal architecture, Outbox and Saga (link your own earlier content), CQRS-lite, 1 mix post |
| **14** | Interview toolkit | A full 45-minute mock walk-through of one new problem (e.g. food delivery), a Q&A bank, a "common mistakes v2" post, cheat sheet v3 |
| **15** | Capstone and wrap-up | Build one small system from scratch in public using many patterns (e.g. an API request pipeline: rate limiter + chain + proxy + decorator + builder). Your own side project can be the vehicle. Then a retrospective post and "what's next" (a system design / HLD series) |

Each full problem gets 2 posts (design, then code), each pattern gets 1-2 posts, and roughly every third post is a mix post (story, reflection, checklist, Q&A).

## Posting rhythm that keeps quality up

| Day | Type |
|---|---|
| Mon | Technical, part 1 of a topic |
| Tue | Mix (story, reflection, or checklist) |
| Wed | Technical, part 2 (code) |
| Thu | Technical or a pattern |
| Fri | Interactive (challenge, poll, Q&A, "spot the bug") |

Rules that worked in the design of this series:
1. One idea per post, one before/after, one image, one question to the readers.
2. Every full problem ends with the **extension test** (add one requirement, count what breaks).
3. Every pattern post says **when NOT to use it**.
4. Honest limits go in the post ("the greedy chain fails here", "this is a heuristic"). Credibility beats polish.
5. Put links in the first comment, not the post body.

## Plan additions (ideas worth doing next)

**Repo**
1. **Practice katas:** for each problem, a `kata/` folder with starter code and *failing* tests that readers make pass. (Turns readers into contributors.)
2. **Diagrams as code:** a Mermaid class diagram per topic in `docs/diagrams/`, so the post images and the repo stay in sync.
3. **More languages:** Go and Kotlin ports (accept community pull requests, keep each example self-checking).
4. **Spring Boot sample app:** one small app that shows several patterns wired together the way real projects do it.
5. **One-click environment:** a dev container / Codespaces config so anyone can run all four languages in the browser.
6. **CI badge and a test per new example** (the workflow already runs all four languages).

**Content**
7. **Code-card carousels:** turn each technical post's image into a swipeable PDF carousel (LinkedIn document posts).
8. **Hindi short videos:** each parable becomes a short Hindi explainer on your stories channel, with the code card at the end.
9. **A downloadable PDF / small e-book** compiled from the posts, offered once the series passes the halfway mark.
10. **Reader-submitted design reviews:** one post a month where you review a reader's class diagram (with permission).
11. **Live mock interview** (or a recorded one) using a problem not yet in the series.

**Measuring what works**
12. Keep a simple sheet, one row per post: impressions, comments, saves, profile views, repo stars, post type. Every 4 weeks, double down on the post types that win and cut the ones that don't. Don't trust a single post.

## Risks to watch

- **Burnout:** 5 posts a week is a lot. Batches are written ahead on purpose; keep a buffer of one full batch.
- **Accuracy:** a wrong claim in a technical series is costly. Run the repo before posting, and keep a short corrections log. Thank readers who fix you.
- **Confidential material:** never use real company code, names, or data in examples. Keep stories generic.
- **Sameness:** the mix posts exist to prevent it. If three technical posts in a row feel heavy, swap in a story.

## Next three actions

1. Publish the repo (see the README) and put the real link in the first comment of the cheat-sheet post.
2. Pick the problem for batch 10 from a reader poll (Tic-Tac-Toe, Snake and Ladder, or Memento first).
3. Write one practice kata (start with Parking Lot) as the first repo addition.
