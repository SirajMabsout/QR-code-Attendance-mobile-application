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

    public void sendResetCode(String email) {
        // Step 1: generate and save token (within transaction)
        PasswordResetToken token = generateAndSaveResetToken(email);

        // Step 2: send email (outside transaction)
        sendResetEmail(email, token.getCode());
    }

    @Transactional
    public PasswordResetToken generateAndSaveResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String code = String.format("%06d", new Random().nextInt(999999));
        Date expiry = new Date(System.currentTimeMillis() + 1000 * 60 * 10);

        tokenRepository.deleteByEmail(email);

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setCode(code);
        token.setUser(user);
        token.setExpiryDate(expiry);

        return tokenRepository.save(token);
    }

    public void sendResetEmail(String email, String code) {
        String body = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            background-color: #f5f5f5;
            margin: 0;
            padding: 0;
            line-height: 1.6;
        }
        .container {
            max-width: 600px;
            margin: 20px auto;
            background-color: #ffffff;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        .header {
            background-color: #2196F3;
            padding: 30px;
            text-align: center;
        }
        .header h1 {
            color: #ffffff;
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }
        .content {
            padding: 40px 30px;
            text-align: center;
        }
        .message {
            color: #333333;
            font-size: 16px;
            margin-bottom: 30px;
        }
        .code-container {
            background-color: #f8f9fa;
            border-radius: 8px;
            padding: 20px;
            margin: 20px 0;
        }
        .verification-code {
            font-size: 32px;
            font-weight: bold;
            color: #2196F3;
            letter-spacing: 8px;
            margin: 10px 0;
        }
        .expiry {
            color: #666666;
            font-size: 14px;
            margin-top: 20px;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            border-top: 1px solid #eeeeee;
        }
        .footer p {
            color: #666666;
            font-size: 14px;
            margin: 0;
        }
        .logo {
            color: #2196F3;
            font-weight: bold;
            font-size: 18px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Verification Code</h1>
        </div>
        <div class="content">
            <div class="message">
                Please use the following code to verify your account:
            </div>
            <div class="code-container">
                <div class="verification-code">%s</div>
            </div>
            <div class="expiry">
                This code will expire in 10 minutes.<br>
                If you didn't request this code, please ignore this email.
            </div>
        </div>
        <div class="footer">
            <p>© 2024 <span class="logo">QR Attendance System</span></p>
        </div>
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
