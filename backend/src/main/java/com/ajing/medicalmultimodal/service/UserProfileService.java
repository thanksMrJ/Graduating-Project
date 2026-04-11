package com.ajing.medicalmultimodal.service;

import com.ajing.medicalmultimodal.common.BusinessException;
import com.ajing.medicalmultimodal.domain.AppUser;
import com.ajing.medicalmultimodal.dto.PatchMeRequest;
import com.ajing.medicalmultimodal.dto.UserDto;
import com.ajing.medicalmultimodal.repo.AppUserRepository;
import com.ajing.medicalmultimodal.security.JwtKind;
import com.ajing.medicalmultimodal.security.JwtPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final AppUserRepository userRepository;

    public UserProfileService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto me(JwtPrincipal principal) {
        requireUser(principal);
        AppUser u = userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        return UserDto.from(u);
    }

    @Transactional
    public UserDto patchMe(JwtPrincipal principal, PatchMeRequest req) {
        requireUser(principal);
        AppUser u = userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (req.age() != null) {
            u.setAge(req.age().trim());
        }
        if (req.gender() != null) {
            u.setGender(req.gender().trim());
        }
        if (req.phone() != null) {
            u.setPhone(req.phone().trim());
        }
        userRepository.save(u);
        return UserDto.from(u);
    }

    private static void requireUser(JwtPrincipal principal) {
        if (principal == null || principal.kind() != JwtKind.USER) {
            throw new BusinessException(403, "需要普通用户登录");
        }
    }
}
