package com.ajing.medicalmultimodal.service;

import com.ajing.medicalmultimodal.common.BusinessException;
import com.ajing.medicalmultimodal.domain.AnalysisRecord;
import com.ajing.medicalmultimodal.domain.AppUser;
import com.ajing.medicalmultimodal.dto.CreateRecordRequest;
import com.ajing.medicalmultimodal.dto.RecordDto;
import com.ajing.medicalmultimodal.repo.AnalysisRecordRepository;
import com.ajing.medicalmultimodal.repo.AppUserRepository;
import com.ajing.medicalmultimodal.security.JwtKind;
import com.ajing.medicalmultimodal.security.JwtPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecordService {

    private final AnalysisRecordRepository recordRepository;
    private final AppUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public RecordService(
            AnalysisRecordRepository recordRepository,
            AppUserRepository userRepository,
            ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecordDto create(JwtPrincipal principal, CreateRecordRequest req) {
        requireUser(principal);
        Map<String, Object> detail = new LinkedHashMap<>(req.detail());
        Object tr = detail.get("textReport");
        if (tr == null || !StringUtils.hasText(String.valueOf(tr))) {
            throw new BusinessException("请填写病例报告（文本）");
        }
        if (!detail.containsKey("result") || detail.get("result") == null) {
            detail.put("result", "（占位）多模态推理完成后由模型接口写入");
        }

        LocalDate day;
        if (StringUtils.hasText(req.createdAt())) {
            try {
                day = LocalDate.parse(req.createdAt().trim());
            } catch (DateTimeParseException e) {
                throw new BusinessException("日期格式应为 YYYY-MM-DD");
            }
        } else {
            day = LocalDate.now();
        }

        String status = StringUtils.hasText(req.status()) ? req.status().trim() : "待分析";

        AnalysisRecord r = new AnalysisRecord();
        r.setId(UUID.randomUUID().toString());
        r.setUserId(principal.id());
        r.setTitle(req.title().trim());
        r.setStatus(status);
        r.setCreatedDate(day);
        try {
            r.setDetailJson(objectMapper.writeValueAsString(detail));
        } catch (Exception e) {
            throw new BusinessException("详情序列化失败");
        }
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        recordRepository.save(r);

        AppUser u = userRepository.findById(principal.id()).orElse(null);
        String uname = u != null ? u.getName() : "";
        return toDto(r, uname);
    }

    public List<RecordDto> listMine(JwtPrincipal principal) {
        requireUser(principal);
        return recordRepository.findByUserIdOrderByCreatedAtDesc(principal.id()).stream()
                .map(r -> toDto(r, null))
                .collect(Collectors.toList());
    }

    public RecordDto getMine(JwtPrincipal principal, String id) {
        requireUser(principal);
        AnalysisRecord r = recordRepository.findByIdAndUserId(id, principal.id())
                .orElseThrow(() -> new BusinessException(404, "记录不存在"));
        return toDto(r, null);
    }

    public List<RecordDto> listAllForAdmin() {
        Map<Long, String> names = userRepository.findAll().stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::getName, (a, b) -> a));
        return recordRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toDto(r, names.getOrDefault(r.getUserId(), "（未知用户）")))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteForAdmin(String id) {
        if (!recordRepository.existsById(id)) {
            throw new BusinessException(404, "记录不存在");
        }
        recordRepository.deleteById(id);
    }

    private RecordDto toDto(AnalysisRecord r, String userName) {
        Map<String, Object> detail;
        try {
            detail = objectMapper.readValue(r.getDetailJson(), new TypeReference<>() {});
        } catch (Exception e) {
            detail = Map.of("raw", r.getDetailJson());
        }
        String uname = userName;
        if (!StringUtils.hasText(uname)) {
            uname = userRepository.findById(r.getUserId()).map(AppUser::getName).orElse("");
        }
        return new RecordDto(
                r.getId(),
                String.valueOf(r.getUserId()),
                uname,
                r.getCreatedDate().toString(),
                r.getTitle(),
                r.getStatus(),
                detail);
    }

    private static void requireUser(JwtPrincipal principal) {
        if (principal == null || principal.kind() != JwtKind.USER) {
            throw new BusinessException(403, "需要普通用户登录");
        }
    }
}
