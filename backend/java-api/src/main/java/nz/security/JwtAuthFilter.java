package nz.compliscan.api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64.Decoder;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwt;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Decoder B64URL = Base64.getUrlDecoder();

    public JwtAuthFilter(JwtUtil jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                // Verify signature & subject
                String username = jwt.verifyAndGetSubject(token);

                // Extract roles (optional claim). Default to USER if none.
                List<String> roles = extractRoles(token);
                List<SimpleGrantedAuthority> authorities = roles.isEmpty()
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        : roles.stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .toList();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities /* Collection<? extends GrantedAuthority> */);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Invalid token → leave unauthenticated; security chain will handle 401s.
            }
        }
        chain.doFilter(req, res);
    }

    private List<String> extractRoles(String jwtToken) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3)
                return List.of();
            String payloadJson = new String(B64URL.decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payload = MAPPER.readTree(payloadJson);
            JsonNode roles = payload.get("roles");
            if (roles == null || !roles.isArray())
                return List.of();
            List<String> out = new ArrayList<>();
            roles.forEach(n -> {
                if (n.isTextual())
                    out.add(n.asText());
            });
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }
}
