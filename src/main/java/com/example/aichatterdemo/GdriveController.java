package com.example.aichatterdemo;

import com.example.aichatterdemo.strategy.BinaryStrategy;
import com.example.aichatterdemo.strategy.DownloadStrategy;
import com.example.aichatterdemo.strategy.GoogleFormatStrategy;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList;
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

    private List<DownloadStrategy>  downloadStrategies = new ArrayList<>();

    @Autowired
    public GdriveController(List<DownloadStrategy> downloadStrategies) {
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
        ZipOutputStream zip = new ZipOutputStream(new ByteArrayOutputStream());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        Drive drive = GoogleDriveClientFactory.create(accessToken);

        for (String id : ids) {
            downloadFilesToZipEntry(id, drive, zip);
        }
        zip.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayOutputStream().toByteArray());
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
