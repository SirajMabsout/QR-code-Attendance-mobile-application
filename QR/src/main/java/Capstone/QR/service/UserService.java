package Capstone.QR.service;

import Capstone.QR.model.User;
import Capstone.QR.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // View current user profile
    public User getProfile(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Update profile details (name, image)
    public User updateProfile(UserDetails userDetails, String name, String profileImage) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (profileImage != null && !profileImage.isBlank()) {
            user.setProfileImage(profileImage);
        }

        return userRepository.save(user);
    }

    // Change password
    public void changePassword(UserDetails userDetails, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}


