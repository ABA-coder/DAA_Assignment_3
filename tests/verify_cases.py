import json
import pathlib
import sys

root = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / "tools"))
from public_validate import check


def read_lines(path):
    return [json.loads(line) for line in path.read_text(encoding="utf-8-sig").splitlines()]


inputs = read_lines(root / "tests" / "cases.jsonl")
expected = read_lines(root / "tests" / "expected.jsonl")
actual = read_lines(root / "tests" / "actual.jsonl")
assert len(inputs) == len(expected) == len(actual) == 13

for index, (request, answer, oracle) in enumerate(zip(inputs, actual, expected), 1):
    assert check(request, answer, oracle), f"Case {index} failed"

for index in (3, 4, 7, 8, 10):
    assert check(inputs[index], expected[index], expected[index])

invalid_forest = dict(actual[3], edge_ids=[0, 3, 4])
invalid_route = dict(actual[10], parent_edge=[-1, 0, 2, 6, -1, -1])
assert not check(inputs[3], invalid_forest, expected[3])
assert not check(inputs[10], invalid_route, expected[10])

print("PASS 13 custom cases, alternative valid answers, invalid forest and route")
