package dev.aidev.config;

import dev.aidev.user.Role;
import dev.aidev.user.RoleRepository;
import dev.aidev.user.User;
import dev.aidev.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds default roles and accounts on first boot (idempotent).
 * admin@aidev.local / Admin#12345 (SUPER_ADMIN)
 * demo@aidev.local  / Demo#12345  (USER)
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role superAdmin = roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("SUPER_ADMIN")));
        Role user = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER")));

        if (userRepository.findByEmail("admin@aidev.local").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@aidev.local");
            admin.setName("Super Admin");
            admin.setPasswordHash(passwordEncoder.encode("Admin#12345"));
            admin.setRoles(Set.of(superAdmin));
            userRepository.save(admin);
            log.info("Seeded SUPER_ADMIN admin@aidev.local");
        }
        if (userRepository.findByEmail("demo@aidev.local").isEmpty()) {
            User demo = new User();
            demo.setEmail("demo@aidev.local");
            demo.setName("Demo User");
            demo.setPasswordHash(passwordEncoder.encode("Demo#12345"));
            demo.setRoles(Set.of(user));
            userRepository.save(demo);
            log.info("Seeded USER demo@aidev.local");
        }
    }
}
