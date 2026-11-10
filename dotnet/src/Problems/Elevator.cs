using Lld.Common;

namespace Lld.Problems.Elevator;

enum Direction { Up, Down, Idle }

class Elevator(int id, int startFloor)
{
    private readonly object _lock = new();
    private int _floor = startFloor;
    private Direction _dir = Direction.Idle;
    private readonly SortedSet<int> _upStops = new();
    private readonly SortedSet<int> _downStops = new(Comparer<int>.Create((a, b) => b.CompareTo(a)));   // descending
    public List<int> DoorOpenedAt { get; } = new();      // for tests and logging
    public int Id { get; } = id;

    public void AddStop(int target)
    {
        lock (_lock)
        {
            if (target == _floor) { OpenDoor(); return; }
            if (target > _floor) _upStops.Add(target); else _downStops.Add(target);
            if (_dir == Direction.Idle) _dir = target > _floor ? Direction.Up : Direction.Down;
        }
    }

    /// <summary>One tick: keep going the same way, serve stops on the way, reverse only when nothing is ahead.</summary>
    public void Step()
    {
        lock (_lock)
        {
            if (_dir == Direction.Up)
            {
                if (_upStops.Count == 0) { _dir = _downStops.Count == 0 ? Direction.Idle : Direction.Down; return; }
                _floor++;
                if (_upStops.Remove(_floor)) OpenDoor();
            }
            else if (_dir == Direction.Down)
            {
                if (_downStops.Count == 0) { _dir = _upStops.Count == 0 ? Direction.Idle : Direction.Up; return; }
                _floor--;
                if (_downStops.Remove(_floor)) OpenDoor();
            }
        }
    }

    /// <summary>Heuristic cost: lower is better. A large penalty when the car must finish and turn around.</summary>
    public int Cost(int target, Direction want)
    {
        lock (_lock)
        {
            var distance = Math.Abs(_floor - target);
            if (_dir == Direction.Idle) return distance;
            var onTheWay = (_dir == Direction.Up && want == Direction.Up && target >= _floor)
                        || (_dir == Direction.Down && want == Direction.Down && target <= _floor);
            return onTheWay ? distance : distance + 100;
        }
    }

    private void OpenDoor() => DoorOpenedAt.Add(_floor);
    public int Floor { get { lock (_lock) return _floor; } }
    public Direction Direction { get { lock (_lock) return _dir; } }
}

interface IElevatorSelectionStrategy
{
    Elevator Select(IReadOnlyList<Elevator> elevators, int floor, Direction want);
}

class NearestElevatorStrategy : IElevatorSelectionStrategy
{
    public Elevator Select(IReadOnlyList<Elevator> elevators, int floor, Direction want) =>
        elevators.OrderBy(e => e.Cost(floor, want)).ThenBy(e => e.Id).First();
}

class ElevatorSystem(List<Elevator> elevators, IElevatorSelectionStrategy strategy)
{
    /// <summary>Hall call: the system CHOOSES an elevator.</summary>
    public Elevator RequestElevator(int floor, Direction want)
    {
        var e = strategy.Select(elevators, floor, want);
        e.AddStop(floor);
        return e;
    }
    /// <summary>Car call: already inside a specific elevator, no choice to make.</summary>
    public void SelectFloor(Elevator e, int floor) => e.AddStop(floor);
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Elevator: SCAN movement + a cost-based selection strategy");
        var solo = new Elevator(0, 0);
        solo.AddStop(9); solo.AddStop(2); solo.AddStop(8);
        for (var tick = 0; tick < 50 && solo.Direction != Direction.Idle; tick++) solo.Step();
        Check.That(solo.DoorOpenedAt.SequenceEqual(new[] { 2, 8, 9 }), "pressed 9,2,8 -> served in order 2, 8, 9 (no zig-zag)");

        var e0 = new Elevator(0, 2);                          // idle at floor 2
        var e1 = new Elevator(1, 7); e1.AddStop(3);           // at 7 heading DOWN
        var e2 = new Elevator(2, 12); e2.AddStop(10);         // at 12 heading DOWN
        Check.That(e0.Cost(6, Direction.Up) == 4 && e1.Cost(6, Direction.Up) == 101 && e2.Cost(6, Direction.Up) == 106, "costs for an UP call at floor 6: 4, 101, 106");
        var system = new ElevatorSystem(new() { e0, e1, e2 }, new NearestElevatorStrategy());
        Check.That(system.RequestElevator(6, Direction.Up).Id == 0, "the idle elevator at 2 wins, not the 'closer' one at 7");

        var turning = new Elevator(3, 5);
        turning.AddStop(8); turning.AddStop(2);
        for (var i = 0; i < 4; i++) turning.Step();
        Check.That(turning.Floor == 8 && turning.Direction == Direction.Down, "reaches the upward stop 8 first, then turns to go DOWN");
        for (var i = 0; i < 12 && turning.Direction != Direction.Idle; i++) turning.Step();
        Check.That(turning.DoorOpenedAt.SequenceEqual(new[] { 8, 2 }), "serves upward stop 8, then reverses for 2");
    }
}
