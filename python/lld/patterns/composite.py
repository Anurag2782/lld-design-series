"""Composite: treat one object and a group of objects the same way."""
from abc import ABC, abstractmethod
from typing import List

from lld.common import check, raises


class FileSystemNode(ABC):
    """Component: files and folders are treated UNIFORMLY."""
    name: str

    @property
    @abstractmethod
    def size(self) -> int: ...


class FileNode(FileSystemNode):
    def __init__(self, name: str, size: int): self.name, self._size = name, size

    @property
    def size(self): return self._size


class DirectoryNode(FileSystemNode):
    def __init__(self, name: str):
        self.name = name
        self._children: List[FileSystemNode] = []

    def add(self, node: FileSystemNode) -> None:
        if node is self or (isinstance(node, DirectoryNode) and node.contains(self)):
            raise ValueError("cycle: a folder cannot contain itself")
        self._children.append(node)

    def contains(self, target: FileSystemNode) -> bool:
        return any(c is target or (isinstance(c, DirectoryNode) and c.contains(target)) for c in self._children)

    @property
    def size(self):
        """A folder's size is the sum of its children: recursion hides the difference between file and folder."""
        return sum(c.size for c in self._children)

    def find(self, suffix: str) -> List[str]:
        out: List[str] = []
        for c in self._children:
            if isinstance(c, DirectoryNode):
                out += [f"{self.name}/{p}" for p in c.find(suffix)]
            elif c.name.endswith(suffix):
                out.append(f"{self.name}/{c.name}")
        return out


def demo() -> None:
    print("Composite: treat one object and a group of objects the same way")
    root, docs, src = DirectoryNode("root"), DirectoryNode("docs"), DirectoryNode("src")
    docs.add(FileNode("a.txt", 100))
    docs.add(FileNode("b.pdf", 900))
    src.add(FileNode("main.py", 400))
    docs.add(src)
    root.add(docs)
    root.add(FileNode("readme.txt", 50))

    node: FileSystemNode = root          # the caller does not care if it's a file or a folder
    check(node.size == 1450, "folder size = sum of everything inside, at any depth")
    check(FileNode("x", 7).size == 7, "a single file answers the same question")
    check(root.find(".txt") == ["root/docs/a.txt", "root/readme.txt"], "find walks the whole tree")
    raises(lambda: src.add(root), ValueError, "adding an ancestor would create a cycle: rejected")


if __name__ == "__main__":
    demo()
