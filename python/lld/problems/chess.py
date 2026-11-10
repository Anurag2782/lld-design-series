"""Chess: each piece owns its rule, the game validates and detects check."""
from abc import ABC, abstractmethod
from enum import Enum
from typing import List, Optional, Tuple

from lld.common import check


class Color(Enum):
    WHITE = "WHITE"
    BLACK = "BLACK"

    def other(self) -> "Color":
        return Color.BLACK if self is Color.WHITE else Color.WHITE


class Piece(ABC):
    """Each piece knows ITS OWN movement rule. Board is row 0 = rank 8 (black side), row 7 = rank 1 (white side)."""
    def __init__(self, color: Color): self.color = color

    @abstractmethod
    def can_move(self, b: "Board", fr: int, fc: int, tr: int, tc: int) -> bool: ...


class King(Piece):
    def can_move(self, b, fr, fc, tr, tc): return abs(tr - fr) <= 1 and abs(tc - fc) <= 1


class Knight(Piece):
    def can_move(self, b, fr, fc, tr, tc):
        dr, dc = abs(tr - fr), abs(tc - fc)
        return (dr, dc) in ((2, 1), (1, 2))


class Rook(Piece):
    def can_move(self, b, fr, fc, tr, tc): return (fr == tr or fc == tc) and b.path_clear(fr, fc, tr, tc)


class Bishop(Piece):
    def can_move(self, b, fr, fc, tr, tc): return abs(tr - fr) == abs(tc - fc) and b.path_clear(fr, fc, tr, tc)


class Queen(Piece):
    def can_move(self, b, fr, fc, tr, tc):
        return (fr == tr or fc == tc or abs(tr - fr) == abs(tc - fc)) and b.path_clear(fr, fc, tr, tc)


class Pawn(Piece):
    def can_move(self, b, fr, fc, tr, tc):
        direction = -1 if self.color is Color.WHITE else 1
        start = 6 if self.color is Color.WHITE else 1
        if fc == tc and b.at(tr, tc) is None:
            if tr - fr == direction:
                return True
            return fr == start and tr - fr == 2 * direction and b.at(fr + direction, fc) is None
        target = b.at(tr, tc)
        return abs(tc - fc) == 1 and tr - fr == direction and target is not None and target.color is not self.color   # capture


def _sign(x: int) -> int:
    return (x > 0) - (x < 0)


class Board:
    def __init__(self): self._grid: List[List[Optional[Piece]]] = [[None] * 8 for _ in range(8)]
    def at(self, r, c): return self._grid[r][c]
    def set(self, r, c, p): self._grid[r][c] = p

    def path_clear(self, fr, fc, tr, tc) -> bool:
        dr, dc = _sign(tr - fr), _sign(tc - fc)
        r, c = fr + dr, fc + dc
        while (r, c) != (tr, tc):
            if self._grid[r][c] is not None:
                return False
            r, c = r + dr, c + dc
        return True

    @staticmethod
    def standard() -> "Board":
        b = Board()
        for c in range(8):
            b.set(1, c, Pawn(Color.BLACK))
            b.set(6, c, Pawn(Color.WHITE))
        for col, r in ((Color.WHITE, 7), (Color.BLACK, 0)):
            back = [Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook]
            for c, kind in enumerate(back):
                b.set(r, c, kind(col))
        return b


class Status(Enum):
    ONGOING = "ONGOING"
    CHECK = "CHECK"
    CHECKMATE = "CHECKMATE"
    STALEMATE = "STALEMATE"


class Game:
    def __init__(self):
        self.board = Board.standard()
        self.turn = Color.WHITE

    def is_in_check(self, color: Color) -> bool:
        king: Optional[Tuple[int, int]] = None
        for r in range(8):
            for c in range(8):
                p = self.board.at(r, c)
                if isinstance(p, King) and p.color is color:
                    king = (r, c)
        for r in range(8):
            for c in range(8):
                p = self.board.at(r, c)
                if p is not None and p.color is not color and p.can_move(self.board, r, c, king[0], king[1]):
                    return True
        return False

    def _is_legal(self, color: Color, fr, fc, tr, tc) -> bool:
        """Piece rules + "your own king must not be left in check" (tested by trying the move)."""
        if not (0 <= tr < 8 and 0 <= tc < 8) or (fr, fc) == (tr, tc):
            return False
        p, target = self.board.at(fr, fc), self.board.at(tr, tc)
        if p is None or p.color is not color or (target is not None and target.color is color):
            return False
        if not p.can_move(self.board, fr, fc, tr, tc):
            return False
        self.board.set(tr, tc, p); self.board.set(fr, fc, None)     # try it
        safe = not self.is_in_check(color)
        self.board.set(fr, fc, p); self.board.set(tr, tc, target)   # undo
        return safe

    def move(self, fr, fc, tr, tc) -> bool:
        if not self._is_legal(self.turn, fr, fc, tr, tc):
            return False
        self.board.set(tr, tc, self.board.at(fr, fc)); self.board.set(fr, fc, None)
        self.turn = self.turn.other()
        return True

    def _has_legal_move(self, color: Color) -> bool:
        return any(self._is_legal(color, fr, fc, tr, tc)
                   for fr in range(8) for fc in range(8) for tr in range(8) for tc in range(8))

    def status(self) -> Status:
        in_check, can_move = self.is_in_check(self.turn), self._has_legal_move(self.turn)
        if in_check:
            return Status.CHECK if can_move else Status.CHECKMATE
        return Status.ONGOING if can_move else Status.STALEMATE


def _sq(s: str) -> Tuple[int, int]:
    """Squares like "e2" -> (row, col)."""
    return 8 - int(s[1]), ord(s[0]) - ord("a")


def _play(g: Game, frm: str, to: str) -> bool:
    (fr, fc), (tr, tc) = _sq(frm), _sq(to)
    return g.move(fr, fc, tr, tc)


def demo() -> None:
    print("Chess: each piece owns its rule, the game validates and detects check")
    g = Game()
    check(not _play(g, "e2", "e5"), "a pawn cannot jump three squares")
    check(not _play(g, "a1", "a3"), "a rook cannot jump over its own pawn")
    check(not _play(g, "e7", "e5"), "it is White's turn, Black cannot move yet")
    check(_play(g, "g1", "f3"), "knight g1-f3 is legal")
    check(_play(g, "e7", "e5"), "Black replies e7-e5")
    check(not _play(g, "f3", "f3"), "a piece must actually move")

    mate = Game()        # Fool's mate: 1. f3 e5 2. g4 Qh4#
    _play(mate, "f2", "f3"); _play(mate, "e7", "e5"); _play(mate, "g2", "g4")
    check(mate.status() is Status.ONGOING, "before the queen move the game is ongoing")
    check(_play(mate, "d8", "h4"), "the queen slides d8-h4 along a clear diagonal")
    check(mate.is_in_check(Color.WHITE), "White's king is in check")
    check(mate.status() is Status.CHECKMATE, "no legal reply exists: checkmate (Fool's mate)")
    check(not _play(mate, "e2", "e3"), "White has no move left: even a harmless pawn move is rejected")


if __name__ == "__main__":
    demo()
