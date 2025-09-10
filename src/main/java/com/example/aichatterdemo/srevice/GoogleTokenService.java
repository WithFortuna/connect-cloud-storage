package com.example.aichatterdemo.srevice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;

@Service
public class GoogleTokenService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    /**
     * 현재 인증된 사용자의 유효한 Access Token 반환
     * 필요시 자동으로 refresh token을 사용해 갱신
     */
    public String getValidAccessToken(Authentication authentication) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient("google", authentication.getName());

        if (authorizedClient == null) {
            throw new RuntimeException("Google OAuth2 인증 정보가 없습니다.");
        }

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

        // 토큰이 만료되었거나 곧 만료될 예정인 경우 갱신
        if (isTokenExpired(accessToken)) {
            return refreshAndSaveAccessToken(authorizedClient);
        }
        return accessToken.getTokenValue();
    }

    /**
     * 토큰 만료 여부 확인 (30초 여유두고 판단)
     */
    private boolean isTokenExpired(OAuth2AccessToken accessToken) {
        if (accessToken.getExpiresAt() == null) {
            return false; // 만료시간이 없으면 만료되지 않는 것으로 간주
        }

        Instant expiresAt = accessToken.getExpiresAt();
        Instant now = Instant.now().plusSeconds(30); // 30초 여유

        return now.isAfter(expiresAt);
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급 및 저장
     */
    public String refreshAndSaveAccessToken(OAuth2AuthorizedClient authorizedClient) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authorizedClient.getPrincipalName(),
                null,
                Collections.emptyList());

        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        if (refreshToken == null) {
            throw new RuntimeException("Refresh Token이 없습니다. 다시 로그인해주세요.");
        }

        try {
            // Google OAuth2 토큰 갱신 API 호출
            GoogleTokenResponse tokenResponse = callGoogleTokenRefreshAPI(
                    authorizedClient.getClientRegistration(),
                    refreshToken.getTokenValue()
            );

            // 새로운 토큰으로 OAuth2AuthorizedClient 업데이트
            OAuth2AccessToken newAccessToken = createAccessTokenFromResponse(tokenResponse);
            OAuth2RefreshToken newRefreshToken = createRefreshTokenFromResponse(refreshToken, tokenResponse);

            OAuth2AuthorizedClient updatedClient = createOAuth2AuthorizedClient(authorizedClient, authentication, newAccessToken, newRefreshToken);

            // 업데이트된 클라이언트 저장
            authorizedClientService.saveAuthorizedClient(updatedClient, authentication);

            return newAccessToken.getTokenValue();
        } catch (Exception e) {
            throw new RuntimeException("토큰 갱신 실패: " + e.getMessage(), e);
        }
    }

    private static OAuth2RefreshToken createRefreshTokenFromResponse(OAuth2RefreshToken refreshToken, GoogleTokenResponse tokenResponse) {
        OAuth2RefreshToken newRefreshToken = refreshToken;
        if (tokenResponse.getRefreshToken() != null) {
            newRefreshToken = new OAuth2RefreshToken(tokenResponse.getRefreshToken(), null);
        }
        return newRefreshToken;
    }

    private static OAuth2AccessToken createAccessTokenFromResponse(GoogleTokenResponse tokenResponse) {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenResponse.getAccessToken(),
                Instant.now(),
                Instant.now().plusSeconds(tokenResponse.getExpiresIn())
        );
    }

    private static OAuth2AuthorizedClient createOAuth2AuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication authentication, OAuth2AccessToken newAccessToken, OAuth2RefreshToken newRefreshToken) {
        OAuth2AuthorizedClient updatedClient = new OAuth2AuthorizedClient(
                authorizedClient.getClientRegistration(),
                authentication.getName(),
                newAccessToken,
                newRefreshToken
        );
        return updatedClient;
    }

    /**
     * Google OAuth2 토큰 갱신 API 직접 호출
     */
    private GoogleTokenResponse callGoogleTokenRefreshAPI(
            ClientRegistration clientRegistration,
            String refreshToken) {

        String tokenUri = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientRegistration.getClientId());
        params.add("client_secret", clientRegistration.getClientSecret());
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                tokenUri, request, GoogleTokenResponse.class);

        return response.getBody();
    }

    @Data
    public static class GoogleTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("token_type")
        private String tokenType;
    }

}

