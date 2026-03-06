package question1;

import java.util.HashMap;
import java.util.Map;

/** Q1(a): Max points on a line — given 2D points, return max count of points on any single line. */
public class MaxPointsOnLine {

    /** GCD so we normalize (dx, dy) to a canonical direction and avoid float slope. */
    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** Counts the maximum number of points that lie on one straight line. */
    public static int maxPoints(int[][] points) {
        int n = points.length;
        if (n <= 2) return n;

        int globalMax = 2;

        for (int i = 0; i < n; i++) {
            Map<String, Integer> slopeCount = new HashMap<>();  // slope key -> count of points on that line from i
            int duplicates = 0;  // points same as points[i]

            for (int j = i + 1; j < n; j++) {
                int dx = points[j][0] - points[i][0];
                int dy = points[j][1] - points[i][1];

                if (dx == 0 && dy == 0) {
                    duplicates++;
                    continue;
                }

                int g = gcd(Math.abs(dx), Math.abs(dy));
                dx /= g;
                dy /= g;
                // canonical form: dx >= 0, and if dx==0 then dy > 0
                if (dx < 0) { dx = -dx; dy = -dy; }
                else if (dx == 0) { dy = Math.abs(dy); }

                String key = dy + "/" + dx;
                slopeCount.put(key, slopeCount.getOrDefault(key, 0) + 1);
            }

            int localMax = 0;
            for (int count : slopeCount.values()) {
                localMax = Math.max(localMax, count);
            }
            // line through i with most other points: localMax + duplicates + 1 (point i)
            globalMax = Math.max(globalMax, localMax + duplicates + 1);
        }

        return globalMax;
    }

    /** Runs sample tests for the max-points-on-a-line problem. */
    public static void main(String[] args) {
        int[][] locations1 = {{1, 1}, {2, 2}, {3, 3}};  // collinear -> 3
        System.out.println("=== Example 1: Ideal Repeater Placement ===");
        System.out.println("Input:    [[1,1],[2,2],[3,3]]");
        System.out.println("Output:   " + maxPoints(locations1));
        System.out.println("Expected: 3");

        System.out.println();

        int[][] locations2 = {{1, 1}, {3, 2}, {5, 3}, {4, 1}, {2, 3}, {1, 4}};  // max 4 on one line
        System.out.println("=== Example 2: Complex Repeater Placement ===");
        System.out.println("Input:    [[1,1],[3,2],[5,3],[4,1],[2,3],[1,4]]");
        System.out.println("Output:   " + maxPoints(locations2));
        System.out.println("Expected: 4");

        System.out.println();

        int[][] single = {{0, 0}};
        System.out.println("=== Edge Case: Single Point ===");
        System.out.println("Output: " + maxPoints(single));

        int[][] dups = {{1, 1}, {1, 1}, {2, 2}};
        System.out.println("=== Edge Case: Duplicate Points ===");
        System.out.println("Output: " + maxPoints(dups));
    }
}
