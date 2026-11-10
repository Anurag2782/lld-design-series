using Lld.Common;

namespace Lld.Solid.Dip;

/// <summary>The abstraction the HIGH-LEVEL code owns.</summary>
interface IMessageSender { void Send(string to, string message); }

class EmailSender : IMessageSender
{
    public void Send(string to, string message) => Console.WriteLine($"  EMAIL to {to}: {message}");
}
class SmsSender : IMessageSender
{
    public void Send(string to, string message) => Console.WriteLine($"  SMS to {to}: {message}");
}
/// <summary>Test double: records calls instead of touching real infrastructure.</summary>
class FakeSender : IMessageSender
{
    public List<string> Sent { get; } = new();
    public void Send(string to, string message) => Sent.Add($"{to}|{message}");
}

/// <summary>High-level policy: knows WHAT it needs, not WHO provides it. (ASP.NET Core injects this for you.)</summary>
class NotificationService(IMessageSender sender)
{
    public void OrderPlaced(string to, string orderId) => sender.Send(to, $"Order {orderId} placed");
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("DIP: depend on abstractions, inject dependencies");
        var fake = new FakeSender();
        new NotificationService(fake).OrderPlaced("a@x.com", "42");
        Check.That(fake.Sent.SequenceEqual(new[] { "a@x.com|Order 42 placed" }), "unit test with zero real infrastructure");

        new NotificationService(new SmsSender()).OrderPlaced("9999", "43");
        new NotificationService(new EmailSender()).OrderPlaced("b@x.com", "44");
        Check.That(true, "same service works with SMS or email");
    }
}
