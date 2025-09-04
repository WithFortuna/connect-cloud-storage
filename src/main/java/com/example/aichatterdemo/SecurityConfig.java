package com.example.aichatterdemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/drive.html", true)
                )
                .logout(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}

