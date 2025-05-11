package de.julianweinelt.caesar.worker.link;

import de.julianweinelt.caesar.worker.CaesarWorker;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CaesarLinkServer extends WebSocketServer {
    private WebSocket connection;

    public CaesarLinkServer() {
        super(new InetSocketAddress(41539));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        connection = webSocket;
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        // Not used
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        if (message.hasArray()) {
            String msg = new String(message.array(), StandardCharsets.UTF_8);

            log.info("CaesarLinkServer received message: {}", msg);
            String[] args = msg.split(";");

            if (args.length == 3) {
                if (args[0].equalsIgnoreCase("tray-message")) {
                    CaesarWorker.getInstance().displayTray(args[1], args[2], TrayIcon.MessageType.NONE);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {
        log.info("CaesarLinkServer started");
    }

    public void sendMessage(String message) {
        if (connection == null) {
            CaesarWorker.getInstance().displayTray("CaesarLinkServer", "No connection to Caesar Panel", TrayIcon.MessageType.ERROR);
            return;
        }
        connection.send(message);
    }
}