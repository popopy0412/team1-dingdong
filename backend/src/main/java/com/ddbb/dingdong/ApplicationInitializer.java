package com.ddbb.dingdong;

import com.ddbb.dingdong.domain.payment.entity.Wallet;
import com.ddbb.dingdong.domain.payment.repository.WalletRepository;
import com.ddbb.dingdong.domain.user.entity.Home;
import com.ddbb.dingdong.domain.user.entity.School;
import com.ddbb.dingdong.domain.user.entity.User;
import com.ddbb.dingdong.domain.user.repository.SchoolRepository;
import com.ddbb.dingdong.domain.user.repository.UserRepository;
import com.ddbb.dingdong.infrastructure.auth.encrypt.PasswordEncoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitializer {
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;

    @PostConstruct
    public void init() {
        if (userRepository.findByEmail("test@test.com").isEmpty()) {
            String password = passwordEncoder.encode("abcd1234!@");
            Home home = new Home(null, 37.5143, 127.0294, 37.513716, 127.029790,"에티버스");
            School school = new School(null, "seoul", "address", 1.0, 1.0);
            school = schoolRepository.save(school);
            User user = new User(null, "test", "test@test.com", password, LocalDateTime.now(), school, null);
            user.associateHome(home);
            user = userRepository.save(user);
            Wallet wallet = new Wallet(null, user.getId(), 50000, LocalDateTime.now(), new ArrayList<>());
            walletRepository.save(wallet);
        }
    }
}
