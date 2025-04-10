package Capstone.QR.service;

import Capstone.QR.dto.Response.ImageUploadResponse;
import Capstone.QR.dto.Response.UserResponse;
import Capstone.QR.model.User;
import Capstone.QR.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImgurImageService imgurImageService;

        public ImageUploadResponse uploadProfileIcon(String email, MultipartFile image) throws IOException, InterruptedException {
            String imageUrl = imgurImageService.uploadImage(image);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NoSuchElementException("User not found"));

            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            return new ImageUploadResponse("success", imageUrl);
        }

        public UserResponse getUserProfile(String email) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NoSuchElementException("User not found"));

            return new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getProfileImageUrl(),
                    user.getRole().name() // Assuming enum or Role object with .name()
            );
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


