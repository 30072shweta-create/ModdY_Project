package com.example.flight.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.flight.config.JwtProperties;
import com.example.flight.entity.User;
import com.example.flight.exception.JwtAuthenticationException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TYPE = "type";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getUserId());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_ROLE, user.getRole());
        claims.put(CLAIM_TYPE, TOKEN_TYPE_ACCESS);

        return buildToken(claims, user.getEmail(), jwtProperties.getAccess().getExpiration());
    }

    // Backwards compatible helper for UserDetails
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_EMAIL, userDetails.getUsername());
        claims.put(CLAIM_TYPE, TOKEN_TYPE_ACCESS);
        return buildToken(claims, userDetails.getUsername(), jwtProperties.getAccess().getExpiration());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiry = new Date(nowMillis + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateAccessToken(String token, String expectedEmail) {
        try {
            Claims claims = extractClaims(token);
            String email = claims.getSubject();
            String tokenType = claims.get(CLAIM_TYPE, String.class);

            if (expectedEmail != null && !expectedEmail.equals(email)) {
                return false;
            }

            if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
                return false;
            }

            return !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return validateAccessToken(token, userDetails.getUsername());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        Object userIdObj = claims.get(CLAIM_USER_ID);
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("JWT token has expired", e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("Invalid JWT token format", e);
        } catch (SignatureException e) {
            throw new JwtAuthenticationException("Invalid JWT signature", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("Unsupported JWT token", e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT claims string is empty", e);
        }
    }

    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private SecretKey getSigningKey() {
        String secret = jwtProperties.getSecret();
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
