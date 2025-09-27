// src/test/java/nz/compliscan/api/security/JwtAuthFilterTest.java
package nz.compliscan.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationWhenTokenValid() throws ServletException, IOException {
        var jwt = mock(JwtUtil.class);
        var filter = new JwtAuthFilter(jwt);

        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwt.verifyAndGetSubject("good-token")).thenReturn("alice");

        filter.doFilterInternal(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void leavesUnauthenticatedOnBadToken() throws ServletException, IOException {
        var jwt = mock(JwtUtil.class);
        var filter = new JwtAuthFilter(jwt);

        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwt.verifyAndGetSubject("bad-token")).thenThrow(new RuntimeException("bad"));

        filter.doFilterInternal(req, res, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, times(1)).doFilter(req, res);
    }
}
