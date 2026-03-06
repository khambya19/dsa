package question3;

// Q3: Max profit with at most maxTrades transactions.
// Time: O(k*n), Space: O(k*n)
public class CommodityTrading {

    // Returns maximum profit with at most maxTrades buy/sell pairs.
    public static int maxProfit(int maxTrades, int[] prices) {
        if (prices == null || prices.length == 0 || maxTrades == 0) return 0;

        int n = prices.length;

        // If trades are effectively unlimited, take every upward move.
        if (maxTrades >= n / 2) {
            int profit = 0;
            for (int i = 1; i < n; i++) {
                if (prices[i] > prices[i - 1]) {
                    profit += prices[i] - prices[i - 1];
                }
            }
            return profit;
        }

        // dp[k][i] = best profit up to day i with at most k trades.
        int[][] dp = new int[maxTrades + 1][n];

        for (int k = 1; k <= maxTrades; k++) {
            // Best value of (previous profit - buy price) seen so far.
            int maxSoFar = -prices[0];

            for (int i = 1; i < n; i++) {
                // Skip today, or sell today using the best prior buy.
                dp[k][i] = Math.max(dp[k][i - 1], prices[i] + maxSoFar);
                // Update best buy state for future days.
                maxSoFar = Math.max(maxSoFar, dp[k - 1][i] - prices[i]);
            }
        }

        return dp[maxTrades][n - 1];
    }

    // Prints simple buy/sell points for monotonic segments.
    public static void printSimpleTradeLog(int[] prices) {
        if (prices == null || prices.length == 0) {
            System.out.println("  Trade Log: no prices provided");
            return;
        }

        System.out.println("  Trade Log:");
        int i = 0;
        int n = prices.length;
        while (i < n - 1) {
            // Local minimum (buy).
            while (i < n - 1 && prices[i + 1] <= prices[i]) i++;
            int buyDay = i;
            // Local maximum (sell).
            while (i < n - 1 && prices[i + 1] >= prices[i]) i++;
            int sellDay = i;
            if (buyDay != sellDay) {
                System.out.printf("    Buy  Day %d @ %d NPR  |  Sell Day %d @ %d NPR  |  Profit = %d NPR%n",
                        buyDay + 1, prices[buyDay],
                        sellDay + 1, prices[sellDay],
                        prices[sellDay] - prices[buyDay]);
            }
        }
    }

    // Test / Driver

    public static void main(String[] args) {

        // Example 1
        System.out.println("=== Example 1: Basic Trade ===");
        int[] prices1 = {2000, 4000, 1000};
        int k1 = 2;
        int profit1 = maxProfit(k1, prices1);
        System.out.println("Prices (NPR): [2000, 4000, 1000]");
        System.out.println("Max Trades:   " + k1);
        System.out.println("Output:       " + profit1 + " NPR");
        System.out.println("Expected:     2000 NPR");

        System.out.println();

        // Example 2
        System.out.println("=== Example 2: Two Profitable Cycles ===");
        int[] prices2 = {3000, 2000, 6000, 5000, 0, 3000};
        int k2 = 2;
        int profit2 = maxProfit(k2, prices2);
        System.out.println("Prices (NPR): [3000, 2000, 6000, 5000, 0, 3000]");
        System.out.println("Max Trades:   " + k2);
        System.out.println("Output:       " + profit2 + " NPR"); // Expected: 7000
        System.out.println("Expected:     7000 NPR  (buy@2000 sell@6000 + buy@0 sell@3000)");

        System.out.println();

        // Example 3
        System.out.println("=== Example 3: Decreasing Prices (No Profit) ===");
        int[] prices3 = {5000, 4000, 3000, 2000, 1000};
        int profit3 = maxProfit(2, prices3);
        System.out.println("Prices (NPR): [5000, 4000, 3000, 2000, 1000]");
        System.out.println("Output:       " + profit3 + " NPR"); // Expected: 0

        System.out.println();

        // Example 4
        System.out.println("=== Example 4: Single Transaction ===");
        int[] prices4 = {1000, 5000, 3000, 8000, 2000};
        int profit4 = maxProfit(1, prices4);
        System.out.println("Prices (NPR): [1000, 5000, 3000, 8000, 2000]");
        System.out.println("Max Trades:   1");
        System.out.println("Output:       " + profit4 + " NPR"); // Expected: 7000
        System.out.println("Expected:     7000 NPR  (buy@1000 sell@8000)");

        System.out.println();
        System.out.println("=== Trade Log for Example 1 ===");
        printSimpleTradeLog(prices1);
    }
}
