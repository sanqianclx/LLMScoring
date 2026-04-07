package com.llm.open.llmscoring.repository;

import com.llm.open.llmscoring.model.Course;
import com.llm.open.llmscoring.model.ExamPaper;
import com.llm.open.llmscoring.model.Question;
import com.llm.open.llmscoring.model.QuestionType;
import com.llm.open.llmscoring.model.ScoringPoint;
import com.llm.open.llmscoring.model.Submission;
import com.llm.open.llmscoring.model.Teacher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPlatformRepository {

    private final Map<UUID, Teacher> teachers = new ConcurrentHashMap<>();
    private final Map<UUID, Course> courses = new ConcurrentHashMap<>();
    private final Map<UUID, ExamPaper> papers = new ConcurrentHashMap<>();
    private final Map<UUID, Submission> submissions = new ConcurrentHashMap<>();

    public InMemoryPlatformRepository() {
        seedDemoData();
    }

    public Teacher saveTeacher(Teacher teacher) {
        teachers.put(teacher.id(), teacher);
        return teacher;
    }

    public Optional<Teacher> findTeacher(UUID teacherId) {
        return Optional.ofNullable(teachers.get(teacherId));
    }

    public Optional<Teacher> findTeacherByUsername(String username) {
        return teachers.values().stream()
                .filter(teacher -> teacher.username().equalsIgnoreCase(username))
                .findFirst();
    }

    public List<Teacher> listTeachers() {
        return teachers.values().stream()
                .sorted(Comparator.comparing(Teacher::createdAt))
                .toList();
    }

    public Course saveCourse(Course course) {
        courses.put(course.id(), course);
        return course;
    }

    public Optional<Course> findCourse(UUID courseId) {
        return Optional.ofNullable(courses.get(courseId));
    }

    public List<Course> listCoursesByTeacher(UUID teacherId) {
        return courses.values().stream()
                .filter(course -> course.teacherId().equals(teacherId))
                .sorted(Comparator.comparing(Course::createdAt))
                .toList();
    }

    public void deleteCourse(UUID courseId) {
        courses.remove(courseId);
    }

    public ExamPaper savePaper(ExamPaper paper) {
        papers.put(paper.id(), paper);
        return paper;
    }

    public Optional<ExamPaper> findPaper(UUID paperId) {
        return Optional.ofNullable(papers.get(paperId));
    }

    public Optional<ExamPaper> findPaperByShareCode(String shareCode) {
        return papers.values().stream()
                .filter(paper -> paper.shareCode().equalsIgnoreCase(shareCode))
                .findFirst();
    }

    public List<ExamPaper> listPapersByTeacher(UUID teacherId) {
        return papers.values().stream()
                .filter(paper -> paper.teacherId().equals(teacherId))
                .sorted(Comparator.comparing(ExamPaper::updatedAt).reversed())
                .toList();
    }

    public List<ExamPaper> listPapersByCourse(UUID courseId) {
        return papers.values().stream()
                .filter(paper -> paper.courseId().equals(courseId))
                .toList();
    }

    public void deletePaper(UUID paperId) {
        papers.remove(paperId);
        new ArrayList<>(submissions.values()).stream()
                .filter(submission -> submission.paperId().equals(paperId))
                .map(Submission::id)
                .forEach(submissions::remove);
    }

    public Submission saveSubmission(Submission submission) {
        submissions.put(submission.id(), submission);
        return submission;
    }

    public Optional<Submission> findSubmission(UUID submissionId) {
        return Optional.ofNullable(submissions.get(submissionId));
    }

    public List<Submission> listSubmissionsByTeacher(UUID teacherId) {
        return submissions.values().stream()
                .filter(submission -> submission.teacherId().equals(teacherId))
                .sorted(Comparator.comparing(Submission::submittedAt).reversed())
                .toList();
    }

    public Optional<Submission> findLatestSubmission(String shareCode, String studentId) {
        return submissions.values().stream()
                .filter(submission -> submission.shareCode().equalsIgnoreCase(shareCode))
                .filter(submission -> submission.studentId().equalsIgnoreCase(studentId))
                .max(Comparator.comparing(Submission::submittedAt));
    }

    private void seedDemoData() {
        Instant now = Instant.now();
        Teacher demoTeacher = saveTeacher(new Teacher(
                UUID.randomUUID(),
                "林老师",
                "teacher",
                "teacher123",
                "星火中学",
                "高中生物",
                now
        ));

        Course demoCourse = saveCourse(new Course(
                UUID.randomUUID(),
                demoTeacher.id(),
                "高中生物核心知识",
                "围绕细胞结构与光合作用的核心单元。",
                now
        ));

        List<Question> questions = List.of(
                new Question(
                        UUID.randomUUID(),
                        QuestionType.FILL_BLANK,
                        "植物细胞中主要进行光合作用的细胞器是什么？",
                        "叶绿体",
                        10,
                        List.of(
                                new ScoringPoint(UUID.randomUUID(), "chloroplast|叶绿体", 10, "能够准确指出叶绿体。")
                        )
                ),
                new Question(
                        UUID.randomUUID(),
                        QuestionType.SHORT_ANSWER,
                        "简述光合作用的基本条件与主要产物。",
                        "光合作用需要光照、水和二氧化碳，产物为有机物和氧气。",
                        20,
                        List.of(
                                new ScoringPoint(UUID.randomUUID(), "light|sunlight|光照", 4, "提到光照是必要条件。"),
                                new ScoringPoint(UUID.randomUUID(), "water|H2O|水", 4, "提到水是反应物。"),
                                new ScoringPoint(UUID.randomUUID(), "carbon dioxide|CO2|二氧化碳", 4, "提到二氧化碳是反应物。"),
                                new ScoringPoint(UUID.randomUUID(), "organic matter|glucose|有机物|葡萄糖", 4, "提到有机物或葡萄糖是产物。"),
                                new ScoringPoint(UUID.randomUUID(), "oxygen|O2|氧气", 4, "提到氧气是产物。")
                        )
                )
        );

        savePaper(new ExamPaper(
                UUID.randomUUID(),
                demoTeacher.id(),
                demoCourse.id(),
                "光合作用单元测评",
                "用于演示学生提交、自动评分和教师审核的示例试卷。",
                "BIO-2026",
                true,
                questions,
                now,
                now
        ));
    }
}