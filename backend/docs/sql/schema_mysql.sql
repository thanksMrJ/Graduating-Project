-- =============================================================================
-- 医疗多模态早筛系统 — MySQL 建表脚本
-- 与 docs/API.md、Vue 前端 appStore（用户 / 病历分析任务）字段对齐
-- 库名：若 IDEA 中已建库为 medi-multi，请保持与下方一致；否则先改库名再执行
-- MySQL 建议：8.0+（支持 JSON 类型与函数）
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `medi-multi`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `medi-multi`;

-- -----------------------------------------------------------------------------
-- 普通用户（姓名 + 密码登录；name 唯一）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `mm_analysis_job`;
DROP TABLE IF EXISTS `mm_uploaded_file`;
DROP TABLE IF EXISTS `mm_analysis_record`;
DROP TABLE IF EXISTS `mm_user`;
DROP TABLE IF EXISTS `mm_admin`;

CREATE TABLE `mm_user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`          VARCHAR(64)     NOT NULL COMMENT '姓名（当前与登录名一致，唯一）',
  `password_hash` VARCHAR(255)    NOT NULL COMMENT '密码哈希（BCrypt 等），禁止存明文',
  `age`           VARCHAR(16)     NULL     COMMENT '年龄（与前端一致可为字符串）',
  `gender`        VARCHAR(16)     NULL     COMMENT '性别',
  `phone`         VARCHAR(32)     NULL     COMMENT '联系方式',
  `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mm_user_name` (`name`),
  KEY `idx_mm_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户';

-- -----------------------------------------------------------------------------
-- 管理员（账号 + 密码；与前端 AdminLogin username 对齐）
-- -----------------------------------------------------------------------------
CREATE TABLE `mm_admin` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`      VARCHAR(64)     NOT NULL COMMENT '管理员登录账号',
  `password_hash` VARCHAR(255)    NOT NULL COMMENT '密码哈希',
  `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mm_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员';

-- -----------------------------------------------------------------------------
-- 病历分析任务 / 推理任务（detail 为 JSON，含 textReport、indicators、imagingDesc、result 等）
-- -----------------------------------------------------------------------------
CREATE TABLE `mm_analysis_record` (
  `id`          VARCHAR(36)   NOT NULL COMMENT '业务主键（可用 UUID 字符串，与前端 r_xxx 风格兼容）',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属用户',
  `title`       VARCHAR(255)  NOT NULL COMMENT '任务标题',
  `status`      VARCHAR(32)   NOT NULL DEFAULT '待分析' COMMENT '待分析/分析中/已完成/失败',
  `created_date` DATE         NOT NULL COMMENT '任务日期（对应前端 createdAt YYYY-MM-DD）',
  `detail`      JSON          NOT NULL COMMENT '业务载荷：textReport、indicators、imagingDesc、result、imagingFileIds 等',
  `fail_reason` VARCHAR(512)  NULL COMMENT '失败时的简要原因（可选）',
  `created_at`  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_mm_record_user_id` (`user_id`),
  KEY `idx_mm_record_status` (`status`),
  KEY `idx_mm_record_created_at` (`created_at`),
  CONSTRAINT `fk_mm_record_user`
    FOREIGN KEY (`user_id`) REFERENCES `mm_user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历分析任务';

-- -----------------------------------------------------------------------------
-- 多模态文件元数据（可选；上传后在此登记，detail 中引用 file_id）
-- -----------------------------------------------------------------------------
CREATE TABLE `mm_uploaded_file` (
  `id`           VARCHAR(36)   NOT NULL COMMENT '文件业务 ID',
  `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '上传者',
  `modality`     VARCHAR(32)   NOT NULL COMMENT 'imaging / document / other',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `stored_path`  VARCHAR(512)  NOT NULL COMMENT '服务器存储路径或对象存储键',
  `mime_type`    VARCHAR(128)  NULL,
  `size_bytes`   BIGINT UNSIGNED NULL,
  `created_at`   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_mm_file_user` (`user_id`),
  CONSTRAINT `fk_mm_file_user`
    FOREIGN KEY (`user_id`) REFERENCES `mm_user` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件元数据';

-- -----------------------------------------------------------------------------
-- 推理编排任务（异步；与 API 中 analysis-jobs 对应）
-- -----------------------------------------------------------------------------
CREATE TABLE `mm_analysis_job` (
  `id`         VARCHAR(36)   NOT NULL COMMENT '任务 ID（job_xxx）',
  `record_id`  VARCHAR(36)   NOT NULL COMMENT '关联 mm_analysis_record.id',
  `status`     VARCHAR(32)   NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED/RUNNING/SUCCEEDED/FAILED',
  `stage`      VARCHAR(64)   NULL COMMENT '数据准备/单模态分析/多模态推理 等',
  `message`    VARCHAR(512)  NULL COMMENT '进度或错误说明',
  `created_at` DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_mm_job_record` (`record_id`),
  CONSTRAINT `fk_mm_job_record`
    FOREIGN KEY (`record_id`) REFERENCES `mm_analysis_record` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推理异步任务';

-- =============================================================================
-- 可选：演示管理员（密码请在应用中改为 BCrypt 写入，勿在生产使用明文）
-- INSERT INTO mm_admin (username, password_hash) VALUES ('admin', '<BCrypt of 123456>');
-- =============================================================================
