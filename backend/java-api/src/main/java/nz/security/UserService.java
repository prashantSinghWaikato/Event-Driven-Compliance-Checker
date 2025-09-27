package nz.compliscan.api.security;

import nz.compliscan.api.config.SecurityUsersProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class UserService {

    public record UserRecord(
            String username,
            String name,
            String email,
            Set<Role> roles,
            String passwordBcrypt) {
    }

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final PasswordEncoder encoder;

    // simple validators
    private static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public UserService(SecurityUsersProperties props, PasswordEncoder encoder) {
        this.encoder = encoder;

        // seed from application.yml (bootstrap users)
        for (var p : props.getUsers()) {
            String bcrypt = (p.getPasswordBcrypt() != null && !p.getPasswordBcrypt().isBlank())
                    ? p.getPasswordBcrypt()
                    : (p.getPassword() != null && !p.getPassword().isBlank() ? encoder.encode(p.getPassword()) : null);

            var seededRoles = p.getRoles() == null ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(p.getRoles());

            var rec = new UserRecord(
                    p.getUsername(),
                    Optional.ofNullable(p.getName()).orElse(p.getUsername()),
                    Optional.ofNullable(p.getEmail()).orElse(""),
                    seededRoles,
                    bcrypt);
            users.put(rec.username(), rec);
        }
    }

    /* ===== Queries ===== */

    public Optional<UserRecord> find(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Collection<UserRecord> list() {
        return users.values();
    }

    public boolean existsUsername(String username) {
        return users.containsKey(username);
    }

    public boolean existsEmail(String email) {
        if (email == null || email.isBlank())
            return false;
        String e = email.trim().toLowerCase();
        return users.values().stream().anyMatch(u -> e.equalsIgnoreCase(u.email()));
    }

    /* ===== Auth ===== */

    public boolean verify(String username, String rawPassword) {
        var u = users.get(username);
        return u != null && u.passwordBcrypt() != null && encoder.matches(rawPassword, u.passwordBcrypt());
    }

    /* ===== Admin upsert (can set roles) ===== */
    public UserRecord upsert(String username, String name, String email, String rawPassword, Set<Role> roles) {
        validateUsername(username);
        if (email != null && !email.isBlank())
            validateEmail(email);
        // if changing email to one used elsewhere, reject
        if (email != null && !email.isBlank()) {
            boolean clash = users.values().stream()
                    .anyMatch(u -> !u.username().equals(username) && email.equalsIgnoreCase(u.email()));
            if (clash)
                throw new IllegalStateException("Email already in use");
        }

        String bcrypt = (rawPassword != null && !rawPassword.isBlank())
                ? encoder.encode(rawPassword)
                : (users.get(username) != null ? users.get(username).passwordBcrypt() : null);

        var rec = new UserRecord(
                username,
                Optional.ofNullable(name).orElse(username),
                Optional.ofNullable(email).orElse(""),
                roles == null ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles),
                bcrypt);
        users.put(username, rec);
        return rec;
    }

    /* ===== Public self-registration (cannot set roles) ===== */
    public UserRecord registerPublic(String username, String password, String name, String email) {
        validateUsername(username);
        validateEmail(email);
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (existsUsername(username))
            throw new IllegalStateException("Username already taken");
        if (existsEmail(email))
            throw new IllegalStateException("Email already registered");

        String bcrypt = encoder.encode(password);
        var rec = new UserRecord(
                username.trim(),
                (name == null || name.isBlank()) ? username.trim() : name.trim(),
                email.trim(),
                EnumSet.of(Role.VIEWER), // default role for self-register
                bcrypt);
        users.put(rec.username(), rec);
        return rec;
    }

    /* ===== helpers ===== */
    private static void validateUsername(String username) {
        if (username == null || !USERNAME_RE.matcher(username.trim()).matches()) {
            throw new IllegalArgumentException("Invalid username");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || !EMAIL_RE.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
}
