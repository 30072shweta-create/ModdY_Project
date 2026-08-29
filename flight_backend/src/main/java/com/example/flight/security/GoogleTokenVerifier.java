package com.example.flight.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.flight.exception.GoogleAuthenticationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
public class GoogleTokenVerifier {

    private final String googleClientId;

    public GoogleTokenVerifier(@Value("${google.client.id:92274469042-idom1lfs8nhao9hbs4i6jofla6rpopqu.apps.googleusercontent.com}") String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public GoogleUserInfo verifyToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new GoogleAuthenticationException("Invalid or forged Google ID token.");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");

            if (firstName == null || firstName.isBlank()) {
                firstName = (String) payload.get("name");
            }
            if (lastName == null) {
                lastName = "";
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
            throw new GoogleAuthenticationException("Failed to verify Google ID token: " + e.getMessage(), e);
        }
    }
}
