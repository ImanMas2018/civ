package civ.net.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class HeartbeatClient {

    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    private final int serverPort;
    private final long playerId;
    private volatile boolean running = true;

    public HeartbeatClient(String host, int port, long playerId) throws IOException {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
        this.playerId = playerId;
    }

    public void start() {
        Thread thread = new Thread(this::loop, "heartbeat-client");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        while (running) {
            try {
                byte[] payload = ("{\"type\":\"ping\",\"playerId\":" + playerId + "}")
                        .getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(payload, payload.length,
                        serverAddress, serverPort));
                Thread.sleep(2000);
            } catch (IOException ex) {
                // A dropped datagram is expected and harmless. Keep going.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void stop() {
        running = false;
        socket.close();
    }
}
