package com.civicshield.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves uploaded files (photos) from the filesystem.
 * Files are stored under the configured upload directory (default: uploads/).
 */
@Controller
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) throws IOException {
        // Strip the leading /uploads/ from the request URI to get the relative path
        String requestUri = request.getRequestURI();
        String relativePath = requestUri.replaceFirst("^/uploads/", "");

        Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // Detect content type; default to octet-stream for unknown types
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = null;
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
