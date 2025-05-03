package Capstone.QR.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // true = HTML
        helper.setFrom("siraj.almabsout@lau.edu");

        mailSender.send(message);
    }

    public String buildVerificationEmail(String name, String verificationUrl) {
        return """
    <!DOCTYPE html>
    <html>
    <head>
        <style>
            body {
                font-family: Arial, sans-serif;
                background-color: #f2f4f8;
                color: #333;
                padding: 20px;
            }
            .container {
                max-width: 600px;
                margin: 0 auto;
                background-color: #ffffff;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            }
            .button {
                display: inline-block;
                padding: 12px 24px;
                margin-top: 20px;
                font-size: 16px;
                color: white;
                background-color: #1976d2;
                text-decoration: none;
                border-radius: 6px;
            }
            .footer {
                margin-top: 30px;
                font-size: 12px;
                color: #999;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <h2>Hello, %s!</h2>
            <p>Thanks for registering. Please click the button below to verify your email address and complete your account setup.</p>
            <a class="button" href="%s">Verify Email</a>
            <p class="footer">If you didn’t sign up for this account, please ignore this email.</p>
        </div>
    </body>
    </html>
    """.formatted(name, verificationUrl);
    }
}
