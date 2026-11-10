import { near } from "../common.ts";

interface DiscountStrategy { apply(amount: number): number }

class RegularDiscount implements DiscountStrategy { apply(a: number) { return a * 0.05; } }
class PremiumDiscount implements DiscountStrategy { apply(a: number) { return a * 0.15; } }
/** Added later: a NEW class, zero edits to Checkout or the existing strategies. */
class FestivalDiscount implements DiscountStrategy { apply(a: number) { return a * 0.2; } }

class Checkout {
  total(amount: number, discount: DiscountStrategy): number { return amount - discount.apply(amount); }
}

export function demo(): void {
  console.log("OCP: extend by adding classes, not editing methods");
  const checkout = new Checkout();
  near(checkout.total(1000, new RegularDiscount()), 950, "regular gets 5% off");
  near(checkout.total(1000, new PremiumDiscount()), 850, "premium gets 15% off");
  near(checkout.total(1000, new FestivalDiscount()), 800, "festival (new class) gets 20% off");
}
