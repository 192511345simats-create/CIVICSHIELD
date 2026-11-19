package com.civicshield.dto;

import com.civicshield.entity.Complaint;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ComplaintSubmitDTO {
    @NotNull(message = "Please select an issue type.")
    private Complaint.Category category;

    @NotBlank(message = "Please provide a description.")
    @Size(max = 500, message = "Description must be 500 characters or less.")
    private String description;

    @Size(max = 255)
    private String locationAddress;
    private Double latitude;
    private Double longitude;

    @Size(max = 100) private String ward;
    @Size(max = 100) private String district;
}
