package com.example.aichatterdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;


@Slf4j
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/**"
                        ).permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2Client(Customizer.withDefaults())
/*                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/drive.html", true)
                )
                .logout(Customizer.withDefaults())*/
        ;

        return http.build();
    }

    // refresh token을 구글로부터 받기위해선 추가 헤더 필요
    @Bean
    public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
            ClientRegistrationRepository clients) {

        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer
                        .additionalParameters(params -> {
                            params.put("access_type", "offline");
                            params.put("prompt", "consent");
                        })
        );

        return resolver;
    }
/*
    @Primary
    @Bean
    public OAuth2AuthorizedClientRepository  authorizedClientRepository() {
        log.info("oauthAuthorizedClientRepository 빈 등록");
        return new OAuth2AuthorizedClientRepository() {
            private final OAuth2AuthorizedClientRepository delegate = new HttpSessionOAuth2AuthorizedClientRepository();

            @Override
            public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
                return delegate.loadAuthorizedClient(clientRegistrationId, principal, request);
            }

            @Override
            public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
                log.info("=== OAuth2AuthorizedClient 저장됨 ===");
                log.info("clientRegistrationId: {}", authorizedClient.getClientRegistration().getRegistrationId());
                log.info("principalName: {}", principal.getName());
                log.info("accessToken: {}", accessToken.getTokenValue());
                log.info("expiresAt: {}", accessToken.getExpiresAt());
                log.info("refreshToken: {}", refreshToken != null ? refreshToken.getTokenValue() : "없음");

                delegate.saveAuthorizedClient(authorizedClient, principal, request, response);
            }

            @Override
            public void removeAuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request, HttpServletResponse response) {
                delegate.removeAuthorizedClient(clientRegistrationId, principal, request, response);
            }
        };
    }*/
}
