package com.example.flight;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.example.flight.dto.AuthResponseDTO;
import com.example.flight.dto.LoginRequestDTO;
import com.example.flight.dto.RegisterRequestDTO;
import com.example.flight.entity.RefreshToken;
import com.example.flight.entity.User;
import com.example.flight.exception.InvalidCredentialsException;
import com.example.flight.exception.RefreshTokenReuseException;
import com.example.flight.repository.RefreshTokenRepository;
import com.example.flight.repository.UserRepository;
import com.example.flight.service.AuthService;
import com.example.flight.service.JwtService;
import com.example.flight.service.RefreshTokenService;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
public class SecurityMigrationVerificationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.findByEmail("security_test@example.com").ifPresent(userRepository::delete);

        testUser = User.builder()
                .email("security_test@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .firstName("Test")
                .lastName("User")
                .role("USER")
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("1. Verify User Registration & Password Hashing")
    void testUserRegistration() {
        RegisterRequestDTO registerDTO = new RegisterRequestDTO();
        registerDTO.setEmail("new_user@example.com");
        registerDTO.setPassword("Pass123!");
        registerDTO.setFirstName("New");
        registerDTO.setLastName("User");

        AuthService.AuthResult result = authService.registerUser(registerDTO, "TestAgent", "127.0.0.1");

        assertNotNull(result.getResponseDTO().getAccessToken());
        assertNotNull(result.getRawRefreshToken());

        Optional<User> dbUser = userRepository.findByEmail("new_user@example.com");
        assertTrue(dbUser.isPresent());
        assertTrue(passwordEncoder.matches("Pass123!", dbUser.get().getPasswordHash()));
    }

    @Test
    @DisplayName("2. Verify Login & 15-Minute Access Token + PostgreSQL Hashed Refresh Token")
    void testLoginAndTokenGeneration() {
        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setEmail("security_test@example.com");
        loginDTO.setPassword("Password123!");

        AuthService.AuthResult result = authService.loginUser(loginDTO, "TestDevice", "127.0.0.1");

        AuthResponseDTO responseDTO = result.getResponseDTO();
        assertNotNull(responseDTO.getAccessToken());
        assertEquals("Bearer", responseDTO.getTokenType());
        assertEquals(900L, responseDTO.getExpiresIn()); // 15 min = 900 seconds

        // Validate Access Token claims
        Claims claims = jwtService.extractClaims(responseDTO.getAccessToken());
        assertEquals("security_test@example.com", claims.getSubject());
        assertEquals("USER", claims.get("role"));
        assertEquals("ACCESS", claims.get("type"));

        // Verify Refresh Token is NOT raw in DB, but stored as SHA-256 hash
        String rawRefresh = result.getRawRefreshToken();
        String expectedHash = jwtService.hashToken(rawRefresh);

        Optional<RefreshToken> tokenEntity = refreshTokenRepository.findByTokenHash(expectedHash);
        assertTrue(tokenEntity.isPresent());
        assertEquals(expectedHash, tokenEntity.get().getTokenHash());
        assertFalse(tokenEntity.get().isRevoked());
    }

    @Test
    @DisplayName("3. Verify Invalid Credentials Rejection")
    void testInvalidCredentials() {
        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setEmail("security_test@example.com");
        loginDTO.setPassword("WrongPassword");

        assertThrows(InvalidCredentialsException.class, () -> authService.loginUser(loginDTO, "TestDevice", "127.0.0.1"));
    }

    @Test
    @DisplayName("4. Verify Refresh Token Rotation (Valid Token -> Revokes Old, Issues New)")
    void testRefreshTokenRotation() {
        AuthService.AuthResult loginResult = authService.loginUser(
                new LoginRequestDTO("security_test@example.com", "Password123!"),
                "Device1",
                "127.0.0.1"
        );

        String oldRawRefresh = loginResult.getRawRefreshToken();
        String oldHash = jwtService.hashToken(oldRawRefresh);

        // Perform Rotation
        AuthService.AuthResult rotateResult = authService.refreshAccessToken(oldRawRefresh, "Device1", "127.0.0.1");

        assertNotNull(rotateResult.getResponseDTO().getAccessToken());
        String newRawRefresh = rotateResult.getRawRefreshToken();
        assertFalse(oldRawRefresh.equals(newRawRefresh));

        entityManager.clear(); // Clear persistence context to fetch fresh DB state

        // Verify Old Token is now REVOKED in PostgreSQL
        Optional<RefreshToken> oldEntity = refreshTokenRepository.findByTokenHash(oldHash);
        assertTrue(oldEntity.isPresent());
        assertTrue(oldEntity.get().isRevoked());

        // Verify New Token is active in PostgreSQL
        String newHash = jwtService.hashToken(newRawRefresh);
        Optional<RefreshToken> newEntity = refreshTokenRepository.findByTokenHash(newHash);
        assertTrue(newEntity.isPresent());
        assertFalse(newEntity.get().isRevoked());
        assertEquals(oldEntity.get().getFamilyId(), newEntity.get().getFamilyId());
    }

    @Test
    @DisplayName("5. Verify Refresh Token Reuse Detection (Revokes Entire Family)")
    void testRefreshTokenReuseDetection() {
        AuthService.AuthResult loginResult = authService.loginUser(
                new LoginRequestDTO("security_test@example.com", "Password123!"),
                "Device1",
                "127.0.0.1"
        );
        String tokenA = loginResult.getRawRefreshToken();

        // Rotate tokenA -> returns tokenB
        AuthService.AuthResult rotateResult = authService.refreshAccessToken(tokenA, "Device1", "127.0.0.1");
        String tokenB = rotateResult.getRawRefreshToken();

        // Attempt REUSE of tokenA (which is already revoked)
        boolean reuseCaught = false;
        try {
            refreshTokenService.rotateRefreshToken(tokenA, "Device1", "127.0.0.1");
        } catch (RefreshTokenReuseException e) {
            reuseCaught = true;
        }
        assertTrue(reuseCaught, "Expected RefreshTokenReuseException on token reuse");

        entityManager.clear(); // Clear L1 cache so Hibernate re-selects updated revoked column from DB

        // Verify that tokenB (and all tokens in the family) were revoked as result of reuse detection!
        String tokenBHash = jwtService.hashToken(tokenB);
        Optional<RefreshToken> tokenBEntity = refreshTokenRepository.findByTokenHash(tokenBHash);
        assertTrue(tokenBEntity.isPresent());
        assertTrue(tokenBEntity.get().isRevoked());
    }

    @Test
    @DisplayName("6. Verify Logout Invalidates Refresh Token")
    void testLogout() {
        AuthService.AuthResult loginResult = authService.loginUser(
                new LoginRequestDTO("security_test@example.com", "Password123!"),
                "Device1",
                "127.0.0.1"
        );
        String rawRefresh = loginResult.getRawRefreshToken();
        String hash = jwtService.hashToken(rawRefresh);

        authService.logout(rawRefresh);

        entityManager.clear();

        Optional<RefreshToken> entity = refreshTokenRepository.findByTokenHash(hash);
        assertTrue(entity.isPresent());
        assertTrue(entity.get().isRevoked());
    }

    @Test
    @DisplayName("7. Verify Logout-All Invalidates All User Tokens")
    void testLogoutAll() {
        AuthService.AuthResult login1 = authService.loginUser(new LoginRequestDTO("security_test@example.com", "Password123!"), "Device1", "127.0.0.1");
        AuthService.AuthResult login2 = authService.loginUser(new LoginRequestDTO("security_test@example.com", "Password123!"), "Device2", "127.0.0.1");

        authService.logoutAllDevices("security_test@example.com");

        entityManager.clear();

        String hash1 = jwtService.hashToken(login1.getRawRefreshToken());
        String hash2 = jwtService.hashToken(login2.getRawRefreshToken());

        Optional<RefreshToken> entity1 = refreshTokenRepository.findByTokenHash(hash1);
        Optional<RefreshToken> entity2 = refreshTokenRepository.findByTokenHash(hash2);

        assertTrue(entity1.isPresent() && entity1.get().isRevoked());
        assertTrue(entity2.isPresent() && entity2.get().isRevoked());
    }
}
