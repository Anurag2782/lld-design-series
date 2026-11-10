"""Command: a request as an object -> undo/redo."""
from abc import ABC, abstractmethod
from typing import List

from lld.common import check


class Document:
    def __init__(self): self._text = ""
    def append(self, s): self._text += s
    def delete_last(self, n): self._text = self._text[:-n] if n else self._text
    @property
    def text(self): return self._text


class Command(ABC):
    @abstractmethod
    def execute(self) -> None: ...
    @abstractmethod
    def undo(self) -> None: ...


class AddText(Command):
    def __init__(self, doc: Document, text: str): self._doc, self._text = doc, text
    def execute(self): self._doc.append(self._text)
    def undo(self): self._doc.delete_last(len(self._text))


class CommandHistory:
    def __init__(self):
        self._undo: List[Command] = []
        self._redo: List[Command] = []

    def run(self, c: Command) -> None:
        c.execute(); self._undo.append(c); self._redo.clear()   # a new command kills redo

    def undo(self) -> None:
        if not self._undo: return
        c = self._undo.pop(); c.undo(); self._redo.append(c)

    def redo(self) -> None:
        if not self._redo: return
        c = self._redo.pop(); c.execute(); self._undo.append(c)


def demo() -> None:
    print("Command: a request as an object -> undo/redo")
    doc, history = Document(), CommandHistory()
    history.run(AddText(doc, "Hello"))
    history.run(AddText(doc, " World"))
    check(doc.text == "Hello World", "two commands executed")
    history.undo()
    check(doc.text == "Hello", "undo removes the last command's effect")
    history.redo()
    check(doc.text == "Hello World", "redo re-applies it")
    history.undo()
    history.run(AddText(doc, "!"))
    history.redo()   # nothing to redo: the new command cleared the redo stack
    check(doc.text == "Hello!", "a new command clears the redo stack")


if __name__ == "__main__":
    demo()
