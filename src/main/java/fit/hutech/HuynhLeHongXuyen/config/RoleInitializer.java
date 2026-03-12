package fit.hutech.HuynhLeHongXuyen.config;

import fit.hutech.HuynhLeHongXuyen.entities.Role;
import fit.hutech.HuynhLeHongXuyen.repositories.IRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RequiredArgsConstructor
@Profile("prod")
public class RoleInitializer {
    private final IRoleRepository roleRepository;

    @Bean
    public CommandLineRunner initRoles() {
        return args -> {
            if (roleRepository.findByName("USER").isEmpty()) {
                Role userRole = new Role();
                userRole.setName("USER");
                roleRepository.save(userRole);
            }
            if (roleRepository.findByName("ADMIN").isEmpty()) {
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                roleRepository.save(adminRole);
            }
        };
    }
}
