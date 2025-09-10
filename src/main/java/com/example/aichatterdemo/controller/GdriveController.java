package com.example.aichatterdemo.controller;

import com.example.aichatterdemo.srevice.GdriveService;
import com.example.aichatterdemo.srevice.GoogleTokenService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RestController
public class GdriveController {
    private final GdriveService gdriveService;
    private final GoogleTokenService tokenService;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${google.api-key:}")
    private String googleApiKey
            ;

    @GetMapping("/")
    public ResponseEntity<Void> home() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/drive.html")
                .build();
    }

    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<Void> handleCallback(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        // 이 시점에 Spring Security가 토큰 교환 완료 후
        // OAuth2AuthorizedClientRepository 에 토큰 저장됨
        log.info("GDRIVE AUTHORIZED CLIENT: {}", authorizedClient.getAccessToken().getTokenValue());
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/drive.html")
                .build();
    }

    @GetMapping("/api/drive/files/download")
    public ResponseEntity<byte[]> downloadFiles(@RequestParam List<String> ids,
                                                @RequestHeader("Authorization") String token
                                                ) throws IOException {

//        String newAccessToken = tokenService.refreshAndSaveAccessToken(authorizedClient);
//        log.info("new access token: {}", newAccessToken);
        String accessToken = token.replace("Bearer ", "");
//        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        log.info("accessTokeN from FE: {}", accessToken);
        ByteArrayOutputStream byteArrayOutputStream = gdriveService.downloadFiles(ids, accessToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayOutputStream.toByteArray());
    }


    @GetMapping("/api/drive/picker-config")
    public Map<String, String> getPickerConfig() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("--------------------------user: {}", name);
        return java.util.Map.of(
                "apiKey", googleApiKey == null ? "" : googleApiKey,
                "clientId", clientId
        );
    }

}
