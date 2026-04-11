package com.ajing.medicalmultimodal.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "请输入姓名") String name,
        @NotBlank(message = "请输入密码") String password,
        String age,
        String gender,
        String phone
) {
}
