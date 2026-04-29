package com.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReportRequest {
    
    @NotBlank(message = "Job description is required")
    private String jobDescription;
    
    @NotBlank(message = "Self description is required")
    private String selfDescription;
    
    private MultipartFile resume;
}
