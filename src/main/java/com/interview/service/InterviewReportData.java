package com.interview.service;

import com.interview.entity.BehavioralQuestion;
import com.interview.entity.PreparationPlan;
import com.interview.entity.SkillGap;
import com.interview.entity.TechnicalQuestion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReportData {
    private Integer matchScore;
    private List<TechnicalQuestion> technicalQuestions;
    private List<BehavioralQuestion> behavioralQuestions;
    private List<SkillGap> skillGaps;
    private List<PreparationPlan> preparationPlan;
    private String title;
}
