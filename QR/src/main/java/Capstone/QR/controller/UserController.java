package Capstone.QR.controller;


import Capstone.QR.dto.Request.ChangePasswordRequest;
import Capstone.QR.dto.Request.UpdateProfileRequest;
import Capstone.QR.model.User;
import Capstone.QR.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. Get current user profile
    @GetMapping("/profile")
    public User getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getProfile(userDetails);
    }

    // 2. Update name or profile image
    @PutMapping("/update")
    public User updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userDetails, request.getName(), request.getProfileImage());
    }

    // 3. Change password
    @PutMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails, request.getOldPassword(), request.getNewPassword());
    }

    // DTOs for input



}
