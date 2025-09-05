package com.example.aichatterdemo.srevice.strategy;

import com.google.api.services.drive.Drive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Component;

@Component
public class BinaryStrategy implements DownloadStrategy {
     private static final String GOOGLE_FORMAT_PREFIX = "application/vnd.google";

    @Override
    public boolean supports(String mimeType) {
        return mimeType == null || !(mimeType.startsWith(GOOGLE_FORMAT_PREFIX));
    }

    @Override
    public void download(File file, Drive drive, ByteArrayOutputStream fileOut) throws IOException {
        drive.files()
                .get(file.getId())
                .setSupportsAllDrives(true)
                .executeMediaAndDownloadTo(fileOut);
    }

    @Override
    public String buildFileName(File file) {
        String extensionAlreadyIncluded = file.getName();
        return extensionAlreadyIncluded;
    }

}
