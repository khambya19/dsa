package question6;

import java.util.*;

/**
 * Task 6: Emergency Supply Logistics after an Earthquake — Nepal
 *
 * Part 1 & 2: Safest Path (Modified Dijkstra using -log(p) weights)
 *   - Transform: w(e) = -log(p(e))
 *   - Standard Dijkstra on transformed weights gives maximum-probability path
 *   - Proof: product of probabilities → sum of logs → min sum ≡ max product
 *
 * Part 3: Maximum Throughput (Edmonds-Karp = BFS-based Ford-Fulkerson)
 *   - Source: KTM, Sink: BS
 *   - Finds max flow (max trucks/hr) from depot to Bhaktapur Shelter
 *
 * Nodes: KTM=0, JA=1, JB=2, PH=3, BS=4
 */
public class EmergencyLogistics {

    static final int KTM = 0, JA = 1, JB = 2, PH = 3, BS = 4;
    static final String[] NAMES = {"KTM", "JA", "JB", "PH", "BS"};
    static final int N = 5;

    // ─── Safety Graph (Part 1 & 2) ────────────────────────────────────────────

    // [u][v] = safety probability p(u,v); 0 = no direct edge
    static final double[][] SAFETY = new double[N][N];

    static {
        // From the assignment table
        SAFETY[KTM][JA] = 0.90;  SAFETY[KTM][JB] = 0.80;
        SAFETY[JA][KTM] = 0.90;  SAFETY[JA][PH]  = 0.95;  SAFETY[JA][BS]  = 0.70;
        SAFETY[JB][KTM] = 0.80;  SAFETY[JB][JA]  = 0.60;  SAFETY[JB][BS]  = 0.90;
        SAFETY[PH][JA]  = 0.95;  SAFETY[PH][BS]  = 0.85;
        SAFETY[BS][JA]  = 0.70;  SAFETY[BS][JB]  = 0.90;  SAFETY[BS][PH]  = 0.85;
    }

    // ─── Capacity Graph (Part 3) ──────────────────────────────────────────────

    // [u][v] = truck capacity c(u,v); 0 = no edge
    static final int[][] CAP = new int[N][N];

    static {
        CAP[KTM][JA] = 10;  CAP[KTM][JB] = 15;
        CAP[JA][KTM] = 10;  CAP[JA][PH]  = 8;   CAP[JA][BS]  = 5;
        CAP[JB][KTM] = 15;  CAP[JB][JA]  = 4;   CAP[JB][BS]  = 12;
        CAP[PH][JA]  = 8;   CAP[PH][BS]  = 6;
        CAP[BS][JA]  = 5;   CAP[BS][JB]  = 12;  CAP[BS][PH]  = 6;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PART 1 & 2: SAFEST PATH — MODIFIED DIJKSTRA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Finds the safest path from source to all nodes.
     *
     * Transformation: w(u,v) = -ln(p(u,v))
     * Since ln is monotone: maximising product of p(e) ≡ minimising sum of -ln(p(e))
     * Standard Dijkstra with min-heap on transformed weights solves this.
     *
     * RELAX operation (modified):
     *   if dist[u] + w(u,v) < dist[v]:   (same as standard — because we transformed)
     *       dist[v] = dist[u] + w(u,v)
     *       prev[v] = u
     *
     * @param src source node
     * @return dist[] array of transformed distances (sum of -log weights)
     */
    static double[] dijkstraSafest(int src, int[] prev) {
        double[] dist = new double[N];
        Arrays.fill(dist, Double.MAX_VALUE);
        dist[src] = 0;
        Arrays.fill(prev, -1);

        // Min-heap: (transformed-distance, node)
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        pq.offer(new double[]{0, src});

        boolean[] visited = new boolean[N];

        while (!pq.isEmpty()) {
            double[] curr = pq.poll();
            int u = (int) curr[1];
            if (visited[u]) continue;
            visited[u] = true;

            for (int v = 0; v < N; v++) {
                if (SAFETY[u][v] == 0) continue;  // no edge
                double w = -Math.log(SAFETY[u][v]); // transform: -ln(p)

                // RELAX
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    prev[v] = u;
                    pq.offer(new double[]{dist[v], v});
                }
            }
        }
        return dist;
    }

    /** Reconstruct path from prev[] array */
    static List<Integer> getPath(int[] prev, int dest) {
        LinkedList<Integer> path = new LinkedList<>();
        for (int v = dest; v != -1; v = prev[v]) path.addFirst(v);
        return path;
    }

    /** Compute actual safety probability of a path */
    static double pathSafety(List<Integer> path) {
        double p = 1.0;
        for (int i = 0; i < path.size() - 1; i++) {
            p *= SAFETY[path.get(i)][path.get(i + 1)];
        }
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PART 3: MAX FLOW — EDMONDS-KARP (BFS-based Ford-Fulkerson)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Edmonds-Karp max flow from KTM to BS.
     * Uses BFS to find shortest augmenting path in residual graph.
     * Time Complexity: O(V * E^2)
     *
     * @return max flow value
     */
    static int edmondsKarp(int source, int sink) {
        int[][] residual = new int[N][N];
        // Copy capacities into residual graph
        for (int u = 0; u < N; u++)
            System.arraycopy(CAP[u], 0, residual[u], 0, N);

        int maxFlow = 0;
        int step = 1;

        while (true) {
            // BFS to find augmenting path
            int[] parent = bfs(residual, source, sink);
            if (parent[sink] == -1) break; // no augmenting path found

            // Find bottleneck (min capacity along path)
            int pathFlow = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, residual[u][v]);
            }

            // Reconstruct and print path
            List<Integer> path = new ArrayList<>();
            for (int v = sink; v != source; v = parent[v]) path.add(0, v);
            path.add(0, source);

            System.out.printf("  Step %d: Path %s | Bottleneck = %d trucks/hr%n",
                    step++, pathToString(path), pathFlow);

            // Update residual graph
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                residual[u][v] -= pathFlow;
                residual[v][u] += pathFlow;
            }

