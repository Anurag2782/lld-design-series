"""Parking Lot: slots decide fit, strategy prices, decorator adds the EV fee."""
import threading
import uuid
from abc import ABC, abstractmethod
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from enum import Enum
from typing import Dict, List, Optional

from lld.common import check, raises


class VehicleType(Enum):
    BIKE = "BIKE"
    CAR = "CAR"
    TRUCK = "TRUCK"


@dataclass(frozen=True)
class Vehicle:
    plate: str
    type: VehicleType
    electric: bool = False


class NoSlotAvailable(Exception):
    pass


class ParkingSlot(ABC):
    """The slot owns its own "does this vehicle fit?" rule."""

    def __init__(self, slot_id: str):
        self.id = slot_id
        self._occupied = False
        self._lock = threading.Lock()

    @abstractmethod
    def accepts(self, v: Vehicle) -> bool: ...

    def try_occupy(self, v: Vehicle) -> bool:
        """Check AND set as one atomic step: two gates can never get the same slot."""
        with self._lock:
            if self._occupied or not self.accepts(v):
                return False
            self._occupied = True
            return True

    def free(self) -> None:
        with self._lock:
            self._occupied = False


class StandardSlot(ParkingSlot):
    def __init__(self, slot_id: str, vtype: VehicleType):
        super().__init__(slot_id)
        self._type = vtype

    def accepts(self, v): return v.type == self._type


class EvChargingSlot(ParkingSlot):
    """The extension: only electric cars fit. No other existing class changes."""
    def accepts(self, v): return v.electric and v.type == VehicleType.CAR


@dataclass(frozen=True)
class Ticket:
    id: str
    vehicle: Vehicle
    slot: ParkingSlot
    entry: datetime


class ParkingFloor:
    def __init__(self, slots: List[ParkingSlot]): self._slots = slots

    def occupy_slot_for(self, v: Vehicle) -> Optional[ParkingSlot]:
        return next((s for s in self._slots if s.try_occupy(v)), None)


def create_floor(floor_no: int, bikes: int, cars: int, trucks: int, ev_slots: int) -> ParkingFloor:
    slots: List[ParkingSlot] = [EvChargingSlot(f"F{floor_no}-EV-{i}") for i in range(ev_slots)]
    for vtype, n in ((VehicleType.BIKE, bikes), (VehicleType.CAR, cars), (VehicleType.TRUCK, trucks)):
        slots += [StandardSlot(f"F{floor_no}-{vtype.value}-{i}", vtype) for i in range(n)]
    return ParkingFloor(slots)


class PricingStrategy(ABC):
    """Money is in paise (int), never float."""
    @abstractmethod
    def calculate(self, t: Ticket, exit_time: datetime) -> int: ...


class HourlyPricing(PricingStrategy):
    def __init__(self, rates: Dict[VehicleType, int]): self._rates = rates

    def calculate(self, t, exit_time):
        minutes = int((exit_time - t.entry).total_seconds() // 60)
        hours = max(1, (minutes + 59) // 60)            # round UP, minimum 1 hour
        return hours * self._rates[t.vehicle.type]


class EvSurcharge(PricingStrategy):
    """Decorator over any PricingStrategy."""
    def __init__(self, base: PricingStrategy, fee: int): self._base, self._fee = base, fee

    def calculate(self, t, exit_time):
        fee = self._base.calculate(t, exit_time)
        return fee + self._fee if t.vehicle.electric else fee


class ParkingLot:
    def __init__(self, floors: List[ParkingFloor], pricing: PricingStrategy):
        self._floors, self._pricing = floors, pricing

    def park(self, v: Vehicle, now: datetime) -> Ticket:
        for f in self._floors:
            slot = f.occupy_slot_for(v)
            if slot:
                return Ticket(str(uuid.uuid4()), v, slot, now)
        raise NoSlotAvailable("No slot available")

    def unpark(self, t: Ticket, exit_time: datetime) -> int:
        fee = self._pricing.calculate(t, exit_time)
        t.slot.free()
        return fee


def demo() -> None:
    print("Parking Lot: slots decide fit, strategy prices, decorator adds EV fee")
    rates = {VehicleType.BIKE: 1000, VehicleType.CAR: 3000, VehicleType.TRUCK: 6000}
    pricing = EvSurcharge(HourlyPricing(rates), 5000)
    lot = ParkingLot([create_floor(1, 2, 3, 1, 1)], pricing)
    t0 = datetime(2026, 11, 20, 10, 0, tzinfo=timezone.utc)

    car = lot.park(Vehicle("UK08-1", VehicleType.CAR), t0)
    check(lot.unpark(car, t0 + timedelta(minutes=61)) == 6000, "61 minutes rounds up to 2 hours x 3000 paise = 6000")

    ev = lot.park(Vehicle("UK08-EV", VehicleType.CAR, True), t0)
    check(isinstance(ev.slot, EvChargingSlot), "electric car is given the EV slot first")
    check(lot.unpark(ev, t0 + timedelta(minutes=30)) == 3000 + 5000, "1 hour minimum + EV surcharge")

    truck = lot.park(Vehicle("UK08-T", VehicleType.TRUCK), t0)
    raises(lambda: lot.park(Vehicle("UK08-T2", VehicleType.TRUCK), t0), NoSlotAvailable, "second truck: only one truck slot exists")
    lot.unpark(truck, t0 + timedelta(hours=1))
    lot.park(Vehicle("UK08-T3", VehicleType.TRUCK), t0)
    check(True, "freed slot can be reused")

    # Concurrency: 10 car slots, 50 gates racing. Exactly 10 may succeed.
    busy = ParkingLot([create_floor(1, 0, 10, 0, 0)], pricing)

    def try_park(i: int):
        try:
            return busy.park(Vehicle(f"CAR-{i}", VehicleType.CAR), t0)
        except NoSlotAvailable:
            return None

    with ThreadPoolExecutor(max_workers=16) as pool:
        tickets = [t for t in pool.map(try_park, range(50)) if t]
    check(len(tickets) == 10, "50 racing cars, exactly 10 tickets issued")
    check(len({t.slot.id for t in tickets}) == 10, "no slot was handed to two cars")


if __name__ == "__main__":
    demo()
