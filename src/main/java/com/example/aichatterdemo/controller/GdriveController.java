package com.example.aichatterdemo.controller;

import com.example.aichatterdemo.srevice.GdriveService;
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

@Slf4j
@RestController
public class GdriveController {
    private final GdriveService gdriveService;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${google.api-key:}")
    private String googleApiKey;

    public GdriveController(GdriveService gdriveService) {
        this.gdriveService = gdriveService;
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
        ByteArrayOutputStream byteArrayOutputStream = gdriveService.downloadFiles(ids, authorizedClient.getAccessToken().getTokenValue());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayOutputStream.toByteArray());
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
