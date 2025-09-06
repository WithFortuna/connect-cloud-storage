package com.example.aichatterdemo;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    /// ----------------
    @GetMapping("/api/drive/files")
    public List<File> listFiles(
            // 외부 리소스 접근 권한을 위임받은 OAuth2 인증정보는 OAuth2AuthorizedClientRepository에 저장
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient
            ) throws IOException {
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

    @GetMapping("/api/drive/picker-config")
    public java.util.Map<String, String> getPickerConfig(
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
