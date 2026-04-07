package com.llm.open.llmscoring.repository;

import com.llm.open.llmscoring.entity.PaperEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperEntityRepository extends JpaRepository<PaperEntity, String> {

    Optional<PaperEntity> findByShareCodeIgnoreCase(String shareCode);

    List<PaperEntity> findByTeacher_IdOrderByUpdatedAtDesc(String teacherId);

    List<PaperEntity> findByCourse_IdOrderByUpdatedAtDesc(String courseId);
}