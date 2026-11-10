package lld.problems.bookmyshow;

import lld.common.Check;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

enum SeatStatus { AVAILABLE, LOCKED, BOOKED }

class SeatUnavailableException extends RuntimeException {
    SeatUnavailableException(String seatId) { super("Seat unavailable: " + seatId); }
}

/** A seat FOR ONE SHOW. (The physical Seat has no status; availability belongs to the show.) */
class ShowSeat {
    private final String seatId;
    private SeatStatus status = SeatStatus.AVAILABLE;
    private String lockedBy;
    private Instant lockExpiry;
    ShowSeat(String seatId) { this.seatId = seatId; }

    synchronized boolean tryLock(String userId, Instant now, Duration hold) {
        if (status == SeatStatus.LOCKED && now.isAfter(lockExpiry)) status = SeatStatus.AVAILABLE;   // expired lock is free
        if (status != SeatStatus.AVAILABLE) return false;
        status = SeatStatus.LOCKED;
        lockedBy = userId;
        lockExpiry = now.plus(hold);
        return true;
    }

    synchronized boolean confirm(String userId, Instant now) {
        if (status != SeatStatus.LOCKED || !userId.equals(lockedBy) || now.isAfter(lockExpiry)) return false;
        status = SeatStatus.BOOKED;
        return true;
    }

    synchronized void release(String userId) {
        if (status == SeatStatus.LOCKED && userId.equals(lockedBy)) status = SeatStatus.AVAILABLE;
    }

    synchronized SeatStatus status() { return status; }
    String seatId() { return seatId; }
}

class Show {
    private final Map<String, ShowSeat> seats = new LinkedHashMap<>();
    Show(List<String> seatIds) { for (String id : seatIds) seats.put(id, new ShowSeat(id)); }
    ShowSeat seat(String id) {
        ShowSeat s = seats.get(id);
        if (s == null) throw new IllegalArgumentException("No such seat: " + id);
        return s;
    }
}

record Booking(String id, String userId, List<String> seatIds) {}

class BookingService {
    private static final Duration HOLD = Duration.ofMinutes(5);
    private final Supplier<Instant> clock;
    BookingService(Supplier<Instant> clock) { this.clock = clock; }

    /** All-or-nothing. Each tryLock holds ONE seat's lock briefly and never waits, so no deadlock. */
    List<ShowSeat> lockSeats(Show show, List<String> seatIds, String userId) {
        List<ShowSeat> locked = new ArrayList<>();
        for (String id : seatIds) {
            ShowSeat s = show.seat(id);
            if (s.tryLock(userId, clock.get(), HOLD)) locked.add(s);
            else {
                locked.forEach(l -> l.release(userId));    // roll back what we already took
                throw new SeatUnavailableException(id);
            }
        }
        return locked;
    }

    Booking confirm(Show show, List<String> seatIds, String userId) {
        List<ShowSeat> seats = seatIds.stream().map(show::seat).toList();
        for (ShowSeat s : seats) {
            if (!s.confirm(userId, clock.get())) throw new SeatUnavailableException(s.seatId());
        }
        return new Booking(UUID.randomUUID().toString(), userId, seatIds);
    }
}

public class Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("BookMyShow: timed seat locks, all-or-nothing, no double booking");
        Instant[] now = { Instant.parse("2026-11-20T18:00:00Z") };
        BookingService service = new BookingService(() -> now[0]);
        Show show = new Show(List.of("C7", "C8", "C9"));

        service.lockSeats(show, List.of("C7", "C8"), "alice");
        Check.that(show.seat("C7").status() == SeatStatus.LOCKED, "alice holds C7 and C8");
        Check.throwsError(() -> service.lockSeats(show, List.of("C7"), "bob"),
            SeatUnavailableException.class, "bob cannot take a locked seat");

        Check.throwsError(() -> service.lockSeats(show, List.of("C9", "C8"), "bob"),
            SeatUnavailableException.class, "bob wants C9 + C8: fails on C8");
        Check.that(show.seat("C9").status() == SeatStatus.AVAILABLE, "all-or-nothing: C9 was released, not left half-held");

        Booking b = service.confirm(show, List.of("C7", "C8"), "alice");
        Check.that(show.seat("C7").status() == SeatStatus.BOOKED && b.seatIds().size() == 2, "alice confirms within the hold time");
        Check.throwsError(() -> service.confirm(show, List.of("C9"), "bob"),
            SeatUnavailableException.class, "cannot confirm seats you never locked");

        service.lockSeats(show, List.of("C9"), "bob");
        now[0] = now[0].plus(Duration.ofMinutes(6));
        Check.throwsError(() -> service.confirm(show, List.of("C9"), "bob"),
            SeatUnavailableException.class, "an expired lock cannot be confirmed");
        service.lockSeats(show, List.of("C9"), "carol");
        Check.that(show.seat("C9").status() == SeatStatus.LOCKED, "after expiry the seat is free again for carol");

        // Concurrency: 100 users click the same seat at once. Exactly one wins.
        Show hot = new Show(List.of("A1"));
        BookingService live = new BookingService(Instant::now);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger winners = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final String user = "user" + i;
            fs.add(pool.submit(() -> {
                try { live.lockSeats(hot, List.of("A1"), user); winners.incrementAndGet(); }
                catch (SeatUnavailableException ignored) { }
            }));
        }
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        Check.that(winners.get() == 1, "100 simultaneous clicks, exactly one lock winner");
    }
}
