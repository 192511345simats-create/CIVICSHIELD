package com.civicshield.algorithm;

import com.civicshield.entity.Complaint;
import com.civicshield.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Detects duplicate complaints using:
 * - Same category (prerequisite)
 * - Geographic proximity (Haversine, configurable radius)
 * - Text similarity (Jaccard on description tokens)
 * - Time window filter
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetector {

    private final ComplaintRepository complaintRepository;

    @Value("${app.duplicate.geo-radius-meters:200}")
    private double geoRadius;

    @Value("${app.duplicate.similarity-threshold:0.4}")
    private double threshold;

    @Value("${app.duplicate.time-window-days:30}")
    private int timeWindow;

    public Optional<Complaint> findDuplicate(Complaint newC) {
        LocalDateTime since = LocalDateTime.now().minusDays(timeWindow);
        Long excludeId = newC.getId() != null ? newC.getId() : -1L;
        List<Complaint> candidates = complaintRepository.findSimilarCategoryRecent(newC.getCategory(), since, excludeId);

        for (Complaint existing : candidates) {
            double score = similarityScore(newC, existing);
            if (score >= threshold) {
                log.info("Possible duplicate: {} ↔ {} (score={:.2f})", newC.getComplaintId(), existing.getComplaintId(), score);
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    private double similarityScore(Complaint a, Complaint b) {
        double score = 0;
        boolean hasGeo = a.getLatitude() != null && b.getLatitude() != null;
        boolean hasText = a.getDescription() != null && b.getDescription() != null;

        if (hasGeo) {
            double dist = haversine(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
            if (dist <= geoRadius) score += 0.4 * (1.0 - dist / geoRadius);
        }
        if (hasText) {
            score += 0.6 * jaccard(a.getDescription(), b.getDescription());
        }
        return score;
    }

    private double jaccard(String textA, String textB) {
        Set<String> a = tokenize(textA), b = tokenize(textB);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a); inter.retainAll(b);
        Set<String> union = new HashSet<>(a); union.addAll(b);
        return (double) inter.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        Set<String> stop = Set.of("the","a","an","is","are","was","in","on","at","and","or","near","there","this","that","it","has","have","been","with","of","to","for");
        Set<String> tokens = new HashSet<>();
        for (String w : text.toLowerCase().split("[\\s.,!?;:()\"']+")) {
            if (w.length() > 2 && !stop.contains(w)) tokens.add(w);
        }
        return tokens;
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
