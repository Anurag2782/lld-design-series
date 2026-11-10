import { check, throws } from "../common.ts";

/** Component: files and folders are treated UNIFORMLY. */
interface FileSystemNode {
  readonly name: string;
  size(): number;
}

class FileNode implements FileSystemNode {
  readonly name: string;
  private bytes: number;
  constructor(name: string, bytes: number) { this.name = name; this.bytes = bytes; }
  size() { return this.bytes; }
}

class DirectoryNode implements FileSystemNode {
  readonly name: string;
  private children: FileSystemNode[] = [];
  constructor(name: string) { this.name = name; }

  add(node: FileSystemNode): void {
    if (node === this || (node instanceof DirectoryNode && node.contains(this)))
      throw new Error("cycle: a folder cannot contain itself");
    this.children.push(node);
  }
  contains(target: FileSystemNode): boolean {
    return this.children.some((c) => c === target || (c instanceof DirectoryNode && c.contains(target)));
  }
  /** A folder's size is the sum of its children: recursion hides the difference between file and folder. */
  size(): number { return this.children.reduce((sum, c) => sum + c.size(), 0); }

  find(suffix: string): string[] {
    const out: string[] = [];
    for (const c of this.children) {
      if (c instanceof DirectoryNode) out.push(...c.find(suffix).map((p) => `${this.name}/${p}`));
      else if (c.name.endsWith(suffix)) out.push(`${this.name}/${c.name}`);
    }
    return out;
  }
}

export function demo(): void {
  console.log("Composite: treat one object and a group of objects the same way");
  const root = new DirectoryNode("root"), docs = new DirectoryNode("docs"), src = new DirectoryNode("src");
  docs.add(new FileNode("a.txt", 100));
  docs.add(new FileNode("b.pdf", 900));
  src.add(new FileNode("main.ts", 400));
  docs.add(src);
  root.add(docs);
  root.add(new FileNode("readme.txt", 50));

  const node: FileSystemNode = root;      // the caller does not care if it's a file or a folder
  check(node.size() === 1450, "folder size = sum of everything inside, at any depth");
  check(new FileNode("x", 7).size() === 7, "a single file answers the same question");
  check(root.find(".txt").join() === "root/docs/a.txt,root/readme.txt", "find walks the whole tree");
  throws(() => src.add(root), "adding an ancestor would create a cycle: rejected");
}
