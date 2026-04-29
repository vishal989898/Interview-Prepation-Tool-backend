package com.interview.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationPlan {
    
    private Integer day;
    private String focus;
    private String tasks;  // Changed from List<String> to String (comma-separated)
}
