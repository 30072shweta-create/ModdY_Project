package com.example.flight.controller;

import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.AuthResponseDTO;
import com.example.flight.dto.ForgotPasswordRequestDTO;
import com.example.flight.dto.GoogleLoginRequestDTO;
import com.example.flight.dto.LoginRequestDTO;
import com.example.flight.dto.RefreshTokenRequestDTO;
import com.example.flight.dto.RegisterRequestDTO;
import com.example.flight.dto.ResetPasswordRequestDTO;
import com.example.flight.dto.UserResponseDTO;
import com.example.flight.dto.VerifyEmailRequestDTO;
import com.example.flight.security.CookieService;
import com.example.flight.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    // REGISTER

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest servletRequest) {

        AuthService.AuthResult result = authService.registerUser(
                request,
                userAgent,
                servletRequest.getRemoteAddr()
        );

        ResponseCookie cookie = cookieService.createRefreshTokenCookie(result.getRawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getResponseDTO());
    }

    // LOGIN

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest servletRequest) {

        AuthService.AuthResult result = authService.loginUser(
                request,
                userAgent,
                servletRequest.getRemoteAddr()
        );

        ResponseCookie cookie = cookieService.createRefreshTokenCookie(result.getRawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getResponseDTO());
    }

    // GOOGLE LOGIN

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> googleLogin(
            @Valid @RequestBody GoogleLoginRequestDTO request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest servletRequest) {

        AuthService.AuthResult result = authService.googleLogin(
                request,
                userAgent,
                servletRequest.getRemoteAddr()
        );

        ResponseCookie cookie = cookieService.createRefreshTokenCookie(result.getRawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getResponseDTO());
    }

    // REFRESH TOKEN

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponseDTO> refreshToken(
            @RequestBody(required = false) RefreshTokenRequestDTO requestDTO,
            HttpServletRequest servletRequest,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        String rawToken = cookieService.extractRefreshToken(servletRequest);
        if ((rawToken == null || rawToken.isBlank()) && requestDTO != null) {
            rawToken = requestDTO.getRefreshToken();
        }

        AuthService.AuthResult result = authService.refreshAccessToken(
                rawToken,
                userAgent,
                servletRequest.getRemoteAddr()
        );

        ResponseCookie cookie = cookieService.createRefreshTokenCookie(result.getRawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getResponseDTO());
    }

    // LOGOUT

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest servletRequest) {
        String rawToken = cookieService.extractRefreshToken(servletRequest);
        authService.logout(rawToken);

        ResponseCookie cleanCookie = cookieService.createCleanRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body("Logged out successfully.");
    }

    // LOGOUT ALL DEVICES

    @PostMapping("/logout-all")
    public ResponseEntity<String> logoutAll(Principal principal) {
        if (principal != null) {
            authService.logoutAllDevices(principal.getName());
        }

        ResponseCookie cleanCookie = cookieService.createCleanRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body("Logged out from all devices successfully.");
    }

    // CURRENT USER PROFILE

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.getCurrentUser(principal.getName()));
    }

    // VERIFY EMAIL

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @Valid @RequestBody VerifyEmailRequestDTO request) {

        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    // RESEND VERIFICATION OTP

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(
            @RequestParam String email) {

        return ResponseEntity.ok(authService.sendEmailVerificationOtp(email));
    }

    // FORGOT PASSWORD

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // RESET PASSWORD

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        return ResponseEntity.ok(authService.resetPassword(request));
    }
}