package com.llm.open.llmscoring.repository;

import com.llm.open.llmscoring.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseEntityRepository extends JpaRepository<CourseEntity, String> {

    List<CourseEntity> findByTeacher_IdOrderByCreatedAtAsc(String teacherId);
}