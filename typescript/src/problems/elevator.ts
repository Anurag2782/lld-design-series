import { check } from "../common.ts";

type Direction = "UP" | "DOWN" | "IDLE";

class Elevator {
  readonly id: number;
  private _floor: number;
  private _dir: Direction = "IDLE";
  private upStops: number[] = [];       // ascending
  private downStops: number[] = [];     // descending
  doorOpenedAt: number[] = [];          // for tests and logging

  constructor(id: number, startFloor: number) { this.id = id; this._floor = startFloor; }

  addStop(target: number): void {
    if (target === this._floor) { this.doorOpenedAt.push(this._floor); return; }
    if (target > this._floor) { if (!this.upStops.includes(target)) { this.upStops.push(target); this.upStops.sort((a, b) => a - b); } }
    else if (!this.downStops.includes(target)) { this.downStops.push(target); this.downStops.sort((a, b) => b - a); }
    if (this._dir === "IDLE") this._dir = target > this._floor ? "UP" : "DOWN";
  }

  /** One tick: keep going the same way, serve stops on the way, reverse only when nothing is ahead. */
  step(): void {
    if (this._dir === "UP") {
      if (this.upStops.length === 0) { this._dir = this.downStops.length === 0 ? "IDLE" : "DOWN"; return; }
      this._floor++;
      const i = this.upStops.indexOf(this._floor);
      if (i >= 0) { this.upStops.splice(i, 1); this.doorOpenedAt.push(this._floor); }
    } else if (this._dir === "DOWN") {
      if (this.downStops.length === 0) { this._dir = this.upStops.length === 0 ? "IDLE" : "UP"; return; }
      this._floor--;
      const i = this.downStops.indexOf(this._floor);
      if (i >= 0) { this.downStops.splice(i, 1); this.doorOpenedAt.push(this._floor); }
    }
  }

  /** Heuristic cost: lower is better. A large penalty when the car must finish and turn around. */
  cost(target: number, want: Direction): number {
    const distance = Math.abs(this._floor - target);
    if (this._dir === "IDLE") return distance;
    const onTheWay = (this._dir === "UP" && want === "UP" && target >= this._floor)
                  || (this._dir === "DOWN" && want === "DOWN" && target <= this._floor);
    return onTheWay ? distance : distance + 100;
  }

  get floor() { return this._floor; }
  get direction() { return this._dir; }
}

interface ElevatorSelectionStrategy { select(elevators: Elevator[], floor: number, want: Direction): Elevator }

class NearestElevatorStrategy implements ElevatorSelectionStrategy {
  select(elevators: Elevator[], floor: number, want: Direction): Elevator {
    return [...elevators].sort((a, b) => a.cost(floor, want) - b.cost(floor, want) || a.id - b.id)[0]!;
  }
}

class ElevatorSystem {
  private elevators: Elevator[];
  private strategy: ElevatorSelectionStrategy;
  constructor(elevators: Elevator[], strategy: ElevatorSelectionStrategy) { this.elevators = elevators; this.strategy = strategy; }

  /** Hall call: the system CHOOSES an elevator. */
  requestElevator(floor: number, want: Direction): Elevator {
    const e = this.strategy.select(this.elevators, floor, want);
    e.addStop(floor);
    return e;
  }
  /** Car call: already inside a specific elevator, no choice to make. */
  selectFloor(e: Elevator, floor: number): void { e.addStop(floor); }
}

export function demo(): void {
  console.log("Elevator: SCAN movement + a cost-based selection strategy");
  const solo = new Elevator(0, 0);
  [9, 2, 8].forEach((f) => solo.addStop(f));
  for (let tick = 0; tick < 50 && solo.direction !== "IDLE"; tick++) solo.step();
  check(solo.doorOpenedAt.join() === "2,8,9", "pressed 9,2,8 -> served in order 2, 8, 9 (no zig-zag)");

  const e0 = new Elevator(0, 2);                        // idle at floor 2
  const e1 = new Elevator(1, 7); e1.addStop(3);         // at 7 heading DOWN
  const e2 = new Elevator(2, 12); e2.addStop(10);       // at 12 heading DOWN
  check([e0, e1, e2].map((e) => e.cost(6, "UP")).join() === "4,101,106", "costs for an UP call at floor 6: 4, 101, 106");
  const system = new ElevatorSystem([e0, e1, e2], new NearestElevatorStrategy());
  check(system.requestElevator(6, "UP").id === 0, "the idle elevator at 2 wins, not the 'closer' one at 7");

  const turning = new Elevator(3, 5);
  turning.addStop(8); turning.addStop(2);
  for (let i = 0; i < 4; i++) turning.step();
  check(turning.floor === 8 && turning.direction === "DOWN", "reaches the upward stop 8 first, then turns to go DOWN");
  for (let i = 0; i < 12 && turning.direction !== "IDLE"; i++) turning.step();
  check(turning.doorOpenedAt.join() === "8,2", "serves upward stop 8, then reverses for 2");
}
