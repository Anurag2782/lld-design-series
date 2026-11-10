"""Elevator: SCAN movement + a cost-based selection strategy."""
import bisect
import threading
from abc import ABC, abstractmethod
from enum import Enum
from typing import List

from lld.common import check


class Direction(Enum):
    UP = "UP"
    DOWN = "DOWN"
    IDLE = "IDLE"


class Elevator:
    def __init__(self, elevator_id: int, start_floor: int):
        self.id = elevator_id
        self._floor = start_floor
        self._dir = Direction.IDLE
        self._up: List[int] = []        # ascending
        self._down: List[int] = []      # kept ascending; we pop from the END for "nearest below"
        self.door_opened_at: List[int] = []
        self._lock = threading.Lock()

    def add_stop(self, target: int) -> None:
        with self._lock:
            if target == self._floor:
                self.door_opened_at.append(self._floor)
                return
            stops = self._up if target > self._floor else self._down
            if target not in stops:
                bisect.insort(stops, target)
            if self._dir == Direction.IDLE:
                self._dir = Direction.UP if target > self._floor else Direction.DOWN

    def step(self) -> None:
        """One tick: keep going the same way, serve stops on the way, reverse only when nothing is ahead."""
        with self._lock:
            if self._dir == Direction.UP:
                if not self._up:
                    self._dir = Direction.IDLE if not self._down else Direction.DOWN
                    return
                self._floor += 1
                if self._floor in self._up:
                    self._up.remove(self._floor)
                    self.door_opened_at.append(self._floor)
            elif self._dir == Direction.DOWN:
                if not self._down:
                    self._dir = Direction.IDLE if not self._up else Direction.UP
                    return
                self._floor -= 1
                if self._floor in self._down:
                    self._down.remove(self._floor)
                    self.door_opened_at.append(self._floor)

    def cost(self, target: int, want: Direction) -> int:
        """Heuristic cost: lower is better. A large penalty when the car must finish and turn around."""
        with self._lock:
            distance = abs(self._floor - target)
            if self._dir == Direction.IDLE:
                return distance
            on_the_way = (self._dir == Direction.UP and want == Direction.UP and target >= self._floor) or \
                         (self._dir == Direction.DOWN and want == Direction.DOWN and target <= self._floor)
            return distance if on_the_way else distance + 100

    @property
    def floor(self) -> int:
        with self._lock:
            return self._floor

    @property
    def direction(self) -> Direction:
        with self._lock:
            return self._dir


class ElevatorSelectionStrategy(ABC):
    @abstractmethod
    def select(self, elevators: List[Elevator], floor: int, want: Direction) -> Elevator: ...


class NearestElevatorStrategy(ElevatorSelectionStrategy):
    def select(self, elevators, floor, want):
        return min(elevators, key=lambda e: (e.cost(floor, want), e.id))


class ElevatorSystem:
    def __init__(self, elevators: List[Elevator], strategy: ElevatorSelectionStrategy):
        self._elevators, self._strategy = elevators, strategy

    def request_elevator(self, floor: int, want: Direction) -> Elevator:
        """Hall call: the system CHOOSES an elevator."""
        e = self._strategy.select(self._elevators, floor, want)
        e.add_stop(floor)
        return e

    def select_floor(self, e: Elevator, floor: int) -> None:
        """Car call: already inside a specific elevator, no choice to make."""
        e.add_stop(floor)


def demo() -> None:
    print("Elevator: SCAN movement + a cost-based selection strategy")
    solo = Elevator(0, 0)
    for f in (9, 2, 8):
        solo.add_stop(f)
    for _ in range(50):
        if solo.direction == Direction.IDLE:
            break
        solo.step()
    check(solo.door_opened_at == [2, 8, 9], "pressed 9,2,8 -> served in order 2, 8, 9 (no zig-zag)")

    e0 = Elevator(0, 2)                                  # idle at floor 2
    e1 = Elevator(1, 7); e1.add_stop(3)                  # at 7 heading DOWN
    e2 = Elevator(2, 12); e2.add_stop(10)                # at 12 heading DOWN
    check((e0.cost(6, Direction.UP), e1.cost(6, Direction.UP), e2.cost(6, Direction.UP)) == (4, 101, 106), "costs for an UP call at floor 6: 4, 101, 106")
    system = ElevatorSystem([e0, e1, e2], NearestElevatorStrategy())
    check(system.request_elevator(6, Direction.UP).id == 0, "the idle elevator at 2 wins, not the 'closer' one at 7")

    turning = Elevator(3, 5)
    turning.add_stop(8); turning.add_stop(2)
    for _ in range(4):
        turning.step()
    check(turning.floor == 8 and turning.direction == Direction.DOWN, "reaches the upward stop 8 first, then turns to go DOWN")
    for _ in range(12):
        if turning.direction == Direction.IDLE:
            break
        turning.step()
    check(turning.door_opened_at == [8, 2], "serves upward stop 8, then reverses for 2")


if __name__ == "__main__":
    demo()
