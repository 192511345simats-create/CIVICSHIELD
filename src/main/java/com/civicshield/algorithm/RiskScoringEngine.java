package com.civicshield.algorithm;

import com.civicshield.entity.Complaint;
import com.civicshield.entity.RiskAssessment;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based risk scoring engine (0–100 weighted score).
 * Weights: Severity 35% | Exposure 25% | Duration 20% | Confirmations 10% | Location 10%
 */
@Component
public class RiskScoringEngine {

    public RiskAssessment assess(Complaint complaint, int confirmationCount, boolean isHotspot) {
        RiskAssessment a = new RiskAssessment();
        a.setComplaint(complaint);
        List<String> factors = new ArrayList<>();

        int sev   = severityScore(complaint.getCategory(), factors);
        int exp   = exposureScore(complaint.getCategory(), factors);
        int dur   = durationScore(complaint.getCreatedAt(), factors);
        int conf  = confirmScore(confirmationCount, factors);
        int loc   = isHotspot ? 80 : 20;
        if (isHotspot) factors.add("Located in a repeated problem area");

        a.setSeverityScore(sev);
        a.setPublicExposureScore(exp);
        a.setReportedDurationScore(dur);
        a.setConfirmationScore(conf);
        a.setLocationRiskScore(loc);

        int total = Math.min(100, (sev*35 + exp*25 + dur*20 + conf*10 + loc*10) / 100);
        a.setTotalScore(total);

        Complaint.Priority priority = classifyPriority(total);
        a.setCalculatedPriority(priority);
        a.setScoreBreakdown(String.format(
            "Severity:%d(×0.35) + Exposure:%d(×0.25) + Duration:%d(×0.20) + Confirmations:%d(×0.10) + Location:%d(×0.10) = %d/100",
            sev, exp, dur, conf, loc, total));
        a.setUpdatedAt(LocalDateTime.now());

        complaint.setRiskScore(total);
        complaint.setRiskFactors(String.join("; ", factors));
        complaint.setPriority(priority);
        return a;
    }

    private int severityScore(Complaint.Category cat, List<String> f) {
        return switch (cat) {
            case OPEN_MANHOLE         -> { f.add("Open manhole — immediate danger"); yield 95; }
            case FALLEN_TREE          -> { f.add("Fallen tree — road/path blocked"); yield 85; }
            case WATER_LEAKAGE        -> { f.add("Water leakage — infrastructure damage risk"); yield 75; }
            case ROAD_DAMAGE          -> { f.add("Road damage — vehicle/pedestrian hazard"); yield 70; }
            case DRAINAGE_BLOCKAGE    -> { f.add("Drainage blockage — flooding risk"); yield 65; }
            case PUBLIC_STRUCTURE_DAMAGE -> { f.add("Damaged public structure"); yield 60; }
            case STREETLIGHT_FAILURE  -> { f.add("Streetlight failure — reduced night visibility"); yield 55; }
            default                   -> { f.add("Public issue reported"); yield 40; }
        };
    }

    private int exposureScore(Complaint.Category cat, List<String> f) {
        int s = switch (cat) {
            case OPEN_MANHOLE, FALLEN_TREE, ROAD_DAMAGE -> 90;
            case WATER_LEAKAGE, DRAINAGE_BLOCKAGE       -> 70;
            case STREETLIGHT_FAILURE                    -> 60;
            case PUBLIC_STRUCTURE_DAMAGE                -> 55;
            default -> 40;
        };
        if (s >= 80) f.add("High public exposure area");
        return s;
    }

    private int durationScore(LocalDateTime reportedAt, List<String> f) {
        if (reportedAt == null) return 20;
        long hours = Duration.between(reportedAt, LocalDateTime.now()).toHours();
        if (hours > 168) { f.add("Reported more than 7 days ago"); return 90; }
        if (hours > 72)  { f.add("Reported more than 3 days ago"); return 70; }
        if (hours > 24)  { f.add("Reported more than 24 hours ago"); return 50; }
        return 20;
    }

    private int confirmScore(int count, List<String> f) {
        if (count >= 10) { f.add(count + " citizens confirmed this issue"); return 100; }
        if (count >= 5)  { f.add(count + " citizens confirmed this issue"); return 80; }
        if (count >= 2)  { f.add(count + " citizens confirmed this issue"); return 60; }
        if (count == 1)  { f.add("1 citizen confirmed this issue"); return 40; }
        return 10;
    }

    public Complaint.Priority classifyPriority(int score) {
        if (score >= 75) return Complaint.Priority.CRITICAL;
        if (score >= 50) return Complaint.Priority.HIGH;
        if (score >= 25) return Complaint.Priority.MEDIUM;
        return Complaint.Priority.LOW;
    }
}
