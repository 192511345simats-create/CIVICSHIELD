package com.civicshield.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class DashboardStatsDTO {
    private long totalReports;
    private long newReports;
    private long underReview;
    private long highPriority;
    private long critical;
    private long resolved;
    private long rejected;
}
