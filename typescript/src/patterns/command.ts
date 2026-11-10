import { check } from "../common.ts";

class TextDocument {
  private text = "";
  append(s: string) { this.text += s; }
  deleteLast(n: number) { this.text = this.text.slice(0, this.text.length - n); }
  getText() { return this.text; }
}

interface Command { execute(): void; undo(): void }

class AddText implements Command {
  private doc: TextDocument;
  private text: string;
  constructor(doc: TextDocument, text: string) { this.doc = doc; this.text = text; }
  execute() { this.doc.append(this.text); }
  undo() { this.doc.deleteLast(this.text.length); }
}

class CommandHistory {
  private undoStack: Command[] = [];
  private redoStack: Command[] = [];

  run(c: Command) { c.execute(); this.undoStack.push(c); this.redoStack = []; }   // a new command kills redo
  undo() { const c = this.undoStack.pop(); if (!c) return; c.undo(); this.redoStack.push(c); }
  redo() { const c = this.redoStack.pop(); if (!c) return; c.execute(); this.undoStack.push(c); }
}

export function demo(): void {
  console.log("Command: a request as an object -> undo/redo");
  const doc = new TextDocument();
  const history = new CommandHistory();
  history.run(new AddText(doc, "Hello"));
  history.run(new AddText(doc, " World"));
  check(doc.getText() === "Hello World", "two commands executed");
  history.undo();
  check(doc.getText() === "Hello", "undo removes the last command's effect");
  history.redo();
  check(doc.getText() === "Hello World", "redo re-applies it");
  history.undo();
  history.run(new AddText(doc, "!"));
  history.redo();   // nothing to redo: the new command cleared the redo stack
  check(doc.getText() === "Hello!", "a new command clears the redo stack");
}
