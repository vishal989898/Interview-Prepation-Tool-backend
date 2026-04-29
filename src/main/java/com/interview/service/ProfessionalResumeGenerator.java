package com.interview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Professional Resume Generator - Creates ATS-friendly, professional resumes
 * tailored to job descriptions using extracted resume data.
 */
public class ProfessionalResumeGenerator {

    public static String generateProfessionalResume(
            String candidateName,
            String professionalSummary,
            String technicalSkills,
            String experienceSection,
            String projectsSection,
            String educationSection) {
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8" />
                <style>
                    @page {
                        size: A4;
                        margin: 1cm;
                    }
                    body { 
                        font-family: 'Calibri', 'Segoe UI', Arial, sans-serif; 
                        line-height: 1.4; 
                        color: #333; 
                        margin: 0;
                        padding: 0;
                        font-size: 10.5pt;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 25px 20px;
                        margin: -1cm -1cm 20px -1cm;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0 0 8px 0;
                        font-size: 26pt;
                        font-weight: 700;
                        letter-spacing: 1px;
                    }
                    .header .subtitle {
                        font-size: 10pt;
                        opacity: 0.95;
                        margin: 0;
                    }
                    .section {
                        margin-bottom: 18px;
                    }
                    .section-header {
                        background-color: #f8f9fa;
                        border-left: 4px solid #667eea;
                        padding: 6px 10px;
                        margin-bottom: 12px;
                    }
                    .section-title {
                        color: #667eea;
                        font-size: 12pt;
                        font-weight: 700;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin: 0;
                    }
                    .summary {
                        padding: 0 5px;
                        font-size: 10pt;
                        line-height: 1.5;
                        color: #555;
                    }
                    .skills-container {
                        padding: 0 5px;
                    }
                    .skill-category {
                        margin-bottom: 10px;
                    }
                    .skill-category-title {
                        font-weight: 600;
                        color: #667eea;
                        font-size: 10pt;
                        margin-bottom: 5px;
                    }
                    .skills-list {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 6px;
                    }
                    .skill-item {
                        background-color: #f0f2ff;
                        color: #667eea;
                        padding: 4px 10px;
                        border-radius: 3px;
                        font-size: 9pt;
                        font-weight: 500;
                        border: 1px solid #e0e3ff;
                    }
                    .skill-item.highlight {
                        background-color: #667eea;
                        color: white;
                        border-color: #667eea;
                    }
                    .experience-item, .project-item, .education-item {
                        margin-bottom: 12px;
                        padding: 0 5px;
                    }
                    .item-header {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 4px;
                    }
                    .title {
                        font-weight: 700;
                        font-size: 11pt;
                        color: #333;
                    }
                    .subtitle-info {
                        color: #667eea;
                        font-size: 10pt;
                        font-weight: 600;
                    }
                    .date {
                        color: #666;
                        font-size: 9pt;
                        font-style: italic;
                    }
                    .description {
                        font-size: 9.5pt;
                        line-height: 1.4;
                        color: #555;
                        margin: 5px 0 0 0;
                    }
                    .description ul {
                        margin: 5px 0 0 15px;
                        padding: 0;
                    }
                    .description li {
                        margin-bottom: 3px;
                    }
                    .tech-stack {
                        color: #667eea;
                        font-size: 8.5pt;
                        font-style: italic;
                        margin-top: 3px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>%s</h1>
                    <p class="subtitle">Software Professional | Open to New Opportunities</p>
                </div>
                
                <div class="section">
                    <div class="section-header">
                        <h2 class="section-title">Professional Summary</h2>
                    </div>
                    <div class="summary">
                        <p>%s</p>
                    </div>
                </div>
                
                <div class="section">
                    <div class="section-header">
                        <h2 class="section-title">Technical Skills</h2>
                    </div>
                    <div class="skills-container">
                        %s
                    </div>
                </div>
                
                %s
                
                %s
                
                %s
            </body>
            </html>
            """,
            candidateName,
            professionalSummary,
            technicalSkills,
            experienceSection.isEmpty() ? "" : "<div class=\"section\"><div class=\"section-header\"><h2 class=\"section-title\">Professional Experience</h2></div>" + experienceSection + "</div>",
            projectsSection.isEmpty() ? "" : "<div class=\"section\"><div class=\"section-header\"><h2 class=\"section-title\">Key Projects</h2></div>" + projectsSection + "</div>",
            educationSection.isEmpty() ? "" : "<div class=\"section\"><div class=\"section-header\"><h2 class=\"section-title\">Education</h2></div>" + educationSection + "</div>"
        );
    }
}
