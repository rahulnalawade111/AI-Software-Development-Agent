package dev.aidev.auth;

import dev.aidev.security.AppPrincipal;
import dev.aidev.security.JwtService;
import dev.aidev.user.User;
import dev.aidev.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(min = 1, max = 100) String name) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, Long id, String email, String name, String role) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        User user = authService.register(req.email(), req.password(), req.name());
        String role = primaryRole(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail(), role);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), role);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return userRepository.findByEmail(req.email())
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .map(user -> {
                    String role = primaryRole(user);
                    String token = jwtService.generateToken(user.getId(), user.getEmail(), role);
                    return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), role));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal AppPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", principal.userId());
        body.put("email", principal.email());
        body.put("role", principal.role());
        return ResponseEntity.ok(body);
    }

    private String primaryRole(User user) {
        return user.getRoles().stream()
                .filter(r -> "SUPER_ADMIN".equals(r.getName()))
                .map(r -> r.getName())
                .findFirst()
                .orElseGet(() -> user.getRoles().stream().findFirst().map(r -> r.getName()).orElse("USER"));
    }
}
