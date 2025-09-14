package de.julianweinelt.caesar.worker.link;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class UnzipFiles {
    public static void unzip(String zipFilePath, String destDir) {
        log.info("Unzipping " + zipFilePath + " to " + destDir);
        File dir = new File(destDir);
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    public static CompletableFuture<Void> unzipFuture(String zipFilePath, String destDir) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        log.info("Unzipping " + zipFilePath + " to " + destDir);
        File dir = new File(destDir);
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            fis.close();
            future.complete(null);
        } catch (IOException e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;

    }

}