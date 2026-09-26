package civ.net.ws;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** RFC 6455 framing — read/write, mask unmask, ping/pong/close. */
public final class WebSocketFrames {

    public static final int OP_TEXT = 0x1;
    public static final int OP_BINARY = 0x2;
    public static final int OP_CLOSE = 0x8;
    public static final int OP_PING = 0x9;
    public static final int OP_PONG = 0xA;

    public static final class Frame {
        public final int opcode;
        public final byte[] payload;

        Frame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }

        public String text() {
            return new String(payload, StandardCharsets.UTF_8);
        }
    }

    private WebSocketFrames() {
    }

    public static Frame read(DataInputStream in) throws IOException {
        int first = in.readUnsignedByte();
        int opcode = first & 0x0F;

        int second = in.readUnsignedByte();
        boolean masked = (second & 0x80) != 0;
        long length = second & 0x7F;

        if (length == 126) {
            length = in.readUnsignedShort();
        } else if (length == 127) {
            length = in.readLong();
        }

        if (length < 0 || length > 1_000_000) {
            throw new IOException("WebSocket frame too large: " + length);
        }

        byte[] mask = new byte[4];
        if (masked) {
            in.readFully(mask);
        }

        byte[] payload = new byte[(int) length];
        in.readFully(payload);

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i % 4];
            }
        }
        return new Frame(opcode, payload);
    }

    public static void write(OutputStream out, int opcode, String text) throws IOException {
        write(out, opcode, text.getBytes(StandardCharsets.UTF_8));
    }

    /** Server-to-client frames are never masked. */
    public static void write(OutputStream out, int opcode, byte[] payload) throws IOException {
        out.write(0x80 | (opcode & 0x0F));

        if (payload.length < 126) {
            out.write(payload.length);
        } else if (payload.length < 65536) {
            out.write(126);
            out.write((payload.length >> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        } else {
            out.write(127);
            for (int shift = 56; shift >= 0; shift -= 8) {
                out.write((int) (((long) payload.length >> shift) & 0xFF));
            }
        }
        out.write(payload);
        out.flush();
    }
}
