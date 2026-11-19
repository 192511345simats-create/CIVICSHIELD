package com.civicshield.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED = List.of("image/jpeg","image/png","image/webp","image/jpg");
    private static final long MAX_BYTES = 10 * 1024 * 1024;

    public String store(MultipartFile file) throws IOException {
        validate(file);
        String datePath = LocalDate.now().toString();
        Path dir = Paths.get(uploadDir, datePath);
        Files.createDirectories(dir);
        String ext = ext(file.getOriginalFilename());
        String name = UUID.randomUUID() + "." + ext;
        Path dest = dir.resolve(name);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored upload: {}", dest);
        return datePath + "/" + name;
    }

    public Path resolvePath(String rel)  { return Paths.get(uploadDir, rel); }
    public File resolveFile(String rel)  { return resolvePath(rel).toFile(); }

    public boolean delete(String rel) {
        try { return Files.deleteIfExists(resolvePath(rel)); }
        catch (IOException e) { log.warn("Delete failed: {}", rel); return false; }
    }

    private void validate(MultipartFile f) throws IOException {
        if (f == null || f.isEmpty()) throw new IOException("No file provided.");
        if (!ALLOWED.contains(f.getContentType())) throw new IOException("Only JPEG, PNG or WebP images are accepted.");
        if (f.getSize() > MAX_BYTES) throw new IOException("File too large (max 10 MB).");
    }

    private String ext(String name) {
        if (name == null) return "jpg";
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i+1).toLowerCase() : "jpg";
    }
}
