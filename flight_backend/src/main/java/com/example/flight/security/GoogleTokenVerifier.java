package com.example.flight.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.flight.exception.GoogleAuthenticationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private final String googleClientId;

    public GoogleTokenVerifier(@Value("${google.client.id:312887704650-uilh510uio68aquinkb3lb2gc12htmm3.apps.googleusercontent.com}") String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public GoogleUserInfo verifyToken(String idTokenString) {
        try {
            List<String> allowedAudiences = new ArrayList<>();
            if (googleClientId != null && !googleClientId.isBlank()) {
                for (String id : googleClientId.split(",")) {
                    if (!id.trim().isEmpty() && !allowedAudiences.contains(id.trim())) {
                        allowedAudiences.add(id.trim());
                    }
                }
            }

            // Always accept known frontend client IDs in this project
            String frontendId1 = "312887704650-uilh510uio68aquinkb3lb2gc12htmm3.apps.googleusercontent.com";
            String backendId1 = "794346247898-hognvlbo4iubhh7mmjdgdp7fugpvkduc.apps.googleusercontent.com";
            String fallbackId = "92274469042-idom1lfs8nhao9hbs4i6jofla6rpopqu.apps.googleusercontent.com";

            if (!allowedAudiences.contains(frontendId1)) allowedAudiences.add(frontendId1);
            if (!allowedAudiences.contains(backendId1)) allowedAudiences.add(backendId1);
            if (!allowedAudiences.contains(fallbackId)) allowedAudiences.add(fallbackId);

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(allowedAudiences)
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Google ID token verification returned null. Allowed audiences: {}", allowedAudiences);
                throw new GoogleAuthenticationException("Invalid or forged Google ID token. Please check that your Google Client ID is configured correctly.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            if (firstName == null || firstName.isBlank()) {
                firstName = (String) payload.get("name");
            }
            if (firstName == null || firstName.isBlank()) {
                firstName = "Google";
            }
            if (lastName == null) {
                lastName = "User";
            }

            if (email == null || email.isBlank()) {
                throw new GoogleAuthenticationException("Email not found in Google ID token payload.");
            }

            return GoogleUserInfo.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .emailVerified(emailVerified)
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to verify Google ID token", e);
            throw new GoogleAuthenticationException("Failed to verify Google ID token: " + e.getMessage(), e);
        }
    }
}
