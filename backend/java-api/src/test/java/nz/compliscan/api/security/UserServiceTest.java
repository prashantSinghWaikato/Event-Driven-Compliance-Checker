package nz.compliscan.api.security;

import nz.compliscan.api.config.SecurityUsersProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void upsertAndVerify() {
        var props = Mockito.mock(SecurityUsersProperties.class);
        // start with no seeded users from properties
        when(props.getUsers()).thenReturn(List.of());

        var svc = new UserService(props, new BCryptPasswordEncoder());

        // create alice
        svc.upsert("alice", "Alice", "a@x.test", "s3cr3t", EnumSet.of(Role.ADMIN));

        assertThat(svc.verify("alice", "s3cr3t")).isTrue();
        assertThat(svc.verify("alice", "wrong")).isFalse();

        // update password
        svc.upsert("alice", "Alice", "a@x.test", "newpass", EnumSet.of(Role.ADMIN));
        assertThat(svc.verify("alice", "newpass")).isTrue();
        assertThat(svc.verify("alice", "s3cr3t")).isFalse();
    }
}
