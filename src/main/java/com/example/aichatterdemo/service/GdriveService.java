package com.example.aichatterdemo.service;

import com.example.aichatterdemo.infrastructure.GoogleDriveClientFactory;
import com.example.aichatterdemo.service.strategy.google.DownloadStrategy;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GdriveService {
    private List<DownloadStrategy>  downloadStrategies = new ArrayList<>();

    @Autowired
    public GdriveService(List<DownloadStrategy> downloadStrategies) {
        this.downloadStrategies = downloadStrategies;
    }

    private DownloadStrategy getDownloadStrategy(String mimeType) {
        for(DownloadStrategy downloadStrategy : downloadStrategies){
            if (downloadStrategy.supports(mimeType)) {
                return downloadStrategy;
            }
        }

        throw new IllegalArgumentException();
    }

    public ByteArrayOutputStream downloadFiles(List<String> ids, String accessToken) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try(ZipOutputStream zip = new ZipOutputStream(byteArrayOutputStream)) {
            Drive drive = GoogleDriveClientFactory.create(accessToken);

            for (String id : ids) {
                downloadFilesToZipEntry(id, drive, zip);
            }
        }

        return  byteArrayOutputStream;
    }

    private void downloadFilesToZipEntry(String id, Drive drive, ZipOutputStream zip) throws IOException {
        File file = drive.files().get(id)
                .setFields("id, name, mimeType")
                .setSupportsAllDrives(true)
                .execute();
        ByteArrayOutputStream fileOut = new ByteArrayOutputStream();

        DownloadStrategy downloadStrategy = getDownloadStrategy(file.getMimeType());
        downloadStrategy.download(file, drive, fileOut);

        putEntryToZip(zip, new ZipEntry(downloadStrategy.buildFileName(file)), fileOut);
    }

    private static void putEntryToZip(ZipOutputStream zip, ZipEntry entry, ByteArrayOutputStream fileOut) throws IOException {
        zip.putNextEntry(entry);
        zip.write(fileOut.toByteArray());
        zip.closeEntry();
    }

}
