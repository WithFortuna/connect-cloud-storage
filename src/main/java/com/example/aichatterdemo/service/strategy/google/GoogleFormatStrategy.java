package com.example.aichatterdemo.service.strategy.google;

import com.google.api.services.drive.Drive;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Component;

@Component
public class GoogleFormatStrategy implements DownloadStrategy{
    private final Map<String, FormatContext> contextMap = Map.ofEntries(
            Map.entry("application/vnd.google-apps.document", new FormatContext("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".dox")),
            Map.entry("application/vnd.google-apps.spreadsheet", new FormatContext("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx")),
            Map.entry("application/vnd.google-apps.presentation", new FormatContext("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"))
    );



    @Override
    public boolean supports(String mimeType) {
        return  contextMap.containsKey(mimeType);
    }

    @Override
    public void download(File file, Drive drive, ByteArrayOutputStream fileOut) throws IOException {
        drive.files()
                .export(file.getId(), getStandardMimeType(file))
                .executeMediaAndDownloadTo(fileOut);
    }

    private String getStandardMimeType(File file) {
        String standardMimeType = contextMap.get(file.getMimeType())
                .getStandardMimeType();
        return standardMimeType;
    }

    @Getter
    private static class FormatContext {
        private String standardMimeType;
        private String extension;

        public FormatContext(String standardMimeType, String extension) {
            this.standardMimeType = standardMimeType;
            this.extension = extension;
        }
    }

    @Override
    public String buildFileName(File file) {
        String extension = contextMap.get(file.getMimeType())
                .getExtension();
        return file.getName() + extension;
    }
}
