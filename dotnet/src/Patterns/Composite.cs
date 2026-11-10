using Lld.Common;

namespace Lld.Patterns.Composite;

/// <summary>Component: files and folders are treated UNIFORMLY.</summary>
interface IFileSystemNode
{
    string Name { get; }
    long Size { get; }
}

class FileNode(string name, long size) : IFileSystemNode
{
    public string Name => name;
    public long Size => size;
}

class DirectoryNode(string name) : IFileSystemNode
{
    private readonly List<IFileSystemNode> _children = new();
    public string Name => name;

    public void Add(IFileSystemNode node)
    {
        if (ReferenceEquals(node, this) || (node is DirectoryNode d && d.Contains(this)))
            throw new ArgumentException("cycle: a folder cannot contain itself");
        _children.Add(node);
    }

    public bool Contains(IFileSystemNode target) =>
        _children.Any(c => ReferenceEquals(c, target) || (c is DirectoryNode d && d.Contains(target)));

    /// <summary>A folder's size is the sum of its children: recursion hides the difference between file and folder.</summary>
    public long Size => _children.Sum(c => c.Size);

    public List<string> Find(string suffix)
    {
        var result = new List<string>();
        foreach (var c in _children)
        {
            if (c is DirectoryNode d) result.AddRange(d.Find(suffix).Select(p => name + "/" + p));
            else if (c.Name.EndsWith(suffix)) result.Add(name + "/" + c.Name);
        }
        return result;
    }
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("Composite: treat one object and a group of objects the same way");
        var root = new DirectoryNode("root");
        var docs = new DirectoryNode("docs");
        docs.Add(new FileNode("a.txt", 100));
        docs.Add(new FileNode("b.pdf", 900));
        var src = new DirectoryNode("src");
        src.Add(new FileNode("Main.cs", 400));
        docs.Add(src);
        root.Add(docs);
        root.Add(new FileNode("readme.txt", 50));

        IFileSystemNode asNode = root;       // the caller does not care if it's a file or a folder
        Check.That(asNode.Size == 1450, "folder size = sum of everything inside, at any depth");
        Check.That(new FileNode("x", 7).Size == 7, "a single file answers the same question");
        Check.That(root.Find(".txt").SequenceEqual(new[] { "root/docs/a.txt", "root/readme.txt" }), "find walks the whole tree");
        Check.Throws<ArgumentException>(() => src.Add(root), "adding an ancestor would create a cycle: rejected");
    }
}
