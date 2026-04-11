package com.ajing.medicalmultimodal.service;

import com.ajing.medicalmultimodal.common.BusinessException;
import com.ajing.medicalmultimodal.domain.AppAdmin;
import com.ajing.medicalmultimodal.dto.AdminLoginRequest;
import com.ajing.medicalmultimodal.repo.AppAdminRepository;
import com.ajing.medicalmultimodal.security.JwtKind;
import com.ajing.medicalmultimodal.security.JwtPrincipal;
import com.ajing.medicalmultimodal.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminAuthService {

    private final AppAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthService(AppAdminRepository adminRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> login(AdminLoginRequest req) {
        AppAdmin a = adminRepository.findByUsername(req.username().trim())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!passwordEncoder.matches(req.password(), a.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        String token = jwtService.createToken(new JwtPrincipal(a.getId(), JwtKind.ADMIN, a.getUsername()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("username", a.getUsername());
        return body;
    }
}
