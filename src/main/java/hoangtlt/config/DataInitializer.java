package hoangtlt.config;

import hoangtlt.entities.Role;
import hoangtlt.entities.User;
import hoangtlt.repositories.RoleRepository;
import hoangtlt.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default roles if not exist
            if (roleRepository.count() == 0) {
                roleRepository.save(new Role("ADMIN"));
                roleRepository.save(new Role("MANAGER"));
                roleRepository.save(new Role("STAFF"));
            }

            // Create admin user if not exist
            if (userRepository.count() == 0) {
                Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("System Administrator");
                admin.setRole(adminRole);
                admin.setStatus(User.UserStatus.ACTIVE);
                userRepository.save(admin);
            }
            System.out.println(
                    "          ____  ____  _      ___ ____   ____  _____ ___ ___  _   _ \r\n" + //
                            "   / \\  |  _ \\|  _ \\| |    |_ _/ ___| / _  ||_   _|_ _| _ \\| | | |\r\n" + //
                            "  / _ \\ | |_) | |_) | |     | | |    / /_| |  | |  | || |_) | | | |\r\n" + //
                            " / ___ \\|  __/|  __/| |___  | | |___ | ___  |  | |  | ||  __/|_|_|\r\n" + //
                            "/_/   \\_\\_|   |_|   |_____|___|____||_|  |_|  |_| |___|_|   (_|_)\r\n" + //
                            "                                                                 \r\n" + //
                            " ____ _____  _    ____ _____ _____ ____  \r\n" + //
                            "/ ___|_   _|/ \\  |  _ \\_   _| ____|  _ \\ \r\n" + //
                            "\\___ \\ | | / _ \\ | |_) || | |  _| | | | |\r\n" + //
                            " ___) || |/ ___ \\|  _ < | | | |___| |_| |\r\n" + //
                            "|____/ |_/_/   \\_\\_| \\_\\|_| |_____|____/");
        };
    }
}
