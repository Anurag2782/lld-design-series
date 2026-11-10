using System.Globalization;
using Lld.Common;

namespace Lld.Patterns.Adapter;

// ---- OUR interface: what our code wants ----
record PaymentResult(bool Success, string Reference)
{
    public static PaymentResult Ok(string txn) => new(true, txn);
    public static PaymentResult Failed(string code) => new(false, code);
}
interface IPaymentGateway { PaymentResult Charge(string userId, long paise); }

// ---- THEIR SDK: we cannot change it (made-up vendor for the example) ----
record AcmeResponse(bool Ok, string? TxnId, string? ErrorCode);
class AcmePayClient
{
    public AcmeResponse MakePayment(string customerRef, decimal rupees, string currency) =>
        rupees > 10000m
            ? new AcmeResponse(false, null, "LIMIT_EXCEEDED")
            : new AcmeResponse(true, $"txn-{customerRef}-{rupees.ToString("0.00", CultureInfo.InvariantCulture)}", null);
}

class FakeGateway : IPaymentGateway
{
    public PaymentResult Charge(string userId, long paise) => PaymentResult.Ok("fake");
}

/// <summary>The adapter translates paise&lt;-&gt;rupees and their response&lt;-&gt;ours.</summary>
class AcmePayAdapter(AcmePayClient client) : IPaymentGateway
{
    public PaymentResult Charge(string userId, long paise)
    {
        var rupees = paise / 100m;                                   // 1234 paise -> 12.34
        var r = client.MakePayment(userId, rupees, "INR");
        return r.Ok ? PaymentResult.Ok(r.TxnId!) : PaymentResult.Failed(r.ErrorCode!);
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Adapter: make an incompatible API fit your interface");
        IPaymentGateway gateway = new AcmePayAdapter(new AcmePayClient());
        var ok = gateway.Charge("u1", 1234);
        Check.That(ok.Success && ok.Reference == "txn-u1-12.34", "paise converted to rupees exactly: 1234 -> 12.34");
        var big = gateway.Charge("u1", 2_000_000);
        Check.That(!big.Success && big.Reference == "LIMIT_EXCEEDED", "vendor error mapped into our PaymentResult");
        Check.That(new FakeGateway().Charge("x", 1).Reference == "fake", "callers depend on the interface, so tests use a fake");
    }
}
