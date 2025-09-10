package com.example.aichatterdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

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
                        .requestMatchers("/api/**")
                        .authenticated()
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

}

