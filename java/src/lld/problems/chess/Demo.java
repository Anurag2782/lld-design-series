package lld.problems.chess;

import lld.common.Check;

enum Color { WHITE, BLACK; Color other() { return this == WHITE ? BLACK : WHITE; } }

/** Each piece knows ITS OWN movement rule. Board is row 0 = rank 8 (black side), row 7 = rank 1 (white side). */
abstract class Piece {
    final Color color;
    Piece(Color color) { this.color = color; }
    abstract boolean canMove(Board b, int fr, int fc, int tr, int tc);
    abstract char symbol();
}

class King extends Piece {
    King(Color c) { super(c); }
    boolean canMove(Board b, int fr, int fc, int tr, int tc) { return Math.abs(tr - fr) <= 1 && Math.abs(tc - fc) <= 1; }
    char symbol() { return 'K'; }
}
class Knight extends Piece {
    Knight(Color c) { super(c); }
    boolean canMove(Board b, int fr, int fc, int tr, int tc) {
        int dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
    char symbol() { return 'N'; }
}
class Rook extends Piece {
    Rook(Color c) { super(c); }
    boolean canMove(Board b, int fr, int fc, int tr, int tc) { return (fr == tr || fc == tc) && b.pathClear(fr, fc, tr, tc); }
    char symbol() { return 'R'; }
}
class Bishop extends Piece {
    Bishop(Color c) { super(c); }
    boolean canMove(Board b, int fr, int fc, int tr, int tc) { return Math.abs(tr - fr) == Math.abs(tc - fc) && b.pathClear(fr, fc, tr, tc); }
    char symbol() { return 'B'; }
}
class Queen extends Piece {
    Queen(Color c) { super(c); }
    boolean canMove(Board b, int fr, int fc, int tr, int tc) {
        boolean line = fr == tr || fc == tc || Math.abs(tr - fr) == Math.abs(tc - fc);
        return line && b.pathClear(fr, fc, tr, tc);
    }
    char symbol() { return 'Q'; }
}
class Pawn extends Piece {
    Pawn(Color c) { super(c); }
    boolean canMove(Board b, int fr, int fc, int tr, int tc) {
        int dir = color == Color.WHITE ? -1 : 1, start = color == Color.WHITE ? 6 : 1;
        if (fc == tc && b.at(tr, tc) == null) {
            if (tr - fr == dir) return true;
            return fr == start && tr - fr == 2 * dir && b.at(fr + dir, fc) == null;
        }
        return Math.abs(tc - fc) == 1 && tr - fr == dir && b.at(tr, tc) != null && b.at(tr, tc).color != color;   // diagonal capture
    }
    char symbol() { return 'P'; }
}

class Board {
    private final Piece[][] grid = new Piece[8][8];
    Piece at(int r, int c) { return grid[r][c]; }
    void set(int r, int c, Piece p) { grid[r][c] = p; }

    boolean pathClear(int fr, int fc, int tr, int tc) {
        int dr = Integer.signum(tr - fr), dc = Integer.signum(tc - fc);
        for (int r = fr + dr, c = fc + dc; r != tr || c != tc; r += dr, c += dc) if (grid[r][c] != null) return false;
        return true;
    }

    static Board standard() {
        Board b = new Board();
        for (int c = 0; c < 8; c++) { b.set(1, c, new Pawn(Color.BLACK)); b.set(6, c, new Pawn(Color.WHITE)); }
        for (Color col : Color.values()) {
            int r = col == Color.WHITE ? 7 : 0;
            Piece[] back = { new Rook(col), new Knight(col), new Bishop(col), new Queen(col), new King(col), new Bishop(col), new Knight(col), new Rook(col) };
            for (int c = 0; c < 8; c++) b.set(r, c, back[c]);
        }
        return b;
    }
}

enum Status { ONGOING, CHECK, CHECKMATE, STALEMATE }

class Game {
    final Board board = Board.standard();
    Color turn = Color.WHITE;

    boolean isInCheck(Color color) {
        int kr = -1, kc = -1;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (board.at(r, c) instanceof King k && k.color == color) { kr = r; kc = c; }
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = board.at(r, c);
            if (p != null && p.color != color && p.canMove(board, r, c, kr, kc)) return true;
        }
        return false;
    }

    /** Pseudo-legal rules + "your own king must not be left in check" (tested by trying the move). */
    private boolean isLegal(Color color, int fr, int fc, int tr, int tc) {
        if (tr < 0 || tr > 7 || tc < 0 || tc > 7 || (fr == tr && fc == tc)) return false;
        Piece p = board.at(fr, fc), target = board.at(tr, tc);
        if (p == null || p.color != color || (target != null && target.color == color)) return false;
        if (!p.canMove(board, fr, fc, tr, tc)) return false;
        board.set(tr, tc, p); board.set(fr, fc, null);                  // try it
        boolean safe = !isInCheck(color);
        board.set(fr, fc, p); board.set(tr, tc, target);                // undo
        return safe;
    }

    boolean move(int fr, int fc, int tr, int tc) {
        if (!isLegal(turn, fr, fc, tr, tc)) return false;
        board.set(tr, tc, board.at(fr, fc)); board.set(fr, fc, null);
        turn = turn.other();
        return true;
    }

    private boolean hasLegalMove(Color color) {
        for (int fr = 0; fr < 8; fr++) for (int fc = 0; fc < 8; fc++)
            for (int tr = 0; tr < 8; tr++) for (int tc = 0; tc < 8; tc++)
                if (isLegal(color, fr, fc, tr, tc)) return true;
        return false;
    }

    Status status() {
        boolean check = isInCheck(turn), canMove = hasLegalMove(turn);
        if (check) return canMove ? Status.CHECK : Status.CHECKMATE;
        return canMove ? Status.ONGOING : Status.STALEMATE;
    }
}

public class Demo {
    /** Squares like "e2" -> {row, col}. */
    static int[] sq(String s) { return new int[] { 8 - (s.charAt(1) - '0'), s.charAt(0) - 'a' }; }
    static boolean play(Game g, String from, String to) { int[] f = sq(from), t = sq(to); return g.move(f[0], f[1], t[0], t[1]); }

    public static void main(String[] args) {
        System.out.println("Chess: each piece owns its rule, the game validates and detects check");
        Game g = new Game();
        Check.that(!play(g, "e2", "e5"), "a pawn cannot jump three squares");
        Check.that(!play(g, "a1", "a3"), "a rook cannot jump over its own pawn");
        Check.that(!play(g, "e7", "e5"), "it is White's turn, Black cannot move yet");
        Check.that(play(g, "g1", "f3"), "knight g1-f3 is legal");
        Check.that(play(g, "e7", "e5"), "Black replies e7-e5");
        Check.that(!play(g, "f3", "f3"), "a piece must actually move");

        Game mate = new Game();        // Fool's mate: 1. f3 e5 2. g4 Qh4#
        play(mate, "f2", "f3"); play(mate, "e7", "e5"); play(mate, "g2", "g4");
        Check.that(mate.status() == Status.ONGOING, "before the queen move the game is ongoing");
        Check.that(play(mate, "d8", "h4"), "the queen slides d8-h4 along a clear diagonal");
        Check.that(mate.isInCheck(Color.WHITE), "White's king is in check");
        Check.that(mate.status() == Status.CHECKMATE, "no legal reply exists: checkmate (Fool's mate)");
        Check.that(!play(mate, "e2", "e3"), "White has no move left: even a harmless pawn move is rejected");
    }
}
