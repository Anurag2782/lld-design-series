using Lld.Common;

namespace Lld.Problems.Library;

record Book(string Isbn, string Title, string Author);

/// <summary>A physical copy. (Book = the title; BookCopy = the thing you borrow. Status belongs to the COPY.)</summary>
class BookCopy(string barcode, Book book)
{
    private readonly object _lock = new();
    private bool _available = true;
    public string Barcode => barcode;
    public Book Book => book;
    public bool TryCheckout() { lock (_lock) { if (!_available) return false; _available = false; return true; } }
    public void Checkin() { lock (_lock) _available = true; }
    public bool IsAvailable { get { lock (_lock) return _available; } }
}

class Member(string id)
{
    public string Id => id;
    public List<Loan> ActiveLoans { get; } = new();
}

record Loan(Member Member, BookCopy Copy, DateOnly Issued, DateOnly Due);

/// <summary>Strategy: fines are money in paise, per day late, after a grace period.</summary>
interface IFinePolicy { long Fine(Loan loan, DateOnly returned); }

class PerDayFine(long ratePerDay, int graceDays) : IFinePolicy
{
    public long Fine(Loan loan, DateOnly returned)
    {
        long late = returned.DayNumber - loan.Due.DayNumber;
        return Math.Max(0, late - graceDays) * ratePerDay;
    }
}

class LoanService(IFinePolicy finePolicy)
{
    public const int MaxLoans = 3;
    public const int LoanDays = 14;
    /// <summary>Observer: told when a copy comes back (e.g. to notify the first member waiting for that title).</summary>
    public event Action<Book>? CopyReturned;

    public Loan Issue(Member m, BookCopy copy, DateOnly today)
    {
        if (m.ActiveLoans.Count >= MaxLoans) throw new InvalidOperationException("Loan limit reached");
        if (!copy.TryCheckout()) throw new InvalidOperationException("Copy already issued");   // atomic: no double issue
        var loan = new Loan(m, copy, today, today.AddDays(LoanDays));
        m.ActiveLoans.Add(loan);
        return loan;
    }

    public long ReturnCopy(Loan loan, DateOnly today)
    {
        var fine = finePolicy.Fine(loan, today);
        loan.Member.ActiveLoans.Remove(loan);
        loan.Copy.Checkin();
        CopyReturned?.Invoke(loan.Copy.Book);
        return fine;
    }
}

class Catalog
{
    private readonly List<BookCopy> _copies = new();
    public void Add(BookCopy c) => _copies.Add(c);
    public List<Book> Search(string text) => _copies.Select(c => c.Book).Distinct()
        .Where(b => b.Title.Contains(text, StringComparison.OrdinalIgnoreCase) || b.Author.Contains(text, StringComparison.OrdinalIgnoreCase)).ToList();
    public BookCopy? AvailableCopyOf(Book b) => _copies.FirstOrDefault(c => c.Book == b && c.IsAvailable);
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Library: Book vs BookCopy, fine strategy, return listener");
        Book dune = new("111", "Dune", "Herbert"), emma = new("222", "Emma", "Austen");
        BookCopy d1 = new("D1", dune), d2 = new("D2", dune);
        var catalog = new Catalog();
        catalog.Add(d1); catalog.Add(d2); catalog.Add(new BookCopy("E1", emma));

        Check.That(catalog.Search("herb").SequenceEqual(new[] { dune }), "search by author substring finds the title once");
        var service = new LoanService(new PerDayFine(200, 2));
        var notified = new List<string>();
        service.CopyReturned += b => notified.Add(b.Title);

        var asha = new Member("asha");
        var day0 = new DateOnly(2026, 11, 1);
        var l1 = service.Issue(asha, d1, day0);
        Check.That(l1.Due == day0.AddDays(14), "due date is 14 days after issue");
        Check.Throws<InvalidOperationException>(() => service.Issue(new Member("ravi"), d1, day0), "the same physical copy cannot be issued twice");
        Check.That(ReferenceEquals(catalog.AvailableCopyOf(dune), d2), "the second copy of the title is still available");

        service.Issue(asha, d2, day0);
        service.Issue(asha, new BookCopy("E1", emma), day0);
        Check.Throws<InvalidOperationException>(() => service.Issue(asha, new BookCopy("X", emma), day0), "limit of 3 active loans enforced");

        Check.That(service.ReturnCopy(l1, day0.AddDays(14)) == 0, "returned on the due date: no fine");
        Check.That(notified.SequenceEqual(new[] { "Dune" }), "listener was told a copy of Dune came back");
        var l2 = asha.ActiveLoans[0];
        Check.That(service.ReturnCopy(l2, l2.Due.AddDays(5)) == 600, "5 days late, 2 days grace: 3 billable days x 200 paise = 600");
    }
}
