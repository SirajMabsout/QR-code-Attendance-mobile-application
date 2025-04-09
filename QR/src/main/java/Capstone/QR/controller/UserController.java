package Capstone.QR.controller;

import Capstone.QR.dto.Request.ChangePasswordRequest;
import Capstone.QR.dto.Response.ImageUploadResponse;
import Capstone.QR.dto.Response.UserResponse;
import Capstone.QR.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ✅ 1. Get current authenticated user's profile
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(userService.getUserProfile(userDetails.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ✅ 2. Upload profile image and update user record
    @PostMapping("/upload-profile-icon")
    public ResponseEntity<ImageUploadResponse> uploadProfileIcon(@RequestParam("image") MultipartFile image,
                                                                    @RequestParam("userId") Long userId) {
        try {
            ImageUploadResponse response = userService.uploadProfileIcon(userId, image);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImageUploadResponse("error", e.getMessage()));
        }
    }

    // ✅ 3. Change password
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(userDetails, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
