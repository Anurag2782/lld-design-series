package lld.patterns.adapter;

import lld.common.Check;
import java.math.BigDecimal;

// ---- OUR interface: what our code wants ----
record PaymentResult(boolean success, String reference) {
    static PaymentResult success(String txn) { return new PaymentResult(true, txn); }
    static PaymentResult failed(String code) { return new PaymentResult(false, code); }
}
interface PaymentGateway { PaymentResult charge(String userId, long paise); }

// ---- THEIR SDK: we cannot change it (made-up vendor for the example) ----
record AcmeResponse(boolean ok, String txnId, String errorCode) {}
class AcmePayClient {
    AcmeResponse makePayment(String customerRef, BigDecimal rupees, String currency) {
        if (rupees.compareTo(new BigDecimal("10000")) > 0) return new AcmeResponse(false, null, "LIMIT_EXCEEDED");
        return new AcmeResponse(true, "txn-" + customerRef + "-" + rupees.toPlainString(), null);
    }
}

/** The adapter translates paise<->rupees and their response<->ours. */
class AcmePayAdapter implements PaymentGateway {
    private final AcmePayClient client;
    AcmePayAdapter(AcmePayClient client) { this.client = client; }

    public PaymentResult charge(String userId, long paise) {
        BigDecimal rupees = BigDecimal.valueOf(paise, 2);          // 1234 paise -> 12.34
        AcmeResponse r = client.makePayment(userId, rupees, "INR");
        return r.ok() ? PaymentResult.success(r.txnId()) : PaymentResult.failed(r.errorCode());
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Adapter: make an incompatible API fit your interface");
        PaymentGateway gateway = new AcmePayAdapter(new AcmePayClient());
        PaymentResult ok = gateway.charge("u1", 1234);
        Check.that(ok.success() && ok.reference().equals("txn-u1-12.34"), "paise converted to rupees exactly: 1234 -> 12.34");
        PaymentResult big = gateway.charge("u1", 2_000_000);
        Check.that(!big.success() && big.reference().equals("LIMIT_EXCEEDED"), "vendor error mapped into our PaymentResult");
        // A test could pass a lambda: PaymentGateway fake = (u, p) -> PaymentResult.success("fake");
        PaymentGateway fake = (u, p) -> PaymentResult.success("fake");
        Check.that(fake.charge("x", 1).reference().equals("fake"), "callers depend on the interface, so tests use a fake");
    }
}
