package de.julianweinelt.caesar.worker.link;

import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class DownloadManager {
    private final HashMap<String, UUID> downloadFiles = new HashMap<>();
    private final HashMap<UUID, Double> downloadProgress = new HashMap<>();

    private final ExecutorService executor;
    private final List<Future<?>> tasks = new ArrayList<>();

    public DownloadManager(int maxConcurrentDownloads) {
        this.executor = Executors.newFixedThreadPool(maxConcurrentDownloads);
    }

    public void downloadFile(String fileURL, String saveDir) {
        Future<?> task = executor.submit(() -> {
            try {
                URL url = new URL(fileURL);
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                int responseCode = httpConn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String fileName;
                    String disposition = httpConn.getHeaderField("Content-Disposition");

                    if (disposition != null) {
                        fileName = disposition.split("filename=")[1].replace("\"", "");
                    } else {
                        fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
                    }

                    downloadFiles.put(fileName, UUID.randomUUID());

                    InputStream inputStream = httpConn.getInputStream();
                    Path savePath = Paths.get(saveDir);
                    Files.createDirectories(savePath.getParent());

                    try (FileOutputStream outputStream = new FileOutputStream(savePath.toFile())) {
                        byte[] buffer = new byte[4096];
                        long totalRead = 0;
                        int bytesRead;
                        long contentLength = httpConn.getContentLengthLong();

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                            downloadProgress.put(downloadFiles.get(fileName), (double) totalRead / contentLength);
                        }
                        log.info("Download of {} complete. {} bytes downloaded.", fileName, totalRead);
                    }
                    inputStream.close();
                    httpConn.disconnect();
                } else {
                    log.error("No file to download. Server replied HTTP code: {}", responseCode);
                }
            } catch (IOException e) {
                log.error("Error downloading file: {}", e.getMessage());
            }
        });

        tasks.add(task);
    }

    public void waitForDownloads() {
        tasks.forEach(task -> {
            try {
                task.get();
            } catch (Exception e) {
                log.error("Error waiting for download: {}", e.getMessage());
            }
        });
        executor.shutdown();
    }

    public double getDownloadProgress(String fileName) {
        return downloadProgress.getOrDefault(downloadFiles.getOrDefault(fileName, UUID.randomUUID()), 0.0);
    }
}