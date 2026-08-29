package com.example.flight.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.example.flight.config.JwtProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class CookieService {

    private final JwtProperties jwtProperties;

    public CookieService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        long expirationSeconds = Duration.ofMillis(jwtProperties.getRefresh().getExpiration()).getSeconds();

        return ResponseCookie.from(jwtProperties.getCookie().getName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.getCookie().isSecure())
                .sameSite(jwtProperties.getCookie().getSameSite())
                .path(jwtProperties.getCookie().getPath())
                .maxAge(expirationSeconds)
                .build();
    }

    public ResponseCookie createCleanRefreshTokenCookie() {
        return ResponseCookie.from(jwtProperties.getCookie().getName(), "")
                .httpOnly(true)
                .secure(jwtProperties.getCookie().isSecure())
                .sameSite(jwtProperties.getCookie().getSameSite())
                .path(jwtProperties.getCookie().getPath())
                .maxAge(0)
                .build();
    }

    public String extractRefreshToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String cookieName = jwtProperties.getCookie().getName();
        if (cookieName == null || cookieName.isBlank()) {
            cookieName = "refreshToken";
        }

        // 1. Standard Servlet getCookies()
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie != null && cookie.getName() != null && cookieName.equalsIgnoreCase(cookie.getName().trim())) {
                    if (cookie.getValue() != null && !cookie.getValue().isBlank()) {
                        return cookie.getValue().trim();
                    }
                }
            }
        }

        // 2. Fallback: Parse raw "Cookie" header
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            String[] pairs = cookieHeader.split(";");
            for (String pair : pairs) {
                String[] nameValue = pair.split("=", 2);
                if (nameValue.length == 2) {
                    String name = nameValue[0].trim();
                    String value = nameValue[1].trim();
                    if (cookieName.equalsIgnoreCase(name) && !value.isBlank()) {
                        return value;
                    }
                }
            }
        }

        return null;
    }
}
