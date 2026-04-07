package com.llm.open.llmscoring.repository;

import com.llm.open.llmscoring.entity.TeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherEntityRepository extends JpaRepository<TeacherEntity, String> {

    Optional<TeacherEntity> findByUsernameIgnoreCase(String username);
}