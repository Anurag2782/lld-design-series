import { check } from "../common.ts";

// ---- OUR interface: what our code wants ----
interface PaymentResult { success: boolean; reference: string }
interface PaymentGateway { charge(userId: string, paise: number): PaymentResult }

// ---- THEIR SDK: we cannot change it (made-up vendor for the example) ----
interface AcmeResponse { ok: boolean; txnId?: string; errorCode?: string }
class AcmePayClient {
  makePayment(customerRef: string, rupees: string, _currency: string): AcmeResponse {
    if (Number(rupees) > 10000) return { ok: false, errorCode: "LIMIT_EXCEEDED" };
    return { ok: true, txnId: `txn-${customerRef}-${rupees}` };
  }
}

/** The adapter translates paise<->rupees and their response<->ours. */
class AcmePayAdapter implements PaymentGateway {
  private client: AcmePayClient;
  constructor(client: AcmePayClient) { this.client = client; }
  charge(userId: string, paise: number): PaymentResult {
    // integer math only: 1234 paise -> "12.34" with no floating-point surprises
    const rupees = `${Math.trunc(paise / 100)}.${String(paise % 100).padStart(2, "0")}`;
    const r = this.client.makePayment(userId, rupees, "INR");
    return r.ok ? { success: true, reference: r.txnId! } : { success: false, reference: r.errorCode! };
  }
}

export function demo(): void {
  console.log("Adapter: make an incompatible API fit your interface");
  const gateway: PaymentGateway = new AcmePayAdapter(new AcmePayClient());
  const ok = gateway.charge("u1", 1234);
  check(ok.success && ok.reference === "txn-u1-12.34", "paise converted to rupees exactly: 1234 -> 12.34");
  const big = gateway.charge("u1", 2_000_000);
  check(!big.success && big.reference === "LIMIT_EXCEEDED", "vendor error mapped into our PaymentResult");
  const fake: PaymentGateway = { charge: () => ({ success: true, reference: "fake" }) };
  check(fake.charge("x", 1).reference === "fake", "callers depend on the interface, so tests use a fake");
}
