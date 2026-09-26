package civ.net.ws;

import civ.net.server.ServerSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Separate TCP port for browser chat (game TCP stays untouched).
 * Hand-rolled RFC 6455 — no Netty / Java-WebSocket.
 */
public class WebSocketChatServer {

    private final int port;
    private final ServerSession session;
    private final Set<WebSocketConnection> connections = ConcurrentHashMap.newKeySet();

    private ServerSocket serverSocket;
    private volatile boolean running;

    public WebSocketChatServer(int port, ServerSession session) {
        this.port = port;
        this.session = session;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptor = new Thread(this::acceptLoop, "ws-accept");
        acceptor.setDaemon(true);
        acceptor.start();
        System.out.println("WebSocket chat on ws://localhost:" + port + "/chat"
                + " (open http://localhost:" + port + "/ for the browser client)");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                handleNewSocket(socket);
            } catch (IOException ex) {
                if (running) {
                    System.err.println("WebSocket accept failed: " + ex.getMessage());
                }
            }
        }
    }

    private void handleNewSocket(Socket socket) {
        Thread worker = new Thread(() -> {
            try {
                WebSocketHandshake.Request request = WebSocketHandshake.readRequest(socket);
                if (request.wantsUpgrade() && pathIsChat(request.path)) {
                    if (WebSocketHandshake.accept(socket, request)) {
                        WebSocketConnection[] holder = new WebSocketConnection[1];
                        holder[0] = new WebSocketConnection(
                                socket,
                                (conn, text) -> session.publishBrowserChat(text),
                                () -> connections.remove(holder[0]));
                        connections.add(holder[0]);
                        holder[0].start();
                        return;
                    }
                }
                if ("GET".equalsIgnoreCase(request.method)) {
                    serveHtml(socket, request.path);
                } else {
                    WebSocketHandshake.writeHttp(socket, 404, "text/plain; charset=utf-8",
                            "Not found".getBytes(StandardCharsets.UTF_8));
                }
                socket.close();
            } catch (Exception ex) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }, "ws-handshake");
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean pathIsChat(String path) {
        if (path == null) {
            return false;
        }
        int q = path.indexOf('?');
        String bare = q >= 0 ? path.substring(0, q) : path;
        return "/chat".equals(bare);
    }

    private void serveHtml(Socket socket, String path) throws IOException {
        String bare = path == null ? "/" : path;
        int q = bare.indexOf('?');
        if (q >= 0) {
            bare = bare.substring(0, q);
        }
        if ("/".equals(bare) || "/chat.html".equals(bare) || "/index.html".equals(bare)) {
            byte[] body = loadChatHtml();
            WebSocketHandshake.writeHttp(socket, 200, "text/html; charset=utf-8", body);
        } else {
            WebSocketHandshake.writeHttp(socket, 404, "text/plain; charset=utf-8",
                    "Not found".getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] loadChatHtml() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/web/chat.html")) {
            if (in == null) {
                return ("<!DOCTYPE html><html><body><p>chat.html missing from resources."
                        + "</p></body></html>").getBytes(StandardCharsets.UTF_8);
            }
            return in.readAllBytes();
        }
    }

    /** Push an already-formatted chat line to every connected browser. */
    public void broadcastLine(String line) {
        for (WebSocketConnection connection : connections) {
            connection.sendText(line);
        }
    }

    public void stop() {
        running = false;
        for (WebSocketConnection connection : connections) {
            connection.closeQuietly();
        }
        connections.clear();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
