package Capstone.QR.service;

import Capstone.QR.model.PasswordResetToken;
import Capstone.QR.model.User;
import Capstone.QR.repository.PasswordResetTokenRepository;
import Capstone.QR.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

        // Expire in 10 minutes
        Date expiry = new Date(System.currentTimeMillis() + 1000 * 60 * 10);

        // Remove old token if exists
        tokenRepository.deleteByEmail(email);

        // Save new token
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setCode(code);
        token.setUser(user);
        token.setExpiryDate(expiry);
        tokenRepository.save(token);

        // Build beautiful HTML email
        String body = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background-color: #f7f7f7; padding: 20px; }
    .container { max-width: 500px; margin: auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
    .title { font-size: 20px; font-weight: bold; color: #333; margin-bottom: 20px; }
    .code-box { font-size: 28px; font-weight: bold; background-color: #f0f0f0; padding: 12px 20px; border-radius: 6px; display: inline-block; letter-spacing: 4px; }
    .footer { margin-top: 30px; font-size: 12px; color: #777; }
  </style>
</head>
<body>
  <div class="container">
    <div class="title">Your Password Reset Code</div>
    <p>Here is your code to reset your password:</p>
    <div class="code-box">%s</div>
    <p>This code will expire in 10 minutes. If you didn’t request this, you can ignore this email.</p>
    <div class="footer">Capstone QR Attendance System</div>
  </div>
</body>
</html>
""".formatted(code);

        try {
            emailService.send(email, "Reset Your Password", body);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send reset email", e);
        }
    }


    public boolean verifyResetCode(String email, String code) {
        PasswordResetToken token = tokenRepository.findByEmailAndCode(email, code)
                .orElseThrow(() -> new RuntimeException("Invalid code"));

        if (token.getExpiryDate().before(new Date())) {
            throw new RuntimeException("Code expired");
        }
        if (token.isVerified()) {
            throw new RuntimeException("Code has already been verified");
        }

        token.setVerified(true); // ✅ mark as used
        tokenRepository.save(token);

        return true;
    }

    public void resetPassword(String email, String code, String newPassword) {
        PasswordResetToken token = tokenRepository.findByEmailAndCode(email, code)
                .orElseThrow(() -> new RuntimeException("Invalid code"));

        if (!token.isVerified()) {
            throw new RuntimeException("Code not verified yet");
        }

        if (token.getExpiryDate().before(new Date())) {
            throw new RuntimeException("Code expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(token); // ✅ one-time use
    }

}
