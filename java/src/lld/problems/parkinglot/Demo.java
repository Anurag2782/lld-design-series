package lld.problems.parkinglot;

import lld.common.Check;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

enum VehicleType { BIKE, CAR, TRUCK }

record Vehicle(String plate, VehicleType type, boolean electric) {
    Vehicle(String plate, VehicleType type) { this(plate, type, false); }
}

record Ticket(String id, Vehicle vehicle, ParkingSlot slot, Instant entry) {}

class NoSlotAvailableException extends RuntimeException {
    NoSlotAvailableException() { super("No slot available"); }
}

/** The slot owns its own "does this vehicle fit?" rule. */
abstract class ParkingSlot {
    private final String id;
    private boolean occupied;
    ParkingSlot(String id) { this.id = id; }
    String id() { return id; }

    protected abstract boolean accepts(Vehicle v);

    /** Check AND set as one atomic step: two gates can never get the same slot. */
    synchronized boolean tryOccupy(Vehicle v) {
        if (occupied || !accepts(v)) return false;
        occupied = true;
        return true;
    }
    synchronized void free() { occupied = false; }
}

class StandardSlot extends ParkingSlot {
    private final VehicleType type;
    StandardSlot(String id, VehicleType type) { super(id); this.type = type; }
    protected boolean accepts(Vehicle v) { return v.type() == type; }
}

/** The extension: only electric cars fit. No other existing class changes. */
class EvChargingSlot extends ParkingSlot {
    EvChargingSlot(String id) { super(id); }
    protected boolean accepts(Vehicle v) { return v.electric() && v.type() == VehicleType.CAR; }
}

class ParkingFloor {
    private final List<ParkingSlot> slots;
    ParkingFloor(List<ParkingSlot> slots) { this.slots = slots; }
    Optional<ParkingSlot> occupySlotFor(Vehicle v) {
        for (ParkingSlot s : slots) if (s.tryOccupy(v)) return Optional.of(s);
        return Optional.empty();
    }
}

class FloorFactory {
    static ParkingFloor create(int floorNo, int bikes, int cars, int trucks, int evSlots) {
        List<ParkingSlot> slots = new ArrayList<>();
        for (int i = 0; i < evSlots; i++) slots.add(new EvChargingSlot("F" + floorNo + "-EV-" + i));
        add(slots, floorNo, VehicleType.BIKE, bikes);
        add(slots, floorNo, VehicleType.CAR, cars);
        add(slots, floorNo, VehicleType.TRUCK, trucks);
        return new ParkingFloor(slots);
    }
    private static void add(List<ParkingSlot> list, int floor, VehicleType type, int n) {
        for (int i = 0; i < n; i++) list.add(new StandardSlot("F" + floor + "-" + type + "-" + i, type));
    }
}

/** Strategy: money is in paise (long), never double. */
interface PricingStrategy { long calculate(Ticket t, Instant exit); }

class HourlyPricing implements PricingStrategy {
    private final Map<VehicleType, Long> ratePerHourPaise;
    HourlyPricing(Map<VehicleType, Long> rates) { this.ratePerHourPaise = rates; }
    public long calculate(Ticket t, Instant exit) {
        long minutes = Duration.between(t.entry(), exit).toMinutes();
        long hours = Math.max(1, (minutes + 59) / 60);              // round UP, minimum 1 hour
        return hours * ratePerHourPaise.get(t.vehicle().type());
    }
}

/** Decorator over any PricingStrategy. */
class EvSurcharge implements PricingStrategy {
    private final PricingStrategy base; private final long chargingFeePaise;
    EvSurcharge(PricingStrategy base, long fee) { this.base = base; this.chargingFeePaise = fee; }
    public long calculate(Ticket t, Instant exit) {
        long fee = base.calculate(t, exit);
        return t.vehicle().electric() ? fee + chargingFeePaise : fee;
    }
}

class ParkingLot {
    private final List<ParkingFloor> floors;
    private final PricingStrategy pricing;
    ParkingLot(List<ParkingFloor> floors, PricingStrategy pricing) { this.floors = floors; this.pricing = pricing; }

    Ticket park(Vehicle v, Instant now) {
        for (ParkingFloor f : floors) {
            Optional<ParkingSlot> s = f.occupySlotFor(v);
            if (s.isPresent()) return new Ticket(UUID.randomUUID().toString(), v, s.get(), now);
        }
        throw new NoSlotAvailableException();
    }

    long unpark(Ticket t, Instant exit) {
        long fee = pricing.calculate(t, exit);
        t.slot().free();
        return fee;
    }
}

public class Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("Parking Lot: slots decide fit, strategy prices, decorator adds EV fee");
        Map<VehicleType, Long> rates = Map.of(VehicleType.BIKE, 1000L, VehicleType.CAR, 3000L, VehicleType.TRUCK, 6000L);
        PricingStrategy pricing = new EvSurcharge(new HourlyPricing(rates), 5000);
        ParkingLot lot = new ParkingLot(List.of(FloorFactory.create(1, 2, 3, 1, 1)), pricing);
        Instant t0 = Instant.parse("2026-11-20T10:00:00Z");

        Ticket car = lot.park(new Vehicle("UK08-1", VehicleType.CAR), t0);
        long fee = lot.unpark(car, t0.plus(Duration.ofMinutes(61)));
        Check.that(fee == 6000, "61 minutes rounds up to 2 hours x 3000 paise = 6000");

        Ticket ev = lot.park(new Vehicle("UK08-EV", VehicleType.CAR, true), t0);
        Check.that(ev.slot() instanceof EvChargingSlot, "electric car is given the EV slot first");
        Check.that(lot.unpark(ev, t0.plus(Duration.ofMinutes(30))) == 3000 + 5000, "1 hour minimum + EV surcharge");

        Ticket truck = lot.park(new Vehicle("UK08-T", VehicleType.TRUCK), t0);
        Check.throwsError(() -> lot.park(new Vehicle("UK08-T2", VehicleType.TRUCK), t0),
            NoSlotAvailableException.class, "second truck: only one truck slot exists");
        lot.unpark(truck, t0.plus(Duration.ofHours(1)));
        lot.park(new Vehicle("UK08-T3", VehicleType.TRUCK), t0);
        Check.that(true, "freed slot can be reused");

        // Concurrency: 10 car slots, 50 gates racing. Exactly 10 may succeed.
        ParkingLot busy = new ParkingLot(List.of(FloorFactory.create(1, 0, 10, 0, 0)), pricing);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger parked = new AtomicInteger();
        Set<String> slotIds = ConcurrentHashMap.newKeySet();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                try {
                    Ticket t = busy.park(new Vehicle("CAR-" + n, VehicleType.CAR), t0);
                    parked.incrementAndGet();
                    slotIds.add(t.slot().id());
                } catch (NoSlotAvailableException ignored) { }
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        Check.that(parked.get() == 10, "50 racing cars, exactly 10 tickets issued");
        Check.that(slotIds.size() == 10, "no slot was handed to two cars");
    }
}
