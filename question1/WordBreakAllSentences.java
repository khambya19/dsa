package question1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Q1(b): Word Break II — return all valid segmentations of the query using the keyword dict. */
public class WordBreakAllSentences {

    /** Returns all valid keyword-based sentences that can form the input query. */
    public static List<String> wordBreak(String userQuery, List<String> keywordDict) {
        Set<String> wordSet = new HashSet<>(keywordDict);   // O(1) lookup for valid words
        Map<Integer, List<String>> memo = new HashMap<>(); // memo[start] = all sentences from index start to end
        return backtrack(userQuery, 0, wordSet, memo);
    }

    /** Returns all ways to segment query[start..] into dict words. */
    private static List<String> backtrack(String query, int start,
                                          Set<String> wordSet,
                                          Map<Integer, List<String>> memo) {
        if (memo.containsKey(start)) return memo.get(start);

        List<String> results = new ArrayList<>();
        if (start == query.length()) {
            results.add("");  // base: one way to segment empty string
            return results;
        }

        for (int end = start + 1; end <= query.length(); end++) {
            String prefix = query.substring(start, end);
            if (!wordSet.contains(prefix)) continue;

            List<String> suffixes = backtrack(query, end, wordSet, memo);  // all segmentations of rest
            for (String suffix : suffixes) {
                String sentence = prefix + (suffix.isEmpty() ? "" : " " + suffix);
                results.add(sentence);
            }
        }

        memo.put(start, results);
        return results;
    }

    /** Runs sample tests for generating all valid segmentations. */
    public static void main(String[] args) {
        String query1 = "nepaltrekkingguide";
        List<String> dict1 = Arrays.asList("nepal", "trekking", "guide", "nepaltrekking");
        System.out.println(wordBreak(query1, dict1));

        String query2 = "visitkathmandunepal";
        List<String> dict2 = Arrays.asList("visit", "kathmandu", "nepal", "visitkathmandu", "kathmandunepal");
        System.out.println(wordBreak(query2, dict2));

        String query3 = "everesthikingtrail";  // no valid segmentation (e.g. "trek" not "trail")
        List<String> dict3 = Arrays.asList("everest", "hiking", "trek");
        System.out.println(wordBreak(query3, dict3));
    }
}
