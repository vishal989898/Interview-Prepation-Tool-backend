package com.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReportResponse {
    private Long id;
    private String jobDescription;
    private String resume;
    private String selfDescription;
    private Integer matchScore;
    private List<TechnicalQuestionResponse> technicalQuestions;
    private List<BehavioralQuestionResponse> behavioralQuestions;
    private List<SkillGapResponse> skillGaps;
    private List<PreparationPlanResponse> preparationPlan;
    private String title;
    private LocalDateTime createdAt;
}
