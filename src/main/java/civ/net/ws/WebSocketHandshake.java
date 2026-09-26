package civ.net.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RFC 6455 opening handshake — no libraries.
 * Headers are read from the raw stream until {@code \r\n\r\n} so no BufferedReader
 * can steal bytes that belong to the first WebSocket frame.
 */
public final class WebSocketHandshake {

    /** Defined by RFC 6455. It is the same for every WebSocket server on earth. */
    private static final String MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static final class Request {
        public final String method;
        public final String path;
        public final Map<String, String> headers;

        Request(String method, String path, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.headers = headers;
        }

        public String header(String name) {
            return headers.get(name.toLowerCase());
        }

        public boolean wantsUpgrade() {
            String upgrade = header("upgrade");
            String connection = header("connection");
            return upgrade != null && upgrade.equalsIgnoreCase("websocket")
                    && connection != null && connection.toLowerCase().contains("upgrade");
        }
    }

    private WebSocketHandshake() {
    }

    public static Request readRequest(Socket socket) throws IOException {
        InputStream raw = socket.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int match = 0; // how many of \r\n\r\n we have matched
        while (match < 4) {
            int b = raw.read();
            if (b < 0) {
                throw new IOException("Connection closed during HTTP handshake.");
            }
            buffer.write(b);
            if (match == 0 && b == '\r'
                    || match == 1 && b == '\n'
                    || match == 2 && b == '\r'
                    || match == 3 && b == '\n') {
                match++;
            } else if (b == '\r') {
                match = 1;
            } else {
                match = 0;
            }
            if (buffer.size() > 8192) {
                throw new IOException("HTTP header too large.");
            }
        }

        String headerBlock = buffer.toString(StandardCharsets.UTF_8);
        String[] lines = headerBlock.split("\r\n");
        if (lines.length == 0 || lines[0].isEmpty()) {
            throw new IOException("Empty HTTP request.");
        }

        String[] parts = lines[0].split(" ");
        String method = parts.length > 0 ? parts[0] : "GET";
        String path = parts.length > 1 ? parts[1] : "/";

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim().toLowerCase();
                String value = line.substring(colon + 1).trim();
                headers.put(name, value);
            }
        }
        return new Request(method, path, headers);
    }

    /**
     * Completes the WebSocket upgrade. Returns false if the request is not a valid
     * upgrade (caller may then answer with ordinary HTTP).
     */
    public static boolean accept(Socket socket, Request request) throws Exception {
        if (!request.wantsUpgrade()) {
            return false;
        }
        String key = request.header("sec-websocket-key");
        if (key == null || key.isEmpty()) {
            return false;
        }

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest((key + MAGIC).getBytes(StandardCharsets.UTF_8));
        String accept = Base64.getEncoder().encodeToString(digest);

        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n"
                + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
        return true;
    }

    public static void writeHttp(Socket socket, int status, String contentType, byte[] body)
            throws IOException {
        String reason = status == 200 ? "OK" : "Not Found";
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }
}
