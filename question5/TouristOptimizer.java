package question5;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import javax.swing.border.TitledBorder;
import java.util.*;
import java.util.List;

/**
 * Q5(a): Tourist Spot Optimizer - GUI with Heuristic-Based Itinerary Planner
 */
public class TouristOptimizer extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Color BG = new Color(240, 245, 251);
    private static final Color PANEL_BG = new Color(255, 255, 255);
    private static final Color PRIMARY = new Color(255, 197, 86);
    private static final Color PRIMARY_DARK = new Color(228, 237, 247);
    private static final Color SECONDARY = new Color(143, 206, 137);
    private static final Color TEXT_DARK = Color.BLACK;
    private static final Color TEXT_MUTED = Color.BLACK;
    private static final Color BORDER = new Color(194, 206, 218);
    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 20);

    static class TouristSpot {
        String name;
        double lat, lon;
        int entryFee;
        int openHour, closeHour;
        String[] tags;
        int visitDurationHr;

        TouristSpot(String name, double lat, double lon, int fee,
                    int open, int close, int dur, String... tags) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.entryFee = fee;
            this.openHour = open;
            this.closeHour = close;
            this.visitDurationHr = dur;
            this.tags = tags;
        }

        boolean hasTag(String tag) {
            for (String t : tags) if (t.equalsIgnoreCase(tag)) return true;
            return false;
        }

        double distanceTo(TouristSpot other) {
            double dx = this.lat - other.lat;
            double dy = this.lon - other.lon;
            return Math.sqrt(dx * dx + dy * dy);
        }
    }

    static final TouristSpot[] ALL_SPOTS = {
        new TouristSpot("Pashupatinath Temple", 27.7104, 85.3488, 100, 6, 18, 2, "culture", "religious"),
        new TouristSpot("Swayambhunath Stupa", 27.7149, 85.2906, 200, 7, 17, 2, "culture", "heritage"),
        new TouristSpot("Garden of Dreams", 27.7125, 85.3170, 150, 9, 21, 1, "nature", "relaxation"),
        new TouristSpot("Chandragiri Hills", 27.6616, 85.2458, 700, 9, 17, 3, "nature", "adventure"),
        new TouristSpot("Kathmandu Durbar Sq.", 27.7048, 85.3076, 100, 10, 17, 2, "culture", "heritage"),
        new TouristSpot("Boudhanath Stupa", 27.7215, 85.3620, 400, 6, 20, 1, "culture", "religious"),
        new TouristSpot("Nagarkot Viewpoint", 27.7150, 85.5200, 0, 6, 18, 2, "nature", "adventure"),
        new TouristSpot("Patan Durbar Square", 27.6710, 85.3243, 250, 10, 17, 2, "culture", "heritage"),
    };

    private JTextField budgetField, timeField;
    private JCheckBox[] tagBoxes;
    private JTextArea resultArea;
    private JPanel mapPanel;
    private JTable comparisonTable;
    private DefaultTableModel tableModel;
    private final String[] availableTags = {"culture", "nature", "adventure", "religious", "heritage", "relaxation"};

    @SuppressWarnings("this-escape")
    public TouristOptimizer() {
        super("Tourist Spot Optimizer - Nepal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        buildUI();
    }

    // Builds the full GUI for input, map view, and comparison view.
    private void buildUI() {
        setLayout(new BorderLayout(14, 14));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel("Tourist Spot Optimizer");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_DARK);
        JLabel subtitle = new JLabel("Heuristic itinerary planner for Kathmandu Valley");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_DARK);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(PANEL_BG);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(8, 8, 8, 8),
            "Your Preferences"
        );
        titledBorder.setTitleColor(TEXT_DARK);
        titledBorder.setTitleFont(UI_FONT_BOLD);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            titledBorder
        ));
        inputPanel.setPreferredSize(new Dimension(290, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        inputPanel.add(styledLabel("Max Budget (NPR):"), gbc);
        gbc.gridy++;
        budgetField = new JTextField("2000", 12);
        styleField(budgetField);
        inputPanel.add(budgetField, gbc);

        gbc.gridy++;
        inputPanel.add(styledLabel("Available Time (hours):"), gbc);
        gbc.gridy++;
        timeField = new JTextField("6", 12);
        styleField(timeField);
        inputPanel.add(timeField, gbc);

        gbc.gridy++;
        inputPanel.add(styledLabel("Interest Tags:"), gbc);
        tagBoxes = new JCheckBox[availableTags.length];
        for (int i = 0; i < availableTags.length; i++) {
            gbc.gridy++;
            tagBoxes[i] = new JCheckBox(availableTags[i]);
            tagBoxes[i].setFont(UI_FONT);
            tagBoxes[i].setBackground(PANEL_BG);
            tagBoxes[i].setForeground(TEXT_DARK);
            inputPanel.add(tagBoxes[i], gbc);
        }

        gbc.gridy++;
        JButton planBtn = makeButton("Plan My Trip (Greedy)", PRIMARY, TEXT_DARK);
        planBtn.addActionListener(e -> runPlanner());
        inputPanel.add(planBtn, gbc);

        gbc.gridy++;
        JButton bruteBtn = makeButton("Compare Brute-Force", SECONDARY, TEXT_DARK);
        bruteBtn.addActionListener(e -> runBruteForce());
        inputPanel.add(bruteBtn, gbc);

        JPanel westWrap = new JPanel(new BorderLayout());
        westWrap.setBackground(BG);
        westWrap.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 0));
        westWrap.add(inputPanel, BorderLayout.CENTER);
        add(westWrap, BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI_FONT_BOLD);
        tabs.setBackground(PANEL_BG);
        tabs.setForeground(TEXT_DARK);

        resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(250, 253, 255));
        resultArea.setForeground(TEXT_DARK);
        resultArea.setMargin(new Insets(10, 12, 10, 12));
        JScrollPane itineraryScroll = new JScrollPane(resultArea);
        itineraryScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tabs.addTab("Itinerary", itineraryScroll);

        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMap(g);
            }
        };
        mapPanel.setBackground(new Color(245, 250, 255));
        mapPanel.setBorder(BorderFactory.createLineBorder(BORDER));
        tabs.addTab("Map View", mapPanel);

        String[] cols = {"Method", "Spots Visited", "Total Cost (NPR)", "Total Time (hr)"};
        tableModel = new DefaultTableModel(cols, 0);
        comparisonTable = new JTable(tableModel);
        comparisonTable.setFont(UI_FONT);
        comparisonTable.setForeground(TEXT_DARK);
        comparisonTable.setRowHeight(26);
        comparisonTable.setGridColor(new Color(227, 233, 239));
        comparisonTable.setShowVerticalLines(false);
        comparisonTable.getTableHeader().setFont(UI_FONT_BOLD);
        comparisonTable.getTableHeader().setBackground(new Color(255, 236, 198));
        comparisonTable.getTableHeader().setForeground(TEXT_DARK);
        JScrollPane tableScroll = new JScrollPane(comparisonTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tabs.addTab("Comparison", tableScroll);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG);
        centerWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 12));
        centerWrap.add(tabs, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);
    }

    // Creates a consistent label style for input form fields.
    private JLabel styledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UI_FONT_BOLD);
        label.setForeground(TEXT_DARK);
        return label;
    }

    // Applies consistent styling to text fields.
    private void styleField(JTextField field) {
        field.setFont(UI_FONT);
        field.setForeground(TEXT_DARK);
        field.setBackground(new Color(252, 254, 255));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    // Creates a styled button used for planner actions.
    private JButton makeButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT_BOLD);
        button.setForeground(fg);
        button.setBackground(bg);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(145, 145, 145)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private transient List<TouristSpot> lastGreedy = new ArrayList<>();
    private transient List<TouristSpot> lastBrute = new ArrayList<>();

    // Selects spots greedily by score while respecting budget/time/open-hours.
    private List<TouristSpot> greedyPlan(int budget, int timeHr, Set<String> interests) {
        List<TouristSpot> selected = new ArrayList<>();
        int spent = 0, timeUsed = 0;

        List<TouristSpot> candidates = new ArrayList<>(Arrays.asList(ALL_SPOTS));
        candidates.sort((a, b) -> {
            int scoreA = interestScore(a, interests) * 10 - a.entryFee / 100;
            int scoreB = interestScore(b, interests) * 10 - b.entryFee / 100;
            return scoreB - scoreA;
        });

        int currentHour = 9;
        for (TouristSpot spot : candidates) {
            if (spent + spot.entryFee > budget) continue;
            if (timeUsed + spot.visitDurationHr > timeHr) continue;
            if (currentHour < spot.openHour || currentHour + spot.visitDurationHr > spot.closeHour) continue;
            selected.add(spot);
            spent += spot.entryFee;
            timeUsed += spot.visitDurationHr;
            currentHour += spot.visitDurationHr;
        }
        return selected;
    }

    // Counts tag matches between a spot and user interests.
    private int interestScore(TouristSpot spot, Set<String> interests) {
        int score = 0;
        for (String tag : interests) if (spot.hasTag(tag)) score++;
        return score;
    }

    // Uses subset brute force on a small spot set for baseline comparison.
    private List<TouristSpot> bruteForcePlan(int budget, int timeHr, Set<String> interests) {
        TouristSpot[] subset = Arrays.copyOf(ALL_SPOTS, Math.min(5, ALL_SPOTS.length));
        int n = subset.length;

        List<TouristSpot> bestPlan = new ArrayList<>();
        int bestScore = -1;

        for (int mask = 0; mask < (1 << n); mask++) {
            List<TouristSpot> plan = new ArrayList<>();
            int cost = 0, time = 0, score = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    plan.add(subset[i]);
                    cost += subset[i].entryFee;
                    time += subset[i].visitDurationHr;
                    score += interestScore(subset[i], interests);
                }
            }
            if (cost <= budget && time <= timeHr && score > bestScore) {
                bestScore = score;
                bestPlan = new ArrayList<>(plan);
            }
        }
        return bestPlan;
    }

    // Reads input and runs only the greedy planner.
    private void runPlanner() {
        try {
            int budget = Integer.parseInt(budgetField.getText().trim());
            int timeHr = Integer.parseInt(timeField.getText().trim());
            Set<String> interests = new HashSet<>();
            for (JCheckBox cb : tagBoxes) if (cb.isSelected()) interests.add(cb.getText());

            lastGreedy = greedyPlan(budget, timeHr, interests);
            displayItinerary(lastGreedy, budget, timeHr, interests, "Greedy");
            mapPanel.repaint();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for budget and time.",
                "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Runs greedy and brute-force and prints their comparison.
    private void runBruteForce() {
        try {
            int budget = Integer.parseInt(budgetField.getText().trim());
            int timeHr = Integer.parseInt(timeField.getText().trim());
            Set<String> interests = new HashSet<>();
            for (JCheckBox cb : tagBoxes) if (cb.isSelected()) interests.add(cb.getText());

            lastGreedy = greedyPlan(budget, timeHr, interests);
            lastBrute = bruteForcePlan(budget, timeHr, interests);

            tableModel.setRowCount(0);
            tableModel.addRow(buildCompRow("Greedy", lastGreedy));
            tableModel.addRow(buildCompRow("Brute-Force", lastBrute));

            StringBuilder sb = new StringBuilder();
            sb.append("==========================================\n");
            sb.append("  BRUTE-FORCE vs GREEDY COMPARISON\n");
            sb.append("==========================================\n\n");
            sb.append(String.format("Greedy  -> %d spots, Cost: %d NPR, Time: %dh, Path dist: %.2f%n",
                lastGreedy.size(), totalCost(lastGreedy), totalTime(lastGreedy), pathDistance(lastGreedy)));
            sb.append(String.format("Brute   -> %d spots, Cost: %d NPR, Time: %dh, Path dist: %.2f%n%n",
                lastBrute.size(), totalCost(lastBrute), totalTime(lastBrute), pathDistance(lastBrute)));
            sb.append("Discussion:\n");
            sb.append("- Greedy is O(n log n): fast even for large spot sets.\n");
            sb.append("- Brute-force is O(2^n): only feasible for small n.\n");
            sb.append("- Greedy may miss globally optimal combinations.\n");
            sb.append("- Brute-force guarantees optimal within the small subset.\n");
            resultArea.setText(sb.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid numbers first.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Builds one row for the comparison table.
    private Object[] buildCompRow(String method, List<TouristSpot> plan) {
        return new Object[]{method, plan.size(), totalCost(plan), totalTime(plan)};
    }

    // Returns total entry fee of all spots in the plan.
    private int totalCost(List<TouristSpot> plan) {
        return plan.stream().mapToInt(s -> s.entryFee).sum();
    }

    // Returns total visit hours for the plan.
    private int totalTime(List<TouristSpot> plan) {
        return plan.stream().mapToInt(s -> s.visitDurationHr).sum();
    }

    /** Total straight-line distance along the path (uses distanceTo between consecutive spots). */
    private double pathDistance(List<TouristSpot> plan) {
        double d = 0;
        for (int i = 1; i < plan.size(); i++) d += plan.get(i - 1).distanceTo(plan.get(i));
        return d;
    }

    // Prints the itinerary details and greedy decision logic.
    private void displayItinerary(List<TouristSpot> plan, int budget, int time,
                                  Set<String> interests, String method) {
        StringBuilder sb = new StringBuilder();
        sb.append("======================================================\n");
        sb.append("  OPTIMIZED ITINERARY  [").append(method).append("]\n");
        sb.append("======================================================\n");
        sb.append(String.format("  Budget: %d NPR | Time: %d hrs | Tags: %s%n%n",
            budget, time, interests.isEmpty() ? "Any" : interests));

        if (plan.isEmpty()) {
            sb.append("  No spots match your constraints.\n");
            sb.append("  Try increasing budget or time, or unchecking some tags.\n");
        } else {
            int hourCursor = 9;
            int spent = 0;
            sb.append(String.format("  %-3s %-28s %-8s %-8s %-8s%n",
                "#", "Spot", "Arrive", "Leave", "Cost"));
            sb.append("  ").append("-".repeat(60)).append("\n");
            int idx = 1;
            for (TouristSpot s : plan) {
                sb.append(String.format("  %-3d %-28s %02d:00  -> %02d:00   %d NPR%n",
                    idx++, s.name, hourCursor, hourCursor + s.visitDurationHr, s.entryFee));
                hourCursor += s.visitDurationHr;
                spent += s.entryFee;
            }
            sb.append("  ").append("-".repeat(60)).append("\n");
            sb.append(String.format("  Total: %d spots | %d NPR spent | %d hrs used%n",
                plan.size(), spent, totalTime(plan)));
            sb.append("\n  Decision Justification (Greedy scoring):\n");
            sb.append("  Score = (interest tag matches x 10) - (fee / 100)\n");
            sb.append("  Spots selected by score, skipping spots violating\n");
            sb.append("  budget, time, or opening-hour constraints.\n");
        }
        resultArea.setText(sb.toString());
    }

    // Draws the map background, all spots, and selected route.
    private void drawMap(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = mapPanel.getWidth(), h = mapPanel.getHeight();
        if (w < 10 || h < 10) return;

        GradientPaint gradient = new GradientPaint(0, 0, new Color(241, 248, 255), 0, h, new Color(252, 255, 255));
        g2.setPaint(gradient);
        g2.fillRect(0, 0, w, h);

        double minLat = 27.65, maxLat = 27.73;
        double minLon = 85.22, maxLon = 85.57;
        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;
        int pad = 50;

        for (TouristSpot s : ALL_SPOTS) {
            int x = pad + (int) ((s.lon - minLon) / lonRange * (w - 2 * pad));
            int y = h - pad - (int) ((s.lat - minLat) / latRange * (h - 2 * pad));
            g2.setColor(new Color(196, 206, 217));
            g2.fillOval(x - 5, y - 5, 10, 10);
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(s.name.substring(0, Math.min(14, s.name.length())), x + 7, y + 4);
        }

        drawPath(g2, lastGreedy, PRIMARY, minLat, latRange, minLon, lonRange, w, h, pad);

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(TEXT_DARK);
        g2.drawString("Greedy Path", 20, 20);
    }

    // Draws route lines and step numbers for the selected path.
    private void drawPath(Graphics2D g2, List<TouristSpot> plan, Color c,
                          double minLat, double latR, double minLon, double lonR,
                          int w, int h, int pad) {
        if (plan.isEmpty()) return;
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2.5f));
        int[] xs = new int[plan.size()], ys = new int[plan.size()];
        for (int i = 0; i < plan.size(); i++) {
            xs[i] = pad + (int) ((plan.get(i).lon - minLon) / lonR * (w - 2 * pad));
            ys[i] = h - pad - (int) ((plan.get(i).lat - minLat) / latR * (h - 2 * pad));
        }
        for (int i = 0; i < plan.size() - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }
        for (int i = 0; i < plan.size(); i++) {
            g2.fillOval(xs[i] - 7, ys[i] - 7, 14, 14);
            g2.setColor(TEXT_DARK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString(String.valueOf(i + 1), xs[i] - 3, ys[i] + 4);
            g2.setColor(c);
        }
    }

    // Starts the desktop app on the Swing event thread.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException
                    | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ignored) {
            }
            new TouristOptimizer().setVisible(true);
        });
    }
}
