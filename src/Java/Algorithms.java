import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class Algorithms {
    private Algorithms() {
    }

    private static Map<String, Object> result(Object... values) {
        Map<String, Object> answer = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            answer.put((String) values[i], values[i + 1]);
        }
        return answer;
    }

    private static final class DisjointSet {
        private final int[] parent;
        private final int[] size;
        private int components;
        private int largest;

        private DisjointSet(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = n == 0 ? 0 : 1;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        private int find(int vertex) {
            int root = vertex;
            while (parent[root] != root) {
                root = parent[root];
            }
            while (parent[vertex] != vertex) {
                int next = parent[vertex];
                parent[vertex] = root;
                vertex = next;
            }
            return root;
        }

        private boolean union(int left, int right) {
            int a = find(left);
            int b = find(right);
            if (a == b) {
                return false;
            }
            if (size[a] < size[b]) {
                int temporary = a;
                a = b;
                b = temporary;
            }
            parent[b] = a;
            size[a] += size[b];
            components--;
            largest = Math.max(largest, size[a]);
            return true;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);
        DisjointSet set = new DisjointSet(n);
        int[][] states = new int[operations.length][2];
        for (int i = 0; i < operations.length; i++) {
            set.union(operations[i][0], operations[i][1]);
            states[i][0] = set.components;
            states[i][1] = set.largest;
        }
        return result("states", states);
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        Integer[] order = new Integer[edges.length];
        for (int i = 0; i < edges.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> {
            int comparison = Integer.compare(edges[a][2], edges[b][2]);
            return comparison != 0 ? comparison : Integer.compare(a, b);
        });
        DisjointSet set = new DisjointSet(n);
        List<Integer> chosen = new ArrayList<>();
        long cost = 0;
        for (int id : order) {
            int[] edge = edges[id];
            if (set.union(edge[0], edge[1])) {
                chosen.add(id);
                cost += edge[2];
            }
        }
        return result("cost", cost, "edge_ids", chosen, "components", set.components);
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        int[] head = new int[n];
        int[] next = new int[edges.length * 2];
        int[] edgeId = new int[edges.length * 2];
        Arrays.fill(head, -1);
        for (int id = 0; id < edges.length; id++) {
            int a = edges[id][0];
            int b = edges[id][1];
            int first = id * 2;
            next[first] = head[a];
            edgeId[first] = id;
            head[a] = first;
            next[first + 1] = head[b];
            edgeId[first + 1] = id;
            head[b] = first + 1;
        }
        boolean[] visited = new boolean[n];
        PriorityQueue<Integer> queue = new PriorityQueue<>((a, b) -> {
            int comparison = Integer.compare(edges[a][2], edges[b][2]);
            return comparison != 0 ? comparison : Integer.compare(a, b);
        });
        List<Integer> chosen = new ArrayList<>();
        long cost = 0;
        int components = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visited[start] = true;
            for (int arc = head[start]; arc != -1; arc = next[arc]) {
                queue.add(edgeId[arc]);
            }
            while (!queue.isEmpty()) {
                int id = queue.remove();
                int a = edges[id][0];
                int b = edges[id][1];
                if (visited[a] == visited[b]) {
                    continue;
                }
                int vertex = visited[a] ? b : a;
                visited[vertex] = true;
                chosen.add(id);
                cost += edges[id][2];
                for (int arc = head[vertex]; arc != -1; arc = next[arc]) {
                    int candidate = edgeId[arc];
                    int other = edges[candidate][0] == vertex ? edges[candidate][1] : edges[candidate][0];
                    if (!visited[other]) {
                        queue.add(candidate);
                    }
                }
            }
        }
        return result("cost", cost, "edge_ids", chosen, "components", components);
    }

    private static final class Adjacency {
        private final int[] head;
        private final int[] next;

        private Adjacency(int n, int[][] edges, boolean transpose) {
            head = new int[n];
            next = new int[edges.length];
            Arrays.fill(head, -1);
            for (int id = 0; id < edges.length; id++) {
                int from = edges[id][transpose ? 1 : 0];
                next[id] = head[from];
                head[from] = id;
            }
        }
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        Adjacency forward = new Adjacency(n, edges, false);
        Adjacency reverse = new Adjacency(n, edges, true);
        boolean[] seen = new boolean[n];
        int[] vertices = new int[n];
        int[] cursor = new int[n];
        int[] finish = new int[n];
        int count = 0;
        for (int start = 0; start < n; start++) {
            if (seen[start]) {
                continue;
            }
            int depth = 0;
            seen[start] = true;
            vertices[0] = start;
            cursor[0] = forward.head[start];
            while (depth >= 0) {
                int arc = cursor[depth];
                if (arc == -1) {
                    finish[count++] = vertices[depth--];
                    continue;
                }
                cursor[depth] = forward.next[arc];
                int neighbor = edges[arc][1];
                if (!seen[neighbor]) {
                    seen[neighbor] = true;
                    vertices[++depth] = neighbor;
                    cursor[depth] = forward.head[neighbor];
                }
            }
        }
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int labels = 0;
        for (int index = count - 1; index >= 0; index--) {
            int start = finish[index];
            if (component[start] != -1) {
                continue;
            }
            int depth = 0;
            vertices[0] = start;
            component[start] = labels;
            while (depth >= 0) {
                int vertex = vertices[depth--];
                for (int arc = reverse.head[vertex]; arc != -1; arc = reverse.next[arc]) {
                    int neighbor = edges[arc][0];
                    if (component[neighbor] == -1) {
                        component[neighbor] = labels;
                        vertices[++depth] = neighbor;
                    }
                }
            }
            labels++;
        }
        HashSet<Long> unique = new HashSet<>();
        List<int[]> condensation = new ArrayList<>();
        for (int[] edge : edges) {
            int from = component[edge[0]];
            int to = component[edge[1]];
            long key = ((long) from << 32) | (to & 0xffffffffL);
            if (from != to && unique.add(key)) {
                condensation.add(new int[]{from, to});
            }
        }
        return result("component", component, "condensation", condensation);
    }

    private static int[] topologicalOrder(int n, int[][] edges, Adjacency graph) {
        byte[] state = new byte[n];
        int[] vertices = new int[n];
        int[] cursor = new int[n];
        int[] finish = new int[n];
        int count = 0;
        for (int start = 0; start < n; start++) {
            if (state[start] != 0) {
                continue;
            }
            int depth = 0;
            state[start] = 1;
            vertices[0] = start;
            cursor[0] = graph.head[start];
            while (depth >= 0) {
                int arc = cursor[depth];
                if (arc == -1) {
                    int vertex = vertices[depth--];
                    state[vertex] = 2;
                    finish[count++] = vertex;
                    continue;
                }
                cursor[depth] = graph.next[arc];
                int neighbor = edges[arc][1];
                if (state[neighbor] == 1) {
                    return null;
                }
                if (state[neighbor] == 0) {
                    state[neighbor] = 1;
                    vertices[++depth] = neighbor;
                    cursor[depth] = graph.head[neighbor];
                }
            }
        }
        for (int left = 0, right = n - 1; left < right; left++, right--) {
            int temporary = finish[left];
            finish[left] = finish[right];
            finish[right] = temporary;
        }
        return finish;
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        int[] order = topologicalOrder(n, edges, new Adjacency(n, edges, false));
        return result("acyclic", order != null, "order", order == null ? new int[0] : order);
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        Adjacency graph = new Adjacency(n, edges, false);
        int[] order = topologicalOrder(n, edges, graph);
        if (order == null) {
            return result("acyclic", false, "distance", new Long[0], "parent_edge", new int[0]);
        }
        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;
        for (int vertex : order) {
            if (distance[vertex] == null) {
                continue;
            }
            for (int arc = graph.head[vertex]; arc != -1; arc = graph.next[arc]) {
                int neighbor = edges[arc][1];
                long candidate = distance[vertex] + edges[arc][2];
                if (distance[neighbor] == null || candidate < distance[neighbor]) {
                    distance[neighbor] = candidate;
                    parentEdge[neighbor] = arc;
                }
            }
        }
        return result("acyclic", true, "distance", distance, "parent_edge", parentEdge);
    }

    static List<Integer> reconstructRoute(int source, int target, int[][] edges, int[] parentEdge) {
        List<Integer> route = new ArrayList<>();
        int current = target;
        while (current != source) {
            route.add(current);
            int id = parentEdge[current];
            if (id < 0) {
                return new ArrayList<>();
            }
            current = edges[id][0];
        }
        route.add(source);
        java.util.Collections.reverse(route);
        return route;
    }
}
