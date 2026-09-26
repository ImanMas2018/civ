package civ.net.ws;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * One browser chat socket: reads frames on its own thread, writes text/control frames out.
 */
public class WebSocketConnection {

    private final Socket socket;
    private final OutputStream out;
    private final DataInputStream in;
    private final BiConsumer<WebSocketConnection, String> onText;
    private final Runnable onClosed;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public WebSocketConnection(Socket socket,
                               BiConsumer<WebSocketConnection, String> onText,
                               Runnable onClosed) throws IOException {
        this.socket = socket;
        this.out = socket.getOutputStream();
        this.in = new DataInputStream(socket.getInputStream());
        this.onText = onText;
        this.onClosed = onClosed;
    }

    public void start() {
        Thread reader = new Thread(this::readLoop, "ws-chat-" + socket.getPort());
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            while (open.get()) {
                WebSocketFrames.Frame frame = WebSocketFrames.read(in);
                switch (frame.opcode) {
                    case WebSocketFrames.OP_TEXT:
                        onText.accept(this, frame.text());
                        break;
                    case WebSocketFrames.OP_PING:
                        synchronized (out) {
                            WebSocketFrames.write(out, WebSocketFrames.OP_PONG, frame.payload);
                        }
                        break;
                    case WebSocketFrames.OP_PONG:
                        break;
                    case WebSocketFrames.OP_CLOSE:
                        try {
                            synchronized (out) {
                                WebSocketFrames.write(out, WebSocketFrames.OP_CLOSE, new byte[0]);
                            }
                        } catch (IOException ignored) {
                        }
                        closeQuietly();
                        return;
                    default:
                        break;
                }
            }
        } catch (IOException ex) {
            closeQuietly();
        }
    }

    public void sendText(String text) {
        if (!open.get()) {
            return;
        }
        try {
            synchronized (out) {
                WebSocketFrames.write(out, WebSocketFrames.OP_TEXT, text);
            }
        } catch (IOException ex) {
            closeQuietly();
        }
    }

    public void closeQuietly() {
        if (!open.compareAndSet(true, false)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        onClosed.run();
    }
}
