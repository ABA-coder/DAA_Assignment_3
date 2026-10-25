# Analysis

## Build, run and test commands

I used these commands from the repository root. The Java code compiles with the Java 17 language level:

```powershell
New-Item -ItemType Directory -Force '.build\Java' | Out-Null
javac --release 17 -encoding UTF-8 -d '.build\Java' 'src\Java\Algorithms.java' 'src\Java\Input.java' 'src\Java\Json.java' 'src\Java\Main.java'
java -Xmx384m -cp '.build\Java' Main
```

The program then reads one JSON request from standard input per line. I used the supplied Python launcher to test the JSON Lines interface without PowerShell's native-pipeline quoting behavior:

```powershell
python tools\run.py --setup
python tools\run.py --check
python tools\run.py --input tests\cases.jsonl
python tests\verify_cases.py
javac --release 17 -encoding UTF-8 -cp '.build\Java' -d '.build\Java' 'tests\RouteChecks.java' 'tests\LimitChecks.java' 'tests\Benchmark.java'
java -Xmx384m -cp '.build\Java' RouteChecks
java -Xmx384m -cp '.build\Java' LimitChecks
java -Xmx384m -cp '.build\Java' Benchmark
```

On a Windows installation where `python` is not on `PATH`, I used the available Python 3.12 executable by its full path for the same launcher commands.

## Interface and shared limits

The supplied `Main`, `Json` and `Input` classes handle UTF-8 JSON Lines and map every original edge position to its ID. Each algorithm method creates its own arrays and collections, so a later request cannot see a previous graph. I used `long` for forest costs and finite DAG distances. The maximum possible magnitude from 250000 edges of weight 1000000000 is 250000000000000, which fits a signed 64-bit integer. DFS traversals use explicit stacks; a chain of 100000 vertices does not depend on Java call-stack depth.

## DSU

I initialize every vertex as a root of size one. `find` first reaches the root and then compresses the complete search path. `union` attaches the smaller root to the larger root. The invariant is that roots represent exactly the connected components formed by all processed operations, and each root size is its component size. A successful union reduces the component count once and updates the largest size; a repeated or self union changes neither. Thus the state after each operation is correct. With no vertices, there are no operations and the state list is empty. Time is `O(n + q alpha(n))` amortized; memory is `O(n + q)` including output.

## Kruskal

I sort original IDs by weight, breaking ties by ID, and accept an edge only when its endpoints have different DSU roots. The selected edges stay acyclic. For each cut between current components, the first eligible edge has minimum weight among edges considered; the cut property makes it safe. Once all edges are processed, every connected component is spanned at minimum total cost. Self-loops are rejected by DSU, parallel edges remain distinguishable by ID, negative weights work with the same ordering, and isolated vertices remain components. Time is `O(n + m log(m+1))`; memory is `O(n+m)`.

## Prim

I build an undirected adjacency list of edge IDs. Starting at every unvisited vertex, I mark it visited and put incident candidates into a min heap. I repeatedly select the cheapest edge crossing from the visited part to an unvisited vertex and add that vertex's outgoing candidates. The invariant is that each selected edge is a minimum crossing edge for the current visited set, so the cut property preserves an optimal forest. When the heap empties, the current input component is complete; the outer loop starts another, including isolates. Loops and stale candidates are discarded, and equal-weight or parallel edges may give a different valid forest from Kruskal. Time is `O(n + m log(m+1))`; memory is `O(n+m)`.

## Strongly connected components

I run iterative DFS on the original directed graph and record vertices at finish time. I then process them in reverse finish order on the transposed graph, assigning one label to every reached vertex. The first pass establishes the ordering between SCCs; in that order, a second-pass search cannot absorb another SCC, while it reaches every vertex in its own SCC. I encode each cross-label pair in a `long` set before adding it to the condensation result. This removes duplicate arcs and excludes internal arcs. Empty graphs, isolates, parallel edges and loops need no special traversal rule. Expected time is `O(n+m)` with hash-based deduplication; memory is `O(n+m)`.

## Topological order

I use three states: unvisited, active and finished. Each stack frame stores its next adjacency arc, so a suspended DFS resumes without rescanning earlier neighbors. An edge to an active vertex closes a directed cycle; this includes self-loops and cycles in disconnected parts. Otherwise, every edge points from a vertex that finishes later to one that finishes earlier, so reversing finish order places its source first. I return an empty order on a cycle. Time and memory are `O(n+m)`.

## DAG shortest paths and routes

I apply the same whole-graph three-state DFS before relaxing any edge. If it detects a cycle anywhere, I return `acyclic=false` and two empty arrays, even when the cycle is unreachable from the source. Otherwise, I set only the source distance to zero. Processing vertices in topological order means every predecessor of a vertex has already had its shortest distance computed. Relaxing each outgoing edge therefore gives the shortest distance and a valid incoming original edge ID. I skip unreachable vertices; their distance remains `null` and parent ID remains `-1`. Negative weights are safe because there is no cycle. Time and memory are `O(n+m)`.

