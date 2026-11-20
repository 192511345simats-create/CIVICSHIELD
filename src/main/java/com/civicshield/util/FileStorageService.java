// TEMPLATE USAGE: th:src="${imagePath != null and #strings.startsWith(imagePath,'http') ? imagePath : '/uploads/' + imagePath}"
package com.civicshield.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Autowired(required = false)
    private Cloudinary cloudinary;

    private static final List<String> ALLOWED = List.of("image/jpeg","image/png","image/webp","image/jpg");
    private static final long MAX_BYTES = 10 * 1024 * 1024;

    public String store(MultipartFile file) throws IOException {
        validate(file);
        if (isCloudinaryEnabled()) {
            // Upload to Cloudinary
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", "civicshield",
                    "resource_type", "image"
            );
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            String url = (String) uploadResult.get("secure_url");
            log.debug("Uploaded to Cloudinary: {}", url);
            return url;
        } else {
            // Local fallback storage
            String datePath = LocalDate.now().toString();
            Path dir = Paths.get(uploadDir, datePath);
            Files.createDirectories(dir);
            String ext = ext(file.getOriginalFilename());
            String name = UUID.randomUUID() + "." + ext;
            Path dest = dir.resolve(name);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored upload locally: {}", dest);
            return datePath + "/" + name;
        }
    }

    public Path resolvePath(String rel) { return Paths.get(uploadDir, rel); }
    public File resolveFile(String rel) { return resolvePath(rel).toFile(); }

    public boolean isCloudinaryEnabled() { return cloudinary != null; }

    public boolean delete(String ref) {
        if (ref != null && ref.startsWith("http")) {
            // Cloudinary-managed files are not deleted here; Cloudinary API deletion could be added.
            log.debug("Skipping delete for Cloudinary URL: {}", ref);
            return true;
        }
        try {
            return Files.deleteIfExists(resolvePath(ref));
        } catch (IOException e) {
            log.warn("Delete failed: {}", ref);
            return false;
        }
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
