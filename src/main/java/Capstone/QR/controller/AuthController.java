package Capstone.QR.controller;

import Capstone.QR.dto.Request.*;
import Capstone.QR.model.*;
import Capstone.QR.repository.*;
import Capstone.QR.security.jwt.JwtUtil;
import Capstone.QR.service.EmailService;
import Capstone.QR.service.PasswordResetService;
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
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final TeacherRepository teacherRepository;
    private final PasswordResetService resetService;

    // === Register ===
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered.");
        }

        Role role = request.getRole();
        User user;

        switch (role) {
            case ADMIN:
                user = new Admin();
                break;

            case TEACHER:
                user = new Teacher();
                break;

            case STUDENT:
                user = new Student();
                break;

            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid role specified.");
        }

        // Common fields for all users
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        userRepository.save(user);  // will insert into correct subclass table

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
                    .maxAge(60*60 )
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




    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        resetService.sendResetCode(request.getEmail());
        return ResponseEntity.ok("Reset code sent");
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody @Valid VerifyCodeRequest request) {
        resetService.verifyResetCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok("Code verified");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        resetService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()

        );
        return ResponseEntity.ok("Password reset successfully");
    }

    }






