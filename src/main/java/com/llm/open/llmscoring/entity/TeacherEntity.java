package com.llm.open.llmscoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
public class TeacherEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 255)
    private String school;

    @Column(name = "taught_course", nullable = false, length = 255)
    private String taughtCourse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}