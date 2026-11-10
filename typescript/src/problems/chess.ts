import { check } from "../common.ts";

type Color = "WHITE" | "BLACK";
const other = (c: Color): Color => (c === "WHITE" ? "BLACK" : "WHITE");

/** Each piece knows ITS OWN movement rule. Board is row 0 = rank 8 (black side), row 7 = rank 1 (white side). */
abstract class Piece {
  readonly color: Color;
  constructor(color: Color) { this.color = color; }
  abstract canMove(b: Board, fr: number, fc: number, tr: number, tc: number): boolean;
}

class King extends Piece {
  canMove(_b: Board, fr: number, fc: number, tr: number, tc: number) { return Math.abs(tr - fr) <= 1 && Math.abs(tc - fc) <= 1; }
}
class Knight extends Piece {
  canMove(_b: Board, fr: number, fc: number, tr: number, tc: number) {
    const dr = Math.abs(tr - fr), dc = Math.abs(tc - fc);
    return (dr === 2 && dc === 1) || (dr === 1 && dc === 2);
  }
}
class Rook extends Piece {
  canMove(b: Board, fr: number, fc: number, tr: number, tc: number) { return (fr === tr || fc === tc) && b.pathClear(fr, fc, tr, tc); }
}
class Bishop extends Piece {
  canMove(b: Board, fr: number, fc: number, tr: number, tc: number) { return Math.abs(tr - fr) === Math.abs(tc - fc) && b.pathClear(fr, fc, tr, tc); }
}
class Queen extends Piece {
  canMove(b: Board, fr: number, fc: number, tr: number, tc: number) {
    return (fr === tr || fc === tc || Math.abs(tr - fr) === Math.abs(tc - fc)) && b.pathClear(fr, fc, tr, tc);
  }
}
class Pawn extends Piece {
  canMove(b: Board, fr: number, fc: number, tr: number, tc: number) {
    const dir = this.color === "WHITE" ? -1 : 1, start = this.color === "WHITE" ? 6 : 1;
    if (fc === tc && b.at(tr, tc) === null) {
      if (tr - fr === dir) return true;
      return fr === start && tr - fr === 2 * dir && b.at(fr + dir, fc) === null;
    }
    const target = b.at(tr, tc);
    return Math.abs(tc - fc) === 1 && tr - fr === dir && target !== null && target.color !== this.color;   // diagonal capture
  }
}

class Board {
  private grid: (Piece | null)[][] = Array.from({ length: 8 }, () => Array<Piece | null>(8).fill(null));
  at(r: number, c: number): Piece | null { return this.grid[r]![c]!; }
  set(r: number, c: number, p: Piece | null): void { this.grid[r]![c] = p; }

  pathClear(fr: number, fc: number, tr: number, tc: number): boolean {
    const dr = Math.sign(tr - fr), dc = Math.sign(tc - fc);
    for (let r = fr + dr, c = fc + dc; r !== tr || c !== tc; r += dr, c += dc) if (this.grid[r]![c] !== null) return false;
    return true;
  }

  static standard(): Board {
    const b = new Board();
    for (let c = 0; c < 8; c++) { b.set(1, c, new Pawn("BLACK")); b.set(6, c, new Pawn("WHITE")); }
    for (const [col, r] of [["WHITE", 7], ["BLACK", 0]] as const) {
      const back = [Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook];
      back.forEach((Kind, c) => b.set(r, c, new Kind(col)));
    }
    return b;
  }
}

type Status = "ONGOING" | "CHECK" | "CHECKMATE" | "STALEMATE";

class Game {
  board = Board.standard();
  turn: Color = "WHITE";

  isInCheck(color: Color): boolean {
    let kr = -1, kc = -1;
    for (let r = 0; r < 8; r++) for (let c = 0; c < 8; c++) { const p = this.board.at(r, c); if (p instanceof King && p.color === color) { kr = r; kc = c; } }
    for (let r = 0; r < 8; r++) for (let c = 0; c < 8; c++) {
      const p = this.board.at(r, c);
      if (p && p.color !== color && p.canMove(this.board, r, c, kr, kc)) return true;
    }
    return false;
  }

  /** Piece rules + "your own king must not be left in check" (tested by trying the move). */
  private isLegal(color: Color, fr: number, fc: number, tr: number, tc: number): boolean {
    if (tr < 0 || tr > 7 || tc < 0 || tc > 7 || (fr === tr && fc === tc)) return false;
    const p = this.board.at(fr, fc), target = this.board.at(tr, tc);
    if (!p || p.color !== color || (target && target.color === color)) return false;
    if (!p.canMove(this.board, fr, fc, tr, tc)) return false;
    this.board.set(tr, tc, p); this.board.set(fr, fc, null);          // try it
    const safe = !this.isInCheck(color);
    this.board.set(fr, fc, p); this.board.set(tr, tc, target);        // undo
    return safe;
  }

  move(fr: number, fc: number, tr: number, tc: number): boolean {
    if (!this.isLegal(this.turn, fr, fc, tr, tc)) return false;
    this.board.set(tr, tc, this.board.at(fr, fc)); this.board.set(fr, fc, null);
    this.turn = other(this.turn);
    return true;
  }

  private hasLegalMove(color: Color): boolean {
    for (let fr = 0; fr < 8; fr++) for (let fc = 0; fc < 8; fc++)
      for (let tr = 0; tr < 8; tr++) for (let tc = 0; tc < 8; tc++)
        if (this.isLegal(color, fr, fc, tr, tc)) return true;
    return false;
  }

  status(): Status {
    const inCheck = this.isInCheck(this.turn), canMove = this.hasLegalMove(this.turn);
    if (inCheck) return canMove ? "CHECK" : "CHECKMATE";
    return canMove ? "ONGOING" : "STALEMATE";
  }
}

/** Squares like "e2" -> [row, col]. */
const sq = (s: string): [number, number] => [8 - Number(s[1]), s.charCodeAt(0) - 97];
const play = (g: Game, from: string, to: string) => { const [fr, fc] = sq(from), [tr, tc] = sq(to); return g.move(fr, fc, tr, tc); };

export function demo(): void {
  console.log("Chess: each piece owns its rule, the game validates and detects check");
  const g = new Game();
  check(!play(g, "e2", "e5"), "a pawn cannot jump three squares");
  check(!play(g, "a1", "a3"), "a rook cannot jump over its own pawn");
  check(!play(g, "e7", "e5"), "it is White's turn, Black cannot move yet");
  check(play(g, "g1", "f3"), "knight g1-f3 is legal");
  check(play(g, "e7", "e5"), "Black replies e7-e5");
  check(!play(g, "f3", "f3"), "a piece must actually move");

  const mate = new Game();        // Fool's mate: 1. f3 e5 2. g4 Qh4#
  play(mate, "f2", "f3"); play(mate, "e7", "e5"); play(mate, "g2", "g4");
  check(mate.status() === "ONGOING", "before the queen move the game is ongoing");
  check(play(mate, "d8", "h4"), "the queen slides d8-h4 along a clear diagonal");
  check(mate.isInCheck("WHITE"), "White's king is in check");
  check(mate.status() === "CHECKMATE", "no legal reply exists: checkmate (Fool's mate)");
  check(!play(mate, "e2", "e3"), "White has no move left: even a harmless pawn move is rejected");
}
