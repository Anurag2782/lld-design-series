using Lld.Common;

namespace Lld.Solid.Ocp;

interface IDiscountStrategy { double Apply(double amount); }

class RegularDiscount : IDiscountStrategy { public double Apply(double a) => a * 0.05; }
class PremiumDiscount : IDiscountStrategy { public double Apply(double a) => a * 0.15; }
/// <summary>Added later: a NEW class, zero edits to Checkout or the existing strategies.</summary>
class FestivalDiscount : IDiscountStrategy { public double Apply(double a) => a * 0.20; }

class Checkout
{
    public double Total(double amount, IDiscountStrategy discount) => amount - discount.Apply(amount);
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("OCP: extend by adding classes, not editing methods");
        var checkout = new Checkout();
        Check.Near(checkout.Total(1000, new RegularDiscount()), 950.0, "regular gets 5% off");
        Check.Near(checkout.Total(1000, new PremiumDiscount()), 850.0, "premium gets 15% off");
        Check.Near(checkout.Total(1000, new FestivalDiscount()), 800.0, "festival (new class) gets 20% off");
    }
}
