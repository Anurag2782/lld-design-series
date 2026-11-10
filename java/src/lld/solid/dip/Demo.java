package lld.solid.dip;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;

/** The abstraction the HIGH-LEVEL code owns. */
interface MessageSender { void send(String to, String message); }

class EmailSender implements MessageSender {
    public void send(String to, String message) { System.out.println("  EMAIL to " + to + ": " + message); }
}
class SmsSender implements MessageSender {
    public void send(String to, String message) { System.out.println("  SMS to " + to + ": " + message); }
}
/** Test double: records calls instead of touching real infrastructure. */
class FakeSender implements MessageSender {
    final List<String> sent = new ArrayList<>();
    public void send(String to, String message) { sent.add(to + "|" + message); }
}

/** High-level policy: knows WHAT it needs, not WHO provides it. */
class NotificationService {
    private final MessageSender sender;          // injected
    NotificationService(MessageSender sender) { this.sender = sender; }
    void orderPlaced(String to, String orderId) { sender.send(to, "Order " + orderId + " placed"); }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("DIP: depend on abstractions, inject dependencies");
        FakeSender fake = new FakeSender();
        new NotificationService(fake).orderPlaced("a@x.com", "42");
        Check.that(fake.sent.equals(List.of("a@x.com|Order 42 placed")), "unit test with zero real infrastructure");

        // Swapping the provider changes wiring, not business code.
        new NotificationService(new SmsSender()).orderPlaced("9999", "43");
        new NotificationService(new EmailSender()).orderPlaced("b@x.com", "44");
        Check.that(true, "same service works with SMS or email");
    }
}
