package nz.compliscan.api.config;

import nz.compliscan.api.security.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityUsersProperties {

    private List<UserProps> users = new ArrayList<>();

    public List<UserProps> getUsers() {
        return users;
    }

    public void setUsers(List<UserProps> users) {
        this.users = users;
    }

    public static class UserProps {
        private String username;
        private String name;
        private String email;
        private String password; // plain (dev)
        private String passwordBcrypt; // prefer in prod
        private Set<Role> roles = EnumSet.noneOf(Role.class);

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

        public Set<Role> getRoles() {
            return roles;
        }

        public void setRoles(Set<Role> roles) {
            this.roles = roles;
        }
    }
}
