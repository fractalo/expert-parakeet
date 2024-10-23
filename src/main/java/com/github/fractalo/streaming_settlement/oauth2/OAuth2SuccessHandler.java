package com.github.fractalo.streaming_settlement.oauth2;

import com.github.fractalo.streaming_settlement.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.github.fractalo.streaming_settlement.jwt.JwtConstants.ACCESS_TOKEN_COOKIE_NAME;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final String frontendBaseUrl;

    public OAuth2SuccessHandler(JWTUtil jwtUtil,
                                @Value("${frontend.base-url}") String frontendBaseUrl) {
        this.jwtUtil = jwtUtil;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User userDetail = (CustomOAuth2User) authentication.getPrincipal();

        Long memberId = userDetail.getMemberId();

        String accessToken = jwtUtil.createAccessToken(memberId, 10 * 60 * 1000L);

        response.addCookie(createCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken));

        if (!frontendBaseUrl.isBlank()) {
            response.sendRedirect(frontendBaseUrl);
        }
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(10 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}
