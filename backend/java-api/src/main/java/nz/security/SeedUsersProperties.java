package nz.compliscan.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.security")
public class SeedUsersProperties {

    private List<SeedUser> users;

    public List<SeedUser> getUsers() {
        return users;
    }

    public void setUsers(List<SeedUser> users) {
        this.users = users;
    }

    public static class SeedUser {
        private String username;
        private String name;
        private String email;
        private String password; // plain (dev only)
        private String passwordBcrypt; // preferred in prod
        private Set<String> roles;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordBcrypt() {
            return passwordBcrypt;
        }

        public void setPasswordBcrypt(String passwordBcrypt) {
            this.passwordBcrypt = passwordBcrypt;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }
    }
}
