package lld.patterns.facade;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;

// ---- The subsystems: each is complex on its own ----
class InventoryService {
    final List<String> log;
    InventoryService(List<String> log) { this.log = log; }
    void reserve(String item) { log.add("reserve:" + item); }
    void release(String item) { log.add("release:" + item); }
}
class PaymentService {
    final List<String> log; boolean failNext;
    PaymentService(List<String> log) { this.log = log; }
    String charge(long paise) {
        if (failNext) throw new IllegalStateException("card declined");
        log.add("charge:" + paise);
        return "rcpt-1";
    }
}
class ShippingService {
    final List<String> log;
    ShippingService(List<String> log) { this.log = log; }
    String schedule(String address) { log.add("ship:" + address); return "track-9"; }
}
class Notifier {
    final List<String> log;
    Notifier(List<String> log) { this.log = log; }
    void orderPlaced(String email) { log.add("notify:" + email); }
}

record OrderConfirmation(String receiptId, String trackingId) {}

/** Facade: ONE simple method hides four subsystems and the order they must be called in. */
class CheckoutFacade {
    private final InventoryService inventory; private final PaymentService payment;
    private final ShippingService shipping; private final Notifier notifier;

    CheckoutFacade(InventoryService i, PaymentService p, ShippingService s, Notifier n) {
        inventory = i; payment = p; shipping = s; notifier = n;
    }

    OrderConfirmation placeOrder(String item, long paise, String address, String email) {
        inventory.reserve(item);
        try {
            String receipt = payment.charge(paise);
            String tracking = shipping.schedule(address);
            notifier.orderPlaced(email);
            return new OrderConfirmation(receipt, tracking);
        } catch (RuntimeException e) {
            inventory.release(item);                 // compensating action: undo what we already did
            throw e;
        }
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Facade: one simple entry point over a complex subsystem");
        List<String> log = new ArrayList<>();
        PaymentService payment = new PaymentService(log);
        CheckoutFacade checkout = new CheckoutFacade(new InventoryService(log), payment, new ShippingService(log), new Notifier(log));

        OrderConfirmation c = checkout.placeOrder("book", 49900, "Haridwar", "a@x.com");
        Check.that(c.receiptId().equals("rcpt-1") && c.trackingId().equals("track-9"), "caller makes ONE call and gets one confirmation");
        Check.that(log.equals(List.of("reserve:book", "charge:49900", "ship:Haridwar", "notify:a@x.com")), "facade calls the subsystems in the right order");

        log.clear();
        payment.failNext = true;
        Check.throwsError(() -> checkout.placeOrder("book", 49900, "Haridwar", "a@x.com"), IllegalStateException.class, "payment failure propagates");
        Check.that(log.equals(List.of("reserve:book", "release:book")), "inventory reservation was released (compensation), nothing shipped");
    }
}
