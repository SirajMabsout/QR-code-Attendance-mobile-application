package Capstone.QR.controller;

import Capstone.QR.dto.Request.ForgotPasswordRequest;
import Capstone.QR.dto.Request.LoginRequest;
import Capstone.QR.dto.Request.RegisterRequest;
import Capstone.QR.dto.Request.ResetPasswordRequest;
import Capstone.QR.model.*;
import Capstone.QR.repository.PasswordResetTokenRepository;
import Capstone.QR.repository.TeacherRepository;
import Capstone.QR.repository.UserRepository;
import Capstone.QR.security.jwt.JwtUtil;
import Capstone.QR.service.EmailService;
import Capstone.QR.service.RefreshTokenService;
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
import java.util.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final TeacherRepository teacherRepository;

    // === Register ===
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered.");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully.");
    }

    // === Login (set HttpOnly cookies) ===
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            // ✅ This authenticates email & password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ BLOCK unapproved teachers
            if (user.getRole() == Role.TEACHER) {
                Teacher teacher = teacherRepository.findById(user.getId())
                        .orElseThrow(() -> new RuntimeException("Teacher profile not found"));

                if (!teacher.isApproved()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Your teacher account is pending admin approval.");
                }
            }

            // ✅ Generate tokens and cookies
            String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            String refreshToken = refreshTokenService.createRefreshToken(user);

            ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                    .httpOnly(true)
                    .secure(false) // Set to true in production
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

            return ResponseEntity.ok("Login was successful");

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }
    }

    // === Refresh Access Token using cookie ===
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
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
            return ResponseEntity.badRequest().body("Refresh token is missing.");
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
            return ResponseEntity.ok("Access token refreshed");

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // === Logout (clear cookies + revoke refresh token) ===
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse response) {
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

        return ResponseEntity.ok("Logged out successfully");
    }

    // === Forgot Password ===
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiry(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(resetToken);

        String link = "http://yourfrontend.com/reset-password?token=" + token;
        emailService.send(email, "Reset your password", "Click to reset: " + link);

        return ResponseEntity.ok("Reset link sent to email.");
    }


    // === Reset Password ===
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        String newPassword = request.getPassword();

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).body("Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok("Password reset successful.");
    }

}
