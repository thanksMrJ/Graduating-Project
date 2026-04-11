package com.ajing.medicalmultimodal.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "请输入姓名") String name,
        @NotBlank(message = "请输入新密码") String newPassword
) {
}
