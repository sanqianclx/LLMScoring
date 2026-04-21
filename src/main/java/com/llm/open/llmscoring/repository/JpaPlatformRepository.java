package com.llm.open.llmscoring.repository;

import com.llm.open.llmscoring.dto.Course;
import com.llm.open.llmscoring.dto.ExamPaper;
import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionScore;
import com.llm.open.llmscoring.dto.QuestionType;
import com.llm.open.llmscoring.dto.ScoringPoint;
import com.llm.open.llmscoring.dto.StudentAnswer;
import com.llm.open.llmscoring.dto.Submission;
import com.llm.open.llmscoring.dto.Teacher;
import com.llm.open.llmscoring.entity.CourseEntity;
import com.llm.open.llmscoring.entity.PaperEntity;
import com.llm.open.llmscoring.entity.QuestionEntity;
import com.llm.open.llmscoring.entity.ScorePhase;
import com.llm.open.llmscoring.entity.ScorePointKind;
import com.llm.open.llmscoring.entity.ScoringPointEntity;
import com.llm.open.llmscoring.entity.SubmissionAnswerEntity;
import com.llm.open.llmscoring.entity.SubmissionEntity;
import com.llm.open.llmscoring.entity.SubmissionQuestionScoreEntity;
import com.llm.open.llmscoring.entity.SubmissionScorePointEntity;
import com.llm.open.llmscoring.entity.TeacherEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaPlatformRepository implements PlatformRepository {

    private final TeacherEntityRepository teacherRepository;
    private final CourseEntityRepository courseRepository;
    private final PaperEntityRepository paperRepository;
    private final SubmissionEntityRepository submissionRepository;

    public JpaPlatformRepository(
            TeacherEntityRepository teacherRepository,
            CourseEntityRepository courseRepository,
            PaperEntityRepository paperRepository,
            SubmissionEntityRepository submissionRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.paperRepository = paperRepository;
        this.submissionRepository = submissionRepository;
    }

    @PostConstruct
    void seedDemoData() {
        if (teacherRepository.findByUsernameIgnoreCase("teacher").isPresent()) {
            return;
        }

        Instant now = Instant.now();
        Teacher demoTeacher = saveTeacher(new Teacher(
                UUID.randomUUID(),
                "演示教师",
                "teacher",
                "teacher123",
                "演示学校",
                "生物学",
                now
        ));

        Course demoCourse = saveCourse(new Course(
                UUID.randomUUID(),
                demoTeacher.id(),
                "生物学核心概念",
                "用于演示试卷创建、学生提交与教师阅卷流程的课程。",
                now
        ));

        List<Question> questions = List.of(
                new Question(
                        UUID.randomUUID(),
                        QuestionType.FILL_BLANK,
                        "植物细胞中负责光合作用的细胞器是什么？",
                        "叶绿体",
                        10,
                        List.of(new ScoringPoint(UUID.randomUUID(), "叶绿体", 10, "识别叶绿体。"))
                ),
                new Question(
                        UUID.randomUUID(),
                        QuestionType.SHORT_ANSWER,
                        "简要描述光合作用的条件和产物。",
                        "光合作用需要光照、水和二氧化碳，并生成葡萄糖和氧气。",
                        20,
                        List.of(
                                new ScoringPoint(UUID.randomUUID(), "光|光照|阳光", 4, "提到光照。"),
                                new ScoringPoint(UUID.randomUUID(), "水|H2O", 4, "提到水。"),
                                new ScoringPoint(UUID.randomUUID(), "二氧化碳|CO2", 4, "提到二氧化碳。"),
                                new ScoringPoint(UUID.randomUUID(), "葡萄糖|有机物", 4, "提到葡萄糖或有机物。"),
                                new ScoringPoint(UUID.randomUUID(), "氧气|O2", 4, "提到氧气。")
                        )
                )
        );

        savePaper(new ExamPaper(
                UUID.randomUUID(),
                demoTeacher.id(),
                demoCourse.id(),
                "光合作用单元小测",
                "用于验证端到端评分流程的演示试卷。",
                "BIO-2026",
                true,
                questions,
                now,
                now
        ));
    }

    @Override
    public Teacher saveTeacher(Teacher teacher) {
        TeacherEntity entity = teacherRepository.findById(teacher.id().toString()).orElseGet(TeacherEntity::new);
        entity.setId(teacher.id().toString());
        entity.setName(teacher.name());
        entity.setUsername(teacher.username());
        entity.setPassword(teacher.password());
        entity.setSchool(teacher.school());
        entity.setTaughtCourse(teacher.taughtCourse());
        entity.setCreatedAt(teacher.createdAt());
        return toTeacher(teacherRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Teacher> findTeacher(UUID teacherId) {
        return teacherRepository.findById(teacherId.toString()).map(this::toTeacher);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Teacher> findTeacherByUsername(String username) {
        return teacherRepository.findByUsernameIgnoreCase(username).map(this::toTeacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> listTeachers() {
        return teacherRepository.findAll().stream().map(this::toTeacher).sorted(Comparator.comparing(Teacher::createdAt)).toList();
    }

    @Override
    public Course saveCourse(Course course) {
        CourseEntity entity = courseRepository.findById(course.id().toString()).orElseGet(CourseEntity::new);
        entity.setId(course.id().toString());
        entity.setTeacher(getTeacherEntity(course.teacherId()));
        entity.setName(course.name());
        entity.setDescription(course.description());
        entity.setCreatedAt(course.createdAt());
        return toCourse(courseRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Course> findCourse(UUID courseId) {
        return courseRepository.findById(courseId.toString()).map(this::toCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> listCoursesByTeacher(UUID teacherId) {
        return courseRepository.findByTeacher_IdOrderByCreatedAtAsc(teacherId.toString()).stream().map(this::toCourse).toList();
    }

    @Override
    public void deleteCourse(UUID courseId) {
        courseRepository.deleteById(courseId.toString());
    }

    @Override
    public ExamPaper savePaper(ExamPaper paper) {
        PaperEntity entity = paperRepository.findById(paper.id().toString()).orElseGet(PaperEntity::new);
        entity.setId(paper.id().toString());
        entity.setTeacher(getTeacherEntity(paper.teacherId()));
        entity.setCourse(getCourseEntity(paper.courseId()));
        entity.setTitle(paper.title());
        entity.setDescription(paper.description());
        entity.setShareCode(paper.shareCode());
        entity.setActive(paper.active());
        entity.setCreatedAt(paper.createdAt());
        entity.setUpdatedAt(paper.updatedAt());
        replaceQuestionEntities(entity, paper.questions());
        return toPaper(paperRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamPaper> findPaper(UUID paperId) {
        return paperRepository.findById(paperId.toString()).map(this::toPaper);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamPaper> findPaperByShareCode(String shareCode) {
        return paperRepository.findByShareCodeIgnoreCase(shareCode).map(this::toPaper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamPaper> listPapersByTeacher(UUID teacherId) {
        return paperRepository.findByTeacher_IdOrderByUpdatedAtDesc(teacherId.toString()).stream().map(this::toPaper).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamPaper> listPapersByCourse(UUID courseId) {
        return paperRepository.findByCourse_IdOrderByUpdatedAtDesc(courseId.toString()).stream().map(this::toPaper).toList();
    }

    @Override
    public void deletePaper(UUID paperId) {
        List<SubmissionEntity> submissions = submissionRepository.findByPaper_Id(paperId.toString());
        if (!submissions.isEmpty()) {
            submissionRepository.deleteAll(submissions);
        }
        paperRepository.deleteById(paperId.toString());
    }

    @Override
    public Submission saveSubmission(Submission submission) {
        SubmissionEntity entity = submissionRepository.findById(submission.id().toString()).orElseGet(SubmissionEntity::new);
        entity.setId(submission.id().toString());
        entity.setPaper(getPaperEntity(submission.paperId()));
        entity.setTeacher(getTeacherEntity(submission.teacherId()));
        entity.setCourse(getCourseEntity(submission.courseId()));
        entity.setShareCode(submission.shareCode());
        entity.setStudentId(submission.studentId());
        entity.setStudentName(submission.studentName());
        entity.setAutoTotal(submission.autoTotal());
        entity.setFinalTotal(submission.finalTotal());
        entity.setOverallFeedback(submission.overallFeedback());
        entity.setStatus(submission.status());
        entity.setSubmittedAt(submission.submittedAt());
        entity.setReviewedAt(submission.reviewedAt());
        entity.getAnswers().clear();
        entity.getQuestionScores().clear();

        int answerOrder = 0;
        for (StudentAnswer answer : submission.answers()) {
            SubmissionAnswerEntity answerEntity = new SubmissionAnswerEntity();
            answerEntity.setId(UUID.randomUUID().toString());
            answerEntity.setSubmission(entity);
            answerEntity.setQuestionId(answer.questionId().toString());
            answerEntity.setAnswerText(answer.answerText());
            answerEntity.setSortOrder(answerOrder++);
            entity.getAnswers().add(answerEntity);
        }

        appendQuestionScores(entity, submission.autoScores(), ScorePhase.AUTO);
        appendQuestionScores(entity, submission.finalScores(), ScorePhase.FINAL);

        return toSubmission(submissionRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Submission> findSubmission(UUID submissionId) {
        return submissionRepository.findById(submissionId.toString()).map(this::toSubmission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Submission> listSubmissionsByTeacher(UUID teacherId) {
        return submissionRepository.findByTeacher_IdOrderBySubmittedAtDesc(teacherId.toString()).stream().map(this::toSubmission).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Submission> findLatestSubmission(String shareCode, String studentId) {
        return submissionRepository.findByShareCodeIgnoreCaseAndStudentIdIgnoreCaseOrderBySubmittedAtDesc(shareCode, studentId).stream().findFirst().map(this::toSubmission);
    }

    private void appendQuestionScores(SubmissionEntity submissionEntity, List<QuestionScore> scores, ScorePhase phase) {
        int scoreOrder = 0;
        for (QuestionScore score : scores) {
            SubmissionQuestionScoreEntity scoreEntity = new SubmissionQuestionScoreEntity();
            scoreEntity.setId(UUID.randomUUID().toString());
            scoreEntity.setSubmission(submissionEntity);
            scoreEntity.setQuestionId(score.questionId().toString());
            scoreEntity.setPhase(phase);
            scoreEntity.setScore(score.score());
            scoreEntity.setMaxScore(score.maxScore());
            scoreEntity.setComment(score.comment());
            scoreEntity.setRationale(score.rationale());
            scoreEntity.setOverridden(score.overridden());
            scoreEntity.setSortOrder(scoreOrder++);

            int pointOrder = 0;
            for (String item : score.matchedPoints()) {
                SubmissionScorePointEntity pointEntity = new SubmissionScorePointEntity();
                pointEntity.setId(UUID.randomUUID().toString());
                pointEntity.setQuestionScore(scoreEntity);
                pointEntity.setPointText(item);
                pointEntity.setPointKind(ScorePointKind.MATCHED);
                pointEntity.setSortOrder(pointOrder++);
                scoreEntity.getPoints().add(pointEntity);
            }
            for (String item : score.missingPoints()) {
                SubmissionScorePointEntity pointEntity = new SubmissionScorePointEntity();
                pointEntity.setId(UUID.randomUUID().toString());
                pointEntity.setQuestionScore(scoreEntity);
                pointEntity.setPointText(item);
                pointEntity.setPointKind(ScorePointKind.MISSING);
                pointEntity.setSortOrder(pointOrder++);
                scoreEntity.getPoints().add(pointEntity);
            }
            submissionEntity.getQuestionScores().add(scoreEntity);
        }
    }

    private TeacherEntity getTeacherEntity(UUID teacherId) {
        return teacherRepository.findById(teacherId.toString()).orElseThrow();
    }

    private CourseEntity getCourseEntity(UUID courseId) {
        return courseRepository.findById(courseId.toString()).orElseThrow();
    }

    private PaperEntity getPaperEntity(UUID paperId) {
        return paperRepository.findById(paperId.toString()).orElseThrow();
    }

    private Teacher toTeacher(TeacherEntity entity) {
        return new Teacher(
                UUID.fromString(entity.getId()),
                entity.getName(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getSchool(),
                entity.getTaughtCourse(),
                entity.getCreatedAt()
        );
    }

    private Course toCourse(CourseEntity entity) {
        return new Course(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getTeacher().getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    private ExamPaper toPaper(PaperEntity entity) {
        List<Question> questions = entity.getQuestions().stream()
                .sorted(Comparator.comparingInt(QuestionEntity::getSortOrder))
                .map(this::toQuestion)
                .toList();
        return new ExamPaper(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getTeacher().getId()),
                UUID.fromString(entity.getCourse().getId()),
                entity.getTitle(),
                entity.getDescription(),
                entity.getShareCode(),
                entity.isActive(),
                questions,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void replaceQuestionEntities(PaperEntity entity, List<Question> questions) {
        entity.getQuestions().clear();

        int questionOrder = 0;
        for (Question question : questions) {
            QuestionEntity questionEntity = new QuestionEntity();
            questionEntity.setId(question.id().toString());
            questionEntity.setPaper(entity);
            questionEntity.setType(question.type());
            questionEntity.setText(question.text());
            questionEntity.setReferenceAnswer(question.referenceAnswer());
            questionEntity.setMaxScore(question.maxScore());
            questionEntity.setSortOrder(questionOrder++);

            int pointOrder = 0;
            for (ScoringPoint point : question.scoringPoints()) {
                ScoringPointEntity pointEntity = new ScoringPointEntity();
                pointEntity.setId(point.id().toString());
                pointEntity.setQuestion(questionEntity);
                pointEntity.setKeyword(point.keyword());
                pointEntity.setScore(point.score());
                pointEntity.setDescription(point.description());
                pointEntity.setSortOrder(pointOrder++);
                questionEntity.getScoringPoints().add(pointEntity);
            }
            entity.getQuestions().add(questionEntity);
        }
    }

    private Question toQuestion(QuestionEntity entity) {
        return new Question(
                UUID.fromString(entity.getId()),
                entity.getType(),
                entity.getText(),
                entity.getReferenceAnswer(),
                entity.getMaxScore(),
                entity.getScoringPoints().stream().sorted(Comparator.comparingInt(ScoringPointEntity::getSortOrder)).map(this::toScoringPoint).toList()
        );
    }

    private ScoringPoint toScoringPoint(ScoringPointEntity entity) {
        return new ScoringPoint(
                UUID.fromString(entity.getId()),
                entity.getKeyword(),
                entity.getScore(),
                entity.getDescription()
        );
    }

    private Submission toSubmission(SubmissionEntity entity) {
        return new Submission(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getPaper().getId()),
                UUID.fromString(entity.getTeacher().getId()),
                UUID.fromString(entity.getCourse().getId()),
                entity.getShareCode(),
                entity.getStudentId(),
                entity.getStudentName(),
                entity.getAnswers().stream().sorted(Comparator.comparingInt(SubmissionAnswerEntity::getSortOrder)).map(this::toStudentAnswer).toList(),
                entity.getQuestionScores().stream().filter(item -> item.getPhase() == ScorePhase.AUTO).sorted(Comparator.comparingInt(SubmissionQuestionScoreEntity::getSortOrder)).map(this::toQuestionScore).toList(),
                entity.getQuestionScores().stream().filter(item -> item.getPhase() == ScorePhase.FINAL).sorted(Comparator.comparingInt(SubmissionQuestionScoreEntity::getSortOrder)).map(this::toQuestionScore).toList(),
                entity.getAutoTotal(),
                entity.getFinalTotal(),
                entity.getOverallFeedback(),
                entity.getStatus(),
                entity.getSubmittedAt(),
                entity.getReviewedAt()
        );
    }

    private StudentAnswer toStudentAnswer(SubmissionAnswerEntity entity) {
        return new StudentAnswer(UUID.fromString(entity.getQuestionId()), entity.getAnswerText());
    }

    private QuestionScore toQuestionScore(SubmissionQuestionScoreEntity entity) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        entity.getPoints().stream().sorted(Comparator.comparingInt(SubmissionScorePointEntity::getSortOrder)).forEach(point -> {
            if (point.getPointKind() == ScorePointKind.MATCHED) {
                matched.add(point.getPointText());
            } else {
                missing.add(point.getPointText());
            }
        });
        return new QuestionScore(
                UUID.fromString(entity.getQuestionId()),
                entity.getScore(),
                entity.getMaxScore(),
                matched,
                missing,
                entity.getComment(),
                entity.getRationale(),
                entity.isOverridden()
        );
    }
}