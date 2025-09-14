package de.julianweinelt.caesar.worker;

import de.julianweinelt.caesar.worker.link.CaesarLinkServer;
import de.julianweinelt.caesar.worker.link.DownloadManager;
import de.julianweinelt.caesar.worker.link.UnzipFiles;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

@Slf4j
public class CaesarWorker {
    public static final String version = "2.0.2";
    @Getter
    private TrayIcon icon;

    @Getter
    private CaesarLinkServer linkServer;

    @Getter
    private DownloadManager downloadManager;

    @Getter
    private static CaesarWorker instance;

    public static void main(String[] args) throws Exception {
        instance = new CaesarWorker();
        instance.start(args);
    }

    public void start(String[] args) throws Exception {
        new File("app.lock").createNewFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            new File("app.lock").delete();
        }));
        downloadManager = new DownloadManager(10);
        log.info("Starting Caesar Worker");
        File trayIconFile = new File("assets/logo.png");
        new File("assets/status").mkdir();
        trayIconFile.getParentFile().mkdir();
        if (!trayIconFile.exists()) {
            log.info("Starting download of necessary assets");
            downloadManager.downloadFile("https://api.caesarnet.cloud/downloads/misc/logo.png", "assets/logo.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/downloads/misc/status/logo_ready.png", "assets/status/logo_ready.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/downloads/misc/status/logo_afk.png", "assets/status/logo_afk.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/downloads/misc/status/logo_dnd.png", "assets/status/logo_dnd.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/downloads/misc/status/logo_busy.png", "assets/status/logo_busy.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/downloads/misc/status/logo_offline.png", "assets/status/logo_offline.png");
        }
        if (!new File("assets/mc-icons").exists()) {
            downloadManager.downloadFileAsync("https://api.caesarnet.cloud/public/download/mc-icons", "assets/mc-icons.zip").thenRun(() -> {
                UnzipFiles.unzip("assets/mc-icons.zip", "assets/mc-icons");
                new File("assets/mc-icons.zip").delete();
            });
        }
        if (!new File("assets/emojis").exists()) {
            downloadManager.downloadFileAsync("https://api.caesarnet.cloud/public/download/emojis", "assets/emojis.zip").thenRun(() -> {
                UnzipFiles.unzip("assets/emojis.zip", "assets/emojis");
                new File("assets/emojis.zip").delete();
            });
        }
        try {
            initTrayIcon();
        } catch (AWTException e) {
            log.error("Error initializing tray icon: {}", e.getMessage());
        }
        linkServer = new CaesarLinkServer();
        linkServer.start();

        if (args.length == 1 && args[0].equalsIgnoreCase("--updated")) return;
        linkServer.runExe("Caesar.exe", List.of(), new File("."));
    }

    private void initTrayIcon() throws AWTException {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            Image image = Toolkit.getDefaultToolkit().createImage("assets/logo.png");

            icon = new TrayIcon(image, "Caesar Worker");
            icon.setImageAutoSize(true);
            icon.setToolTip("Caesar Worker");
            tray.add(icon);

            Menu statusMenu = new Menu("Status");

            MenuItem readyItem = new MenuItem("Ready");
            MenuItem awayItem = new MenuItem("Away");
            MenuItem doNotDisturbItem = new MenuItem("Do not disturb");
            MenuItem busyItem = new MenuItem("Busy");


            readyItem.addActionListener(e -> {
                icon.setImage(Toolkit.getDefaultToolkit().getImage("assets/status/logo_ready.png"));
                linkServer.sendMessage("status;ready");
            });

            awayItem.addActionListener(e -> {
                icon.setImage(Toolkit.getDefaultToolkit().getImage("assets/status/logo_afk.png"));
                linkServer.sendMessage("status;afk");
            });

            doNotDisturbItem.addActionListener(e -> {
                icon.setImage(Toolkit.getDefaultToolkit().getImage("assets/status/logo_dnd.png"));
                linkServer.sendMessage("status;dnd");
            });

            busyItem.addActionListener(e -> {
                icon.setImage(Toolkit.getDefaultToolkit().getImage("assets/status/logo_busy.png"));
                linkServer.sendMessage("status;busy");
            });

            statusMenu.add(readyItem);
            statusMenu.add(awayItem);
            statusMenu.add(doNotDisturbItem);
            statusMenu.add(busyItem);

            final PopupMenu menu = new PopupMenu();

            MenuItem aboutItem = new MenuItem("About");
            MenuItem displayItem = new MenuItem("Open Caesar");
            displayItem.addActionListener(e -> {
                //TODO: Open Caesar
            });

            MenuItem exitItem = new MenuItem("Close");
            exitItem.addActionListener(e -> {
                System.exit(0);
            });

            menu.add(aboutItem);
            menu.addSeparator();
            menu.add(displayItem);
            menu.add(statusMenu);
            menu.addSeparator();
            menu.add(exitItem);

            icon.setPopupMenu(menu);

            icon.addActionListener(e -> {
                //TODO: Do something cool
            });
        } else {
            System.err.println("System tray not supported!");
        }
    }

    public void displayTray(String title, String desc) {
        title = title.replace("_", " ");
        desc = desc.replace("_", " ");

        icon.displayMessage(title, desc, TrayIcon.MessageType.NONE);
    }

    public void displayTray(String title, String desc, TrayIcon.MessageType type) {

        title = title.replace("_", " ");
        desc = desc.replace("_", " ");

        log.debug("Trying to display tray");
        icon.displayMessage(title, desc, type);
    }
}
