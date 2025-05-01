package com.flashcardapp.config;

import com.flashcardapp.models.ERole;
import com.flashcardapp.models.Role;
import com.flashcardapp.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DbInitializer implements CommandLineRunner {
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, ERole.ROLE_USER));
            roleRepository.save(new Role(null, ERole.ROLE_SUPERVISOR));
            roleRepository.save(new Role(null, ERole.ROLE_ADMIN));

            System.out.println("Initialized role data");
        }
    }
}