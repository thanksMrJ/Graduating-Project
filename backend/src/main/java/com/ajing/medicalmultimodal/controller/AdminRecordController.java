package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.dto.RecordDto;
import com.ajing.medicalmultimodal.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/records")
@Tag(name = "AdminRecords", description = "全量病历任务")
public class AdminRecordController {

    private final RecordService recordService;

    public AdminRecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    @Operation(summary = "全部任务")
    public ApiResponse<List<RecordDto>> list() {
        return ApiResponse.ok(recordService.listAllForAdmin());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public ApiResponse<Void> delete(@PathVariable String id) {
        recordService.deleteForAdmin(id);
        return ApiResponse.ok();
    }
}
