package de.julianweinelt.caesar.worker.voicechat;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
public class VoiceClient {

    private final DatagramSocket socket;
    private final InetSocketAddress server;
    private final UUID roomID;
    private final UUID userID;
    private final int bitrate;

    private volatile boolean running = true;

    // Audio Config
    private final int SAMPLE_RATE = 48000;
    private final int CHANNELS = 1;
    private final int FRAME_SIZE = 960;

    private Thread senderThread;
    private Thread receiverThread;

    public VoiceClient(String host, int port, UUID roomID, UUID userID, int bitrate) throws SocketException {
        this.socket = new DatagramSocket();
        this.server = new InetSocketAddress(host, port);
        this.roomID = roomID;
        this.userID = userID;
        this.bitrate = bitrate;
        socket.setSoTimeout(1000);
    }

    public void start() {
        running = true;
        joinRoom();

        senderThread = new Thread(this::captureAndSendAudio, "VoiceSender");
        receiverThread = new Thread(this::receivePackets, "VoiceReceiver");

        senderThread.start();
        receiverThread.start();
    }

    public void stop() {
        running = false;
        leaveRoom();
        socket.close();
    }


    private void joinRoom() {
        String msg = "join:" + roomID + ":" + userID + ":" + bitrate;
        sendControl(msg);
    }

    private void leaveRoom() {
        String msg = "leave:" + roomID + ":" + userID;
        sendControl(msg);
    }

    private void sendControl(String msg) {
        VoicePacket packet = VoicePacket.createControl(msg);
        byte[] data = packet.toBytes();
        try {
            socket.send(new DatagramPacket(data, data.length, server));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private void captureAndSendAudio() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
            mic.open(format);
            mic.start();

            byte[] buffer = new byte[FRAME_SIZE * 2];
            while (running) {
                int bytesRead = mic.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {

                    VoicePacket packet = VoicePacket.createAudio(buffer);
                    byte[] data = packet.toBytes();
                    socket.send(new DatagramPacket(data, data.length, server));
                }
            }

            mic.stop();
            mic.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    private void receivePackets() {
        byte[] buffer = new byte[4096];

        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, CHANNELS, true, false);
            SourceDataLine speaker = AudioSystem.getSourceDataLine(format);
            speaker.open(format);
            speaker.start();

            while (running) {
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(udpPacket);
                    VoicePacket packet = VoicePacket.fromBytes(buffer, udpPacket.getLength());

                    switch (packet.type) {
                        case 0x00 -> {
                            byte[] pcm = packet.payload; // Hier würdest du Opus-Decode einfügen
                            speaker.write(pcm, 0, pcm.length);
                        }
                        case 0x01 -> {
                            String event = new String(packet.payload, StandardCharsets.UTF_8);
                            log.info("[EVENT] {}", event);
                        }
                        case 0x02 -> {

                        }
                    }
                } catch (SocketTimeoutException ignored) {}
            }

            speaker.drain();
            speaker.stop();
            speaker.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
