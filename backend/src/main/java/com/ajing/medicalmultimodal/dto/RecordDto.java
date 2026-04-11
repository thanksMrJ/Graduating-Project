package com.ajing.medicalmultimodal.dto;

import java.util.Map;

public record RecordDto(
        String id,
        String userId,
        String userName,
        String createdAt,
        String title,
        String status,
        Map<String, Object> detail
) {
}
