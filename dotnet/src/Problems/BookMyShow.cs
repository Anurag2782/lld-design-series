using Lld.Common;

namespace Lld.Problems.BookMyShow;

enum SeatStatus { Available, Locked, Booked }

class SeatUnavailableException(string seatId) : Exception("Seat unavailable: " + seatId);

/// <summary>A seat FOR ONE SHOW. (The physical Seat has no status; availability belongs to the show.)</summary>
class ShowSeat(string seatId)
{
    private readonly object _lock = new();
    private SeatStatus _status = SeatStatus.Available;
    private string? _lockedBy;
    private DateTimeOffset _lockExpiry;
    public string SeatId { get; } = seatId;

    public bool TryLock(string userId, DateTimeOffset now, TimeSpan hold)
    {
        lock (_lock)
        {
            if (_status == SeatStatus.Locked && now > _lockExpiry) _status = SeatStatus.Available;   // expired lock is free
            if (_status != SeatStatus.Available) return false;
            _status = SeatStatus.Locked;
            _lockedBy = userId;
            _lockExpiry = now + hold;
            return true;
        }
    }

    public bool Confirm(string userId, DateTimeOffset now)
    {
        lock (_lock)
        {
            if (_status != SeatStatus.Locked || userId != _lockedBy || now > _lockExpiry) return false;
            _status = SeatStatus.Booked;
            return true;
        }
    }

    public void Release(string userId)
    {
        lock (_lock) { if (_status == SeatStatus.Locked && userId == _lockedBy) _status = SeatStatus.Available; }
    }

    public SeatStatus Status { get { lock (_lock) return _status; } }
}

class Show(IEnumerable<string> seatIds)
{
    private readonly Dictionary<string, ShowSeat> _seats = seatIds.ToDictionary(id => id, id => new ShowSeat(id));
    public ShowSeat Seat(string id) => _seats.TryGetValue(id, out var s) ? s : throw new ArgumentException("No such seat: " + id);
}

record Booking(string Id, string UserId, List<string> SeatIds);

class BookingService(Func<DateTimeOffset> clock)
{
    private static readonly TimeSpan Hold = TimeSpan.FromMinutes(5);

    /// <summary>All-or-nothing. Each TryLock holds ONE seat's lock briefly and never waits, so no deadlock.</summary>
    public List<ShowSeat> LockSeats(Show show, List<string> seatIds, string userId)
    {
        var locked = new List<ShowSeat>();
        foreach (var id in seatIds)
        {
            var s = show.Seat(id);
            if (s.TryLock(userId, clock(), Hold)) locked.Add(s);
            else
            {
                foreach (var l in locked) l.Release(userId);     // roll back what we already took
                throw new SeatUnavailableException(id);
            }
        }
        return locked;
    }

    public Booking Confirm(Show show, List<string> seatIds, string userId)
    {
        foreach (var s in seatIds.Select(show.Seat))
            if (!s.Confirm(userId, clock())) throw new SeatUnavailableException(s.SeatId);
        return new Booking(Guid.NewGuid().ToString(), userId, seatIds);
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("BookMyShow: timed seat locks, all-or-nothing, no double booking");
        var now = DateTimeOffset.Parse("2026-11-20T18:00:00Z");
        var service = new BookingService(() => now);
        var show = new Show(new[] { "C7", "C8", "C9" });

        service.LockSeats(show, new() { "C7", "C8" }, "alice");
        Check.That(show.Seat("C7").Status == SeatStatus.Locked, "alice holds C7 and C8");
        Check.Throws<SeatUnavailableException>(() => service.LockSeats(show, new() { "C7" }, "bob"), "bob cannot take a locked seat");

        Check.Throws<SeatUnavailableException>(() => service.LockSeats(show, new() { "C9", "C8" }, "bob"), "bob wants C9 + C8: fails on C8");
        Check.That(show.Seat("C9").Status == SeatStatus.Available, "all-or-nothing: C9 was released, not left half-held");

        var b = service.Confirm(show, new() { "C7", "C8" }, "alice");
        Check.That(show.Seat("C7").Status == SeatStatus.Booked && b.SeatIds.Count == 2, "alice confirms within the hold time");
        Check.Throws<SeatUnavailableException>(() => service.Confirm(show, new() { "C9" }, "bob"), "cannot confirm seats you never locked");

        service.LockSeats(show, new() { "C9" }, "bob");
        now += TimeSpan.FromMinutes(6);
        Check.Throws<SeatUnavailableException>(() => service.Confirm(show, new() { "C9" }, "bob"), "an expired lock cannot be confirmed");
        service.LockSeats(show, new() { "C9" }, "carol");
        Check.That(show.Seat("C9").Status == SeatStatus.Locked, "after expiry the seat is free again for carol");

        // Concurrency: 100 users click the same seat at once. Exactly one wins.
        var hot = new Show(new[] { "A1" });
        var live = new BookingService(() => DateTimeOffset.UtcNow);
        var winners = 0;
        Parallel.For(0, 100, new ParallelOptions { MaxDegreeOfParallelism = 16 }, i =>
        {
            try { live.LockSeats(hot, new() { "A1" }, "user" + i); Interlocked.Increment(ref winners); }
            catch (SeatUnavailableException) { }
        });
        Check.That(winners == 1, "100 simultaneous clicks, exactly one lock winner");
    }
}
