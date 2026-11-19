package com.civicshield.algorithm;

import com.civicshield.entity.Complaint;
import com.civicshield.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Grid-based hotspot detector (~100m cells).
 * Locations with 3+ complaints in the same grid cell are hotspots.
 */
@Component
@RequiredArgsConstructor
public class HotspotDetector {

    private final ComplaintRepository complaintRepository;
    private static final double CELL = 0.001; // ~100m at equatorial latitudes
    private static final int    THRESHOLD = 3;

    public List<HotspotResult> detectHotspots() {
        Map<String, List<Complaint>> clusters = new HashMap<>();
        for (Complaint c : complaintRepository.findAllWithLocation()) {
            clusters.computeIfAbsent(key(c.getLatitude(), c.getLongitude()), k -> new ArrayList<>()).add(c);
        }
        List<HotspotResult> hotspots = new ArrayList<>();
        for (var entry : clusters.entrySet()) {
            if (entry.getValue().size() >= THRESHOLD) {
                List<Complaint> grp = entry.getValue();
                double lat = grp.stream().mapToDouble(Complaint::getLatitude).average().orElse(0);
                double lng = grp.stream().mapToDouble(Complaint::getLongitude).average().orElse(0);
                hotspots.add(new HotspotResult(lat, lng, grp.size(), grp));
            }
        }
        hotspots.sort(Comparator.comparingInt(HotspotResult::count).reversed());
        return hotspots;
    }

    public boolean isHotspot(double lat, double lng) {
        String myKey = key(lat, lng);
        return complaintRepository.findAllWithLocation().stream()
            .filter(c -> key(c.getLatitude(), c.getLongitude()).equals(myKey))
            .count() >= THRESHOLD;
    }

    private String key(double lat, double lng) {
        return Math.round(lat / CELL) + "_" + Math.round(lng / CELL);
    }

    public record HotspotResult(double latitude, double longitude, int count, List<Complaint> complaints) {}
}
