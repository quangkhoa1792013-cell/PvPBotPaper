package com.pvpbot.web;

import com.pvpbot.stats.StatsDatabase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.bukkit.Bukkit;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardWebServer {

    private HttpServer server;
    private final List<SSEClient> sseClients = new CopyOnWriteArrayList<>();
    private final AtomicInteger activeBots = new AtomicInteger(0);
    private final AtomicInteger onlinePlayers = new AtomicInteger(0);
    private volatile double currentTps = 20.0;
    private String dashboardHtml;
    private ScheduledExecutorService keepaliveScheduler;

    public void start(int port) {
        try {
            keepaliveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pvpbot-sse-keepalive");
                t.setDaemon(true);
                return t;
            });
            keepaliveScheduler.scheduleAtFixedRate(this::broadcastKeepalive, 15, 15, TimeUnit.SECONDS);

            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", this::handleRoot);
            server.createContext("/api/stats", this::handleStats);
            server.createContext("/api/history", this::handleHistory);
            server.createContext("/api/stream", this::handleStream);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            org.bukkit.Bukkit.getLogger().info("[PvPBot Dashboard] Web server started on port " + port);
        } catch (IOException e) {
            org.bukkit.Bukkit.getLogger().warning("[PvPBot Dashboard] Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (keepaliveScheduler != null) {
            keepaliveScheduler.shutdown();
            try { keepaliveScheduler.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        if (server != null) server.stop(1);
        for (SSEClient client : sseClients) {
            try { client.close(); } catch (Exception ignored) {}
        }
        sseClients.clear();
    }

    public void updateStats(int bots, int players, double tps) {
        activeBots.set(bots);
        onlinePlayers.set(players);
        currentTps = tps;
    }

    public void broadcast(String eventType, String data) {
        byte[] message = ("event: " + eventType + "\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8);
        for (SSEClient client : sseClients) {
            if (!client.send(message)) {
                sseClients.remove(client);
                try { client.close(); } catch (Exception ignored) {}
            }
        }
    }

    public void broadcastSnapshot() {
        StatsDatabase db = StatsDatabase.getInstance();
        String json = "{\"activeBots\":" + activeBots.get()
                + ",\"onlinePlayers\":" + onlinePlayers.get()
                + ",\"tps\":" + String.format("%.1f", currentTps)
                + ",\"totalSpawned\":" + db.getTotalBotsSpawned()
                + ",\"maxConcurrent\":" + db.getMaxConcurrentBots() + "}";
        broadcast("snapshot", json);
    }

    // --- HTTP Handlers ---

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (dashboardHtml == null) {
            dashboardHtml = buildDashboardHtml();
        }
        byte[] bytes = dashboardHtml.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        StatsDatabase db = StatsDatabase.getInstance();
        String json = "{\"activeBots\":" + activeBots.get()
                + ",\"onlinePlayers\":" + onlinePlayers.get()
                + ",\"tps\":" + String.format("%.1f", currentTps)
                + ",\"totalSpawned\":" + db.getTotalBotsSpawned()
                + ",\"maxConcurrent\":" + db.getMaxConcurrentBots()
                + ",\"topNames\":" + toJsonArray(db.getTopNames(5)) + "}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void handleHistory(HttpExchange exchange) throws IOException {
        long since = Instant.now().minusSeconds(86400 * 7).getEpochSecond();
        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && kv[0].equals("since")) {
                    try { since = Long.parseLong(URLDecoder.decode(kv[1], "UTF-8")) / 1000; }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        StatsDatabase db = StatsDatabase.getInstance();
        String json = "{\"points\":" + toJsonArray(db.getHistory(since)) + "}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void broadcastKeepalive() {
        byte[] ping = ": keepalive\n\n".getBytes(StandardCharsets.UTF_8);
        for (SSEClient client : sseClients) {
            if (!client.send(ping)) {
                sseClients.remove(client);
                try { client.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void handleStream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        OutputStream out = exchange.getResponseBody();
        SSEClient client = new SSEClient(out);
        sseClients.add(client);

        try {
            client.await();
        } finally {
            sseClients.remove(client);
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    // --- JSON Helpers ---

    private String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map<String, Object> map : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            boolean f2 = true;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!f2) sb.append(",");
                f2 = false;
                sb.append("\"").append(e.getKey()).append("\":");
                Object v = e.getValue();
                if (v instanceof String s) {
                    sb.append("\"").append(s.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
                } else if (v instanceof Number) {
                    sb.append(v);
                } else {
                    sb.append("\"").append(v).append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    // --- HTML Dashboard ---

    private String buildDashboardHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PvPBot Dashboard</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Segoe UI', system-ui, sans-serif; background: #0d1117; color: #c9d1d9; min-height: 100vh; padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header h1 { font-size: 24px; color: #58a6ff; }
.status { color: #8b949e; font-size: 14px; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
.card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 20px; }
.card .label { font-size: 12px; text-transform: uppercase; color: #8b949e; margin-bottom: 4px; }
.card .value { font-size: 28px; font-weight: 700; color: #f0f6fc; }
.card .value.green { color: #3fb950; }
.card .value.blue { color: #58a6ff; }
.card .value.yellow { color: #d29922; }
.card .value.purple { color: #bc8cff; }
.chart-container { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 20px; margin-bottom: 24px; }
.chart-container h2 { font-size: 16px; margin-bottom: 12px; color: #f0f6fc; }
.chart-wrapper { position: relative; height: 300px; }
.top-names { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 20px; }
.top-names h2 { font-size: 16px; margin-bottom: 12px; color: #f0f6fc; }
.top-names table { width: 100%; border-collapse: collapse; }
.top-names th, .top-names td { padding: 8px 12px; text-align: left; border-bottom: 1px solid #21262d; }
.top-names th { color: #8b949e; font-size: 12px; text-transform: uppercase; }
.top-names td { font-size: 14px; }
.status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; }
.status-dot.online { background: #3fb950; }
.status-dot.offline { background: #f85149; }
.footer { text-align: center; color: #484f58; font-size: 12px; margin-top: 24px; }
</style>
</head>
<body>
<div class="header">
<h1>⚔ PvPBot Dashboard</h1>
<div class="status"><span class="status-dot online" id="statusDot"></span><span id="statusText">Live</span></div>
</div>

<div class="cards">
<div class="card"><div class="label">Active Bots</div><div class="value blue" id="activeBots">0</div></div>
<div class="card"><div class="label">Online Players</div><div class="value green" id="onlinePlayers">0</div></div>
<div class="card"><div class="label">TPS</div><div class="value yellow" id="tps">20.0</div></div>
<div class="card"><div class="label">Total Spawned</div><div class="value purple" id="totalSpawned">0</div></div>
<div class="card"><div class="label">Max Concurrent</div><div class="value" id="maxConcurrent">0</div></div>
</div>

<div class="chart-container">
<h2>Server Activity (7 days)</h2>
<div class="chart-wrapper">
<canvas id="activityChart"></canvas>
</div>
</div>

<div class="top-names">
<h2>Top Bot Names</h2>
<table><thead><tr><th>#</th><th>Name</th><th>Spawns</th></tr></thead><tbody id="nameTable"></tbody></table>
</div>

<div class="footer">PvPBot &mdash; Live Dashboard &mdash; Data updates in real-time via SSE</div>

<script>
let activityChart = null;

function initChart(points) {
  const ctx = document.getElementById('activityChart').getContext('2d');
  const labels = points.map(p => new Date(p.t).toLocaleString());
  const botsData = points.map(p => p.b);
  const playersData = points.map(p => p.p);

  if (activityChart) { activityChart.destroy(); }

  activityChart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [
        { label: 'Bots', data: botsData, borderColor: '#58a6ff', backgroundColor: 'rgba(88,166,255,0.1)', fill: true, tension: 0.3, pointRadius: 2 },
        { label: 'Players', data: playersData, borderColor: '#3fb950', backgroundColor: 'rgba(63,185,80,0.1)', fill: true, tension: 0.3, pointRadius: 2 }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { labels: { color: '#c9d1d9' } } },
      scales: {
        x: { ticks: { color: '#8b949e', maxTicksLimit: 10 }, grid: { color: '#21262d' } },
        y: { beginAtZero: true, ticks: { color: '#8b949e' }, grid: { color: '#21262d' } }
      }
    }
  });
}

function updateCards(stats) {
  document.getElementById('activeBots').textContent = stats.activeBots;
  document.getElementById('onlinePlayers').textContent = stats.onlinePlayers;
  document.getElementById('tps').textContent = stats.tps;
  document.getElementById('totalSpawned').textContent = stats.totalSpawned;
  document.getElementById('maxConcurrent').textContent = stats.maxConcurrent;
}

function updateNames(names) {
  const tbody = document.getElementById('nameTable');
  tbody.innerHTML = '';
  names.forEach((n, i) => {
    const tr = document.createElement('tr');
    tr.innerHTML = '<td>' + (i + 1) + '</td><td>' + n.name + '</td><td>' + n.count + '</td>';
    tbody.appendChild(tr);
  });
}

async function loadHistory() {
  try {
    const res = await fetch('/api/history');
    const data = await res.json();
    if (data.points && data.points.length > 0) initChart(data.points);
  } catch (e) { console.error('History load failed', e); }
}

async function loadStats() {
  try {
    const res = await fetch('/api/stats');
    const stats = await res.json();
    updateCards(stats);
    if (stats.topNames) updateNames(stats.topNames);
  } catch (e) { console.error('Stats load failed', e); }
}

const evtSource = new EventSource('/api/stream');
evtSource.addEventListener('snapshot', function(e) {
  try {
    const data = JSON.parse(e.data);
    updateCards(data);
  } catch (err) {}
});
evtSource.addEventListener('history', function(e) {
  try {
    const data = JSON.parse(e.data);
    if (data.points) initChart(data.points);
  } catch (err) {}
});
evtSource.onerror = function() {
  document.getElementById('statusText').textContent = 'Reconnecting...';
  document.getElementById('statusDot').className = 'status-dot offline';
};

loadStats();
loadHistory();
setInterval(loadStats, 30000);
</script>
</body>
</html>""";
    }

    private static class SSEClient {
        private final OutputStream out;
        private final CountDownLatch disconnectLatch = new CountDownLatch(1);

        SSEClient(OutputStream out) {
            this.out = out;
        }

        boolean send(byte[] data) {
            try {
                out.write(data);
                out.flush();
                return true;
            } catch (IOException e) {
                disconnectLatch.countDown();
                return false;
            }
        }

        void send(String message) {
            send(message.getBytes(StandardCharsets.UTF_8));
        }

        void await() {
            try {
                disconnectLatch.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        void close() throws IOException {
            disconnectLatch.countDown();
            out.close();
        }
    }
}
