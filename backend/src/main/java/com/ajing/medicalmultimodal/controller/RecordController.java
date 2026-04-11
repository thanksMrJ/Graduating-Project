package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.dto.CreateRecordRequest;
import com.ajing.medicalmultimodal.dto.RecordDto;
import com.ajing.medicalmultimodal.security.JwtPrincipal;
import com.ajing.medicalmultimodal.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/records")
@Tag(name = "Records", description = "病历分析任务")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    @Operation(summary = "创建任务")
    public ApiResponse<RecordDto> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateRecordRequest req) {
        return ApiResponse.ok(recordService.create(principal, req));
    }

    @GetMapping
    @Operation(summary = "我的任务列表")
    public ApiResponse<List<RecordDto>> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(recordService.listMine(principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务详情")
    public ApiResponse<RecordDto> get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String id) {
        return ApiResponse.ok(recordService.getMine(principal, id));
    }
}
