# My tests

I kept each JSON Lines request in `cases.jsonl`. The same line in `expected.jsonl` is the answer I reasoned out, and the same line in `actual.jsonl` is the program output I recorded. I use semantic comparison because an optimal forest, SCC label set, topological order or shortest-path parent witness need not match one exact serialization.

From the repository root, I run:

```sh
python tools/run.py --input tests/cases.jsonl
python tests/verify_cases.py
javac --release 17 -encoding UTF-8 -cp .build/Java -d .build/Java tests/RouteChecks.java tests/LimitChecks.java tests/Benchmark.java
java -Xmx384m -cp .build/Java RouteChecks
java -Xmx384m -cp .build/Java LimitChecks
java -Xmx384m -cp .build/Java Benchmark
```

`verify_cases.py` checks all 13 recorded outputs using the template's semantic validator. It also accepts alternative valid witnesses from my expected file and rejects a forest containing a self-loop and a DAG parent edge that points to the wrong vertex. `RouteChecks.java` calls the route helper for a reachable target, the source, an unreachable target and tied parallel edges. `LimitChecks.java` generates boundary inputs with 100000 vertices and 250000 edges or operations and checks compact actual results. `Benchmark.java` generates three fixed-seed graphs and prints five timings per method per size. The derivation of the expected values and the recorded benchmark output are in `ANALYSIS.md`.
