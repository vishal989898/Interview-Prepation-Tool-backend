package com.interview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "interview_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 5000)
    private String jobDescription;
    
    @Column(length = 10000)
    private String resume;
    
    @Column(length = 5000)
    private String selfDescription;
    
    private Integer matchScore;
    
    @ElementCollection
    @CollectionTable(name = "technical_questions", joinColumns = @JoinColumn(name = "report_id"))
    @Column(length = 2000)
    private List<TechnicalQuestion> technicalQuestions;
    
    @ElementCollection
    @CollectionTable(name = "behavioral_questions", joinColumns = @JoinColumn(name = "report_id"))
    @Column(length = 2000)
    private List<BehavioralQuestion> behavioralQuestions;
    
    @ElementCollection
    @CollectionTable(name = "skill_gaps", joinColumns = @JoinColumn(name = "report_id"))
    private List<SkillGap> skillGaps;
    
    @ElementCollection
    @CollectionTable(name = "preparation_plans", joinColumns = @JoinColumn(name = "report_id"))
    private List<PreparationPlan> preparationPlan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String title;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
