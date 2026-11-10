package lld.solid.ocp;

import lld.common.Check;

interface DiscountStrategy { double apply(double amount); }

class RegularDiscount implements DiscountStrategy { public double apply(double a) { return a * 0.05; } }
class PremiumDiscount implements DiscountStrategy { public double apply(double a) { return a * 0.15; } }
/** Added later: a NEW class, zero edits to Checkout or the existing strategies. */
class FestivalDiscount implements DiscountStrategy { public double apply(double a) { return a * 0.20; } }

class Checkout {
    double total(double amount, DiscountStrategy discount) {
        return amount - discount.apply(amount);
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("OCP: extend by adding classes, not editing methods");
        Checkout checkout = new Checkout();
        Check.near(checkout.total(1000, new RegularDiscount()), 950.0, "regular gets 5% off");
        Check.near(checkout.total(1000, new PremiumDiscount()), 850.0, "premium gets 15% off");
        Check.near(checkout.total(1000, new FestivalDiscount()), 800.0, "festival (new class) gets 20% off");
    }
}
