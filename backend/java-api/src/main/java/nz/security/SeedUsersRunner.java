package nz.compliscan.api.security;

import nz.compliscan.api.users.UserEntity;
import nz.compliscan.api.users.UsersRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SeedUsersRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedUsersRunner.class);

    private final UsersRepo usersRepo;
    private final PasswordEncoder encoder;
    private final SeedUsersProperties props;

    public SeedUsersRunner(UsersRepo usersRepo, PasswordEncoder encoder, SeedUsersProperties props) {
        this.usersRepo = usersRepo;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.getUsers() == null || props.getUsers().isEmpty()) {
            log.info("No seed users configured.");
            return;
        }

        props.getUsers().forEach(su -> {
            if (su.getUsername() == null || su.getUsername().isBlank())
                return;

            String uname = su.getUsername().trim().toLowerCase();
            var existing = usersRepo.getByUsername(uname);
            if (existing.isPresent()) {
                log.info("Seed user '{}' already exists — skipping", uname);
                return;
            }

            String hash = (su.getPasswordBcrypt() != null && !su.getPasswordBcrypt().isBlank())
                    ? su.getPasswordBcrypt()
                    : encoder.encode(su.getPassword() == null ? "" : su.getPassword());

            Set<String> roles = (su.getRoles() == null || su.getRoles().isEmpty())
                    ? Set.of("VIEWER")
                    : su.getRoles();

            UserEntity u = UserEntity.newActive(
                    uname,
                    su.getEmail() == null ? "" : su.getEmail().trim(),
                    su.getName() == null ? uname : su.getName().trim(),
                    hash,
                    roles);
            usersRepo.put(u);
            log.info("Seeded user '{}' with roles {}", uname, roles);
        });
    }
}
