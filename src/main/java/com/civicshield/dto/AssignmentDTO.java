package com.civicshield.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssignmentDTO {
    @NotNull(message = "Please select a department.")
    private Long departmentId;
    private String workerName;
    private String workerContact;
    private LocalDateTime deadline;
    private String instructions;
}
