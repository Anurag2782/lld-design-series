using Lld.Common;

namespace Lld.Problems.Chess;

enum Color { White, Black }

/// <summary>Each piece knows ITS OWN movement rule. Board is row 0 = rank 8 (black side), row 7 = rank 1 (white side).</summary>
abstract class Piece(Color color)
{
    public Color Color => color;
    public abstract bool CanMove(Board b, int fr, int fc, int tr, int tc);
}

class King(Color c) : Piece(c)
{
    public override bool CanMove(Board b, int fr, int fc, int tr, int tc) => Math.Abs(tr - fr) <= 1 && Math.Abs(tc - fc) <= 1;
}
class Knight(Color c) : Piece(c)
{
    public override bool CanMove(Board b, int fr, int fc, int tr, int tc)
    {
        int dr = Math.Abs(tr - fr), dc = Math.Abs(tc - fc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
}
class Rook(Color c) : Piece(c)
{
    public override bool CanMove(Board b, int fr, int fc, int tr, int tc) => (fr == tr || fc == tc) && b.PathClear(fr, fc, tr, tc);
}
class Bishop(Color c) : Piece(c)
{
    public override bool CanMove(Board b, int fr, int fc, int tr, int tc) => Math.Abs(tr - fr) == Math.Abs(tc - fc) && b.PathClear(fr, fc, tr, tc);
}
class Queen(Color c) : Piece(c)
{
    public override bool CanMove(Board b, int fr, int fc, int tr, int tc) =>
        (fr == tr || fc == tc || Math.Abs(tr - fr) == Math.Abs(tc - fc)) && b.PathClear(fr, fc, tr, tc);
}
class Pawn(Color c) : Piece(c)
{
    public override bool CanMove(Board b, int fr, int fc, int tr, int tc)
    {
        int dir = Color == Color.White ? -1 : 1, start = Color == Color.White ? 6 : 1;
        if (fc == tc && b.At(tr, tc) is null)
        {
            if (tr - fr == dir) return true;
            return fr == start && tr - fr == 2 * dir && b.At(fr + dir, fc) is null;
        }
        return Math.Abs(tc - fc) == 1 && tr - fr == dir && b.At(tr, tc) is { } t && t.Color != Color;   // diagonal capture
    }
}

class Board
{
    private readonly Piece?[,] _grid = new Piece?[8, 8];
    public Piece? At(int r, int c) => _grid[r, c];
    public void Set(int r, int c, Piece? p) => _grid[r, c] = p;

    public bool PathClear(int fr, int fc, int tr, int tc)
    {
        int dr = Math.Sign(tr - fr), dc = Math.Sign(tc - fc);
        for (int r = fr + dr, c = fc + dc; r != tr || c != tc; r += dr, c += dc)
            if (_grid[r, c] is not null) return false;
        return true;
    }

    public static Board Standard()
    {
        var b = new Board();
        for (var c = 0; c < 8; c++) { b.Set(1, c, new Pawn(Color.Black)); b.Set(6, c, new Pawn(Color.White)); }
        foreach (var col in new[] { Color.White, Color.Black })
        {
            var r = col == Color.White ? 7 : 0;
            Piece[] back = { new Rook(col), new Knight(col), new Bishop(col), new Queen(col), new King(col), new Bishop(col), new Knight(col), new Rook(col) };
            for (var c = 0; c < 8; c++) b.Set(r, c, back[c]);
        }
        return b;
    }
}

enum Status { Ongoing, Check, Checkmate, Stalemate }

class Game
{
    public Board Board { get; } = Board.Standard();
    public Color Turn { get; private set; } = Color.White;
    private static Color Other(Color c) => c == Color.White ? Color.Black : Color.White;

    public bool IsInCheck(Color color)
    {
        int kr = -1, kc = -1;
        for (var r = 0; r < 8; r++) for (var c = 0; c < 8; c++) if (Board.At(r, c) is King k && k.Color == color) { kr = r; kc = c; }
        for (var r = 0; r < 8; r++) for (var c = 0; c < 8; c++)
            if (Board.At(r, c) is { } p && p.Color != color && p.CanMove(Board, r, c, kr, kc)) return true;
        return false;
    }

    /// <summary>Piece rules + "your own king must not be left in check" (tested by trying the move).</summary>
    private bool IsLegal(Color color, int fr, int fc, int tr, int tc)
    {
        if (tr < 0 || tr > 7 || tc < 0 || tc > 7 || (fr == tr && fc == tc)) return false;
        var p = Board.At(fr, fc); var target = Board.At(tr, tc);
        if (p is null || p.Color != color || (target is not null && target.Color == color)) return false;
        if (!p.CanMove(Board, fr, fc, tr, tc)) return false;
        Board.Set(tr, tc, p); Board.Set(fr, fc, null);                  // try it
        var safe = !IsInCheck(color);
        Board.Set(fr, fc, p); Board.Set(tr, tc, target);                // undo
        return safe;
    }

    public bool Move(int fr, int fc, int tr, int tc)
    {
        if (!IsLegal(Turn, fr, fc, tr, tc)) return false;
        Board.Set(tr, tc, Board.At(fr, fc)); Board.Set(fr, fc, null);
        Turn = Other(Turn);
        return true;
    }

    private bool HasLegalMove(Color color)
    {
        for (var fr = 0; fr < 8; fr++) for (var fc = 0; fc < 8; fc++)
            for (var tr = 0; tr < 8; tr++) for (var tc = 0; tc < 8; tc++)
                if (IsLegal(color, fr, fc, tr, tc)) return true;
        return false;
    }

    public Status Status()
    {
        bool check = IsInCheck(Turn), canMove = HasLegalMove(Turn);
        if (check) return canMove ? Chess.Status.Check : Chess.Status.Checkmate;
        return canMove ? Chess.Status.Ongoing : Chess.Status.Stalemate;
    }
}

public static class Demo
{
    /// <summary>Squares like "e2" -> (row, col).</summary>
    static (int, int) Sq(string s) => (8 - (s[1] - '0'), s[0] - 'a');
    static bool Play(Game g, string from, string to) { var (fr, fc) = Sq(from); var (tr, tc) = Sq(to); return g.Move(fr, fc, tr, tc); }

    public static void Run()
    {
        Console.WriteLine("Chess: each piece owns its rule, the game validates and detects check");
        var g = new Game();
        Check.That(!Play(g, "e2", "e5"), "a pawn cannot jump three squares");
        Check.That(!Play(g, "a1", "a3"), "a rook cannot jump over its own pawn");
        Check.That(!Play(g, "e7", "e5"), "it is White's turn, Black cannot move yet");
        Check.That(Play(g, "g1", "f3"), "knight g1-f3 is legal");
        Check.That(Play(g, "e7", "e5"), "Black replies e7-e5");
        Check.That(!Play(g, "f3", "f3"), "a piece must actually move");

        var mate = new Game();        // Fool's mate: 1. f3 e5 2. g4 Qh4#
        Play(mate, "f2", "f3"); Play(mate, "e7", "e5"); Play(mate, "g2", "g4");
        Check.That(mate.Status() == Status.Ongoing, "before the queen move the game is ongoing");
        Check.That(Play(mate, "d8", "h4"), "the queen slides d8-h4 along a clear diagonal");
        Check.That(mate.IsInCheck(Color.White), "White's king is in check");
        Check.That(mate.Status() == Status.Checkmate, "no legal reply exists: checkmate (Fool's mate)");
        Check.That(!Play(mate, "e2", "e3"), "White has no move left: even a harmless pawn move is rejected");
    }
}
