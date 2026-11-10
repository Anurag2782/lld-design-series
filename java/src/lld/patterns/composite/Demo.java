package lld.patterns.composite;

import lld.common.Check;
import java.util.ArrayList;
import java.util.List;

/** Component: files and folders are treated UNIFORMLY. */
interface FileSystemNode {
    String name();
    long size();
}

class FileNode implements FileSystemNode {
    private final String name; private final long size;
    FileNode(String name, long size) { this.name = name; this.size = size; }
    public String name() { return name; }
    public long size() { return size; }
}

class DirectoryNode implements FileSystemNode {
    private final String name;
    private final List<FileSystemNode> children = new ArrayList<>();
    DirectoryNode(String name) { this.name = name; }
    public String name() { return name; }

    void add(FileSystemNode node) {
        if (node == this || (node instanceof DirectoryNode d && d.contains(this)))
            throw new IllegalArgumentException("cycle: a folder cannot contain itself");
        children.add(node);
    }
    boolean contains(FileSystemNode target) {
        for (FileSystemNode c : children)
            if (c == target || (c instanceof DirectoryNode d && d.contains(target))) return true;
        return false;
    }
    /** A folder's size is the sum of its children: recursion hides the difference between file and folder. */
    public long size() { return children.stream().mapToLong(FileSystemNode::size).sum(); }

    List<String> find(String suffix) {
        List<String> out = new ArrayList<>();
        for (FileSystemNode c : children) {
            if (c instanceof DirectoryNode d) out.addAll(d.find(suffix).stream().map(p -> name + "/" + p).toList());
            else if (c.name().endsWith(suffix)) out.add(name + "/" + c.name());
        }
        return out;
    }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("Composite: treat one object and a group of objects the same way");
        DirectoryNode root = new DirectoryNode("root");
        DirectoryNode docs = new DirectoryNode("docs");
        docs.add(new FileNode("a.txt", 100));
        docs.add(new FileNode("b.pdf", 900));
        DirectoryNode src = new DirectoryNode("src");
        src.add(new FileNode("Main.java", 400));
        docs.add(src);
        root.add(docs);
        root.add(new FileNode("readme.txt", 50));

        FileSystemNode asNode = root;       // the caller does not care if it's a file or a folder
        Check.that(asNode.size() == 1450, "folder size = sum of everything inside, at any depth");
        Check.that(new FileNode("x", 7).size() == 7, "a single file answers the same question");
        Check.that(root.find(".txt").equals(List.of("root/docs/a.txt", "root/readme.txt")), "find walks the whole tree");
        Check.throwsError(() -> src.add(root), IllegalArgumentException.class, "adding an ancestor would create a cycle: rejected");
    }
}
