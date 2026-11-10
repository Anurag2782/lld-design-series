using System.Text;
using Lld.Common;

namespace Lld.Patterns.Command;

class Document
{
    private readonly StringBuilder _text = new();
    public void Append(string s) => _text.Append(s);
    public void DeleteLast(int n) => _text.Length -= n;
    public string Text => _text.ToString();
}

interface ICommand { void Execute(); void Undo(); }

class AddText(Document doc, string text) : ICommand
{
    public void Execute() => doc.Append(text);
    public void Undo() => doc.DeleteLast(text.Length);
}

class CommandHistory
{
    private readonly Stack<ICommand> _undo = new();
    private readonly Stack<ICommand> _redo = new();

    public void Run(ICommand c) { c.Execute(); _undo.Push(c); _redo.Clear(); }   // a new command kills redo
    public void Undo() { if (_undo.Count == 0) return; var c = _undo.Pop(); c.Undo(); _redo.Push(c); }
    public void Redo() { if (_redo.Count == 0) return; var c = _redo.Pop(); c.Execute(); _undo.Push(c); }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Command: a request as an object -> undo/redo");
        var doc = new Document();
        var history = new CommandHistory();
        history.Run(new AddText(doc, "Hello"));
        history.Run(new AddText(doc, " World"));
        Check.That(doc.Text == "Hello World", "two commands executed");
        history.Undo();
        Check.That(doc.Text == "Hello", "undo removes the last command's effect");
        history.Redo();
        Check.That(doc.Text == "Hello World", "redo re-applies it");
        history.Undo();
        history.Run(new AddText(doc, "!"));
        history.Redo();   // nothing to redo: the new command cleared the redo stack
        Check.That(doc.Text == "Hello!", "a new command clears the redo stack");
    }
}
