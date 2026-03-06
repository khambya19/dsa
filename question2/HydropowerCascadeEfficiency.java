package question2;

/**
 * Q2: Hydropower Plant Cascade Efficiency
 * Maximum path sum in a binary tree (any connected sequence of plants).
 * Each node can be used at most once in the path.
 */
public class HydropowerCascadeEfficiency {

    // Node: one plant; val = net power (or cost if negative), left/right = upstream tributaries
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        // Creates a single-node plant.
        TreeNode(int val) {
            this.val = val;
        }

        // Creates a plant with left and right tributary links.
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * Returns the maximum net power generation from any valid sequence of
     * connected hydropower plants (maximum path sum in the tree).
     */
    public static int maxPathSum(TreeNode root) {
        int[] maxSum = { Integer.MIN_VALUE };  // best path sum seen so far (array so helper can update it)
        maxGain(root, maxSum);
        return maxSum[0];
    }

    /**
     * Returns the maximum gain from a path that starts at node and goes
     * downward through at most one child. Updates maxSum with the best
     * "arch" path that goes through this node (left + node + right).
     */
    private static int maxGain(TreeNode node, int[] maxSum) {
        if (node == null) return 0;

        // best sum from left/right subtree if we take only one branch (0 = skip if negative)
        int leftGain = Math.max(0, maxGain(node.left, maxSum));
        int rightGain = Math.max(0, maxGain(node.right, maxSum));

        // path that goes left -> this node -> right; candidate for overall best path
        int pathThroughNode = node.val + leftGain + rightGain;
        maxSum[0] = Math.max(maxSum[0], pathThroughNode);

        // for parent: best we can contribute is this node + the better of the two branches
        return node.val + Math.max(leftGain, rightGain);
    }

    /** Runs sample tests for the hydropower cascade path sum. */
    public static void main(String[] args) {
        // Example 1: root=1, left=2, right=3 -> best path 2->1->3 = 6
        TreeNode root1 = new TreeNode(1, new TreeNode(2), new TreeNode(3));
        System.out.println("Example 1: " + maxPathSum(root1));

        // Example 2: -10 at root; 9 left; 20 right with children 15,7 -> best path 15->20->7 = 42
        TreeNode root2 = new TreeNode(-10,
            new TreeNode(9),
            new TreeNode(20, new TreeNode(15), new TreeNode(7))
        );
        System.out.println("Example 2: " + maxPathSum(root2));
    }
}
