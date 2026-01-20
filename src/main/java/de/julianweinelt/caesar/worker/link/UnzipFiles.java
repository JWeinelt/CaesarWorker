package de.julianweinelt.caesar.worker.link;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class UnzipFiles {

    private UnzipFiles() {
    }

    public static void unzip(String target, String destination, CopyOption... copyOptions) {
        try {
            log.info("Unzipping {} to {}", target, destination);

            Path destinationPath = Path.of(destination);
            if (!Files.exists(destinationPath)) {
                Files.createDirectories(destinationPath);
            }

            try (FileInputStream fileInputStream = new FileInputStream(target);
                 ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
                while (!Thread.currentThread().isInterrupted()) {
                    ZipEntry entry = zipInputStream.getNextEntry();

                    if (entry == null) {
                        break;
                    }

                    try {
                        String entryName = entry.getName();
                        Path entryDestination = destinationPath.resolve(entryName);
                        if (entry.isDirectory()) {
                            Files.createDirectories(entryDestination);
                            continue;
                        }

                        Files.copy(zipInputStream, entryDestination, copyOptions);
                    } finally {
                        zipInputStream.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to unzip %s: %s".formatted(
                    target,
                    e.getMessage()
            ), e);
        }
    }

    public static CompletableFuture<Void> unzipFuture(String target, String destination, CopyOption... copyOptions) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            unzip(target, destination, copyOptions);
            future.complete(null);
        } catch (UncheckedIOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

}