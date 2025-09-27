package nz.compliscan.api.users;

import java.time.Instant;
import java.util.Set;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
public class UserEntity {

    private String username; // PK
    private String email; // GSI: email-index
    private String name;
    private String passwordHash; // bcrypt only
    private Set<String> roles; // e.g. ADMIN, ANALYST, UPLOADER, VIEWER
    private Boolean emailVerified;
    private String status; // ACTIVE, LOCKED
    private Long createdAt; // epoch ms
    private Long updatedAt; // epoch ms

    @DynamoDbPartitionKey
    @DynamoDbAttribute("username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDbAttribute("passwordHash")
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @DynamoDbAttribute("roles")
    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @DynamoDbAttribute("emailVerified")
    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @DynamoDbAttribute("createdAt")
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static UserEntity newActive(String username, String email, String name, String passwordHash,
            Set<String> roles) {
        var u = new UserEntity();
        u.setUsername(username);
        u.setEmail(email);
        u.setName(name);
        u.setPasswordHash(passwordHash);
        u.setRoles(roles);
        u.setEmailVerified(false);
        u.setStatus("ACTIVE");
        long now = Instant.now().toEpochMilli();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return u;
    }
}
