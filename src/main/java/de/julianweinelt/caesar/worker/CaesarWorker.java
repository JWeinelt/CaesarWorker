package de.julianweinelt.caesar.worker;

import de.julianweinelt.caesar.worker.link.CaesarLinkServer;
import de.julianweinelt.caesar.worker.link.DownloadManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.File;

@Slf4j
public class CaesarWorker {
    @Getter
    private TrayIcon icon;

    @Getter
    private CaesarLinkServer linkServer;

    @Getter
    private DownloadManager downloadManager;

    @Getter
    private static CaesarWorker instance;

    public static void main(String[] args) {
        instance = new CaesarWorker();
        instance.start();
    }

    public void start() {
        downloadManager = new DownloadManager(10);
        log.info("Starting Caesar Worker");
        File trayIconFile = new File("assets/logo.png");
        new File("assets/status").mkdir();
        trayIconFile.getParentFile().mkdir();
        if (!trayIconFile.exists()) {
            log.info("Starting download of necessary assets");
            downloadManager.downloadFile("https://api.caesarnet.cloud/download/misc/logo.png", "assets/logo.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/download/misc/status/logo_ready.png", "assets/status/logo_ready.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/download/misc/status/logo_afk.png", "assets/status/logo_afk.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/download/misc/status/logo_dnd.png", "assets/status/logo_dnd.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/download/misc/status/logo_busy.png", "assets/status/logo_busy.png");
            downloadManager.downloadFile("https://api.caesarnet.cloud/download/misc/status/logo_offline.png", "assets/status/logo_offline.png");
            System.exit(1);
        }
        try {
            initTrayIcon();
        } catch (AWTException e) {
            log.error("Error initializing tray icon: {}", e.getMessage());
        }
        linkServer = new CaesarLinkServer();
        linkServer.start();
    }

    private void initTrayIcon() throws AWTException {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            Image image = Toolkit.getDefaultToolkit().createImage("assets/logo.png");

            icon = new TrayIcon(image, "Caesar Worker");
            //Let the system resize the image if needed
            icon.setImageAutoSize(true);
            //Set tooltip text for the tray icon
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

        icon.displayMessage(title, desc, type);
    }
}
