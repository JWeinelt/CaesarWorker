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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class DownloadManager {

    private final HashMap<String, Double> downloadProgress = new HashMap<>();
    private final List<Future<?>> tasks = new ArrayList<>();

    private final ExecutorService executor;

    public DownloadManager(int maxConcurrentDownloads) {
        this.executor = Executors.newFixedThreadPool(maxConcurrentDownloads);
    }

    private void download(String fileURL, String saveDir) {
        try {
            URL url = new URL(fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

            try {
                int responseCode = httpConn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    log.error("No file to download. Server replied HTTP code: {}", responseCode);
                    return;
                }

                String fileName;
                String disposition = httpConn.getHeaderField("Content-Disposition");

                if (disposition != null) {
                    fileName = disposition.split("filename=")[1].replace("\"", "");
                } else {
                    fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);
                }

                Path savePath = Paths.get(saveDir);
                Files.createDirectories(savePath.getParent());

                try (InputStream inputStream = httpConn.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(savePath.toFile())) {
                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    int bytesRead;
                    long contentLength = httpConn.getContentLengthLong();

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        downloadProgress.put(fileName, (double) totalRead / contentLength);
                    }
                    log.info("Download of {} complete. {} bytes downloaded.", fileName, totalRead);
                }
            } finally {
                httpConn.disconnect();
            }
        } catch (IOException e) {
            log.error("Error downloading file: {}", e.getMessage());
        }
    }

    public void downloadFile(String fileURL, String saveDir) {
        Future<?> task = executor.submit(() -> this.download(fileURL, saveDir));
        tasks.add(task);
    }

    public CompletableFuture<Void> downloadFileAsync(String fileURL, String saveDir) {
        return CompletableFuture.runAsync(() -> this.download(fileURL, saveDir), executor);
    }

    public void waitForDownloads() {
        tasks.forEach(task -> {
            try {
                task.get();
            } catch (Exception e) {
                log.error("Error waiting for download: {}", e.getMessage());
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }

    public double getDownloadProgress(String fileName) {
        return downloadProgress.getOrDefault(fileName, 0.0);
    }

}