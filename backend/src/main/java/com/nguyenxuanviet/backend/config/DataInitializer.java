package com.nguyenxuanviet.backend.config;

import com.nguyenxuanviet.backend.model.User;
import com.nguyenxuanviet.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Khởi tạo user admin nếu chưa tồn tại
            String username = "viet4628";
            String email = "vietadmin@gmail.com";
            
            if (!userRepository.existsByUsername(username) && !userRepository.existsByEmail(email)) {
                User admin = new User();
                admin.setUsername(username);
                admin.setEmail(email);
                admin.setPassword(passwordEncoder.encode("Viet4628@"));
                userRepository.save(admin);
                System.out.println(">>> Đã khởi tạo User Admin thành công: " + username);
            } else {
                System.out.println(">>> User Admin đã tồn tại, bỏ qua bước khởi tạo.");
            }
        };
    }
}