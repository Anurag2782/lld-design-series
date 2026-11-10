// Runs every example. Usage:  dotnet run            (all)   |   dotnet run -- strategy   (one)
using Lld;

var demos = new (string Name, Action Run)[]
{
    ("srp", Lld.Solid.Srp.Demo.Run),
    ("ocp", Lld.Solid.Ocp.Demo.Run),
    ("lsp", Lld.Solid.Lsp.Demo.Run),
    ("isp", Lld.Solid.Isp.Demo.Run),
    ("dip", Lld.Solid.Dip.Demo.Run),
    ("strategy", Lld.Patterns.Strategy.Demo.Run),
    ("factory", Lld.Patterns.Factory.Demo.Run),
    ("observer", Lld.Patterns.Observer.Demo.Run),
    ("builder", Lld.Patterns.Builder.Demo.Run),
    ("singleton", Lld.Patterns.Singleton.Demo.Run),
    ("decorator", Lld.Patterns.Decorator.Demo.Run),
    ("state", Lld.Patterns.State.Demo.Run),
    ("command", Lld.Patterns.Command.Demo.Run),
    ("chain", Lld.Patterns.Chain.Demo.Run),
    ("proxy", Lld.Patterns.Proxy.Demo.Run),
    ("adapter", Lld.Patterns.Adapter.Demo.Run),
    ("templatemethod", Lld.Patterns.TemplateMethod.Demo.Run),
    ("facade", Lld.Patterns.Facade.Demo.Run),
    ("composite", Lld.Patterns.Composite.Demo.Run),
    ("parkinglot", Lld.Problems.ParkingLot.Demo.Run),
    ("splitwise", Lld.Problems.Splitwise.Demo.Run),
    ("bookmyshow", Lld.Problems.BookMyShow.Demo.Run),
    ("elevator", Lld.Problems.Elevator.Demo.Run),
    ("ratelimiter", Lld.Problems.RateLimiter.Demo.Run),
    ("atm", Lld.Problems.Atm.Demo.Run),
    ("library", Lld.Problems.Library.Demo.Run),
    ("chess", Lld.Problems.Chess.Demo.Run),
};

var filter = args.Length > 0 ? args[0] : null;
var selected = demos.Where(d => filter is null || d.Name == filter).ToList();
if (selected.Count == 0) { Console.Error.WriteLine("No such example: " + filter); return 1; }
foreach (var d in selected) { Console.WriteLine($"=== {d.Name}"); d.Run(); }
Console.WriteLine($"\nALL {selected.Count} .NET EXAMPLES PASSED");
return 0;
