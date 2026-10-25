import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class LimitChecks {
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static List<List<Integer>> chain(int n, int count, boolean weighted, int firstWeight, int extraWeight) {
        List<List<Integer>> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int from = i < n - 1 ? i : (i - n + 1) % (n - 1);
            int to = from + 1;
            if (weighted) {
                rows.add(List.of(from, to, i < n - 1 ? firstWeight : extraWeight));
            } else {
                rows.add(List.of(from, to));
            }
        }
        return rows;
    }

    public static void main(String[] args) {
        int n = 100000;
        int count = 250000;
        List<List<Integer>> operations = chain(n, count, false, 0, 0);
        Map<String, Object> dsu = Algorithms.dsu(Map.of("n", n, "operations", operations));
        int[][] states = (int[][]) dsu.get("states");
        require(states.length == count && states[count - 1][0] == 1 && states[count - 1][1] == n, "DSU limit");
        System.out.println("PASS DSU n=100000 q=250000");

        List<List<Integer>> undirected = chain(n, count, true, 1, 2);
        for (String method : List.of("kruskal", "prim")) {
            Map<String, Object> answer = method.equals("kruskal")
                    ? Algorithms.kruskal(Map.of("n", n, "edges", undirected))
                    : Algorithms.prim(Map.of("n", n, "edges", undirected));
            require(((Long) answer.get("cost")) == n - 1L, method + " cost");
            require(((Integer) answer.get("components")) == 1, method + " components");
            List<?> ids = (List<?>) answer.get("edge_ids");
            require(ids.size() == n - 1, method + " edge count");
            require(new HashSet<>(ids).size() == n - 1, method + " unique edges");
            System.out.println("PASS " + method + " n=100000 m=250000");
        }

        List<List<Integer>> directed = chain(n, count, false, 0, 0);
        Map<String, Object> scc = Algorithms.scc(Map.of("n", n, "edges", directed));
        int[] labels = (int[]) scc.get("component");
        HashSet<Integer> uniqueLabels = new HashSet<>();
        for (int label : labels) {
            uniqueLabels.add(label);
        }
        require(uniqueLabels.size() == n, "SCC labels");
        require(((List<?>) scc.get("condensation")).size() == n - 1, "SCC condensation");
        System.out.println("PASS scc n=100000 m=250000");

        Map<String, Object> topo = Algorithms.topo(Map.of("n", n, "edges", directed));
        int[] order = (int[]) topo.get("order");
        require((Boolean) topo.get("acyclic") && order.length == n, "Topological order");
        for (int i = 0; i < n; i++) {
            require(order[i] == i, "Topological chain position " + i);
        }
        System.out.println("PASS topo n=100000 m=250000");

        List<List<Integer>> weightedDirected = chain(n, count, true, -1, 1);
        Map<String, Object> dag = Algorithms.dag(Map.of("n", n, "source", 0, "edges", weightedDirected));
        Long[] distance = (Long[]) dag.get("distance");
        int[] parent = (int[]) dag.get("parent_edge");
        require((Boolean) dag.get("acyclic") && distance[n - 1] == -(n - 1L), "DAG distance");
        require(parent[n - 1] == n - 2, "DAG parent");
        System.out.println("PASS dag n=100000 m=250000");
    }
}
