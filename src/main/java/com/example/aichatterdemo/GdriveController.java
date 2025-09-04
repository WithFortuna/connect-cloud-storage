package com.example.aichatterdemo;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class GdriveController {
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @GetMapping("/api/drive/files")
    public List<File> listFiles(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) throws IOException {

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        Drive drive = GoogleDriveClientFactory.create(accessToken);

        FileList result = drive.files().list()
                .setPageSize(20)
                .setQ("trashed = false")
                .setFields("files(id, name, mimeType, size, modifiedTime, webViewLink)")
                .execute();

        return result.getFiles(); // JSON 응답으로 내려주면 프론트에서 목록 렌더링 가능
    }

    @GetMapping("/api/drive/files/download")
    public ResponseEntity<byte[]> downloadFiles(@RequestParam List<String> ids,
                                                @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(zipOutput);

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        Drive drive = GoogleDriveClientFactory.create(accessToken);

        for (String id : ids) {
            ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
            drive.files().get(id).executeMediaAndDownloadTo(fileOut);

            ZipEntry entry = new ZipEntry(id + ".bin");
            zip.putNextEntry(entry);
            zip.write(fileOut.toByteArray());
            zip.closeEntry();
        }

        zip.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipOutput.toByteArray());
    }


}
