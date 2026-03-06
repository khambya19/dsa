package question4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Q4: DP + greedy source allocation for a smart grid.
public class SmartEnergyGridOptimization {

    private static final double SHORTFALL_WEIGHT = 1000.0;
    private static final double DEVIATION_WEIGHT = 1.0;
    private static final double DIESEL_SOFT_PENALTY = 0.05;

    static class EnergySource {
        String id;
        String type;
        int maxCapacity;
        int availStart;
        int availEnd;
        double costPerKWh;
        boolean renewable;

        EnergySource(String id, String type, int maxCapacity, int availStart, int availEnd,
                     double costPerKWh, boolean renewable) {
            this.id = id;
            this.type = type;
            this.maxCapacity = maxCapacity;
            this.availStart = availStart;
            this.availEnd = availEnd;
            this.costPerKWh = costPerKWh;
            this.renewable = renewable;
        }

        // Checks if this source can supply power at the given hour.
        boolean isAvailable(int hour) {
            if (availStart <= availEnd) {
                return hour >= availStart && hour <= availEnd;
            }
            return hour >= availStart || hour <= availEnd;
        }
    }

    static class AllocationRow {
        int hour;
        String district;
        int solar;
        int hydro;
        int diesel;
        int totalUsed;
        int target;
        int demand;
        boolean withinTolerance;

        AllocationRow(int hour, String district, int solar, int hydro, int diesel, int target, int demand) {
            this.hour = hour;
            this.district = district;
            this.solar = solar;
            this.hydro = hydro;
            this.diesel = diesel;
            this.totalUsed = solar + hydro + diesel;
            this.target = target;
            this.demand = demand;
            int minAllowed = (int) Math.ceil(demand * 0.90);
            int maxAllowed = (int) Math.floor(demand * 1.10);
            this.withinTolerance = totalUsed >= minAllowed && totalUsed <= maxAllowed;
        }

        // Returns how much of the district demand was covered.
        double percentFulfilled() {
            return demand == 0 ? 100.0 : (totalUsed * 100.0) / demand;
        }
    }

    static class HourResult {
        int hour;
        List<AllocationRow> rows = new ArrayList<>();
        boolean usedRelaxedDp;
        double hourCost;
        int hourDemand;
        int renewableCapacity;
        boolean solarAvailable;
    }

    static class Plan {
        double objective;
        double totalCost;
        int totalDiesel;
        int[][] allocByDistrictSource;
        int[] targetByDistrict;
    }

