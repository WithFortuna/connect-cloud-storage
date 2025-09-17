package com.example.aichatterdemo.service.strategy.google;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface DownloadStrategy {
    boolean supports(String mimeType);
    void download(File file, Drive drive, ByteArrayOutputStream fileOut) throws IOException;
    String buildFileName(File file);
}
