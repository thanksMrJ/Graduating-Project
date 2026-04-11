package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.dto.UserDto;
import com.ajing.medicalmultimodal.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "AdminUsers", description = "用户管理")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "用户列表")
    public ApiResponse<List<UserDto>> list() {
        return ApiResponse.ok(adminUserService.listAll());
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户")
    public ApiResponse<Void> delete(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ApiResponse.ok();
    }
}
