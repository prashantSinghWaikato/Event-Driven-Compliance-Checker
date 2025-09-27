package nz.compliscan.api.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import nz.compliscan.api.security.JwtUtil;
import nz.compliscan.api.users.UsersRepo;
import nz.compliscan.api.users.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class AuthController {

    private final UsersRepo usersRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    // Comma-separated roles to assign on signup (defaults to ALL for demo)
    private final String signupRolesCsv;

    public AuthController(
            UsersRepo usersRepo,
            PasswordEncoder encoder,
            JwtUtil jwt,
            @Value("${app.security.signupRoles:ADMIN,ANALYST,UPLOADER,VIEWER}") String signupRolesCsv) {
        this.usersRepo = usersRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.signupRolesCsv = signupRolesCsv;
    }

    // ===== DTOs =====
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 8, max = 200) String password,
            @NotBlank @Size(min = 2, max = 128) String name,
            @Email String email) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UserDto(String username, String name, String email, List<String> roles) {
    }

    public record LoginResponse(String token, UserDto user) {
    }

    // ===== Register (public) =====
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        String uname = req.username().trim().toLowerCase(Locale.ROOT);
        String email = req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);

        if (usersRepo.existsByUsername(uname)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
        }
        if (!email.isBlank() && usersRepo.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already in use"));
        }

        String hash = encoder.encode(req.password());

        // Parse roles from config; for demo defaults to all roles
        Set<String> roles = Arrays.stream(signupRolesCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roles.isEmpty()) {
            roles = Set.of("ADMIN", "ANALYST", "UPLOADER", "VIEWER");
        }

        UserEntity e = UserEntity.newActive(uname, email, req.name().trim(), hash, roles);
        usersRepo.put(e);

        var dto = new UserDto(e.getUsername(), e.getName(), e.getEmail(), new ArrayList<>(e.getRoles()));
        String token = jwt.issue(
                e.getUsername(),
                Map.of("name", e.getName(), "email", e.getEmail(), "roles", dto.roles()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(token, dto));
    }

    // ===== Login (public) =====
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String uname = req.username().trim().toLowerCase(Locale.ROOT);

        var maybe = usersRepo.getByUsername(uname);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
        var u = maybe.get();
        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Account not active"));
        }
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        var dto = new UserDto(u.getUsername(), u.getName(), u.getEmail(), new ArrayList<>(u.getRoles()));
        String token = jwt.issue(
                u.getUsername(),
                Map.of("name", u.getName(), "email", u.getEmail(), "roles", dto.roles()));
        return ResponseEntity.ok(new LoginResponse(token, dto));
    }

    // ===== Me (authenticated) =====
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing token"));
        }
        String sub = jwt.verifyAndGetSubject(authHeader.substring(7));
        var maybe = usersRepo.getByUsername(sub);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        var u = maybe.get();
        return ResponseEntity
                .ok(new UserDto(u.getUsername(), u.getName(), u.getEmail(), new ArrayList<>(u.getRoles())));
    }
}
