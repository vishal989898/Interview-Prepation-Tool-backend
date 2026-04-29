package com.interview.controller;

import com.interview.dto.ApiResponse;
import com.interview.dto.request.InterviewReportRequest;
import com.interview.dto.response.BehavioralQuestionResponse;
import com.interview.dto.response.InterviewReportResponse;
import com.interview.dto.response.PreparationPlanResponse;
import com.interview.dto.response.SkillGapResponse;
import com.interview.dto.response.TechnicalQuestionResponse;
import com.interview.entity.InterviewReport;
import com.interview.entity.User;
import com.interview.repository.InterviewReportRepository;
import com.interview.service.AiService;
import com.interview.service.InterviewReportData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class InterviewController {
    
    private final AiService aiService;
    private final InterviewReportRepository interviewReportRepository;
    
    @PostMapping("/")
    public ResponseEntity<ApiResponse<InterviewReportResponse>> generateInterviewReport(
            @ModelAttribute InterviewReportRequest request,
            @AuthenticationPrincipal User user) throws IOException {
        
        // Log received data for debugging
        System.out.println("Received interview report request");
        System.out.println("Job Description: " + (request.getJobDescription() != null ? "present" : "missing"));
        System.out.println("Self Description: " + (request.getSelfDescription() != null ? "present" : "missing"));
        System.out.println("Resume: " + (request.getResume() != null && !request.getResume().isEmpty() ? "present" : "missing"));
        
        // Validate job description
        if (request.getJobDescription() == null || request.getJobDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Job description is required");
        }
        
        // Validate that at least one of resume or self-description is provided
        boolean hasResume = request.getResume() != null && !request.getResume().isEmpty();
        boolean hasSelfDescription = request.getSelfDescription() != null && !request.getSelfDescription().trim().isEmpty();
        
        if (!hasResume && !hasSelfDescription) {
            throw new IllegalArgumentException("Either resume file or self-description must be provided");
        }
        
        // Extract resume text from file if provided, otherwise use self-description
        String resumeText;
        if (hasResume) {
            resumeText = extractTextFromPdf(request.getResume());
        } else {
            resumeText = request.getSelfDescription();
        }
        
        InterviewReportData reportData = aiService.generateInterviewReport(
            resumeText,
            request.getSelfDescription(),
            request.getJobDescription()
        );
        
        InterviewReport interviewReport = new InterviewReport();
        interviewReport.setJobDescription(request.getJobDescription());
        interviewReport.setResume(resumeText);
        interviewReport.setSelfDescription(request.getSelfDescription());
        interviewReport.setMatchScore(reportData.getMatchScore());
        interviewReport.setTitle(reportData.getTitle());
        interviewReport.setUser(user);
        interviewReport.setTechnicalQuestions(reportData.getTechnicalQuestions());
        interviewReport.setBehavioralQuestions(reportData.getBehavioralQuestions());
        interviewReport.setSkillGaps(reportData.getSkillGaps());
        interviewReport.setPreparationPlan(reportData.getPreparationPlan());
        
        interviewReportRepository.save(interviewReport);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<InterviewReportResponse>builder()
                .message("Interview report generated successfully.")
                .data(toInterviewReportResponse(interviewReport))
                .build());
    }
    
    @GetMapping("/report/{interviewId}")
    public ResponseEntity<ApiResponse<InterviewReportResponse>> getInterviewReportById(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal User user) {
        
        InterviewReport interviewReport = interviewReportRepository.findByIdAndUserId(interviewId, user.getId())
                .orElseThrow(() -> new RuntimeException("Interview report not found."));
        
        return ResponseEntity.ok(ApiResponse.<InterviewReportResponse>builder()
                .message("Interview report fetched successfully.")
                .data(toInterviewReportResponse(interviewReport))
                .build());
    }
    
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<InterviewReportResponse>>> getAllInterviewReports(
            @AuthenticationPrincipal User user) {
        
        List<InterviewReport> reports = interviewReportRepository.findByUserId(user.getId());
        
        List<InterviewReportResponse> reportResponses = reports.stream()
                .map(this::toInterviewReportResponseSummary)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.<List<InterviewReportResponse>>builder()
                .message("Interview reports fetched successfully.")
                .data(reportResponses)
                .build());
    }
    
 @PostMapping("/resume/pdf/{interviewReportId}")
public ResponseEntity<byte[]> generateResumePdf(
        @PathVariable Long interviewReportId,
        @AuthenticationPrincipal User user) {

    try {

        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        InterviewReport interviewReport = interviewReportRepository
                .findByIdAndUserId(interviewReportId, user.getId())
                .orElseThrow(() -> new RuntimeException("Interview report not found"));

        String resume = interviewReport.getResume() != null ? interviewReport.getResume() : "";
        String selfDescription = interviewReport.getSelfDescription() != null ? interviewReport.getSelfDescription() : "";
        String jobDescription = interviewReport.getJobDescription() != null ? interviewReport.getJobDescription() : "";

        System.out.println("Generating PDF...");
        System.out.println("Resume length: " + resume.length());
        System.out.println("Self description length: " + selfDescription.length());
        System.out.println("Job description length: " + jobDescription.length());

        byte[] pdfBytes = aiService.generateResumePdf(resume, selfDescription, jobDescription);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new RuntimeException("PDF generation returned empty result");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "resume_" + interviewReportId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);

    } catch (Exception e) {

        e.printStackTrace();   // 🔴 THIS WILL SHOW REAL ERROR IN TERMINAL

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("Error generating PDF: " + e.getMessage()).getBytes());
    }
}
    
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }
        
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    

    private InterviewReportResponse toInterviewReportResponse(InterviewReport report) {
        return InterviewReportResponse.builder()
                .id(report.getId())
                .jobDescription(report.getJobDescription())
                .resume(report.getResume())
                .selfDescription(report.getSelfDescription())
                .matchScore(report.getMatchScore())
                .technicalQuestions(report.getTechnicalQuestions().stream()
                        .map(q -> TechnicalQuestionResponse.builder()
                                .question(q.getQuestion())
                                .intention(q.getIntention())
                                .answer(q.getAnswer())
                                .build())
                        .collect(Collectors.toList()))
                .behavioralQuestions(report.getBehavioralQuestions().stream()
                        .map(q -> BehavioralQuestionResponse.builder()
                                .question(q.getQuestion())
                                .intention(q.getIntention())
                                .answer(q.getAnswer())
                                .build())
                        .collect(Collectors.toList()))
                .skillGaps(report.getSkillGaps().stream()
                        .map(g -> SkillGapResponse.builder()
                                .skill(g.getSkill())
                                .severity(g.getSeverity())
                                .build())
                        .collect(Collectors.toList()))
                .preparationPlan(report.getPreparationPlan().stream()
                        .map(p -> PreparationPlanResponse.builder()
                                .day(p.getDay())
                                .focus(p.getFocus())
                                .tasks(p.getTasks() != null && !p.getTasks().isEmpty() 
                                    ? java.util.Arrays.asList(p.getTasks().split(",\\s*"))
                                    : java.util.Collections.emptyList())
                                .build())
                        .collect(Collectors.toList()))
                .title(report.getTitle())
                .createdAt(report.getCreatedAt())
                .build();
    }
    
    private InterviewReportResponse toInterviewReportResponseSummary(InterviewReport report) {
        return InterviewReportResponse.builder()
                .id(report.getId())
                .title(report.getTitle())
                .matchScore(report.getMatchScore())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
