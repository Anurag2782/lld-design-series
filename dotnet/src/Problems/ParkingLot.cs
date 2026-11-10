using System.Collections.Concurrent;
using Lld.Common;

namespace Lld.Problems.ParkingLot;

enum VehicleType { Bike, Car, Truck }

record Vehicle(string Plate, VehicleType Type, bool Electric = false);

record Ticket(string Id, Vehicle Vehicle, ParkingSlot Slot, DateTimeOffset Entry);

class NoSlotAvailableException() : Exception("No slot available");

/// <summary>The slot owns its own "does this vehicle fit?" rule.</summary>
abstract class ParkingSlot(string id)
{
    private readonly object _lock = new();
    private bool _occupied;
    public string Id { get; } = id;

    protected abstract bool Accepts(Vehicle v);

    /// <summary>Check AND set as one atomic step: two gates can never get the same slot.</summary>
    public bool TryOccupy(Vehicle v)
    {
        lock (_lock)
        {
            if (_occupied || !Accepts(v)) return false;
            _occupied = true;
            return true;
        }
    }
    public void Free() { lock (_lock) _occupied = false; }
}

class StandardSlot(string id, VehicleType type) : ParkingSlot(id)
{
    protected override bool Accepts(Vehicle v) => v.Type == type;
}

/// <summary>The extension: only electric cars fit. No other existing class changes.</summary>
class EvChargingSlot(string id) : ParkingSlot(id)
{
    protected override bool Accepts(Vehicle v) => v.Electric && v.Type == VehicleType.Car;
}

class ParkingFloor(List<ParkingSlot> slots)
{
    public ParkingSlot? OccupySlotFor(Vehicle v) => slots.FirstOrDefault(s => s.TryOccupy(v));
}

static class FloorFactory
{
    public static ParkingFloor Create(int floorNo, int bikes, int cars, int trucks, int evSlots)
    {
        var slots = new List<ParkingSlot>();
        for (var i = 0; i < evSlots; i++) slots.Add(new EvChargingSlot($"F{floorNo}-EV-{i}"));
        Add(slots, floorNo, VehicleType.Bike, bikes);
        Add(slots, floorNo, VehicleType.Car, cars);
        Add(slots, floorNo, VehicleType.Truck, trucks);
        return new ParkingFloor(slots);
    }
    private static void Add(List<ParkingSlot> list, int floor, VehicleType type, int n)
    {
        for (var i = 0; i < n; i++) list.Add(new StandardSlot($"F{floor}-{type}-{i}", type));
    }
}

/// <summary>Strategy: money is in paise (long), never double/float.</summary>
interface IPricingStrategy { long Calculate(Ticket t, DateTimeOffset exit); }

class HourlyPricing(IReadOnlyDictionary<VehicleType, long> ratePerHourPaise) : IPricingStrategy
{
    public long Calculate(Ticket t, DateTimeOffset exit)
    {
        var minutes = (long)(exit - t.Entry).TotalMinutes;
        var hours = Math.Max(1, (minutes + 59) / 60);                // round UP, minimum 1 hour
        return hours * ratePerHourPaise[t.Vehicle.Type];
    }
}

/// <summary>Decorator over any IPricingStrategy.</summary>
class EvSurcharge(IPricingStrategy baseStrategy, long chargingFeePaise) : IPricingStrategy
{
    public long Calculate(Ticket t, DateTimeOffset exit)
    {
        var fee = baseStrategy.Calculate(t, exit);
        return t.Vehicle.Electric ? fee + chargingFeePaise : fee;
    }
}

class ParkingLot(List<ParkingFloor> floors, IPricingStrategy pricing)
{
    public Ticket Park(Vehicle v, DateTimeOffset now)
    {
        foreach (var f in floors)
        {
            var slot = f.OccupySlotFor(v);
            if (slot is not null) return new Ticket(Guid.NewGuid().ToString(), v, slot, now);
        }
        throw new NoSlotAvailableException();
    }

    public long Unpark(Ticket t, DateTimeOffset exit)
    {
        var fee = pricing.Calculate(t, exit);
        t.Slot.Free();
        return fee;
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Parking Lot: slots decide fit, strategy prices, decorator adds EV fee");
        var rates = new Dictionary<VehicleType, long> { [VehicleType.Bike] = 1000, [VehicleType.Car] = 3000, [VehicleType.Truck] = 6000 };
        IPricingStrategy pricing = new EvSurcharge(new HourlyPricing(rates), 5000);
        var lot = new ParkingLot(new() { FloorFactory.Create(1, 2, 3, 1, 1) }, pricing);
        var t0 = DateTimeOffset.Parse("2026-11-20T10:00:00Z");

        var car = lot.Park(new Vehicle("UK08-1", VehicleType.Car), t0);
        Check.That(lot.Unpark(car, t0.AddMinutes(61)) == 6000, "61 minutes rounds up to 2 hours x 3000 paise = 6000");

        var ev = lot.Park(new Vehicle("UK08-EV", VehicleType.Car, true), t0);
        Check.That(ev.Slot is EvChargingSlot, "electric car is given the EV slot first");
        Check.That(lot.Unpark(ev, t0.AddMinutes(30)) == 3000 + 5000, "1 hour minimum + EV surcharge");

        var truck = lot.Park(new Vehicle("UK08-T", VehicleType.Truck), t0);
        Check.Throws<NoSlotAvailableException>(() => lot.Park(new Vehicle("UK08-T2", VehicleType.Truck), t0), "second truck: only one truck slot exists");
        lot.Unpark(truck, t0.AddHours(1));
        lot.Park(new Vehicle("UK08-T3", VehicleType.Truck), t0);
        Check.That(true, "freed slot can be reused");

        // Concurrency: 10 car slots, 50 gates racing. Exactly 10 may succeed.
        var busy = new ParkingLot(new() { FloorFactory.Create(1, 0, 10, 0, 0) }, pricing);
        var parked = 0;
        var slotIds = new ConcurrentDictionary<string, bool>();
        Parallel.For(0, 50, new ParallelOptions { MaxDegreeOfParallelism = 16 }, i =>
        {
            try
            {
                var t = busy.Park(new Vehicle($"CAR-{i}", VehicleType.Car), t0);
                Interlocked.Increment(ref parked);
                slotIds.TryAdd(t.Slot.Id, true);
            }
            catch (NoSlotAvailableException) { }
        });
        Check.That(parked == 10, "50 racing cars, exactly 10 tickets issued");
        Check.That(slotIds.Count == 10, "no slot was handed to two cars");
    }
}
