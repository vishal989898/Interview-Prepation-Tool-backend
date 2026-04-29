package com.interview.repository;

import com.interview.entity.InterviewReport;
import com.interview.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {
    List<InterviewReport> findByUserId(Long userId);
    Optional<InterviewReport> findByIdAndUserId(Long id, Long userId);
}
