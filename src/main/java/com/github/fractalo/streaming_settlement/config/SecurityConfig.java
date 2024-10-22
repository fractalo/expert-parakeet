package com.github.fractalo.streaming_settlement.config;

import com.github.fractalo.streaming_settlement.security.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(endpointConfig ->
                        endpointConfig.userService(customOAuth2UserService)));

        http.authorizeHttpRequests(registry -> registry
                .requestMatchers("/login").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}