`reconstructRoute(source,target,edges,parentEdge)` follows incoming parent edge IDs backward and reverses the collected vertices. For a valid DAG result, its time and output space are `O(L)` for a route of `L` vertices. I chose `[]` for an unreachable target. My direct calls in `RouteChecks` used the graph in `tests/cases.jsonl`:

| Target | Selected parent IDs | Returned vertices | Weight |
|---|---|---|---:|
| 3, reachable | 1 then 5 | `[0,1,3]` | `3 + (-3) = 0` |
| 0, source | none | `[0]` | 0 |
| 5, unreachable | none | `[]` | no route |
| 1, tied parallel edges | 1 (edge 0 ties) | `[0,1]` | 3 |

The parent edges point forward along each returned route; following them backward reaches source 0.

## My tests

I ran `python tools/run.py --setup` and `python tools/run.py --check`. Ping, all 36 public cases and the repeated two-batch state-reset check passed. I then ran `python tools/run.py --input tests/cases.jsonl` and saved its 13 actual JSON results in `tests/actual.jsonl`. The input, expected and actual files have matching line numbers.

The custom DSU case has five vertices: joining 0-1 gives `[4,2]`; self and repeated unions keep the partition; joining 1-2 gives `[3,3]`; joining 3-4 ends at `[2,3]`. The forest case's two components use IDs 2, 3 and 4 for cost `-3 + 2 + 0 = -1`; loops have lower weights but cannot be in a forest. The three billion cost case checks `long`. The SCC case has groups `{0,1}`, `{2,3}`, `{4}` and two condensation arcs. The acyclic topo case permits several orders; another component contains a 2-3 cycle. The DAG case has distances `[0,3,-4,0,null,null]`, with two equal choices into vertex 1 and two equal shortest paths into vertex 3. Its second case has a cycle unreachable from source 0.

I used `python tests/verify_cases.py` to check the actual results against the independently reasoned expectations with the supplied semantic validator. The expected file deliberately uses alternative valid forest orders, SCC labels, topo order and DAG parents; all were accepted. An invalid forest containing loop ID 0 and an invalid DAG parent ID 6 for vertex 3 were rejected. The direct Java route test passed.

`LimitChecks` constructs 100000-vertex, 250000-row cases and passed DSU, Kruskal, Prim, SCC, topo and DAG under `-Xmx384m`. Chains exercise deep traversal. The weighted forest has 99999 chain edges of weight 1 and more expensive parallel edges, so its optimum is 99999; the DAG has chain edges of weight -1, so its final distance is -99999. The SCC and topo chain checks verify 100000 separate SCCs and the only chain order. These cases are generated by `tests/LimitChecks.java`, so the large JSON arrays are not stored as oversized text files.

## Kruskal and Prim benchmark

I used `tests/Benchmark.java` with Java `Random` seed `20261025 + n`. Each graph starts with a chain of weight -1000 and adds random edges with weights from 0 to 1000000, so the unique minimum cost is `-1000(n-1)`. Both methods process exactly the same graph at each size. I checked cost, component count, edge count, selected edge validity, total selected weight and connectivity after every call. The process performs one warm-up call per method and size, then five measured calls per method; method order alternates. `System.nanoTime` starts immediately before the algorithm method call and stops immediately after it. The boundary includes `Input.edges` conversion, algorithm data structures, sorting or heap work and answer creation. It excludes graph generation, JSON parsing, process startup, compilation, validation and printing for both methods.

Environment: Windows 11 Pro; Intel Core i5-12450HX, 12 logical processors, 15.7 GB RAM; Java 21.0.12.1 running Java 17-compatible bytecode, heap limit `-Xmx384m`. Commands from repository root:

```sh
javac --release 17 -encoding UTF-8 -cp .build/Java -d .build/Java tests/Benchmark.java
java -Xmx384m -cp .build/Java Benchmark
```

Times are milliseconds from one local run:

| n | m | Kruskal five times | Median | Prim five times | Median |
|---:|---:|---|---:|---|---:|
| 1000 | 3000 | 9.877, 6.651, 4.579, 5.925, 3.908 | 5.925 | 9.501, 9.747, 7.318, 6.316, 4.116 | 7.318 |
| 5000 | 15000 | 16.659, 21.604, 17.921, 21.304, 18.395 | 18.395 | 17.152, 12.133, 10.958, 14.168, 22.929 | 14.168 |
| 20000 | 60000 | 160.014, 123.256, 126.591, 108.617, 88.321 | 123.256 | 89.420, 65.130, 93.229, 64.967, 95.089 | 89.420 |

Prim had the lower median at the two larger sizes in this run, while Kruskal was lower at the smallest size. These numbers are sensitive to JVM warm-up, allocation, garbage collection and other machine activity; they are observations, not an asymptotic proof. Both algorithms have `O(m log m)` heap or sort work in these implementations.

## Sources and assistance

I used the supplied assignment PDF, repository specification, adapter and public validator. I used OpenAI Codex to write README and ANALYSIS and to help implement and test the Java solution. I ran the reported commands and checked their outputs locally.
