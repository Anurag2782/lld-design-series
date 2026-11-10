import { check, throws } from "../common.ts";

class InventoryService {
  private log: string[];
  constructor(log: string[]) { this.log = log; }
  reserve(item: string) { this.log.push("reserve:" + item); }
  release(item: string) { this.log.push("release:" + item); }
}
class PaymentService {
  failNext = false;
  private log: string[];
  constructor(log: string[]) { this.log = log; }
  charge(paise: number): string {
    if (this.failNext) throw new Error("card declined");
    this.log.push(`charge:${paise}`);
    return "rcpt-1";
  }
}
class ShippingService {
  private log: string[];
  constructor(log: string[]) { this.log = log; }
  schedule(address: string): string { this.log.push("ship:" + address); return "track-9"; }
}
class Notifier {
  private log: string[];
  constructor(log: string[]) { this.log = log; }
  orderPlaced(email: string) { this.log.push("notify:" + email); }
}

interface OrderConfirmation { receiptId: string; trackingId: string }

/** Facade: ONE simple method hides four subsystems and the order they must be called in. */
class CheckoutFacade {
  private inventory: InventoryService;
  private payment: PaymentService;
  private shipping: ShippingService;
  private notifier: Notifier;
  constructor(i: InventoryService, p: PaymentService, s: ShippingService, n: Notifier) {
    this.inventory = i; this.payment = p; this.shipping = s; this.notifier = n;
  }

  placeOrder(item: string, paise: number, address: string, email: string): OrderConfirmation {
    this.inventory.reserve(item);
    try {
      const receiptId = this.payment.charge(paise);
      const trackingId = this.shipping.schedule(address);
      this.notifier.orderPlaced(email);
      return { receiptId, trackingId };
    } catch (e) {
      this.inventory.release(item);          // compensating action: undo what we already did
      throw e;
    }
  }
}

export function demo(): void {
  console.log("Facade: one simple entry point over a complex subsystem");
  const log: string[] = [];
  const payment = new PaymentService(log);
  const checkout = new CheckoutFacade(new InventoryService(log), payment, new ShippingService(log), new Notifier(log));

  const c = checkout.placeOrder("book", 49900, "Haridwar", "a@x.com");
  check(c.receiptId === "rcpt-1" && c.trackingId === "track-9", "caller makes ONE call and gets one confirmation");
  check(log.join() === "reserve:book,charge:49900,ship:Haridwar,notify:a@x.com", "facade calls the subsystems in the right order");

  log.length = 0;
  payment.failNext = true;
  throws(() => checkout.placeOrder("book", 49900, "Haridwar", "a@x.com"), "payment failure propagates");
  check(log.join() === "reserve:book,release:book", "inventory reservation was released (compensation), nothing shipped");
}
