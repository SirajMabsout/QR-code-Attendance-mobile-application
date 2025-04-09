package Capstone.QR.controller;

import Capstone.QR.dto.Request.ChangePasswordRequest;
import Capstone.QR.dto.Response.ApiResponse;
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

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            UserResponse profile = userService.getUserProfile(userDetails.getUsername());
            return ResponseEntity.ok(new ApiResponse<>("Profile fetched successfully", profile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>("User not found", null));
        }
    }

    @PostMapping("/upload-profile-icon")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadProfileIcon(@RequestParam("image") MultipartFile image,
                                                                              @RequestParam("userId") Long userId) {
        try {
            ImageUploadResponse response = userService.uploadProfileIcon(userId, image);
            return ResponseEntity.ok(new ApiResponse<>("Image uploaded successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Image upload failed", new ImageUploadResponse("error", e.getMessage())));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(userDetails, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(new ApiResponse<>("Password changed successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null));
        }
    }
}
