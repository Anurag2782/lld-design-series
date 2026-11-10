package lld.patterns.command;

import lld.common.Check;
import java.util.ArrayDeque;
import java.util.Deque;

class Document {
    private final StringBuilder text = new StringBuilder();
    void append(String s) { text.append(s); }
    void deleteLast(int n) { text.setLength(text.length() - n); }
    String text() { return text.toString(); }
}

interface Command { void execute(); void undo(); }

class AddText implements Command {
    private final Document doc; private final String text;
    AddText(Document doc, String text) { this.doc = doc; this.text = text; }
    public void execute() { doc.append(text); }
    public void undo()    { doc.deleteLast(text.length()); }
}

class CommandHistory {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    void run(Command c) { c.execute(); undoStack.push(c); redoStack.clear(); }   // new command kills redo
    void undo() {
        if (undoStack.isEmpty()) return;
        Command c = undoStack.pop(); c.undo(); redoStack.push(c);
    }
    void redo() {
        if (redoStack.isEmpty()) return;
        Command c = redoStack.pop(); c.execute(); undoStack.push(c);
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Command: a request as an object -> undo/redo");
        Document doc = new Document();
        CommandHistory history = new CommandHistory();
        history.run(new AddText(doc, "Hello"));
        history.run(new AddText(doc, " World"));
        Check.that(doc.text().equals("Hello World"), "two commands executed");
        history.undo();
        Check.that(doc.text().equals("Hello"), "undo removes the last command's effect");
        history.redo();
        Check.that(doc.text().equals("Hello World"), "redo re-applies it");
        history.undo();
        history.run(new AddText(doc, "!"));
        history.redo();   // nothing to redo: the new command cleared the redo stack
        Check.that(doc.text().equals("Hello!"), "a new command clears the redo stack");
    }
}
