package com.ajing.medicalmultimodal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateRecordRequest(
        @NotBlank(message = "请填写任务标题") String title,
        String createdAt,
        String status,
        @NotNull(message = "请填写详情") Map<String, Object> detail
) {
}
