package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.repo.AppUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Stats", description = "公开统计")
public class StatsController {

    private final AppUserRepository userRepository;

    public StatsController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/user-count")
    @Operation(summary = "注册用户数量")
    public ApiResponse<Map<String, Long>> userCount() {
        return ApiResponse.ok(Map.of("count", userRepository.count()));
    }
}
