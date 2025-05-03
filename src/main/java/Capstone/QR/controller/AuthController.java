package Capstone.QR.controller;

import Capstone.QR.dto.Request.*;
import Capstone.QR.dto.Response.ApiResponse;
import Capstone.QR.dto.Response.LoginResponse;
import Capstone.QR.model.*;
import Capstone.QR.repository.*;
import Capstone.QR.security.jwt.JwtUtil;
import Capstone.QR.service.EmailService;
import Capstone.QR.service.PasswordResetService;
import Capstone.QR.service.RefreshTokenService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final TeacherRepository teacherRepository;
    private final PasswordResetService resetService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegisterRequest request) throws MessagingException {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>("Email already registered", null));
        }

        if (request.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>("Admin registration is not allowed", null));
        }

        // Generate token and save temporary data
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verification = new EmailVerificationToken();
        verification.setEmail(request.getEmail());
        verification.setName(request.getName());
        verification.setEncodedPassword(passwordEncoder.encode(request.getPassword()));
        verification.setRole(request.getRole());
        verification.setToken(token);
        verification.setCreatedAt(LocalDateTime.now());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        emailVerificationTokenRepository.save(verification);

        // Send email
        String verificationLink = "https://qr-backend.azurewebsites.net/auth/verify-email?token=" + token;
        String emailBody = emailService.buildVerificationEmail(request.getName(), verificationLink);
        emailService.send(request.getEmail(), "Verify Your Email", emailBody);


        return ResponseEntity.ok(new ApiResponse<>("Verification email is  sent to your mail box. Verify the account in order to use it", null));
    }


    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        EmailVerificationToken verification = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(new ApiResponse<>("Token expired", null));
        }

        User user = switch (verification.getRole()) {
            case STUDENT -> new Student();
            case TEACHER -> new Teacher();
            default -> throw new RuntimeException("Invalid role");
        };

        user.setName(verification.getName());
        user.setEmail(verification.getEmail());
        user.setPassword(verification.getEncodedPassword());
        user.setRole(verification.getRole());

        userRepository.save(user);
        emailVerificationTokenRepository.deleteByToken(token);

        return ResponseEntity.ok(new ApiResponse<>("Email verified and account created, You may log in now.", null));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            // 1. Check if user exists
            Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

            if (optionalUser.isEmpty()) {
                // 2. If user is not found, check if email has pending verification
                boolean hasPendingVerification = emailVerificationTokenRepository
                        .findByEmail(request.getEmail())
                        .isPresent();

                if (hasPendingVerification) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ApiResponse<>("Account needs to be verified before logging in.", null));
                }

                // 3. If no user and no verification token, respond with email not found
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>("Email not registered.", null));
            }

            User user = optionalUser.get();

            // 4. Authenticate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 5. Check teacher approval
            if (user.getRole() == Role.TEACHER) {
                Teacher teacher = teacherRepository.findById(user.getId())
                        .orElseThrow(() -> new RuntimeException("Teacher profile not found"));
                if (!teacher.isApproved()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ApiResponse<>("Your teacher account is pending admin approval.", null));
                }
            }

            // 6. Generate tokens
            String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            String refreshToken = refreshTokenService.createRefreshToken(user);

            ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(60 * 60)
                    .sameSite("Strict")
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(60 * 60 * 24 * 30)
                    .sameSite("Strict")
                    .build();

            response.addHeader("Set-Cookie", accessCookie.toString());
            response.addHeader("Set-Cookie", refreshCookie.toString());

            // 7. Return success response
            LoginResponse loginResponse = new LoginResponse("Login was successful", user.getRole().name(), user.getName(), user.getId());
            return ResponseEntity.ok(new ApiResponse<>("Login was successful", loginResponse));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>("Incorrect password.", null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Refresh token is missing.", null));
        }

        try {
            RefreshToken validToken = refreshTokenService.validateRefreshToken(refreshToken);
            User user = validToken.getUser();
            String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

            ResponseCookie newAccess = ResponseCookie.from("access_token", newAccessToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(60 * 60)
                    .sameSite("Strict")
                    .build();

            response.addHeader("Set-Cookie", newAccess.toString());
            return ResponseEntity.ok(new ApiResponse<>("Access token refreshed", null));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse response) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenService.revokeRefreshToken(user);

        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());

        return ResponseEntity.ok(new ApiResponse<>("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        resetService.sendResetCode(request.getEmail());
        return ResponseEntity.ok(new ApiResponse<>("Reset code sent", null));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<ApiResponse<String>> verifyResetCode(@RequestBody @Valid VerifyCodeRequest request) {
        resetService.verifyResetCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(new ApiResponse<>("Code verified", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        resetService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(new ApiResponse<>("Password reset successfully", null));
    }



}
