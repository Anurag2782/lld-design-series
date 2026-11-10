using Lld.Common;

namespace Lld.Patterns.Facade;

class InventoryService(List<string> log)
{
    public void Reserve(string item) => log.Add("reserve:" + item);
    public void Release(string item) => log.Add("release:" + item);
}
class PaymentService(List<string> log)
{
    public bool FailNext;
    public string Charge(long paise)
    {
        if (FailNext) throw new InvalidOperationException("card declined");
        log.Add("charge:" + paise);
        return "rcpt-1";
    }
}
class ShippingService(List<string> log)
{
    public string Schedule(string address) { log.Add("ship:" + address); return "track-9"; }
}
class Notifier(List<string> log)
{
    public void OrderPlaced(string email) => log.Add("notify:" + email);
}

record OrderConfirmation(string ReceiptId, string TrackingId);

/// <summary>Facade: ONE simple method hides four subsystems and the order they must be called in.</summary>
class CheckoutFacade(InventoryService inventory, PaymentService payment, ShippingService shipping, Notifier notifier)
{
    public OrderConfirmation PlaceOrder(string item, long paise, string address, string email)
    {
        inventory.Reserve(item);
        try
        {
            var receipt = payment.Charge(paise);
            var tracking = shipping.Schedule(address);
            notifier.OrderPlaced(email);
            return new OrderConfirmation(receipt, tracking);
        }
        catch
        {
            inventory.Release(item);                 // compensating action: undo what we already did
            throw;
        }
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Facade: one simple entry point over a complex subsystem");
        var log = new List<string>();
        var payment = new PaymentService(log);
        var checkout = new CheckoutFacade(new InventoryService(log), payment, new ShippingService(log), new Notifier(log));

        var c = checkout.PlaceOrder("book", 49900, "Haridwar", "a@x.com");
        Check.That(c.ReceiptId == "rcpt-1" && c.TrackingId == "track-9", "caller makes ONE call and gets one confirmation");
        Check.That(log.SequenceEqual(new[] { "reserve:book", "charge:49900", "ship:Haridwar", "notify:a@x.com" }), "facade calls the subsystems in the right order");

        log.Clear();
        payment.FailNext = true;
        Check.Throws<InvalidOperationException>(() => checkout.PlaceOrder("book", 49900, "Haridwar", "a@x.com"), "payment failure propagates");
        Check.That(log.SequenceEqual(new[] { "reserve:book", "release:book" }), "inventory reservation was released (compensation), nothing shipped");
    }
}
