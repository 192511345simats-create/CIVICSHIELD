package com.civicshield.imageverification;

import com.civicshield.entity.ComplaintImage;
import com.civicshield.entity.ImageVerification;
import com.civicshield.repository.ComplaintImageRepository;
import com.civicshield.repository.ImageVerificationRepository;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageVerificationService {

    private final ComplaintImageRepository imageRepository;
    private final ImageVerificationRepository verificationRepository;
    private final ObjectMapper objectMapper;

    public String computeSha256(MultipartFile file) throws IOException {
        return DigestUtils.sha256Hex(file.getInputStream());
    }

    public String computePerceptualHash(MultipartFile file) throws IOException {
        BufferedImage img = ImageIO.read(file.getInputStream());
        return img != null ? pHash(img) : null;
    }

    public String computePerceptualHash(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        return img != null ? pHash(img) : null;
    }

    private String pHash(BufferedImage original) {
        BufferedImage small = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original.getScaledInstance(32, 32, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        double[][] pixels = new double[32][32];
        for (int y = 0; y < 32; y++)
            for (int x = 0; x < 32; x++)
                pixels[y][x] = small.getRaster().getSample(x, y, 0);

        double[][] dct = applyDCT(pixels);

        double sum = 0;
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                if (x != 0 || y != 0) sum += dct[y][x];
        double avg = sum / 63.0;

        StringBuilder bits = new StringBuilder();
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                bits.append(dct[y][x] > avg ? "1" : "0");

        return String.format("%016x", Long.parseUnsignedLong(bits.toString(), 2));
    }

    private double[][] applyDCT(double[][] p) {
        int n = p.length;
        double[][] d = new double[n][n];
        for (int u = 0; u < n; u++) {
            for (int v = 0; v < n; v++) {
                double s = 0;
                for (int y = 0; y < n; y++)
                    for (int x = 0; x < n; x++)
                        s += p[y][x] * Math.cos((2*x+1)*u*Math.PI/(2.0*n))
                                     * Math.cos((2*y+1)*v*Math.PI/(2.0*n));
                double cu = u == 0 ? 1/Math.sqrt(2) : 1.0;
                double cv = v == 0 ? 1/Math.sqrt(2) : 1.0;
                d[u][v] = (2.0/n) * cu * cv * s;
            }
        }
        return d;
    }

    public int hammingDistance(String h1, String h2) {
        if (h1 == null || h2 == null || h1.length() != 16 || h2.length() != 16) return 64;
        return Long.bitCount(Long.parseUnsignedLong(h1, 16) ^ Long.parseUnsignedLong(h2, 16));
    }

    @Transactional
    public void extractAndStoreExif(ComplaintImage image, File file) {
        Map<String, String> exif = new HashMap<>();
        try {
            Metadata meta = ImageMetadataReader.readMetadata(file);

            ExifIFD0Directory d0 = meta.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (d0 != null) {
                String make = d0.getString(ExifIFD0Directory.TAG_MAKE);
                String model = d0.getString(ExifIFD0Directory.TAG_MODEL);
                if (make  != null) exif.put("cameraMake", make);
                if (model != null) exif.put("cameraModel", model);
            }

            ExifSubIFDDirectory sub = meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (sub != null) {
                java.util.Date dt = sub.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dt != null) {
                    LocalDateTime ldt = dt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    image.setExifCaptureTime(ldt);
                    exif.put("captureTime", ldt.toString());
                }
            }

            GpsDirectory gps = meta.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null) {
                com.drew.lang.GeoLocation geo = gps.getGeoLocation();
                if (geo != null) {
                    image.setExifLatitude(geo.getLatitude());
                    image.setExifLongitude(geo.getLongitude());
                    exif.put("gpsLat", String.valueOf(geo.getLatitude()));
                    exif.put("gpsLon", String.valueOf(geo.getLongitude()));
                }
            }
            image.setExifMetadataJson(objectMapper.writeValueAsString(exif));
        } catch (Exception e) {
            log.debug("EXIF unavailable: {}", e.getMessage());
            try { image.setExifMetadataJson(objectMapper.writeValueAsString(exif)); } catch (Exception ignored) {}
        }
        imageRepository.save(image);
    }

    @Transactional
    public ImageVerification performVerification(ComplaintImage newImg) {
        ImageVerification v = new ImageVerification();
        v.setImage(newImg);
        v.setComplaint(newImg.getComplaint());

        // 1. SHA-256 exact match
        if (newImg.getSha256Hash() != null) {
            Optional<ComplaintImage> exact = imageRepository.findBySha256Hash(newImg.getSha256Hash());
            if (exact.isPresent() && !exact.get().getId().equals(newImg.getId())) {
                v.setExactDuplicate(true);
                v.setDuplicateOfImageId(exact.get().getId());
                v.setResult(ImageVerification.VerificationResult.POSSIBLE_DUPLICATE);
            }
        }

        // 2. pHash visual similarity
        if (!v.isExactDuplicate() && newImg.getPerceptualHash() != null) {
            List<ComplaintImage> all = imageRepository.findByPerceptualHashIsNotNull();
            for (ComplaintImage ex : all) {
                if (ex.getId().equals(newImg.getId())) continue;
                int dist = hammingDistance(newImg.getPerceptualHash(), ex.getPerceptualHash());
                if (dist <= 10) {
                    v.setVisuallySimilar(true);
                    v.setDuplicateOfImageId(ex.getId());
                    v.setPerceptualHashDistance((double) dist);
                    v.setResult(ImageVerification.VerificationResult.POSSIBLE_DUPLICATE);
                    break;
                }
            }
        }

        // 3. EXIF indicators
        v.setHasGpsData(newImg.getExifLatitude() != null);
        v.setHasTimestamp(newImg.getExifCaptureTime() != null);
        Map<String, String> exif = new HashMap<>();
        if (newImg.getExifMetadataJson() != null) {
            try { exif = objectMapper.readValue(newImg.getExifMetadataJson(), Map.class); } catch (Exception ignored) {}
        }
        v.setHasCameraInfo(exif.containsKey("cameraMake") || exif.containsKey("cameraModel"));

        // 4. Final result
        if (v.getResult() == null) v.setResult(ImageVerification.VerificationResult.CLEAN);

        return verificationRepository.save(v);
    }
}
