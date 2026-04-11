package com.ajing.medicalmultimodal.config;

import com.ajing.medicalmultimodal.domain.AppAdmin;
import com.ajing.medicalmultimodal.repo.AppAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppAdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() > 0) {
            return;
        }
        AppAdmin a = new AppAdmin();
        a.setUsername("admin");
        a.setPasswordHash(passwordEncoder.encode("123456"));
        adminRepository.save(a);
        log.info("已初始化默认管理员账号: admin / 123456（请在生产环境立即修改密码）");
    }
}
