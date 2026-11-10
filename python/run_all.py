#!/usr/bin/env python3
"""Runs every example. Usage:  python run_all.py   (all)   |   python run_all.py strategy   (one)"""
import importlib
import sys

TOPICS = [
    "solid.srp", "solid.ocp", "solid.lsp", "solid.isp", "solid.dip",
    "patterns.strategy", "patterns.factory", "patterns.observer", "patterns.builder", "patterns.singleton",
    "patterns.decorator", "patterns.state", "patterns.command", "patterns.chain", "patterns.proxy", "patterns.adapter",
    "patterns.templatemethod", "patterns.facade", "patterns.composite",
    "problems.parkinglot", "problems.splitwise", "problems.bookmyshow", "problems.elevator", "problems.ratelimiter",
    "problems.atm", "problems.library", "problems.chess",
]


def main() -> int:
    wanted = sys.argv[1] if len(sys.argv) > 1 else None
    selected = [t for t in TOPICS if wanted is None or t.split(".")[-1] == wanted]
    if not selected:
        print("No such example:", wanted, file=sys.stderr)
        return 1
    for topic in selected:
        print(f"=== {topic}")
        importlib.import_module(f"lld.{topic}").demo()
    print(f"\nALL {len(selected)} PYTHON EXAMPLES PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
