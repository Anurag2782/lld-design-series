package lld.patterns.state;

import lld.common.Check;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ---------- Order lifecycle: each state knows what is allowed FROM it ----------
interface OrderState {
    default OrderState pay()    { throw bad("pay"); }
    default OrderState ship()   { throw bad("ship"); }
    default OrderState cancel() { throw bad("cancel"); }
    private IllegalStateException bad(String action) {
        return new IllegalStateException("Cannot " + action + " when " + getClass().getSimpleName());
    }
}
class Created implements OrderState {
    public OrderState pay()    { return new Paid(); }
    public OrderState cancel() { return new Cancelled(); }
}
class Paid implements OrderState {
    public OrderState ship()   { return new Shipped(); }
    public OrderState cancel() { return new Cancelled(); }   // refund would happen here
}
class Shipped implements OrderState { }
class Cancelled implements OrderState { }

class Order {
    private OrderState state = new Created();
    void pay()    { state = state.pay(); }
    void ship()   { state = state.ship(); }
    void cancel() { state = state.cancel(); }
    String status() { return state.getClass().getSimpleName(); }
}

// ---------- Vending machine ----------
interface VendingState {
    void insert(VendingMachine m, int coin);
    void select(VendingMachine m, String item);
}

class VendingMachine {
    private VendingState state = new Idle();
    private int balance;
    final Map<String, Integer> stock = new HashMap<>();
    final Map<String, Integer> prices = new HashMap<>();
    final List<String> dispensed = new ArrayList<>();
    int returned;

    void insert(int coin) { state.insert(this, coin); }
    void select(String item) { state.select(this, item); }

    void setState(VendingState s) { state = s; }
    String stateName() { return state.getClass().getSimpleName(); }
    void addBalance(int coin) { balance += coin; }
    int balance() { return balance; }
    void dispense(String item) {
        stock.merge(item, -1, Integer::sum);
        dispensed.add(item);
        returned += balance - prices.get(item);   // change
        balance = 0;
    }
    void refundAll() { returned += balance; balance = 0; }
}

class Idle implements VendingState {
    public void insert(VendingMachine m, int coin) { m.addBalance(coin); m.setState(new HasMoney()); }
    public void select(VendingMachine m, String item) { /* insert money first: ignored */ }
}

class HasMoney implements VendingState {
    public void insert(VendingMachine m, int coin) { m.addBalance(coin); }
    public void select(VendingMachine m, String item) {
        if (m.stock.getOrDefault(item, 0) == 0) { m.refundAll(); m.setState(new Idle()); return; }
        if (m.balance() < m.prices.get(item)) return;     // need more money, stay in HasMoney
        m.dispense(item);
        m.setState(new Idle());
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("State: behavior changes with lifecycle, no if (status == ...) chains");
        Order o = new Order();
        Check.that(o.status().equals("Created"), "starts Created");
        o.pay(); o.ship();
        Check.that(o.status().equals("Shipped"), "Created -> Paid -> Shipped");
        Check.throwsError(o::cancel, IllegalStateException.class, "a shipped order cannot be cancelled");
        Order fresh = new Order();
        Check.throwsError(fresh::ship, IllegalStateException.class, "cannot ship before paying");

        VendingMachine m = new VendingMachine();
        m.stock.put("chips", 1); m.prices.put("chips", 30);
        m.select("chips");
        Check.that(m.stateName().equals("Idle") && m.dispensed.isEmpty(), "select with no money does nothing");
        m.insert(20);
        m.select("chips");
        Check.that(m.stateName().equals("HasMoney") && m.dispensed.isEmpty(), "not enough money: stays in HasMoney");
        m.insert(30);
        m.select("chips");
        Check.that(m.dispensed.equals(List.of("chips")) && m.returned == 20, "dispensed and returned 20 change");
        Check.that(m.stateName().equals("Idle"), "back to Idle after dispensing");
        m.insert(50);
        m.select("chips");   // out of stock now
        Check.that(m.returned == 70 && m.stateName().equals("Idle"), "out of stock: full refund, back to Idle");
    }
}
