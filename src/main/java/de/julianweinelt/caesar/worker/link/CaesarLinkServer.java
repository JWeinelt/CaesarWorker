package de.julianweinelt.caesar.worker.link;

import de.julianweinelt.caesar.worker.CaesarWorker;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.awt.TrayIcon;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CaesarLinkServer extends WebSocketServer {
    private WebSocket connection;
    private boolean waitForUpdate = false;

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

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("stop-caesar")) {
                    conn.send("stop-ok");
                    if (waitForUpdate) {
                        CaesarWorker.getInstance().displayTray("Starting update...", "This may take a few minutes.");
                        UnzipFiles.unzipFuture("./temp/cup.zip", "update").thenRun(() -> {
                            try {
                                Files.copy(Path.of("update", "caesar.exe"), new File("Caesar.exe").toPath(), StandardCopyOption.REPLACE_EXISTING);
                                runExe("Caesar.exe", List.of("--updated"), new File("."));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } else if (args[0].equalsIgnoreCase("get-version")) {
                    conn.send("version;" + CaesarWorker.version);
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("update")) {
                    String newVer = args[1];
                    CaesarWorker.getInstance().displayTray("Downloading update...", "Download started. You can further use Caesar.",
                            TrayIcon.MessageType.INFO);
                    CaesarWorker.getInstance().getDownloadManager().downloadFileAsync("https://api.caesarnet.cloud/public/download/client/" +
                            newVer, "./temp/cup.zip").thenRun(() -> {
                                CaesarWorker.getInstance().displayTray("Download complete!", "Close Caesar to start updating.");
                                waitForUpdate = true;
                    });
                } else if (args[0].equalsIgnoreCase("worker-update")) {
                    CaesarWorker.getInstance().displayTray("Starting update...", "CaesarWorker is being updated. (" + args[1] + ")");
                    CaesarWorker.getInstance().getDownloadManager().downloadFileAsync("https://api.caesarnet.cloud/public/download/worker/" + args[1],
                            "./update/worker.exe").thenRun(() -> {
                                try {Thread.sleep(3000);} catch (InterruptedException ignored) {}
                        File updaterFile = new File("WorkerUpdater.exe");
                        if (!updaterFile.exists()) {
                            CaesarWorker.getInstance().getDownloadManager().downloadFileAsync("https://api.caesarnet.cloud/downloads/misc/WorkerUpdater.exe",
                                    "./update/updater.exe").thenRun(() -> {

                                try {
                                    Files.copy(Path.of("update", "updater.exe"), new File("WorkerUpdater.exe").toPath());
                                    ProcessBuilder pb = new ProcessBuilder(
                                            "powershell",
                                            "-Command",
                                            "Start-Process '" + "WorkerUpdater" + "' -Verb RunAs"
                                    );
                                    pb.start();
                                    System.exit(0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        } else {
                            try {
                                ProcessBuilder pb = new ProcessBuilder(
                                        "powershell",
                                        "-Command",
                                        "Start-Process '" + "WorkerUpdater" + "' -Verb RunAs"
                                );
                                pb.start();
                                System.exit(0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                            });
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("tray-message")) {
                    CaesarWorker.getInstance().displayTray(args[1], args[2], TrayIcon.MessageType.NONE);
                }
            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("tray-message")) {
                    CaesarWorker.getInstance().displayTray(args[1], args[2], TrayIcon.MessageType.valueOf(args[3].toUpperCase()));
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

    public List<String> runExe(String exePath, List<String> args, File workDir) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(exePath);
        if (args != null) {
            command.addAll(args);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDir != null) {
            pb.directory(workDir);
        }

        Process process = pb.start();

        List<String> output = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.add("OUT: " + line);
            }
            while ((line = errorReader.readLine()) != null) {
                output.add("ERR: " + line);
            }
        }

        int exitCode = process.waitFor();
        output.add("EXIT CODE: " + exitCode);

        return output;
    }

    public int runExe(String exePath, File workDir) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(exePath);

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDir != null) {
            pb.directory(workDir);
        }

        Process process = pb.start();

        List<String> output = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.add("OUT: " + line);
            }
            while ((line = errorReader.readLine()) != null) {
                output.add("ERR: " + line);
            }
        }

        return process.waitFor();
    }
}