package com.llm.open.llmscoring.repository;

import com.llm.open.llmscoring.dto.Course;
import com.llm.open.llmscoring.dto.ExamPaper;
import com.llm.open.llmscoring.dto.Submission;
import com.llm.open.llmscoring.dto.Teacher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformRepository {

    Teacher saveTeacher(Teacher teacher);

    Optional<Teacher> findTeacher(UUID teacherId);

    Optional<Teacher> findTeacherByUsername(String username);

    List<Teacher> listTeachers();

    Course saveCourse(Course course);

    Optional<Course> findCourse(UUID courseId);

    List<Course> listCoursesByTeacher(UUID teacherId);

    void deleteCourse(UUID courseId);

    ExamPaper savePaper(ExamPaper paper);

    Optional<ExamPaper> findPaper(UUID paperId);

    Optional<ExamPaper> findPaperByShareCode(String shareCode);

    List<ExamPaper> listPapersByTeacher(UUID teacherId);

    List<ExamPaper> listPapersByCourse(UUID courseId);

    void deletePaper(UUID paperId);

    Submission saveSubmission(Submission submission);

    Optional<Submission> findSubmission(UUID submissionId);

    List<Submission> listSubmissionsByTeacher(UUID teacherId);

    Optional<Submission> findLatestSubmission(String shareCode, String studentId);
}