package com.example.aichatterdemo;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
public class GdriveController {
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${google.api-key:}")
    private String googleApiKey;


    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<Void> handleCallback() {
        // 이 시점에 Spring Security가 토큰 교환 완료 후
        // OAuth2AuthorizedClientRepository 에 토큰 저장됨
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/drive.html")
                .build();
    }


    @GetMapping("/api/drive/files/download")
    public ResponseEntity<byte[]> downloadFiles(@RequestParam List<String> ids,
                                                @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(zipOutput);

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        Drive drive = GoogleDriveClientFactory.create(accessToken);

        for (String id : ids) {
            downloadFilesToZipEntry(id, drive, zip);
        }
        zip.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipOutput.toByteArray());
    }

    private static void downloadFilesToZipEntry(String id, Drive drive, ZipOutputStream zip) throws IOException {
        File file = drive.files().get(id)
                .setFields("id, name, mimeType")
                .setSupportsAllDrives(true)
                .execute();
        String mimeType = file.getMimeType();
        String fileName = file.getName();
        ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
        String standardMimeType;
        switch (mimeType) {
            case "application/vnd.google-apps.document":
                standardMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; // DOCX
                fileName += ".docx";
                break;

            case "application/vnd.google-apps.spreadsheet":
                standardMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";  // XLSX
                fileName += ".xlsx";
                break;

            case "application/vnd.google-apps.presentation":
                standardMimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";  // PPTX
                fileName += ".pptx";
                break;

            default:
                standardMimeType = mimeType;
                // 원래 확장자 그대로 사용 가능
        }
        if (mimeType.startsWith("application/vnd.google")) {
            downloadFile(id, drive, fileOut, standardMimeType);
        } else {
            downloadFile(id, drive, fileOut);
        }

        ZipEntry entry = new ZipEntry(fileName);
        putEntryToZip(zip, entry, fileOut);
    }

    private static void putEntryToZip(ZipOutputStream zip, ZipEntry entry, ByteArrayOutputStream fileOut) throws IOException {
        zip.putNextEntry(entry);
        zip.write(fileOut.toByteArray());
        zip.closeEntry();
    }

    private static void downloadFile(String id, Drive drive, ByteArrayOutputStream fileOut, String standardMimeType) throws IOException {
        drive.files()
                .export(id, standardMimeType)
                .executeMediaAndDownloadTo(fileOut);
    }

    private static void downloadFile(String id, Drive drive, ByteArrayOutputStream fileOut) throws IOException {
        drive.files()
                .get(id)
                .setSupportsAllDrives(true)
                .executeMediaAndDownloadTo(fileOut);
    }


    @GetMapping("/api/drive/picker-config")
    public Map<String, String> getPickerConfig(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient
    ) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("--------------------------user: {}", name);
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        return java.util.Map.of(
                "apiKey", googleApiKey == null ? "" : googleApiKey,
                "oauthToken", accessToken
        );
    }

}
