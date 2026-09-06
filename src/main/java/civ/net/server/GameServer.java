package civ.net.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {

    private final int port;
    private final ServerSession session;
    private final ClientRegistry clients = new ClientRegistry();
    private final HeartbeatServer heartbeatServer;

    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public GameServer(int port) {
        this.port = port;
        this.session = new ServerSession(clients);
        this.heartbeatServer = new HeartbeatServer(port, session);
        this.session.setHeartbeatServer(heartbeatServer);
    }

    public int getPort() {
        return port;
    }

    public ServerSession getSession() {
        return session;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        Thread acceptor = new Thread(this::acceptLoop, "accept-loop");
        acceptor.setDaemon(true);
        acceptor.start();

        heartbeatServer.start();

        System.out.println("Server listening on port " + port + " (TCP+UDP)");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, session, clients);
                handler.start();
            } catch (IOException ex) {
                if (running) {
                    System.err.println("Accept failed: " + ex.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        heartbeatServer.stop();
        clients.closeAll();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
