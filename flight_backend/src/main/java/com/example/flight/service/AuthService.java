package com.example.flight.service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.flight.config.JwtProperties;
import com.example.flight.dto.AuthResponseDTO;
import com.example.flight.dto.ForgotPasswordRequestDTO;
import com.example.flight.dto.GoogleLoginRequestDTO;
import com.example.flight.dto.LoginRequestDTO;
import com.example.flight.dto.RefreshTokenResponseDTO;
import com.example.flight.dto.RegisterRequestDTO;
import com.example.flight.dto.ResetPasswordRequestDTO;
import com.example.flight.dto.UserResponseDTO;
import com.example.flight.dto.VerifyEmailRequestDTO;
import com.example.flight.entity.EmailOtp;
import com.example.flight.entity.OtpType;
import com.example.flight.entity.User;
import com.example.flight.exception.InvalidCredentialsException;
import com.example.flight.exception.UserAlreadyExistsException;
import com.example.flight.repository.EmailOtpRepository;
import com.example.flight.repository.UserRepository;
import com.example.flight.security.GoogleTokenVerifier;
import com.example.flight.security.GoogleUserInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailOtpRepository emailOtpRepository;
    private final EmailService emailService;

    @Getter
    @AllArgsConstructor
    public static class AuthResult {
        private final AuthResponseDTO responseDTO;
        private final String rawRefreshToken;
    }

    // ================= REGISTER =================

    @Transactional
    public AuthResult registerUser(RegisterRequestDTO request, String deviceInfo, String ipAddress) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with email '" + request.getEmail() + "' already exists.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role("USER")
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        // Send verification OTP
        try {
            sendEmailVerificationOtp(savedUser.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send email verification OTP to {}", savedUser.getEmail(), ex);
            // Log but don't fail registration if mail host is unreachable in local test
        }

        return createAuthResult(savedUser, null, deviceInfo, ipAddress);
    }

    @Transactional
    public String register(RegisterRequestDTO request) {
        registerUser(request, null, null);
        return "Registration successful. OTP sent to your email.";
    }

    // ================= LOGIN =================

    @Transactional
    public AuthResult loginUser(LoginRequestDTO request, String deviceInfo, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email or password combination.", ex);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password combination."));

        return createAuthResult(user, null, deviceInfo, ipAddress);
    }

    public RefreshTokenResponseDTO login(LoginRequestDTO request) {
        AuthResult result = loginUser(request, null, null);
        return new RefreshTokenResponseDTO(result.getResponseDTO().getAccessToken(), result.getRawRefreshToken());
    }

    // ================= GOOGLE LOGIN =================

    @Transactional
    public AuthResult googleLogin(GoogleLoginRequestDTO request, String deviceInfo, String ipAddress) {
        GoogleUserInfo googleUser = googleTokenVerifier.verifyToken(request.getIdToken());

        User user = userRepository.findByEmail(googleUser.getEmail()).orElseGet(() -> {
            User newUser = User.builder()
                    .firstName(googleUser.getFirstName())
                    .lastName(googleUser.getLastName())
                    .email(googleUser.getEmail())
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role("USER")
                    .emailVerified(true)
                    .build();
            return userRepository.save(newUser);
        });

        return createAuthResult(user, null, deviceInfo, ipAddress);
    }

    // ================= REFRESH TOKEN ROTATION =================

    @Transactional
    public AuthResult refreshAccessToken(String rawRefreshToken, String deviceInfo, String ipAddress) {
        RefreshTokenService.RotationResult rotationResult = refreshTokenService.rotateRefreshToken(rawRefreshToken, deviceInfo, ipAddress);
        User user = rotationResult.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);
        UserResponseDTO userDTO = mapToUserResponse(user);

        AuthResponseDTO responseDTO = AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccess().getExpiration() / 1000)
                .user(userDTO)
                .build();

        return new AuthResult(responseDTO, rotationResult.getRawRefreshToken());
    }

    // ================= LOGOUT & LOGOUT ALL =================

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.logout(rawRefreshToken);
        }
    }

    @Transactional
    public void logoutAllDevices(String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            userRepository.findByEmail(userEmail).ifPresent(user -> {
                refreshTokenService.logoutAllUserTokens(user.getUserId());
            });
        }
    }

    // ================= GET CURRENT USER =================

    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found for email: " + email));
        return mapToUserResponse(user);
    }

    // ================= HELPER METHODS =================

    private AuthResult createAuthResult(User user, String familyId, String deviceInfo, String ipAddress) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenService.RotationResult tokenResult = refreshTokenService.createRefreshToken(user, familyId, deviceInfo, ipAddress);

        UserResponseDTO userResponse = mapToUserResponse(user);

        AuthResponseDTO authResponse = AuthResponseDTO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccess().getExpiration() / 1000)
                .user(userResponse)
                .build();

        return new AuthResult(authResponse, tokenResult.getRawRefreshToken());
    }

    private UserResponseDTO mapToUserResponse(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    // ================= EMAIL VERIFICATION =================

    public String sendEmailVerificationOtp(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();

        EmailOtp emailOtp = EmailOtp.builder()
                .email(email)
                .otp(otp)
                .type(OtpType.EMAIL_VERIFICATION)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        emailOtpRepository.save(emailOtp);
        try {
            emailService.sendOtpEmail(email, otp, "EMAIL_VERIFICATION");
        } catch (Exception ex) {
            log.warn("Failed to send email verification OTP to {}", email, ex);
            // Log but don't fail transaction if mail host is unreachable in local test environment
        }

        return "Verification OTP sent";
    }

    public String verifyEmail(VerifyEmailRequestDTO request) {
        EmailOtp otp = emailOtpRepository
                .findTopByEmailAndTypeAndUsedFalseOrderByIdDesc(request.getEmail(), OtpType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otp.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        otp.setUsed(true);
        emailOtpRepository.save(otp);

        return "Email verified successfully";
    }

    // ================= FORGOT PASSWORD =================

    public String forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();

        EmailOtp emailOtp = EmailOtp.builder()
                .email(user.getEmail())
                .otp(otp)
                .type(OtpType.PASSWORD_RESET)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        emailOtpRepository.save(emailOtp);
        try {
            emailService.sendOtpEmail(user.getEmail(), otp, "PASSWORD_RESET");
        } catch (Exception ex) {
            log.warn("Failed to send password reset OTP to {}", user.getEmail(), ex);
            // Log but don't fail transaction if mail host is unreachable in local test environment
        }

        return "Password reset OTP sent to your email";
    }

    // ================= RESET PASSWORD =================

    public String resetPassword(ResetPasswordRequestDTO request) {
        EmailOtp otp = emailOtpRepository
                .findTopByEmailAndTypeAndUsedFalseOrderByIdDesc(request.getEmail(), OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otp.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otp.setUsed(true);
        emailOtpRepository.save(otp);

        return "Password reset successfully";
    }

    // ================= OTP GENERATOR =================

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
