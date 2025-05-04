package Capstone.QR.controller;

import Capstone.QR.dto.Request.RegisterRequest;
import Capstone.QR.model.Role;
import Capstone.QR.model.User;
import Capstone.QR.repository.PasswordResetTokenRepository;
import Capstone.QR.repository.UserRepository;
import Capstone.QR.security.jwt.JwtUtil;
import Capstone.QR.service.EmailService;
import Capstone.QR.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private EmailService emailService;
    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void register_ShouldReturn201_WhenNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest("Ali", "ali@test.com", "pass123", Role.STUDENT);
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_ShouldReturnConflict_WhenEmailExists() throws Exception {
        RegisterRequest request = new RegisterRequest("Ali", "ali@test.com", "pass123", Role.STUDENT);
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
