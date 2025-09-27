package nz.compliscan.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Boots the Spring context and tests /auth/login via MockMvc.
 * Injects a known test user via @TestPropertySource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // --- Seed one test user (matches your SecurityUsersProperties -> users list)
        // ---
        "app.security.users[0].username=alice",
        "app.security.users[0].name=Alice",
        "app.security.users[0].email=alice@example.com",
        "app.security.users[0].password=secret", // plain; UserService encodes if bcrypt missing
        "app.security.users[0].roles[0]=ADMIN", // ✅ enum collections need indexed props
        "app.security.users[0].roles[1]=USER",

        // JWT + CORS so JwtUtil & SecurityConfig initialize
        "app.security.jwtSecret=test-secret-test-secret-test-secret-1234567890",
        "app.security.allowedOrigins=http://localhost:5173",

        // Keep external dependencies harmless in tests
        "app.aws.region=us-east-1",
        "app.aws.s3Bucket=test-bucket",
        "app.aws.sqsQueueUrl=",
        "app.aws.ddbTable=TestComplianceResults",
        "app.aws.jobsTable=TestComplianceJobs",

        // Avoid initializing beans that might fetch remote data during context startup
        "spring.main.lazy-initialization=true"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void login_ok() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.user.name").value("Alice"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"));
    }

    @Test
    void login_unauthorized() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
