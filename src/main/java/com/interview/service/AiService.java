package com.interview.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.interview.entity.BehavioralQuestion;
import com.interview.entity.PreparationPlan;
import com.interview.entity.SkillGap;
import com.interview.entity.TechnicalQuestion;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiService {

    @Value("${google.genai.api.key}")
    private String apiKey;

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    public InterviewReportData generateInterviewReport(String resume, String selfDescription, String jobDescription) throws IOException {
        System.out.println("=== Generating Interview Report ===");
        System.out.println("Resume length: " + (resume != null ? resume.length() : 0));
        System.out.println("Job Description length: " + (jobDescription != null ? jobDescription.length() : 0));
        
        // Check if API key is configured
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("\n=== No API key configured - Using MOCK data ===\n");
            return generateMockReport(resume, jobDescription);
        }
        
        try {
            String prompt = String.format("""
                Generate an interview report for a candidate with the following details:
                Resume: %s
                Self Description: %s
                Job Description: %s
                
                Return a JSON object with the following structure:
                {
                    "matchScore": number (0-100),
                    "technicalQuestions": [{"question": string, "intention": string, "answer": string}],
                    "behavioralQuestions": [{"question": string, "intention": string, "answer": string}],
                    "skillGaps": [{"skill": string, "severity": "low|medium|high"}],
                    "preparationPlan": [{"day": number, "focus": string, "tasks": [string]}],
                    "title": string
                }
                """, resume, selfDescription, jobDescription);
            
            JsonObject responseJson = callGeminiApi(prompt);
            return parseInterviewReport(responseJson);
            
        } catch (Exception e) {
            System.err.println("AI Service Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\n=== Falling back to MOCK data ===\n");
            return generateMockReport(resume, jobDescription);
        }
    }

    private InterviewReportData generateMockReport(String resumeText, String jobDescription) {
        System.out.println("=== Generating MOCK Report with Dynamic Match Score ===");
        
        // Calculate actual match score based on skills and keywords
        int matchScore = calculateMatchScore(resumeText, jobDescription);
        System.out.println("Calculated Match Score: " + matchScore + "%");
        
        InterviewReportData mockData = new InterviewReportData();
        mockData.setMatchScore(matchScore);
        mockData.setTitle("Interview Plan - " + (jobDescription.length() > 30 ? jobDescription.substring(0, 30) : jobDescription) + "...");
        
        List<TechnicalQuestion> techQuestions = generateTechnicalQuestions(resumeText, jobDescription);
        mockData.setTechnicalQuestions(techQuestions);
        
        List<BehavioralQuestion> behavioralQuestions = generateBehavioralQuestions(resumeText, jobDescription);
        mockData.setBehavioralQuestions(behavioralQuestions);
        
        // Generate dynamic skill gaps based on job requirements and resume
        List<SkillGap> skillGaps = identifySkillGaps(resumeText, jobDescription);
        mockData.setSkillGaps(skillGaps);
        
        // Generate dynamic preparation plan based on skill gaps
        List<PreparationPlan> prepPlan = createDynamicRoadmap(skillGaps, jobDescription, resumeText);
        mockData.setPreparationPlan(prepPlan);
        
        return mockData;
    }

    private List<SkillGap> identifySkillGaps(String resume, String jobDescription) {
        List<SkillGap> gaps = new ArrayList<>();
        
        if (resume == null || jobDescription == null || resume.trim().isEmpty() || jobDescription.trim().isEmpty()) {
            gaps.add(new SkillGap("General technical skills", "medium"));
            return gaps;
        }
        
        String resumeLower = resume.toLowerCase();
        String jobLower = jobDescription.toLowerCase();
        
        // Skills to check
        String[] skillCategories = {
            "java", "python", "javascript", "typescript",
            "react", "angular", "vue", "spring boot", "node.js",
            "mysql", "postgresql", "mongodb", "redis",
            "aws", "azure", "gcp", "docker", "kubernetes",
            "machine learning", "ai", "data science",
            "rest api", "graphql", "microservices",
            "ci/cd", "jenkins", "git",
            "system design", "architecture", "agile"
        };
        
        // Find skills required in job but missing/weak in resume
        for (String skill : skillCategories) {
            if (jobLower.contains(skill.toLowerCase())) {
                // Skill is in job description
                if (!resumeLower.contains(skill.toLowerCase())) {
                    // Missing from resume - determine severity
                    String severity = determineSeverity(skill, jobLower);
                    gaps.add(new SkillGap(skill, severity));
                }
            }
        }
        
        // If no gaps found, add a generic one
        if (gaps.isEmpty()) {
            gaps.add(new SkillGap("Advanced optimization techniques", "low"));
        }
        
        // Limit to max 5 gaps
        return gaps.size() > 5 ? gaps.subList(0, 5) : gaps;
    }

    private String determineSeverity(String skill, String jobDescription) {
        String jobLower = jobDescription.toLowerCase();
        
        // Check if skill is mentioned multiple times (high importance)
        int count = 0;
        int index = 0;
        while ((index = jobLower.indexOf(skill.toLowerCase(), index)) != -1) {
            count++;
            index += skill.length();
        }
        
        // Check for emphasis words
        boolean hasEmphasis = jobLower.contains("must have") || 
                             jobLower.contains("required") || 
                             jobLower.contains("essential") ||
                             jobLower.contains("critical");
        
        if (count > 1 || (count > 0 && hasEmphasis)) {
            return "high";
        } else if (count > 0) {
            return "medium";
        }
        return "low";
    }

    private List<PreparationPlan> createPreparationPlan(List<SkillGap> gaps, String jobDescription) {
        List<PreparationPlan> plan = new ArrayList<>();
        
        if (gaps.isEmpty()) {
            plan.add(new PreparationPlan(1, "Review and Practice", "Review core concepts, Practice interview questions"));
            plan.add(new PreparationPlan(2, "Mock Interviews", "Take mock interviews, Work on communication skills"));
            return plan;
        }
        
        // Day 1-2: Address high severity gaps
        List<SkillGap> highGaps = gaps.stream()
            .filter(g -> "high".equals(g.getSeverity()))
            .limit(2)
            .collect(Collectors.toList());
        
        if (!highGaps.isEmpty()) {
            String focus = "Critical Skills: " + String.join(" & ", highGaps.stream().map(SkillGap::getSkill).collect(Collectors.toList()));
            String tasks = "Study fundamentals, Complete tutorials, Build small projects";
            plan.add(new PreparationPlan(1, focus, tasks));
        }
        
        // Day 3-4: Address medium severity gaps
        List<SkillGap> mediumGaps = gaps.stream()
            .filter(g -> "medium".equals(g.getSeverity()))
            .limit(2)
            .collect(Collectors.toList());
        
        if (!mediumGaps.isEmpty()) {
            String focus = "Important Skills: " + String.join(" & ", mediumGaps.stream().map(SkillGap::getSkill).collect(Collectors.toList()));
            String tasks = "Watch video courses, Read documentation, Practice exercises";
            plan.add(new PreparationPlan(2, focus, tasks));
        }
        
        // Day 5: Review and practice
        plan.add(new PreparationPlan(3, "Interview Preparation", "Practice mock interviews, Review common questions, Prepare STAR stories"));
        
        return plan;
    }

    private List<TechnicalQuestion> generateTechnicalQuestions(String resume, String jobDescription) {
        List<TechnicalQuestion> questions = new ArrayList<>();
        
        // Common technical questions asked in most interviews
        questions.add(new TechnicalQuestion(
            "Can you walk me through your experience with [key technology from job]?",
            "Assess hands-on experience and depth of knowledge",
            "Highlight specific projects, mention years of experience, discuss challenges overcome"
        ));
        
        questions.add(new TechnicalQuestion(
            "How do you approach debugging a complex issue in your code?",
            "Evaluate problem-solving methodology and systematic thinking",
            "Explain your step-by-step process: reproduce, isolate, identify root cause, fix, test"
        ));
        
        questions.add(new TechnicalQuestion(
            "Describe a challenging technical problem you solved recently.",
            "Assess real-world problem-solving and technical depth",
            "Use STAR method, explain the problem, your approach, solution, and impact"
        ));
        
        questions.add(new TechnicalQuestion(
            "How do you ensure code quality and maintainability in your projects?",
            "Evaluate best practices and engineering mindset",
            "Discuss code reviews, testing, documentation, design patterns, refactoring"
        ));
        
        questions.add(new TechnicalQuestion(
            "What's your experience with version control and collaboration tools?",
            "Assess teamwork and development workflow knowledge",
            "Mention Git workflows, branching strategies, CI/CD pipelines, code review processes"
        ));
        
        questions.add(new TechnicalQuestion(
            "How do you stay updated with new technologies and industry trends?",
            "Evaluate continuous learning and growth mindset",
            "Mention blogs, courses, conferences, side projects, tech communities"
        ));
        
        // Add job-specific questions based on technologies mentioned
        if (jobDescription != null) {
            String jobLower = jobDescription.toLowerCase();
            
            if (jobLower.contains("database") || jobLower.contains("sql")) {
                questions.add(new TechnicalQuestion(
                    "Explain database indexing and how it improves query performance.",
                    "Assess database optimization knowledge",
                    "Discuss B-tree indexes, when to use indexes, impact on read/write operations"
                ));
            }
            
            if (jobLower.contains("api") || jobLower.contains("rest")) {
                questions.add(new TechnicalQuestion(
                    "What are the key principles of designing a RESTful API?",
                    "Evaluate API design understanding",
                    "Discuss HTTP methods, status codes, resource naming, versioning, authentication"
                ));
            }
            
            if (jobLower.contains("cloud") || jobLower.contains("aws") || jobLower.contains("azure")) {
                questions.add(new TechnicalQuestion(
                    "What cloud services have you worked with and how did you use them?",
                    "Assess cloud computing experience",
                    "Discuss specific services (compute, storage, databases), architecture decisions"
                ));
            }
            
            if (jobLower.contains("microservice")) {
                questions.add(new TechnicalQuestion(
                    "What are the advantages and challenges of microservices architecture?",
                    "Evaluate architectural understanding",
                    "Discuss scalability, independence, complexity, distributed systems challenges"
                ));
            }
        }
        
        // Limit to 6-7 questions
        return questions.size() > 7 ? questions.subList(0, 7) : questions;
    }

    private List<BehavioralQuestion> generateBehavioralQuestions(String resume, String jobDescription) {
        List<BehavioralQuestion> questions = new ArrayList<>();
        
        // Most commonly asked behavioral questions
        questions.add(new BehavioralQuestion(
            "Tell me about yourself and your professional background.",
            "Assess communication skills and career narrative",
            "Give a 2-minute summary: current role, key experience, what brings you here"
        ));
        
        questions.add(new BehavioralQuestion(
            "Describe a time you had a conflict with a team member. How did you handle it?",
            "Evaluate interpersonal skills and conflict resolution",
            "Focus on communication, empathy, finding common ground, positive outcome"
        ));
        
        questions.add(new BehavioralQuestion(
            "Tell me about a time you failed or made a mistake. What did you learn?",
            "Assess self-awareness and growth mindset",
            "Be honest, focus on what you learned, how you improved, no blame-shifting"
        ));
        
        questions.add(new BehavioralQuestion(
            "Describe a situation where you had to meet a tight deadline.",
            "Evaluate time management and stress handling",
            "Explain prioritization, communication, how you delivered quality under pressure"
        ));
        
        questions.add(new BehavioralQuestion(
            "Tell me about a time you showed leadership, even if not in a formal role.",
            "Assess initiative and leadership potential",
            "Discuss taking ownership, mentoring others, driving results, influencing decisions"
        ));
        
        questions.add(new BehavioralQuestion(
            "Why do you want to work here, and what interests you about this role?",
            "Evaluate motivation and cultural fit",
            "Research the company, align your goals with their mission, show enthusiasm"
        ));
        
        questions.add(new BehavioralQuestion(
            "Where do you see yourself in 3-5 years?",
            "Assess career goals and long-term fit",
            "Show ambition but stay realistic, align with company growth, focus on skill development"
        ));
        
        questions.add(new BehavioralQuestion(
            "Describe a time you had to learn something completely new quickly.",
            "Evaluate adaptability and learning agility",
            "Discuss your learning process, resources used, how you applied the knowledge"
        ));
        
        return questions;
    }

    private List<PreparationPlan> createDynamicRoadmap(List<SkillGap> gaps, String jobDescription, String resume) {
        List<PreparationPlan> roadmap = new ArrayList<>();
        
        if (gaps.isEmpty()) {
            roadmap.add(new PreparationPlan(1, "Foundation Review", "Review core concepts in your expertise area, Practice coding problems"));
            roadmap.add(new PreparationPlan(2, "System Design", "Study architecture patterns, Practice designing scalable systems"));
            roadmap.add(new PreparationPlan(3, "Mock Interviews", "Take practice interviews, Work on communication and STAR method"));
            return roadmap;
        }
        
        // Extract job-specific technologies
        String jobLower = jobDescription != null ? jobDescription.toLowerCase() : "";
        String resumeLower = resume != null ? resume.toLowerCase() : "";
        
        // Day 1: Critical skill gaps
        List<SkillGap> highGaps = gaps.stream()
            .filter(g -> "high".equals(g.getSeverity()))
            .limit(2)
            .collect(Collectors.toList());
        
        if (!highGaps.isEmpty()) {
            String skills = String.join(" & ", highGaps.stream().map(SkillGap::getSkill).collect(Collectors.toList()));
            roadmap.add(new PreparationPlan(1, "Master Critical Skills: " + skills, 
                "Complete online tutorials, Build hands-on projects, Understand fundamentals deeply"));
        } else {
            roadmap.add(new PreparationPlan(1, "Technical Foundation", 
                "Review data structures and algorithms, Practice coding challenges, Study system design basics"));
        }
        
        // Day 2: Medium priority gaps + job-specific prep
        List<SkillGap> mediumGaps = gaps.stream()
            .filter(g -> "medium".equals(g.getSeverity()))
            .limit(2)
            .collect(Collectors.toList());
        
        String day2Focus = "Advanced Topics";
        String day2Tasks = "Study best practices, Read documentation, Watch expert talks";
        
        if (!mediumGaps.isEmpty()) {
            String skills = String.join(" & ", mediumGaps.stream().map(SkillGap::getSkill).collect(Collectors.toList()));
            day2Focus = "Strengthen Key Skills: " + skills;
            day2Tasks = "Complete practice exercises, Build mini-projects, Review case studies";
        }
        
        // Add job-specific focus
        if (jobLower.contains("frontend") || jobLower.contains("react") || jobLower.contains("javascript")) {
            day2Tasks += ", Practice UI/UX implementation";
        } else if (jobLower.contains("backend") || jobLower.contains("api") || jobLower.contains("database")) {
            day2Tasks += ", Design and implement APIs";
        } else if (jobLower.contains("cloud") || jobLower.contains("devops")) {
            day2Tasks += ", Practice cloud deployments";
        }
        
        roadmap.add(new PreparationPlan(2, day2Focus, day2Tasks));
        
        // Day 3: Interview-specific preparation
        roadmap.add(new PreparationPlan(3, "Interview Skills & Communication", 
            "Practice behavioral questions using STAR method, Prepare your project stories, Work on whiteboard coding"));
        
        // Day 4: Mock interviews and company research
        String day4Tasks = "Take 2-3 mock interviews, Research company culture and products";
        if (jobDescription != null && jobDescription.length() > 50) {
            day4Tasks += ", Prepare questions to ask interviewer";
        }
        roadmap.add(new PreparationPlan(4, "Mock Interviews & Company Research", day4Tasks));
        
        // Day 5: Final review and confidence building
        roadmap.add(new PreparationPlan(5, "Final Preparation", 
            "Review weak areas, Practice common questions, Prepare your interview outfit, Get good rest"));
        
        return roadmap;
    }

    private int calculateMatchScore(String resume, String jobDescription) {
        if (resume == null || jobDescription == null || resume.trim().isEmpty() || jobDescription.trim().isEmpty()) {
            return 0;
        }
        
        String resumeLower = resume.toLowerCase();
        String jobLower = jobDescription.toLowerCase();
        
        // Key technical skills to match
        String[] keySkills = {
            "java", "python", "javascript", "typescript", "react", "angular", "vue",
            "spring boot", "node.js", "express", "django", "flask",
            "mysql", "postgresql", "mongodb", "redis", "oracle",
            "aws", "azure", "gcp", "docker", "kubernetes",
            "git", "jenkins", "ci/cd", "agile", "scrum",
            "rest api", "graphql", "microservices", "html", "css",
            "spring", "hibernate", "jpa", "sql", "nosql",
            "machine learning", "ai", "data science", "analytics"
        };
        
        int matchedSkills = 0;
        int totalJobSkills = 0;
        
        // Count how many skills are mentioned in job description
        for (String skill : keySkills) {
            if (jobLower.contains(skill.toLowerCase())) {
                totalJobSkills++;
                // Check if resume also has this skill
                if (resumeLower.contains(skill.toLowerCase())) {
                    matchedSkills++;
                }
            }
        }
        
        // If no specific skills found, do a general keyword match
        if (totalJobSkills == 0) {
            String[] keywords = {
                "experience", "knowledge", "skills", "proficient", "expert",
                "familiar", "understanding", "background", "work", "develop",
                "design", "implement", "manage", "lead", "analyze"
            };
            
            int matchedKeywords = 0;
            int totalKeywords = 0;
            
            for (String keyword : keywords) {
                if (jobLower.contains(keyword)) {
                    totalKeywords++;
                    if (resumeLower.contains(keyword)) {
                        matchedKeywords++;
                    }
                }
            }
            
            if (totalKeywords > 0) {
                return Math.min(100, (int) ((double) matchedKeywords / totalKeywords * 100));
            }
            return 50; // Default if nothing matches
        }
        
        // Calculate percentage based on skill match
        double matchPercentage = (double) matchedSkills / totalJobSkills * 100;
        
        // Add bonus for experience mentions and project relevance
        if (resumeLower.contains("year") || resumeLower.contains("experience")) {
            matchPercentage += 5;
        }
        if (resumeLower.contains("project") || resumeLower.contains("developed")) {
            matchPercentage += 5;
        }
        if (resumeLower.contains("degree") || resumeLower.contains("education") || resumeLower.contains("university")) {
            matchPercentage += 3;
        }
        
        // Cap at 100
        return Math.min(100, Math.max(0, (int) matchPercentage));
    }

    public byte[] generateResumePdf(String resume, String selfDescription, String jobDescription) throws IOException {
        System.out.println("=== Generating Smart Resume PDF ===");
        System.out.println("Resume length: " + (resume != null ? resume.length() : 0));
        System.out.println("Job Description length: " + (jobDescription != null ? jobDescription.length() : 0));
        
        // Generate a smart HTML resume tailored to the job
        String htmlContent = generateSmartHtmlResume(resume, selfDescription, jobDescription);
        
        return convertHtmlToPdf(htmlContent);
    }
    
    private String generateSmartHtmlResume(String resume, String selfDescription, String jobDescription) {
        // Extract meaningful content from inputs
        String resumeContent = (resume != null && !resume.trim().isEmpty()) ? resume : "";
        String selfContent = (selfDescription != null && !selfDescription.trim().isEmpty()) ? selfDescription : "";
        String jobContent = (jobDescription != null && !jobDescription.trim().isEmpty()) ? jobDescription : "";
        
        // Extract candidate name
        String candidateName = extractCandidateName(resumeContent, selfContent);
        
        // Generate tailored sections
        String professionalSummary = generateProfessionalSummary(resumeContent, selfContent, jobContent);
        String skillsSection = extractAndFormatSkills(resumeContent, jobContent);
        String experienceSection = formatExperience(resumeContent);
        String educationSection = extractEducation(resumeContent);
        String keyProjects = extractProjects(resumeContent);
        
        // Extract job requirements to highlight
        List<String> jobKeywords = extractJobKeywords(jobContent);
        String targetedSummary = createTargetedSummary(resumeContent, selfContent, jobContent);
        
        System.out.println("Candidate name: " + candidateName);
        System.out.println("Extracted " + jobKeywords.size() + " job keywords");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8" />
                <style>
                    @page {
                        size: A4;
                        margin: 1.5cm;
                    }
                    body { 
                        font-family: 'Segoe UI', Arial, sans-serif; 
                        line-height: 1.5; 
                        color: #2c3e50; 
                        max-width: 100%%;
                        padding: 0;
                        margin: 0;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 3px solid #2563eb;
                        padding-bottom: 15px;
                        margin-bottom: 20px;
                    }
                    .header h1 {
                        color: #1e40af;
                        font-size: 28px;
                        margin: 0 0 5px 0;
                        font-weight: 700;
                    }
                    .header .contact {
                        color: #64748b;
                        font-size: 12px;
                        margin: 0;
                    }
                    .section {
                        margin-bottom: 18px;
                    }
                    .section-title {
                        color: #1e40af;
                        font-size: 16px;
                        font-weight: 700;
                        text-transform: uppercase;
                        border-bottom: 2px solid #e5e7eb;
                        padding-bottom: 5px;
                        margin-bottom: 10px;
                    }
                    .summary {
                        background-color: #f0f9ff;
                        padding: 12px;
                        border-left: 4px solid #2563eb;
                        border-radius: 4px;
                        margin-bottom: 15px;
                    }
                    .summary p {
                        margin: 0;
                        font-size: 13px;
                        line-height: 1.6;
                        color: #334155;
                    }
                    .skills-grid {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 8px;
                        margin: 10px 0;
                    }
                    .skill-tag {
                        background-color: #eff6ff;
                        color: #1e40af;
                        padding: 4px 10px;
                        border-radius: 12px;
                        font-size: 11px;
                        font-weight: 600;
                        border: 1px solid #bfdbfe;
                    }
                    .skill-tag.highlight {
                        background-color: #fef3c7;
                        color: #92400e;
                        border-color: #fbbf24;
                    }
                    .experience-item {
                        margin-bottom: 15px;
                    }
                    .experience-item h3 {
                        color: #1e40af;
                        font-size: 14px;
                        margin: 0 0 3px 0;
                        font-weight: 600;
                    }
                    .experience-item .company {
                        color: #64748b;
                        font-size: 12px;
                        font-style: italic;
                        margin: 0 0 5px 0;
                    }
                    .experience-item p {
                        margin: 5px 0;
                        font-size: 12px;
                        color: #334155;
                    }
                    .experience-item ul {
                        margin: 5px 0 0 20px;
                        padding: 0;
                    }
                    .experience-item li {
                        margin: 3px 0;
                        font-size: 12px;
                        color: #475569;
                    }
                    .highlight-box {
                        background-color: #fefce8;
                        border: 1px solid #fbbf24;
                        border-radius: 4px;
                        padding: 10px;
                        margin: 10px 0;
                    }
                    .highlight-box p {
                        margin: 0;
                        font-size: 12px;
                        color: #713f12;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>%s</h1>
                    <p class="contact">Tailored Resume for Target Position</p>
                </div>
                
                <div class="summary">
                    <p><strong>Professional Summary:</strong> %s</p>
                </div>
                
                %s
                
                %s
                
                %s
                
                %s
                
                <div class="highlight-box">
                    <p><strong>Target Position Alignment:</strong> This resume has been customized to highlight skills and experience relevant to the job description provided.</p>
                </div>
            </body>
            </html>
            """, 
            candidateName,
            targetedSummary,
            skillsSection,
            experienceSection,
            educationSection,
            keyProjects
        );
    }
    
    private String extractCandidateName(String resume, String selfDescription) {
        // Try to extract name from first line of resume
        if (resume != null && !resume.trim().isEmpty()) {
            String[] lines = resume.split("\n");
            if (lines.length > 0) {
                String firstLine = lines[0].trim();
                // If first line is short, likely a name
                if (firstLine.length() > 2 && firstLine.length() < 50) {
                    return firstLine;
                }
            }
        }
        
        // Try to find name in self-description
        if (selfDescription != null && !selfDescription.trim().isEmpty()) {
            String[] words = selfDescription.split(" ");
            if (words.length >= 2) {
                return words[0] + " " + words[1];
            }
        }
        
        return "Professional Candidate";
    }
    
    private String generateProfessionalSummary(String resume, String selfDescription, String jobDescription) {
        // Use self-description if available, otherwise extract from resume
        if (selfDescription != null && !selfDescription.trim().isEmpty()) {
            return selfDescription.length() > 300 ? 
                selfDescription.substring(0, 300) + "..." : selfDescription;
        }
        
        // Extract first paragraph from resume
        if (resume != null && !resume.trim().isEmpty()) {
            String[] paragraphs = resume.split("\n\n");
            if (paragraphs.length > 0) {
                return paragraphs[0].length() > 300 ? 
                    paragraphs[0].substring(0, 300) + "..." : paragraphs[0];
            }
        }
        
        return "Experienced professional with demonstrated expertise in relevant technologies and proven track record of delivering high-quality solutions.";
    }
    
    private String createTargetedSummary(String resume, String selfDescription, String jobDescription) {
        String baseSummary = generateProfessionalSummary(resume, selfDescription, jobDescription);
        
        // If we have job description, add targeted language
        if (jobDescription != null && !jobDescription.trim().isEmpty()) {
            List<String> keywords = extractJobKeywords(jobDescription);
            if (!keywords.isEmpty()) {
                String topKeywords = String.join(", ", keywords.subList(0, Math.min(5, keywords.size())));
                return baseSummary + " Seeking to leverage expertise in " + topKeywords + " to contribute to team success.";
            }
        }
        
        return baseSummary;
    }
    
    private String extractAndFormatSkills(String resume, String jobDescription) {
        List<String> skills = new ArrayList<>();
        List<String> highlightedSkills = new ArrayList<>();
        
        // Common technical skills patterns
        String[] skillPatterns = {
            "Java", "Python", "JavaScript", "TypeScript", "React", "Angular", "Vue",
            "Spring Boot", "Node.js", "Express", "Django", "Flask",
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Oracle",
            "AWS", "Azure", "GCP", "Docker", "Kubernetes",
            "Git", "Jenkins", "CI/CD", "Agile", "Scrum",
            "REST API", "GraphQL", "Microservices", "System Design",
            "HTML", "CSS", "SASS", "Bootstrap", "Tailwind",
            "Machine Learning", "AI", "Data Science", "Analytics"
        };
        
        // Extract skills from resume
        String resumeUpper = resume != null ? resume.toUpperCase() : "";
        for (String skill : skillPatterns) {
            if (resumeUpper.contains(skill.toUpperCase())) {
                skills.add(skill);
            }
        }
        
        // Identify which skills are mentioned in job description (to highlight)
        String jobUpper = jobDescription != null ? jobDescription.toUpperCase() : "";
        for (String skill : skills) {
            if (jobUpper.contains(skill.toUpperCase())) {
                highlightedSkills.add(skill);
            }
        }
        
        // Build skills HTML
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">");
        html.append("<div class=\"section-title\">Technical Skills</div>");
        html.append("<div class=\"skills-grid\">");
        
        // Show highlighted skills first
        for (String skill : highlightedSkills) {
            html.append("<span class=\"skill-tag highlight\">").append(escapeHtml(skill)).append("</span>");
        }
        
        // Then show other skills
        for (String skill : skills) {
            if (!highlightedSkills.contains(skill)) {
                html.append("<span class=\"skill-tag\">").append(escapeHtml(skill)).append("</span>");
            }
        }
        
        // If no skills extracted, add generic ones
        if (skills.isEmpty()) {
            html.append("<span class=\"skill-tag\">Problem Solving</span>");
            html.append("<span class=\"skill-tag\">Team Collaboration</span>");
            html.append("<span class=\"skill-tag\">Technical Communication</span>");
            html.append("<span class=\"skill-tag\">Project Management</span>");
        }
        
        html.append("</div></div>");
        
        return html.toString();
    }
    
    private String formatExperience(String resume) {
        if (resume == null || resume.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"section\">");
        html.append("<div class=\"section-title\">Professional Experience</div>");
        
        // Try to identify experience sections
        String[] sections = resume.split("(?i)(experience|work history|employment)");
        
        if (sections.length > 1) {
            // Use the content after "experience" keyword
            String experienceContent = sections[1];
            String[] lines = experienceContent.split("\n");
            
            html.append("<div class=\"experience-item\">");
            
            boolean firstContent = true;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Skip if we hit another major section
                if (line.matches("(?i)(education|skills|projects|certifications).*")) {
                    break;
                }
                
                if (firstContent && line.length() < 100) {
                    // Likely a job title
                    html.append("<h3>").append(escapeHtml(line)).append("</h3>");
                    firstContent = false;
                } else if (line.length() < 80 && !firstContent) {
                    // Likely company name
                    html.append("<p class=\"company\">").append(escapeHtml(line)).append("</p>");
                } else if (line.length() > 20) {
                    // Description or bullet point
                    if (line.startsWith("-") || line.startsWith("•")) {
                        html.append("<li>").append(escapeHtml(line.substring(1).trim())).append("</li>");
                    } else {
                        html.append("<p>").append(escapeHtml(line)).append("</p>");
                    }
                }
            }
            
            html.append("</div>");
        } else {
            // Just include relevant content from resume
            String[] paragraphs = resume.split("\n\n");
            int count = 0;
            for (String para : paragraphs) {
                if (count >= 3) break; // Limit to 3 paragraphs
                para = para.trim();
                if (para.length() > 50 && !para.matches("(?i)(education|skills|contact).*")) {
                    html.append("<div class=\"experience-item\">");
                    html.append("<p>").append(escapeHtml(para.length() > 200 ? 
                        para.substring(0, 200) + "..." : para)).append("</p>");
                    html.append("</div>");
                    count++;
                }
            }
        }
        
        html.append("</div>");
        return html.toString();
    }
    
    private String extractEducation(String resume) {
        if (resume == null || resume.trim().isEmpty()) {
            return "";
        }
        
        // Look for education section
        String[] sections = resume.split("(?i)(education|academic background)");
        
        if (sections.length > 1) {
            String educationContent = sections[1];
            // Take first 250 characters
            String education = educationContent.trim();
            if (education.length() > 250) {
                education = education.substring(0, 250) + "...";
            }
            
            if (!education.isEmpty()) {
                return String.format("""
                    <div class="section">
                        <div class="section-title">Education</div>
                        <p>%s</p>
                    </div>
                    """, escapeHtml(education));
            }
        }
        
        return "";
    }
    
    private String extractProjects(String resume) {
        if (resume == null || resume.trim().isEmpty()) {
            return "";
        }
        
        // Look for projects section
        String[] sections = resume.split("(?i)(projects|key projects|notable projects)");
        
        if (sections.length > 1) {
            String projectsContent = sections[1];
            // Stop at next major section
            String[] endSections = projectsContent.split("(?i)(education|skills|experience|certifications)");
            String projects = endSections[0].trim();
            
            if (projects.length() > 300) {
                projects = projects.substring(0, 300) + "...";
            }
            
            if (!projects.isEmpty()) {
                return String.format("""
                    <div class="section">
                        <div class="section-title">Key Projects</div>
                        <p>%s</p>
                    </div>
                    """, escapeHtml(projects));
            }
        }
        
        return "";
    }
    
    private List<String> extractJobKeywords(String jobDescription) {
        List<String> keywords = new ArrayList<>();
        
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return keywords;
        }
        
        // Common technical keywords to look for
        String[] techKeywords = {
            "Java", "Python", "JavaScript", "TypeScript", "React", "Angular", "Vue",
            "Spring Boot", "Node.js", "Express", "Django", "Flask",
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Database",
            "AWS", "Azure", "GCP", "Cloud", "Docker", "Kubernetes",
            "Git", "CI/CD", "Agile", "Scrum", "DevOps",
            "REST", "API", "GraphQL", "Microservices", "System Design",
            "HTML", "CSS", "Frontend", "Backend", "Full Stack",
            "Machine Learning", "AI", "Data Science", "Analytics",
            "Leadership", "Communication", "Problem Solving", "Team"
        };
        
        String jobUpper = jobDescription.toUpperCase();
        for (String keyword : techKeywords) {
            if (jobUpper.contains(keyword.toUpperCase())) {
                keywords.add(keyword);
            }
        }
        
        return keywords;
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    private JsonObject callGeminiApi(String prompt) throws IOException {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
        
        JsonObject requestBody = new JsonObject();
        JsonObject contents = new JsonObject();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        contents.add("parts", new com.google.gson.JsonArray());
        contents.getAsJsonArray("parts").add(part);
        
        JsonObject config = new JsonObject();
        config.addProperty("responseMimeType", "application/json");
        
        requestBody.add("contents", new com.google.gson.JsonArray());
        requestBody.getAsJsonArray("contents").add(contents);
        requestBody.add("generationConfig", config);
        
        GenericUrl url = new GenericUrl(API_URL + "?key=" + apiKey);
        HttpRequest request = requestFactory.buildPostRequest(url, 
            new ByteArrayContent("application/json", requestBody.toString().getBytes()));
        request.getHeaders().set("Content-Type", "application/json");
        
        HttpResponse response = request.execute();
        String responseText = response.parseAsString();
        
        JsonObject jsonResponse = JsonParser.parseString(responseText).getAsJsonObject();
        String generatedText = jsonResponse.getAsJsonArray("candidates")
            .getAsJsonArray()
            .get(0).getAsJsonObject()
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).getAsJsonObject()
            .get("text")
            .getAsString();
        
        return JsonParser.parseString(generatedText).getAsJsonObject();
    }
    
    private InterviewReportData parseInterviewReport(JsonObject json) {
        InterviewReportData data = new InterviewReportData();
        
        data.setMatchScore(json.has("matchScore") ? json.get("matchScore").getAsInt() : 0);
        data.setTitle(json.has("title") ? json.get("title").getAsString() : "Interview Report");
        
        if (json.has("technicalQuestions")) {
            List<TechnicalQuestion> questions = new ArrayList<>();
            json.getAsJsonArray("technicalQuestions").forEach(elem -> {
                JsonObject q = elem.getAsJsonObject();
                questions.add(new TechnicalQuestion(
                    q.get("question").getAsString(),
                    q.get("intention").getAsString(),
                    q.get("answer").getAsString()
                ));
            });
            data.setTechnicalQuestions(questions);
        }
        
        if (json.has("behavioralQuestions")) {
            List<BehavioralQuestion> questions = new ArrayList<>();
            json.getAsJsonArray("behavioralQuestions").forEach(elem -> {
                JsonObject q = elem.getAsJsonObject();
                questions.add(new BehavioralQuestion(
                    q.get("question").getAsString(),
                    q.get("intention").getAsString(),
                    q.get("answer").getAsString()
                ));
            });
            data.setBehavioralQuestions(questions);
        }
        
        if (json.has("skillGaps")) {
            List<SkillGap> gaps = new ArrayList<>();
            json.getAsJsonArray("skillGaps").forEach(elem -> {
                JsonObject g = elem.getAsJsonObject();
                gaps.add(new SkillGap(
                    g.get("skill").getAsString(),
                    g.get("severity").getAsString()
                ));
            });
            data.setSkillGaps(gaps);
        }
        
        if (json.has("preparationPlan")) {
            List<PreparationPlan> plans = new ArrayList<>();
            json.getAsJsonArray("preparationPlan").forEach(elem -> {
                JsonObject p = elem.getAsJsonObject();
                String tasks = p.has("tasks") ? p.get("tasks").getAsString() : "";
                plans.add(new PreparationPlan(
                    p.get("day").getAsInt(),
                    p.get("focus").getAsString(),
                    tasks
                ));
            });
            data.setPreparationPlan(plans);
        }
        
        return data;
    }
    
    private byte[] convertHtmlToPdf(String html) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.toStream(os);
            builder.withHtmlContent(html, null);
            builder.run();
            return os.toByteArray();
        }
    }
}
