import { check } from "../common.ts";

/** The abstraction the HIGH-LEVEL code owns. */
interface MessageSender { send(to: string, message: string): void }

class EmailSender implements MessageSender {
  send(to: string, message: string) { console.log(`  EMAIL to ${to}: ${message}`); }
}
class SmsSender implements MessageSender {
  send(to: string, message: string) { console.log(`  SMS to ${to}: ${message}`); }
}
/** Test double: records calls instead of touching real infrastructure. */
class FakeSender implements MessageSender {
  sent: string[] = [];
  send(to: string, message: string) { this.sent.push(`${to}|${message}`); }
}

/** High-level policy: knows WHAT it needs, not WHO provides it. */
class NotificationService {
  private sender: MessageSender;
  constructor(sender: MessageSender) { this.sender = sender; }
  orderPlaced(to: string, orderId: string): void { this.sender.send(to, `Order ${orderId} placed`); }
}

export function demo(): void {
  console.log("DIP: depend on abstractions, inject dependencies");
  const fake = new FakeSender();
  new NotificationService(fake).orderPlaced("a@x.com", "42");
  check(fake.sent.join() === "a@x.com|Order 42 placed", "unit test with zero real infrastructure");
  new NotificationService(new SmsSender()).orderPlaced("9999", "43");
  new NotificationService(new EmailSender()).orderPlaced("b@x.com", "44");
  check(true, "same service works with SMS or email");
}