    static final int[] HOURS = {6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    static final String[] DISTRICTS = {"A", "B", "C"};

    // Demand[hourIndex][districtIndex]
    static final int[][] DEMAND = {
        {20, 15, 25},
        {22, 16, 28},
        {25, 18, 30},
        {28, 20, 32},
        {30, 22, 35},
        {32, 24, 38},
        {35, 25, 40},
        {33, 23, 37},
        {30, 20, 34},
        {28, 18, 30},
        {25, 16, 28},
        {22, 15, 26},
        {20, 14, 24},
        {25, 18, 30},
        {28, 20, 32}
    };

    static final EnergySource SOLAR = new EnergySource("S1", "Solar", 50, 6, 18, 1.0, true);
    static final EnergySource HYDRO = new EnergySource("S2", "Hydro", 40, 0, 24, 1.5, true);
    static final EnergySource DIESEL = new EnergySource("S3", "Diesel", 60, 17, 23, 3.0, false);

    // Cheapest first.
    static final EnergySource[] SOURCES = {SOLAR, HYDRO, DIESEL};

    // Builds one-hour allocations using strict DP first, then relaxed DP if needed.
    private static HourResult allocateHour(int hour, int[] demandByDistrict) {
        int[] caps = new int[SOURCES.length];
        for (int s = 0; s < SOURCES.length; s++) {
            caps[s] = SOURCES[s].isAvailable(hour) ? SOURCES[s].maxCapacity : 0;
        }

        int[] minNeed = new int[demandByDistrict.length];
        int[] maxNeed = new int[demandByDistrict.length];
        for (int i = 0; i < demandByDistrict.length; i++) {
            minNeed[i] = (int) Math.ceil(demandByDistrict[i] * 0.90);
            maxNeed[i] = (int) Math.floor(demandByDistrict[i] * 1.10);
        }

        Map<Long, Plan> strictMemo = new HashMap<>();
        Plan best = solveDp(
            0, caps[0], caps[1], caps[2], demandByDistrict, minNeed, maxNeed, false, strictMemo
        );

        boolean usedRelaxedDp = false;
        if (best == null) {
            usedRelaxedDp = true;
            Map<Long, Plan> relaxedMemo = new HashMap<>();
            best = solveDp(
                0, caps[0], caps[1], caps[2], demandByDistrict, minNeed, maxNeed, true, relaxedMemo
            );
        }

        if (best == null) {
            throw new IllegalStateException("No feasible allocation found for hour " + hour);
        }

        HourResult result = new HourResult();
        result.hour = hour;
        result.usedRelaxedDp = usedRelaxedDp;
        result.hourCost = best.totalCost;
        result.hourDemand = Arrays.stream(demandByDistrict).sum();
        result.renewableCapacity = caps[0] + caps[1];
        result.solarAvailable = caps[0] > 0;

        for (int d = 0; d < DISTRICTS.length; d++) {
            int solarUsed = best.allocByDistrictSource[d][0];
            int hydroUsed = best.allocByDistrictSource[d][1];
            int dieselUsed = best.allocByDistrictSource[d][2];
            AllocationRow row = new AllocationRow(
                hour, DISTRICTS[d], solarUsed, hydroUsed, dieselUsed, best.targetByDistrict[d], demandByDistrict[d]
            );
            result.rows.add(row);
        }
        return result;
    }

    // DP over districts and remaining capacities to minimize cost and penalty.
    private static Plan solveDp(
        int districtIdx,
        int remSolar,
        int remHydro,
        int remDiesel,
        int[] demand,
        int[] minNeed,
        int[] maxNeed,
        boolean relaxed,
        Map<Long, Plan> memo
    ) {
        if (districtIdx == demand.length) {
            Plan base = new Plan();
            base.objective = 0;
            base.totalCost = 0;
            base.totalDiesel = 0;
            base.allocByDistrictSource = new int[demand.length][SOURCES.length];
            base.targetByDistrict = new int[demand.length];
            return base;
        }

        long key = encodeKey(districtIdx, remSolar, remHydro, remDiesel);
        if (memo.containsKey(key)) return memo.get(key);

        int lower = relaxed ? 0 : minNeed[districtIdx];
        int upper = maxNeed[districtIdx];

        Plan best = null;
        for (int target = lower; target <= upper; target++) {
            // Greedy source use: solar -> hydro -> diesel.
            int useSolar = Math.min(remSolar, target);
            int remaining = target - useSolar;

            int useHydro = Math.min(remHydro, remaining);
            remaining -= useHydro;

            int useDiesel = Math.min(remDiesel, remaining);
            remaining -= useDiesel;

            if (remaining > 0) continue;

            Plan next = solveDp(
                districtIdx + 1,
                remSolar - useSolar,
                remHydro - useHydro,
                remDiesel - useDiesel,
                demand,
                minNeed,
                maxNeed,
                relaxed,
                memo
            );
            if (next == null) continue;

            double thisCost = useSolar * SOLAR.costPerKWh
                + useHydro * HYDRO.costPerKWh
                + useDiesel * DIESEL.costPerKWh;

            int shortfall = Math.max(0, minNeed[districtIdx] - target);
            int deviation = Math.abs(demand[districtIdx] - target);
            double penalty = relaxed
                ? shortfall * SHORTFALL_WEIGHT + deviation * DEVIATION_WEIGHT
                : 0.0;

            double objective = penalty + thisCost + (useDiesel * DIESEL_SOFT_PENALTY) + next.objective;
            double totalCost = thisCost + next.totalCost;
            int totalDiesel = useDiesel + next.totalDiesel;

            if (best == null || objective < best.objective - 1e-9
                || (Math.abs(objective - best.objective) < 1e-9 && totalDiesel < best.totalDiesel)
                || (Math.abs(objective - best.objective) < 1e-9 && totalDiesel == best.totalDiesel
                    && totalCost < best.totalCost)) {
                Plan candidate = new Plan();
                candidate.objective = objective;
                candidate.totalCost = totalCost;
                candidate.totalDiesel = totalDiesel;
                candidate.allocByDistrictSource = copy2d(next.allocByDistrictSource);
                candidate.targetByDistrict = Arrays.copyOf(next.targetByDistrict, next.targetByDistrict.length);
                candidate.allocByDistrictSource[districtIdx][0] = useSolar;
                candidate.allocByDistrictSource[districtIdx][1] = useHydro;
                candidate.allocByDistrictSource[districtIdx][2] = useDiesel;
                candidate.targetByDistrict[districtIdx] = target;
                best = candidate;
            }
        }

        memo.put(key, best);
        return best;
    }

    // Explains why diesel was needed in a specific hour.
    private static String dieselReason(HourResult hr) {
        if (!hr.solarAvailable) {
            return "solar unavailable after 18:00; hydro alone was not enough";
        }
        if (hr.hourDemand > hr.renewableCapacity) {
            return "hourly demand exceeded renewable capacity";
        }
        if (hr.usedRelaxedDp) {
            return "strict 90% minimum not feasible, relaxed DP used";
        }
        return "renewable capacity was exhausted by earlier allocations";
    }

    // Creates a deep copy of a 2D int array.
    private static int[][] copy2d(int[][] src) {
        int[][] out = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            out[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return out;
    }

    // Encodes DP state into a compact long key for memoization.
    private static long encodeKey(int idx, int s, int h, int d) {
        return (((((long) idx) << 8) | s) << 8 | h) << 8 | d;
    }

    // Runs full-day optimization and prints allocation plus summary tables.
    public static void main(String[] args) {
        List<HourResult> dayResults = new ArrayList<>();

        System.out.println("Smart Energy Grid Load Distribution Optimization");
        System.out.println();
        System.out.println("Sources (cheapest to costliest):");
        for (EnergySource src : SOURCES) {
            System.out.printf(
                "  %s (%s): capacity=%d kWh/h, hours=%02d-%02d, cost=Rs. %.2f, renewable=%s%n",
                src.id,
                src.type,
                src.maxCapacity,
                src.availStart,
                src.availEnd,
                src.costPerKWh,
                src.renewable ? "yes" : "no"
            );
        }
        System.out.println();
        System.out.printf(
            "%-5s %-8s %-7s %-7s %-7s %-8s %-10s %-8s %-9s %-8s%n",
            "Hour", "District", "Solar", "Hydro", "Diesel", "Target", "TotalUsed", "Demand", "Fulfilled", "Status"
        );
        System.out.println("----------------------------------------------------------------------------------------");

        for (int i = 0; i < HOURS.length; i++) {
            HourResult hourResult = allocateHour(HOURS[i], DEMAND[i]);
            dayResults.add(hourResult);

            for (AllocationRow row : hourResult.rows) {
                System.out.printf(
                    "%-5d %-8s %-7d %-7d %-7d %-8d %-10d %-8d %-8.1f%% %-8s%n",
                    row.hour,
                    "Dist-" + row.district,
                    row.solar,
                    row.hydro,
                    row.diesel,
                    row.target,
                    row.totalUsed,
                    row.demand,
                    row.percentFulfilled(),
                    row.withinTolerance ? "OK" : "RELAXED"
                );
            }
            if (hourResult.usedRelaxedDp) {
                System.out.printf(
                    "      note (hour %02d): strict 90%%-110%% bounds were infeasible; relaxed DP used.%n",
                    hourResult.hour
                );
            }
            System.out.println();
        }

        double totalCost = 0.0;
        int totalSolar = 0;
        int totalHydro = 0;
        int totalDiesel = 0;
        int totalDemand = 0;
        int totalDelivered = 0;
        int[] sourceUsage = new int[SOURCES.length];

        for (HourResult hr : dayResults) {
            totalCost += hr.hourCost;
            for (AllocationRow row : hr.rows) {
                totalSolar += row.solar;
                totalHydro += row.hydro;
                totalDiesel += row.diesel;
                sourceUsage[0] += row.solar;
                sourceUsage[1] += row.hydro;
                sourceUsage[2] += row.diesel;
                totalDemand += row.demand;
                totalDelivered += row.totalUsed;
            }
        }

        int renewableDelivered = 0;
        for (int s = 0; s < SOURCES.length; s++) {
            if (SOURCES[s].renewable) {
                renewableDelivered += sourceUsage[s];
            }
        }
        double renewableShare = totalDelivered == 0 ? 0 : (renewableDelivered * 100.0) / totalDelivered;
        double demandCoverage = totalDemand == 0 ? 100.0 : (totalDelivered * 100.0) / totalDemand;

        System.out.println("Summary");
        System.out.println("-------");
        System.out.printf("Total cost: Rs. %.2f%n", totalCost);
        System.out.printf("Total demand: %d kWh%n", totalDemand);
        System.out.printf("Total delivered: %d kWh (%.1f%% of demand)%n", totalDelivered, demandCoverage);
        System.out.printf("Renewable energy share: %.1f%%%n", renewableShare);
        System.out.printf("Solar used: %d kWh, Hydro used: %d kWh, Diesel used: %d kWh%n",
            totalSolar, totalHydro, totalDiesel);
        System.out.println("Source-wise energy and cost:");
        for (int s = 0; s < SOURCES.length; s++) {
            System.out.printf(
                "  %s (%s): %d kWh, Rs. %.2f%n",
                SOURCES[s].id,
                SOURCES[s].type,
                sourceUsage[s],
                sourceUsage[s] * SOURCES[s].costPerKWh
            );
        }
        System.out.println();

        System.out.println("Diesel usage details");
        System.out.println("--------------------");
        boolean dieselFound = false;
        for (HourResult hr : dayResults) {
            String reason = dieselReason(hr);
            for (AllocationRow row : hr.rows) {
                if (row.diesel > 0) {
                    System.out.printf(
                        "Hour %02d, District %s -> Diesel %d kWh, reason: %s%n",
                        row.hour, row.district, row.diesel, reason
                    );
                    dieselFound = true;
                }
            }
        }
        if (!dieselFound) {
            System.out.println("No diesel required.");
        }
        System.out.println();

        System.out.println("Efficiency and trade-offs");
        System.out.println("-------------------------");
        System.out.println("DP checks feasible district allocations under capacity constraints each hour.");
        System.out.println("Greedy ordering keeps source choice low-cost (solar then hydro then diesel).");
        System.out.println("Trade-off: optimization is hour-wise, so it does not optimize storage or multi-hour coupling.");
    }
}
