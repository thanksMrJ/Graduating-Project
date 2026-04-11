package com.ajing.medicalmultimodal.service;

import com.ajing.medicalmultimodal.common.BusinessException;
import com.ajing.medicalmultimodal.dto.UserDto;
import com.ajing.medicalmultimodal.domain.AppUser;
import com.ajing.medicalmultimodal.repo.AnalysisRecordRepository;
import com.ajing.medicalmultimodal.repo.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final AnalysisRecordRepository recordRepository;

    public AdminUserService(AppUserRepository userRepository, AnalysisRecordRepository recordRepository) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
    }

    public List<UserDto> listAll() {
        return userRepository.findAll().stream().map(UserDto::from).collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        AppUser u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        recordRepository.deleteByUserId(u.getId());
        userRepository.delete(u);
    }
}
