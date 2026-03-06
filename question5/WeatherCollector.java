package question5;

import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Q5(b): Multi-threaded Weather Data Collector
 * API: OpenWeatherMap (single free API)
 */
public class WeatherCollector extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String[] CITIES = {
        "Kathmandu", "Pokhara", "Biratnagar", "Nepalgunj", "Dhangadhi"
    };

    private static final Color BG = new Color(240, 245, 251);
    private static final Color PANEL_BG = Color.WHITE;
    private static final Color PRIMARY = new Color(255, 197, 86);
    private static final Color PRIMARY_DARK = new Color(228, 237, 247);
    private static final Color TEXT_DARK = Color.BLACK;
    private static final Color BORDER = new Color(194, 206, 218);

    private transient final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private transient final Map<String, Integer> cityRowMap = new ConcurrentHashMap<>();

    private JTextField apiKeyField;
    private JButton fetchButton;
    private JLabel statusLabel;
    private JLabel sequentialTimeLabel;
    private JLabel parallelTimeLabel;

    private JTable weatherTable;
    private DefaultTableModel tableModel;
    private LatencyChartPanel chartPanel;

    static class WeatherResult {
        String city;
        Double tempC;
        Integer humidity;
        Integer pressure;
        String condition;
        String status;

        WeatherResult(String city, Double tempC, Integer humidity, Integer pressure, String condition, String status) {
            this.city = city;
            this.tempC = tempC;
            this.humidity = humidity;
            this.pressure = pressure;
            this.condition = condition;
            this.status = status;
        }
    }

    @SuppressWarnings("this-escape")
    public WeatherCollector() {
        super("Multi-threaded Weather Data Collector");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        buildUI();
    }

    // Builds the weather dashboard UI and table/chart sections.
    private void buildUI() {
        setLayout(new BorderLayout(12, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel("Nepal Weather Dashboard (Sequential vs Parallel)");
        title.setForeground(TEXT_DARK);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel subtitle = new JLabel("OpenWeatherMap API - 5 Nepal cities - thread-safe Swing updates");
        subtitle.setForeground(TEXT_DARK);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBackground(PANEL_BG);
        controls.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel keyLabel = new JLabel("OpenWeatherMap API Key:");
        keyLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        keyLabel.setForeground(TEXT_DARK);
        controls.add(keyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        apiKeyField = new JTextField(32);
        apiKeyField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        apiKeyField.setForeground(TEXT_DARK);
        apiKeyField.setBackground(new Color(252, 254, 255));
        apiKeyField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
        controls.add(apiKeyField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        fetchButton = new JButton("Fetch Weather");
        fetchButton.setBackground(PRIMARY);
        fetchButton.setForeground(TEXT_DARK);
        fetchButton.setFocusPainted(false);
        fetchButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        fetchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fetchButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(191, 141, 56)),
            BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        fetchButton.addActionListener(e -> startFetchWorkflow());
        controls.add(fetchButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT_DARK);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        controls.add(statusLabel, gbc);

        center.add(controls, BorderLayout.NORTH);

        String[] columns = {"City", "Temp (°C)", "Humidity (%)", "Pressure (hPa)", "Condition", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        weatherTable = new JTable(tableModel);
        weatherTable.setRowHeight(26);
        weatherTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        weatherTable.setForeground(TEXT_DARK);
        weatherTable.setBackground(Color.WHITE);
        weatherTable.setSelectionForeground(TEXT_DARK);
        weatherTable.setSelectionBackground(new Color(255, 244, 219));
        weatherTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        weatherTable.getTableHeader().setBackground(new Color(255, 236, 198));
        weatherTable.getTableHeader().setForeground(TEXT_DARK);
        weatherTable.setGridColor(new Color(219, 227, 236));
        weatherTable.setShowVerticalLines(false);

        JScrollPane tableScroll = new JScrollPane(weatherTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        center.add(tableScroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.setBackground(BG);

        JPanel timings = new JPanel(new GridLayout(1, 2, 10, 0));
        timings.setBackground(BG);
        sequentialTimeLabel = makeMetricLabel("Sequential: - ms");
        parallelTimeLabel = makeMetricLabel("Parallel: - ms");
        timings.add(sequentialTimeLabel);
        timings.add(parallelTimeLabel);

        chartPanel = new LatencyChartPanel();
        chartPanel.setPreferredSize(new Dimension(0, 210));
        chartPanel.setBorder(BorderFactory.createLineBorder(BORDER));
        chartPanel.setBackground(PANEL_BG);

        south.add(timings, BorderLayout.NORTH);
        south.add(chartPanel, BorderLayout.CENTER);
        center.add(south, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        resetTable();
    }

    // Creates a styled label used for latency metrics.
    private JLabel makeMetricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(PANEL_BG);
        label.setForeground(TEXT_DARK);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return label;
    }

    // Resets table rows to waiting state for all tracked cities.
    private void resetTable() {
        tableModel.setRowCount(0);
        cityRowMap.clear();
        for (int i = 0; i < CITIES.length; i++) {
            tableModel.addRow(new Object[]{CITIES[i], "-", "-", "-", "-", "Waiting"});
            cityRowMap.put(CITIES[i], i);
        }
    }

    // Validates input and runs sequential + parallel fetch workflow.
    private void startFetchWorkflow() {
        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter your OpenWeatherMap API key.",
                "Missing API Key",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        fetchButton.setEnabled(false);
        statusLabel.setText("Fetching sequentially...");
        sequentialTimeLabel.setText("Sequential: running...");
        parallelTimeLabel.setText("Parallel: waiting...");
        chartPanel.setTimes(0, 0);
        resetTable();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            long sequentialMs;
            long parallelMs;

            @Override
            protected Void doInBackground() {
                sequentialMs = runSequential(apiKey);
                SwingUtilities.invokeLater(() -> {
                    sequentialTimeLabel.setText("Sequential: " + sequentialMs + " ms");
                    statusLabel.setText("Fetching in parallel with 5 threads...");
                });
                parallelMs = runParallel(apiKey);
                return null;
            }

            @Override
            protected void done() {
                double speedup = parallelMs > 0 ? (double) sequentialMs / parallelMs : 1.0;
                sequentialTimeLabel.setText("Sequential: " + sequentialMs + " ms");
                parallelTimeLabel.setText(String.format("Parallel: %d ms (%.2fx faster)", parallelMs, speedup));
                chartPanel.setTimes(sequentialMs, parallelMs);
                statusLabel.setText("Done. Table shows parallel results.");
                fetchButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    // Measures one-by-one fetching latency for all cities.
    private long runSequential(String apiKey) {
        long start = System.nanoTime();
        for (String city : CITIES) {
            fetchWeatherForCity(city, apiKey);
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    // Fetches all cities concurrently using one thread per city.
    private long runParallel(String apiKey) {
        long start = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(CITIES.length);
        CountDownLatch latch = new CountDownLatch(CITIES.length);
        ConcurrentHashMap<String, WeatherResult> parallelResults = new ConcurrentHashMap<>();

        for (String city : CITIES) {
            pool.submit(() -> {
                try {
                    WeatherResult result = fetchWeatherForCity(city, apiKey);
                    parallelResults.put(city, result);
                    SwingUtilities.invokeLater(() -> updateRow(result));
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
        }

        return (System.nanoTime() - start) / 1_000_000;
    }

    // Calls OpenWeatherMap for one city and parses core weather values.
    private WeatherResult fetchWeatherForCity(String city, String apiKey) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + encodedCity + "&units=metric&appid=" + encodedKey;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new WeatherResult(city, null, null, null, "-", "HTTP " + response.statusCode());
            }

            String json = response.body();
            Double temp = extractDouble(json, "\\\"temp\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
            Double humidityRaw = extractDouble(json, "\\\"humidity\\\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
            Double pressureRaw = extractDouble(json, "\\\"pressure\\\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
            String condition = extractString(json, "\\\"description\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

            if (temp == null || humidityRaw == null || pressureRaw == null) {
                return new WeatherResult(city, null, null, null, "-", "Parse error");
            }

            return new WeatherResult(
                city,
                temp,
                (int) Math.round(humidityRaw),
                (int) Math.round(pressureRaw),
                condition == null ? "-" : condition,
                "OK"
            );
        } catch (Exception e) {
            return new WeatherResult(city, null, null, null, "-", "Error: " + e.getClass().getSimpleName());
        }
    }

    // Extracts one numeric field from JSON using regex.
    private Double extractDouble(String input, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        if (!matcher.find()) return null;
        return Double.parseDouble(matcher.group(1));
    }

    // Extracts one string field from JSON using regex.
    private String extractString(String input, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    // Updates exactly one table row safely on the Swing thread.
    private void updateRow(WeatherResult result) {
        Integer row = cityRowMap.get(result.city);
        if (row == null) return;

        tableModel.setValueAt(result.tempC == null ? "-" : String.format("%.1f", result.tempC), row, 1);
        tableModel.setValueAt(result.humidity == null ? "-" : result.humidity, row, 2);
        tableModel.setValueAt(result.pressure == null ? "-" : result.pressure, row, 3);
        tableModel.setValueAt(result.condition, row, 4);
        tableModel.setValueAt(result.status, row, 5);
    }

    static class LatencyChartPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private long sequentialMs;
        private long parallelMs;

        // Updates latency values and redraws the chart bars.
        void setTimes(long sequentialMs, long parallelMs) {
            this.sequentialMs = sequentialMs;
            this.parallelMs = parallelMs;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int left = 60;
            int bottom = h - 35;
            int chartH = h - 65;
            int barW = 150;

            g2.setColor(new Color(246, 250, 255));
            g2.fillRect(0, 0, w, h);

            g2.setColor(TEXT_DARK);
            g2.drawLine(left, 20, left, bottom);
            g2.drawLine(left, bottom, w - 30, bottom);

            long max = Math.max(1, Math.max(sequentialMs, parallelMs));
            int seqX = left + 70;
            int parX = seqX + barW + 120;
            int seqBarH = (int) ((sequentialMs * 1.0 / max) * chartH);
            int parBarH = (int) ((parallelMs * 1.0 / max) * chartH);

            g2.setColor(new Color(255, 197, 86));
            g2.fillRoundRect(seqX, bottom - seqBarH, barW, seqBarH, 12, 12);
            g2.setColor(new Color(143, 206, 137));
            g2.fillRoundRect(parX, bottom - parBarH, barW, parBarH, 12, 12);

            g2.setColor(TEXT_DARK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("Sequential", seqX + 36, bottom + 18);
            g2.drawString("Parallel", parX + 47, bottom + 18);
            g2.drawString(sequentialMs + " ms", seqX + 45, bottom - seqBarH - 8);
            g2.drawString(parallelMs + " ms", parX + 53, bottom - parBarH - 8);
        }
    }

    // Starts the weather app on the Swing event dispatch thread.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new WeatherCollector().setVisible(true);
        });
    }
}
