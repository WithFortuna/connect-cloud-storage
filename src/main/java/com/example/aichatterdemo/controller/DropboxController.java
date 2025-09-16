package com.example.aichatterdemo.controller;

import com.example.aichatterdemo.service.DropboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
public class DropboxController {

    private final DropboxService dropboxService;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    @Value("${spring.security.oauth2.client.registration.dropbox.client-id}")
    private String dropboxClientId;

    @GetMapping("/login/oauth2/code/dropbox")
    public ResponseEntity<Void> handleCallback() {
        // 이 시점에 Spring Security가 토큰 교환 완료 후
        // OAuth2AuthorizedClientRepository 에 토큰 저장됨
        log.info("로그인 성공 후 홈화면 리다이렉트");

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/drive.html")
                .build();
    }

    @GetMapping("/api/dropbox/chooser-config")
    public Map<String, String> chooserConfig() {
        return Map.of("appKey", dropboxClientId == null ? "" : dropboxClientId);
    }

    // TODO: cloud storage 연동 됐는지 oauth2AuthorizedClientRepository에서 조회 필요
    @GetMapping("/api/dropbox/auth-status")
    public ResponseEntity<Void> isAuthorized() {
        try {
            // OAuth2AuthorizedClientArgumentResolver와 동일한 로직
            Authentication principal = SecurityContextHolder.getContext().getAuthentication();
            if (principal == null) {
                principal = new AnonymousAuthenticationToken("anonymous", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
            }

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("dropbox")
                    .principal(principal)
                    .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null) {
                log.info("인증여부 API 호출 \n인증된 클라이언트: NULL");
            } else {
                log.info("인증여부 API 호출 \n인증된 클라이언트: Not NULL, \naccess token: {}", authorizedClient.getAccessToken().getTokenValue());
            }
            return authorizedClient != null ?
                    ResponseEntity.ok().build() :
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.info("인증여부 API 호출 \n예외 발생");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/api/dropbox/files/download")
    public ResponseEntity<byte[]> downloadFiles(@RequestParam List<String> ids,
                                                @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient)
            throws IOException, InterruptedException {
        ByteArrayOutputStream out = dropboxService.downloadFiles(ids, authorizedClient.getAccessToken().getTokenValue());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dropbox_files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out.toByteArray());
    }

    @GetMapping("/api/dropbox/test")
    public String getAuthorizedClient(@RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        return authorizedClient == null ? "인증 정보 저장 안 됨 authorizedClient is null" : "인증정보 저장 됨" + authorizedClient.getAccessToken().getTokenValue();
    }
}
