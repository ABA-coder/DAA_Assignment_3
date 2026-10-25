import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Benchmark {
    private static List<List<Integer>> graph(int n, int m, long seed) {
        Random random = new Random(seed);
        List<List<Integer>> edges = new ArrayList<>(m);
        for (int vertex = 0; vertex < n - 1; vertex++) {
            edges.add(List.of(vertex, vertex + 1, -1000));
        }
        while (edges.size() < m) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            int weight = random.nextInt(1000001);
            edges.add(List.of(a, b, weight));
        }
        return edges;
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static int root(int[] parent, int vertex) {
        while (parent[vertex] != vertex) {
            parent[vertex] = parent[parent[vertex]];
            vertex = parent[vertex];
        }
        return vertex;
    }

    private static void validate(Map<String, Object> answer, Map<String, Object> request, int n) {
        @SuppressWarnings("unchecked")
        List<List<Integer>> edges = (List<List<Integer>>) request.get("edges");
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) answer.get("edge_ids");
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        long cost = 0;
        for (int id : ids) {
            if (id < 0 || id >= edges.size()) {
                throw new AssertionError("Invalid edge ID");
            }
            List<Integer> edge = edges.get(id);
            int a = root(parent, edge.get(0));
            int b = root(parent, edge.get(1));
            if (a == b) {
                throw new AssertionError("Cyclic forest");
            }
            parent[a] = b;
            cost += edge.get(2);
        }
        if (cost != (Long) answer.get("cost")) {
            throw new AssertionError("Incorrect forest cost");
        }
        for (int vertex = 1; vertex < n; vertex++) {
            if (root(parent, vertex) != root(parent, 0)) {
                throw new AssertionError("Disconnected forest");
            }
        }
    }

    private static double run(String method, Map<String, Object> request, int n) {
        long start = System.nanoTime();
        Map<String, Object> answer = method.equals("kruskal")
                ? Algorithms.kruskal(request)
                : Algorithms.prim(request);
        long end = System.nanoTime();
        long expected = -1000L * (n - 1);
        if (((Long) answer.get("cost")) != expected
                || ((Integer) answer.get("components")) != 1
                || ((List<?>) answer.get("edge_ids")).size() != n - 1) {
            throw new AssertionError(method + " returned an invalid forest");
        }
        validate(answer, request, n);
        return (end - start) / 1000000.0;
    }

    public static void main(String[] args) {
        int[][] sizes = {{1000, 3000}, {5000, 15000}, {20000, 60000}};
        long seed = 20261025L;
        System.out.println("seed=" + seed + ", timer=System.nanoTime, unit=ms, warmup=1 run per method and size");
        for (int[] size : sizes) {
            int n = size[0];
            int m = size[1];
            Map<String, Object> request = Map.of("n", n, "edges", graph(n, m, seed + n));
            run("kruskal", request, n);
            run("prim", request, n);
            double[] kruskal = new double[5];
            double[] prim = new double[5];
            for (int repetition = 0; repetition < 5; repetition++) {
                if (repetition % 2 == 0) {
                    kruskal[repetition] = run("kruskal", request, n);
                    prim[repetition] = run("prim", request, n);
                } else {
                    prim[repetition] = run("prim", request, n);
                    kruskal[repetition] = run("kruskal", request, n);
                }
            }
            System.out.printf("n=%d m=%d kruskal=%s median=%.3f prim=%s median=%.3f%n",
                    n, m, Arrays.toString(kruskal), median(kruskal), Arrays.toString(prim), median(prim));
        }
    }
}
