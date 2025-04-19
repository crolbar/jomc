package com.jomc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.util.Consumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ORGB
{
    private final static int NET_PACKET_ID_REQUEST_CONTROLLER_COUNT = 0;
    private final static int NET_PACKET_ID_REQUEST_CONTROLLER_DATA = 1;
    private final static int NET_PACKET_ID_SET_CLIENT_NAME = 50;
    private final static int NET_PACKET_ID_RGBCONTROLLER_UPDATELEDS = 1050;

    private final byte[] pkt_magic = "ORGB".getBytes(StandardCharsets.UTF_8);
    private final static byte[] client_name =
      "jomc\0".getBytes(StandardCharsets.UTF_8);

    private final ExecutorService executor =
      Executors.newSingleThreadExecutor();

    private Socket socket;
    private final Context context;

    private int controllerCount;
    private boolean doneSettingColor;

    public ORGB(Context ctx)
    {
        this.context = ctx;
        this.controllerCount = 0;
        this.doneSettingColor = true;
    }

    public void connect(String host, int port)
    {
        executor.submit(() -> {
            try {
                this.socket = new Socket(host, port);
                sendMsg(NET_PACKET_ID_SET_CLIENT_NAME, 0, client_name);
            } catch (IOException e) {
                Log.i("tag", e.toString());
                new Handler(Looper.getMainLooper())
                  .post(()
                          -> Toast
                               .makeText(this.context,
                                         "Error making connection with host: " +
                                           e.getMessage(),
                                         Toast.LENGTH_SHORT)
                               .show());
                return;
            }

            new Handler(Looper.getMainLooper())
              .post(()
                      -> Toast
                           .makeText(this.context,
                                     "Connection opened",
                                     Toast.LENGTH_SHORT)
                           .show());
        });
    }

    public boolean isConnAlive()
    {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void executorAdd(Runnable r) { executor.submit(r); }

    public void closeConn()
    {
        executor.submit(() -> {
            try {
                socket.close();
            } catch (IOException e) {
                Log.i("tag", e.toString());
                new Handler(Looper.getMainLooper())
                  .post(()
                          -> Toast
                               .makeText(this.context,
                                         "Error closing connection: " +
                                           e.getMessage(),
                                         Toast.LENGTH_SHORT)
                               .show());
                return;
            }

            new Handler(Looper.getMainLooper())
              .post(()
                      -> Toast
                           .makeText(this.context,
                                     "Connection closed",
                                     Toast.LENGTH_SHORT)
                           .show());

            this.doneSettingColor = true;
        });
    }

    public void updateControllerCount()
    {
        sendMsg(NET_PACKET_ID_REQUEST_CONTROLLER_COUNT, 0, new byte[0]);

        readMessage((controllerCountBytes) -> {
            ByteBuffer bb = ByteBuffer.wrap(controllerCountBytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            this.controllerCount = bb.getInt();
        });
    }

    public void setColor(byte[] color)
    {
        if (!this.isConnAlive()) {
            return;
        }
        if (!doneSettingColor) {
            return;
        }

        this.doneSettingColor = false;

        for (int i = 0; i < controllerCount; i++) {
            sendMsg(NET_PACKET_ID_REQUEST_CONTROLLER_DATA, i, new byte[0]);
            int finalI = i;

            readMessage((b) -> {
                int numColors = getNumberOfColors(b);

                int size = 2 + 4 * numColors;
                ByteBuffer bb = ByteBuffer.allocate(size + 4);
                bb.order(ByteOrder.LITTLE_ENDIAN);

                bb.putInt(size);
                bb.putShort((short)numColors); // num colors

                for (int j = 0; j < numColors; j++) {
                    bb.put(color); // colors
                }

                sendMsg(
                  NET_PACKET_ID_RGBCONTROLLER_UPDATELEDS, finalI, bb.array());

                if (finalI == controllerCount - 1) {
                    executor.submit(() -> this.doneSettingColor = true);
                }
            });

            // fail safe
            // if somehow setColor is called without controllerCount set
            if (i == controllerCount - 1) {
                return;
            }
        }

        this.doneSettingColor = true;
    }

    // parses the controller data and returns just the number of colors for the
    // controller
    private int getNumberOfColors(byte[] data)
    {
        int off = 4;
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        off += 4; // type

        // Name
        {
            bb.position(off);
            int size = bb.getChar();
            off += 2; // name_size

            off += size; // name
        }

        // Description
        {
            bb.position(off);
            int size = bb.getChar();
            off += 2; // desc_size

            off += size; // desc
        }

        // Version
        {
            bb.position(off);
            int size = bb.getChar();
            off += 2; // version_size

            off += size; // version
        }

        // Serial
        {
            bb.position(off);
            int size = bb.getChar();
            off += 2; // serial_size

            off += size; // serial
        }

        // Location
        {
            bb.position(off);
            int size = bb.getChar();
            off += 2;

            off += size;
        }

        // modes
        {
            bb.position(off);
            int numModes = bb.getChar();
            off += 2; // num_modes

            off += 4; // active mode

            for (int i = 0; i < numModes; i++) {
                bb.position(off);
                int nameLen = bb.getChar();
                off += 2; // name len

                off += nameLen; // name

                off += 4 + // value
                       4 + // flags
                       4 + // speed min
                       4 + // speed max
                       4 + // colors min
                       4 + // colors max
                       4 + // speed
                       4 + // direction
                       4;  // color mode

                bb.position(off);
                int colorsNum = bb.getChar();
                off += 2; // colors num

                off += colorsNum * (4 * 1);
            }
        }

        // Zones
        {
            bb.position(off);
            int numZones = bb.getChar();
            off += 2; // num zones

            for (int i = 0; i < numZones; i++) {
                bb.position(off);
                int nameLen = bb.getChar();
                off += 2;       // name len
                off += nameLen; // name

                off += 4 + // type
                       4 + // leds min
                       4 + // leds max
                       4;  // leds count

                bb.position(off);
                int matrixLen = bb.getChar();
                off += 2;         // matrix len
                off += matrixLen; // matrix
            }
        }

        // LEDS
        {
            bb.position(off);
            int numLEDS = bb.getChar();
            off += 2; // num zones

            for (int i = 0; i < numLEDS; i++) {
                bb.position(off);
                int nameLen = bb.getChar();
                off += 2;       // name len
                off += nameLen; // name
                off += 4;       // value
            }
        }

        bb.position(off);
        int numColors = bb.getChar();

        return numColors;
    }

    private void sendMsg(int command, int devId, byte[] buff)
    {
        executor.submit(() -> {
            try {
                int size = 4 + // orgb magic val
                           4 + // dev id
                           4 + // cmd id
                           4 + // pkt_size
                           buff.length;

                ByteBuffer bb = ByteBuffer.allocate(size);
                bb.order(ByteOrder.LITTLE_ENDIAN);

                bb.put(pkt_magic);
                bb.putInt(devId);       // pkt_dev_idx
                bb.putInt(command);     // pkt_id
                bb.putInt(buff.length); // pkt size
                bb.put(buff);

                // Log.i("tag", Arrays.toString(bb.array()));

                OutputStream out = socket.getOutputStream();
                out.write(bb.array());
                out.flush();
            } catch (IOException e) {
                Log.i("tag", e.toString());
            }
        });
    }

    private void readMessage(Consumer<byte[]> c)
    {
        executor.submit(() -> {
            try {
                byte[] headerBuf = new byte[4 * 4];

                InputStream in = socket.getInputStream();
                in.read(headerBuf);

                ByteBuffer bb = ByteBuffer.wrap(headerBuf);
                bb.order(ByteOrder.LITTLE_ENDIAN);

                bb.getInt();
                Integer devIdx = bb.getInt();
                Integer id = bb.getInt();
                Integer size = bb.getInt();

                byte[] buf = new byte[size];
                in.read(buf);

                c.accept(buf);
            } catch (IOException e) {
                Log.i("tag", e.toString());
            }
        });
    }
}
