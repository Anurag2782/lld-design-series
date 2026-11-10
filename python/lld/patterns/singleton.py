"""Singleton: one shared instance, thread-safe."""
import threading
from concurrent.futures import ThreadPoolExecutor

from lld.common import check


class Config:
    """Double-checked locking. (A module-level object is the most Pythonic singleton.)"""
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance


class _Registry:
    def __init__(self): self._data = {}
    def put(self, k, v): self._data[k] = v
    def get(self, k): return self._data.get(k)


REGISTRY = _Registry()   # module-level singleton: imported once, shared everywhere


def demo() -> None:
    print("Singleton: one shared instance, thread-safe")
    with ThreadPoolExecutor(max_workers=16) as pool:
        instances = list(pool.map(lambda _: Config(), range(50)))
    check(all(i is instances[0] for i in instances), "50 concurrent callers all receive the same Config instance")
    REGISTRY.put("env", "prod")
    check(REGISTRY.get("env") == "prod", "module-level singleton holds shared state")
    check(Config() is Config(), "always the same Config")


if __name__ == "__main__":
    demo()
