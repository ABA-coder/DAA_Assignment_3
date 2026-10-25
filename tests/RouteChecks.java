import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RouteChecks {
    private static void expect(Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Expected " + expected + ", received " + actual);
        }
    }

    public static void main(String[] args) {
        String json = """
                {"task":"dag","n":6,"source":0,"edges":[[0,1,3],[0,1,3],[1,2,-7],[0,2,8],[2,3,4],[1,3,-3],[4,5,-2]]}
                """;
        Map<String, Object> answer = Algorithms.dag(Json.parseRequest(json));
        int[][] edges = Input.edges(Json.parseRequest(json));
        int[] parent = (int[]) answer.get("parent_edge");
        Long[] distance = (Long[]) answer.get("distance");
        expect(Algorithms.reconstructRoute(0, 3, edges, parent), List.of(0, 1, 3));
        expect(Algorithms.reconstructRoute(0, 0, edges, parent), List.of(0));
        expect(Algorithms.reconstructRoute(0, 5, edges, parent), List.of());
        expect(Algorithms.reconstructRoute(0, 1, edges, parent), List.of(0, 1));
        expect(parent[1], 1);
        expect(parent[3], 5);
        expect(distance[3], 0L);
        expect(distance[5], null);
        long routeCost = edges[parent[1]][2] + edges[parent[3]][2];
        expect(routeCost, distance[3]);
        System.out.println("PASS routes: target=[0, 1, 3], source=[0], unreachable=[], tied parallel edge=1, cost=0");
    }
}
