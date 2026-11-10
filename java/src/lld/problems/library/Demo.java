package lld.problems.library;

import lld.common.Check;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

record Book(String isbn, String title, String author) {}

/** A physical copy. (Book = the title; BookCopy = the thing you borrow. Status belongs to the COPY.) */
class BookCopy {
    private final String barcode; private final Book book; private boolean available = true;
    BookCopy(String barcode, Book book) { this.barcode = barcode; this.book = book; }
    String barcode() { return barcode; }
    Book book() { return book; }
    synchronized boolean tryCheckout() { if (!available) return false; available = false; return true; }
    synchronized void checkin() { available = true; }
    synchronized boolean isAvailable() { return available; }
}

class Member {
    final String id; final List<Loan> activeLoans = new ArrayList<>();
    Member(String id) { this.id = id; }
}

record Loan(Member member, BookCopy copy, LocalDate issued, LocalDate due) {}

/** Strategy: fines are money in paise, per day late, after a grace period. */
interface FinePolicy { long fine(Loan loan, LocalDate returned); }

class PerDayFine implements FinePolicy {
    private final long ratePerDay; private final int graceDays;
    PerDayFine(long ratePerDay, int graceDays) { this.ratePerDay = ratePerDay; this.graceDays = graceDays; }
    public long fine(Loan loan, LocalDate returned) {
        long late = ChronoUnit.DAYS.between(loan.due(), returned);
        long billable = Math.max(0, late - graceDays);
        return billable * ratePerDay;
    }
}

/** Observer: told when a copy comes back (e.g. to notify the first member waiting for that title). */
interface CopyReturnedListener { void onReturned(Book book); }

class LoanService {
    static final int MAX_LOANS = 3;
    static final int LOAN_DAYS = 14;
    private final FinePolicy finePolicy;
    private final List<CopyReturnedListener> listeners = new ArrayList<>();
    LoanService(FinePolicy finePolicy) { this.finePolicy = finePolicy; }
    void addListener(CopyReturnedListener l) { listeners.add(l); }

    Loan issue(Member m, BookCopy copy, LocalDate today) {
        if (m.activeLoans.size() >= MAX_LOANS) throw new IllegalStateException("Loan limit reached");
        if (!copy.tryCheckout()) throw new IllegalStateException("Copy already issued");   // atomic: no double issue
        Loan loan = new Loan(m, copy, today, today.plusDays(LOAN_DAYS));
        m.activeLoans.add(loan);
        return loan;
    }

    long returnCopy(Loan loan, LocalDate today) {
        long fine = finePolicy.fine(loan, today);
        loan.member().activeLoans.remove(loan);
        loan.copy().checkin();
        listeners.forEach(l -> l.onReturned(loan.copy().book()));
        return fine;
    }
}

class Catalog {
    private final List<BookCopy> copies = new ArrayList<>();
    void add(BookCopy c) { copies.add(c); }
    List<Book> search(String text) {
        String t = text.toLowerCase();
        return copies.stream().map(BookCopy::book).distinct()
            .filter(b -> b.title().toLowerCase().contains(t) || b.author().toLowerCase().contains(t)).toList();
    }
    Optional<BookCopy> availableCopyOf(Book b) {
        return copies.stream().filter(c -> c.book().equals(b) && c.isAvailable()).findFirst();
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Library: Book vs BookCopy, fine strategy, return listener");
        Book dune = new Book("111", "Dune", "Herbert"), emma = new Book("222", "Emma", "Austen");
        BookCopy d1 = new BookCopy("D1", dune), d2 = new BookCopy("D2", dune);
        Catalog catalog = new Catalog();
        catalog.add(d1); catalog.add(d2); catalog.add(new BookCopy("E1", emma));

        Check.that(catalog.search("herb").equals(List.of(dune)), "search by author substring finds the title once");
        LoanService service = new LoanService(new PerDayFine(200, 2));
        List<String> notified = new ArrayList<>();
        service.addListener(b -> notified.add(b.title()));

        Member asha = new Member("asha");
        LocalDate day0 = LocalDate.of(2026, 11, 1);
        Loan l1 = service.issue(asha, d1, day0);
        Check.that(l1.due().equals(day0.plusDays(14)), "due date is 14 days after issue");
        Check.throwsError(() -> service.issue(new Member("ravi"), d1, day0), IllegalStateException.class, "the same physical copy cannot be issued twice");
        Check.that(catalog.availableCopyOf(dune).orElseThrow() == d2, "the second copy of the title is still available");

        service.issue(asha, d2, day0);
        service.issue(asha, new BookCopy("E1", emma), day0);
        Check.throwsError(() -> service.issue(asha, new BookCopy("X", emma), day0), IllegalStateException.class, "limit of 3 active loans enforced");

        Check.that(service.returnCopy(l1, day0.plusDays(14)) == 0, "returned on the due date: no fine");
        Check.that(notified.equals(List.of("Dune")), "listener was told a copy of Dune came back");
        Loan l2 = asha.activeLoans.get(0);
        Check.that(service.returnCopy(l2, l2.due().plusDays(5)) == 600, "5 days late, 2 days grace: 3 billable days x 200 paise = 600");
    }
}
