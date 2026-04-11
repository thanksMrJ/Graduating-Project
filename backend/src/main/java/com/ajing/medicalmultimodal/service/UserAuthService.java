package com.ajing.medicalmultimodal.service;

import com.ajing.medicalmultimodal.common.BusinessException;
import com.ajing.medicalmultimodal.domain.AppUser;
import com.ajing.medicalmultimodal.dto.LoginRequest;
import com.ajing.medicalmultimodal.dto.RegisterRequest;
import com.ajing.medicalmultimodal.dto.ResetPasswordRequest;
import com.ajing.medicalmultimodal.dto.UserDto;
import com.ajing.medicalmultimodal.repo.AppUserRepository;
import com.ajing.medicalmultimodal.security.JwtKind;
import com.ajing.medicalmultimodal.security.JwtPrincipal;
import com.ajing.medicalmultimodal.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserAuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserAuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest req) {
        String name = req.name().trim();
        if (userRepository.existsByName(name)) {
            throw new BusinessException("该姓名已注册，请直接登录");
        }
        AppUser u = new AppUser();
        u.setName(name);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setAge(req.age() != null ? req.age().trim() : "");
        u.setGender(req.gender() != null ? req.gender().trim() : "");
        u.setPhone(req.phone() != null ? req.phone().trim() : "");
        userRepository.save(u);

        UserDto dto = UserDto.from(u);
        String token = jwtService.createToken(new JwtPrincipal(u.getId(), JwtKind.USER, u.getName()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("user", dto);
        return body;
    }

    public Map<String, Object> login(LoginRequest req) {
        AppUser u = userRepository.findByName(req.name().trim())
                .orElseThrow(() -> new BusinessException("姓名或密码错误"));
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new BusinessException("姓名或密码错误");
        }
        UserDto dto = UserDto.from(u);
        String token = jwtService.createToken(new JwtPrincipal(u.getId(), JwtKind.USER, u.getName()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("user", dto);
        return body;
    }

    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest req) {
        AppUser u = userRepository.findByName(req.name().trim())
                .orElseThrow(() -> new BusinessException("未找到该用户，请确认姓名是否正确"));
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(u);
        UserDto dto = UserDto.from(u);
        String token = jwtService.createToken(new JwtPrincipal(u.getId(), JwtKind.USER, u.getName()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("user", dto);
        return body;
    }
}