            maxFlow += pathFlow;
            System.out.printf("         Cumulative max flow so far: %d%n%n", maxFlow);
        }

        // Print final residual graph summary
        System.out.println("  Final Residual Graph (non-zero forward capacities):");
        for (int u = 0; u < N; u++)
            for (int v = 0; v < N; v++)
                if (residual[u][v] > 0 && CAP[u][v] > 0)
                    System.out.printf("    %s → %s : %d remaining capacity%n",
                            NAMES[u], NAMES[v], residual[u][v]);

        return maxFlow;
    }

    /** BFS on residual graph; returns parent array */
    static int[] bfs(int[][] residual, int source, int sink) {
        int[] parent = new int[N];
        Arrays.fill(parent, -1);
        parent[source] = source;
        Queue<Integer> queue = new LinkedList<>();
        queue.add(source);

        while (!queue.isEmpty() && parent[sink] == -1) {
            int u = queue.poll();
            for (int v = 0; v < N; v++) {
                if (parent[v] == -1 && residual[u][v] > 0) {
                    parent[v] = u;
                    queue.add(v);
                }
            }
        }
        return parent;
    }

    /** Find min s-t cut using BFS reachability on residual */
    static void findMinCut(int source, int sink) {
        // Run Edmonds-Karp to saturation, then find reachable set from source
        int[][] residual = new int[N][N];
        for (int u = 0; u < N; u++)
            System.arraycopy(CAP[u], 0, residual[u], 0, N);

        int[] parent;
        while (true) {
            parent = bfs(residual, source, sink);
            if (parent[sink] == -1) break;
            int pf = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v])
                pf = Math.min(pf, residual[parent[v]][v]);
            for (int v = sink; v != source; v = parent[v]) {
                residual[parent[v]][v] -= pf;
                residual[v][parent[v]] += pf;
            }
        }

        // BFS to find all nodes reachable from source in residual graph
        boolean[] reachable = new boolean[N];
        Queue<Integer> q = new LinkedList<>();
        q.add(source); reachable[source] = true;
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v = 0; v < N; v++)
                if (!reachable[v] && residual[u][v] > 0) {
                    reachable[v] = true; q.add(v);
                }
        }

        System.out.println("\n  MIN S-T CUT EDGES (reachable → non-reachable):");
        int cutCap = 0;
        for (int u = 0; u < N; u++)
            if (reachable[u])
                for (int v = 0; v < N; v++)
                    if (!reachable[v] && CAP[u][v] > 0) {
                        System.out.printf("    %s → %s : capacity = %d%n", NAMES[u], NAMES[v], CAP[u][v]);
                        cutCap += CAP[u][v];
                    }
        System.out.printf("  Min-Cut Capacity = %d  (should equal Max-Flow by Max-Flow Min-Cut theorem)%n", cutCap);
    }

    static String pathToString(List<Integer> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(NAMES[path.get(i)]);
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN
    // ═══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   EMERGENCY SUPPLY LOGISTICS — NEPAL EARTHQUAKE RESPONSE    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // ── Part 1 & 2: Safest Path ────────────────────────────────────────
        System.out.println("\n══ PART 1 & 2: SAFEST PATH (Modified Dijkstra) ══════════════════");
        System.out.println("  Transformation: w(e) = -ln(p(e))");
        System.out.println("  Maximise product of p ≡ Minimise sum of -ln(p)\n");

        int[] prev = new int[N];
        double[] dist = dijkstraSafest(KTM, prev);

        System.out.printf("  %-8s %-30s %-12s%n", "Dest", "Safest Path", "Safety Prob");
        System.out.println("  " + "-".repeat(55));
        for (int dest = 0; dest < N; dest++) {
            if (dest == KTM) continue;
            List<Integer> path = getPath(prev, dest);
            double safety = pathSafety(path);
            System.out.printf("  %-8s %-30s %.4f%n",
                    NAMES[dest], pathToString(path), safety);
        }

        System.out.println("\n  Safest path to Patan Hospital (PH):");
        List<Integer> phPath = getPath(prev, PH);
        System.out.println("    " + pathToString(phPath));
        System.out.printf("    Safety probability = %.4f%n", pathSafety(phPath));

        // ── Part 3: Max Flow ───────────────────────────────────────────────
        System.out.println("\n══ PART 3: MAXIMUM THROUGHPUT (Edmonds-Karp) ════════════════════");
        System.out.println("  Source: KTM (Tribhuvan Airport Depot)");
        System.out.println("  Sink:   BS  (Bhaktapur Shelter)\n");

        System.out.println("  AUGMENTING PATHS (BFS order):");
        int maxFlow = edmondsKarp(KTM, BS);

        System.out.println("\n  ══════════════════════════════════════════");
        System.out.printf("  MAXIMUM FLOW = %d trucks/hour%n", maxFlow);
        System.out.println("  ══════════════════════════════════════════");

        findMinCut(KTM, BS);

        System.out.println("\n  PROOF — Max-Flow Min-Cut Theorem:");
        System.out.println("  The max-flow equals the min-cut capacity.");
        System.out.println("  This means no set of roads can carry more trucks than the min-cut allows.");
        System.out.println("  Increasing throughput requires upgrading edges in the min-cut set.");

        // avoid unused warning for transformed dist
        if (dist[KTM] < 0) {
            System.out.println();
        }
    }
}
