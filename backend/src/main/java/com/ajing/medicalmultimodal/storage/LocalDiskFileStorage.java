package com.ajing.medicalmultimodal.storage;

import com.aliyun.oss.OSS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnMissingBean(OSS.class)
public class LocalDiskFileStorage implements FileStorageService {

    private final Path root;

    public LocalDiskFileStorage(@Value("${app.upload.dir:./data/uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public UploadResult store(MultipartFile file, String folder, Long userId) throws IOException {
        String safeFolder = StringUtils.hasText(folder) ? folder.trim().replace("..", "") : "files";
        String sub = "u" + userId;
        Path dir = root.resolve(sub).resolve(safeFolder);
        Files.createDirectories(dir);

        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) {
            original = "file";
        }
        int slash = Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\'));
        if (slash >= 0) {
            original = original.substring(slash + 1);
        }
        String name = UUID.randomUUID() + "-" + original;
        Path target = dir.resolve(name);
        file.transferTo(target);

        String key = sub + "/" + safeFolder + "/" + name;
        return new UploadResult(key, "");
    }
}
