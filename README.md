# Assignment 3: Graph Algorithms

I implemented the six required graph tasks in Java 17. The program reads one JSON request per line and writes one JSON result per line. It supports multiple requests in the same process and keeps the supplied `ping` response.

## What I implemented

- `dsu`: path compression and union by size, with component count and largest component after each union.
- `kruskal`: a minimum spanning forest built from sorted original edge IDs.
- `prim`: a separate minimum spanning forest algorithm using adjacency lists and a priority queue, restarted for every component.
- `scc`: iterative Kosaraju DFS, transpose, component labels and deduplicated condensation edges.
- `topo`: iterative three-state DFS with cycle detection.
- `dag`: whole-graph cycle detection, shortest paths in topological order and original parent edge IDs.

I added `Algorithms.reconstructRoute` for vertex routes. It returns an empty list for an unreachable target. My tests call it directly.

## Build and run

From the repository root, I use JDK 17 or newer and Python 3.12 or newer for the provided launcher:

```sh
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/cases.jsonl
```

To compile and run Java without the launcher:

```powershell
New-Item -ItemType Directory -Force .build/Java
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/Algorithms.java src/Java/Input.java src/Java/Json.java src/Java/Main.java
java -Xmx384m -cp .build/Java Main
```

I keep my own JSON Lines inputs, independently reasoned expected answers and actual outputs in `tests/`. I verify them with:

```sh
python tests/verify_cases.py
javac --release 17 -encoding UTF-8 -cp .build/Java -d .build/Java tests/RouteChecks.java tests/LimitChecks.java tests/Benchmark.java
java -Xmx384m -cp .build/Java RouteChecks
java -Xmx384m -cp .build/Java LimitChecks
java -Xmx384m -cp .build/Java Benchmark
```

The benchmark uses fixed seeds and prints five timings per algorithm at each of three graph sizes. My method explanations, test reasoning, commands, measured medians and disclosure are in [ANALYSIS.md](ANALYSIS.md).
