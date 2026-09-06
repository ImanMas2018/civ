package civ.net.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HeartbeatServer {

    private static final long TIMEOUT_MS = 10_000;

    private final int port;
    private final ServerSession session;
    private final Map<Long, Long> lastSeen = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    private DatagramSocket socket;

    public HeartbeatServer(int port, ServerSession session) {
        this.port = port;
        this.session = session;
    }

    public void start() {
        Thread receive = new Thread(this::receiveLoop, "heartbeat-receive");
        receive.setDaemon(true);
        receive.start();
        Thread sweep = new Thread(this::sweepLoop, "heartbeat-sweep");
        sweep.setDaemon(true);
        sweep.start();
    }

    public void note(long playerId) {
        if (playerId > 0) {
            lastSeen.put(playerId, System.currentTimeMillis());
        }
    }

    public void forget(long playerId) {
        lastSeen.remove(playerId);
    }

    private void receiveLoop() {
        try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
            this.socket = datagramSocket;
            byte[] buffer = new byte[256];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);

                String text = new String(packet.getData(), 0, packet.getLength(),
                        StandardCharsets.UTF_8);
                long playerId = parsePlayerId(text);
                if (playerId > 0) {
                    lastSeen.put(playerId, System.currentTimeMillis());
                }
            }
        } catch (IOException ex) {
            if (running) {
                System.err.println("Heartbeat socket closed: " + ex.getMessage());
            }
        }
    }

    private void sweepLoop() {
        while (running) {
            try {
                Thread.sleep(3000);
                long now = System.currentTimeMillis();
                for (Map.Entry<Long, Long> entry : lastSeen.entrySet()) {
                    if (now - entry.getValue() > TIMEOUT_MS) {
                        Long playerId = entry.getKey();
                        lastSeen.remove(playerId);
                        session.markSilent(playerId);
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static long parsePlayerId(String text) {
        int key = text.indexOf("\"playerId\"");
        if (key < 0) {
            return -1;
        }
        int colon = text.indexOf(':', key);
        if (colon < 0) {
            return -1;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = colon + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return -1;
        }
        try {
            return Long.parseLong(digits.toString());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}
