package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.dto.PatchMeRequest;
import com.ajing.medicalmultimodal.dto.UserDto;
import com.ajing.medicalmultimodal.security.JwtPrincipal;
import com.ajing.medicalmultimodal.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Me", description = "当前用户资料")
public class MeController {

    private final UserProfileService userProfileService;

    public MeController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户信息")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(userProfileService.me(principal));
    }

    @PatchMapping("/me")
    @Operation(summary = "更新资料")
    public ApiResponse<UserDto> patch(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestBody PatchMeRequest req) {
        return ApiResponse.ok(userProfileService.patchMe(principal, req));
    }
}
