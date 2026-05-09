-- 文件信息表
CREATE TABLE IF NOT EXISTS `file_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name` VARCHAR(255) NOT NULL COMMENT '存储文件名（UUID）',
    `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `content_type` VARCHAR(100) DEFAULT NULL COMMENT '文件MIME类型',
    `extension` VARCHAR(20) DEFAULT NULL COMMENT '文件扩展名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    `uploaded_by` BIGINT DEFAULT NULL COMMENT '上传者ID',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '文件描述',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stored_name` (`stored_name`),
    KEY `idx_original_name` (`original_name`),
    KEY `idx_content_type` (`content_type`),
    KEY `idx_upload_time` (`upload_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';
