package lld.problems.elevator;

import lld.common.Check;
import java.util.*;

enum Direction { UP, DOWN, IDLE }

class Elevator {
    private final int id;
    private int floor;
    private Direction dir = Direction.IDLE;
    private final TreeSet<Integer> upStops = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>(Comparator.reverseOrder());
    final List<Integer> doorOpenedAt = new ArrayList<>();      // for tests and logging

    Elevator(int id, int startFloor) { this.id = id; this.floor = startFloor; }

    synchronized void addStop(int target) {
        if (target == floor) { openDoor(); return; }
        if (target > floor) upStops.add(target); else downStops.add(target);
        if (dir == Direction.IDLE) dir = target > floor ? Direction.UP : Direction.DOWN;
    }

    /** One tick: keep going the same way, serve stops on the way, reverse only when nothing is ahead. */
    synchronized void step() {
        if (dir == Direction.UP) {
            if (upStops.isEmpty()) { dir = downStops.isEmpty() ? Direction.IDLE : Direction.DOWN; return; }
            floor++;
            if (upStops.remove(floor)) openDoor();
        } else if (dir == Direction.DOWN) {
            if (downStops.isEmpty()) { dir = upStops.isEmpty() ? Direction.IDLE : Direction.UP; return; }
            floor--;
            if (downStops.remove(floor)) openDoor();
        }
    }

    /** Heuristic cost: lower is better. A large penalty when the car must finish and turn around. */
    synchronized int cost(int target, Direction want) {
        int distance = Math.abs(floor - target);
        if (dir == Direction.IDLE) return distance;
        boolean onTheWay = (dir == Direction.UP && want == Direction.UP && target >= floor)
                        || (dir == Direction.DOWN && want == Direction.DOWN && target <= floor);
        return onTheWay ? distance : distance + 100;
    }

    private void openDoor() { doorOpenedAt.add(floor); }
    synchronized int floor() { return floor; }
    synchronized Direction direction() { return dir; }
    int id() { return id; }
}

interface ElevatorSelectionStrategy {
    Elevator select(List<Elevator> elevators, int floor, Direction want);
}

class NearestElevatorStrategy implements ElevatorSelectionStrategy {
    public Elevator select(List<Elevator> elevators, int floor, Direction want) {
        return elevators.stream().min(Comparator.comparingInt((Elevator e) -> e.cost(floor, want)).thenComparingInt(Elevator::id)).orElseThrow();
    }
}

class ElevatorSystem {
    private final List<Elevator> elevators;
    private final ElevatorSelectionStrategy strategy;
    ElevatorSystem(List<Elevator> elevators, ElevatorSelectionStrategy strategy) { this.elevators = elevators; this.strategy = strategy; }

    /** Hall call: the system CHOOSES an elevator. */
    Elevator requestElevator(int floor, Direction want) {
        Elevator e = strategy.select(elevators, floor, want);
        e.addStop(floor);
        return e;
    }
    /** Car call: already inside a specific elevator, no choice to make. */
    void selectFloor(Elevator e, int floor) { e.addStop(floor); }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Elevator: SCAN movement + a cost-based selection strategy");
        Elevator solo = new Elevator(0, 0);
        solo.addStop(9); solo.addStop(2); solo.addStop(8);
        for (int tick = 0; tick < 50 && solo.direction() != Direction.IDLE; tick++) solo.step();
        Check.that(solo.doorOpenedAt.equals(List.of(2, 8, 9)), "pressed 9,2,8 -> served in order 2, 8, 9 (no zig-zag)");

        Elevator e0 = new Elevator(0, 2);                       // idle at floor 2
        Elevator e1 = new Elevator(1, 7); e1.addStop(3);        // at 7 heading DOWN
        Elevator e2 = new Elevator(2, 12); e2.addStop(10);      // at 12 heading DOWN
        Check.that(e0.cost(6, Direction.UP) == 4 && e1.cost(6, Direction.UP) == 101 && e2.cost(6, Direction.UP) == 106,
            "costs for an UP call at floor 6: 4, 101, 106");
        ElevatorSystem system = new ElevatorSystem(List.of(e0, e1, e2), new NearestElevatorStrategy());
        Check.that(system.requestElevator(6, Direction.UP).id() == 0, "the idle elevator at 2 wins, not the 'closer' one at 7");

        Elevator turning = new Elevator(3, 5);
        turning.addStop(8); turning.addStop(2);
        for (int i = 0; i < 4; i++) turning.step();
        Check.that(turning.floor() == 8 && turning.direction() == Direction.DOWN, "reaches the upward stop 8 first, then turns to go DOWN");
        for (int i = 0; i < 12 && turning.direction() != Direction.IDLE; i++) turning.step();
        Check.that(turning.doorOpenedAt.equals(List.of(8, 2)), "serves upward stop 8, then reverses for 2");
    }
}
