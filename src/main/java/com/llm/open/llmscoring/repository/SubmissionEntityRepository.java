package com.llm.open.llmscoring.repository;

import com.llm.open.llmscoring.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionEntityRepository extends JpaRepository<SubmissionEntity, String> {

    List<SubmissionEntity> findByTeacher_IdOrderBySubmittedAtDesc(String teacherId);

    List<SubmissionEntity> findByShareCodeIgnoreCaseAndStudentIdIgnoreCaseOrderBySubmittedAtDesc(String shareCode, String studentId);

    List<SubmissionEntity> findByPaper_Id(String paperId);
}