package com.llm.open.llmscoring.service;

import com.llm.open.llmscoring.dto.Course;
import com.llm.open.llmscoring.dto.ExamPaper;
import com.llm.open.llmscoring.dto.Question;
import com.llm.open.llmscoring.dto.QuestionScore;
import com.llm.open.llmscoring.dto.QuestionType;
import com.llm.open.llmscoring.dto.ScoringPoint;
import com.llm.open.llmscoring.dto.StudentAnswer;
import com.llm.open.llmscoring.dto.Submission;
import com.llm.open.llmscoring.dto.SubmissionStatus;
import com.llm.open.llmscoring.dto.Teacher;
import com.llm.open.llmscoring.repository.PlatformRepository;
import com.llm.open.llmscoring.dto.ApiModels;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PlatformService {

    private final PlatformRepository repository;
    private final ScoringService scoringService;

    public PlatformService(PlatformRepository repository, ScoringService scoringService) {
        this.repository = repository;
        this.scoringService = scoringService;
    }

    public ApiModels.BootstrapView bootstrap() {
        Teacher demoTeacher = repository.findTeacherByUsername("teacher")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "默认教师数据缺失"));
        return new ApiModels.BootstrapView(
                "已加载本地开发演示账号。",
                toTeacherView(demoTeacher),
                "BIO-2026",
                repository.listTeachers().stream().map(this::toTeacherView).toList()
        );
    }

    public ApiModels.TeacherView registerTeacher(ApiModels.RegisterTeacherRequest request) {
        requireNonBlank(request.name(), "教师姓名不能为空");
        requireNonBlank(request.username(), "用户名不能为空");
        requireNonBlank(request.password(), "密码不能为空");
        requireNonBlank(request.school(), "学校不能为空");
        requireNonBlank(request.taughtCourse(), "至少填写一门任教课程");

        repository.findTeacherByUsername(request.username()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        });

        Teacher teacher = repository.saveTeacher(new Teacher(
                UUID.randomUUID(),
                request.name().trim(),
                request.username().trim(),
                request.password().trim(),
                request.school().trim(),
                request.taughtCourse().trim(),
                Instant.now()
        ));
        return toTeacherView(teacher);
    }

    public ApiModels.TeacherDashboardView login(ApiModels.LoginRequest request) {
        Teacher teacher = repository.findTeacherByUsername(trimmed(request.username()))
                .filter(found -> Objects.equals(found.password(), trimmed(request.password())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        return getTeacherDashboard(teacher.id());
    }

    public ApiModels.TeacherDashboardView getTeacherDashboard(UUID teacherId) {
        Teacher teacher = getTeacher(teacherId);
        List<Course> courses = repository.listCoursesByTeacher(teacherId);
        List<ExamPaper> papers = repository.listPapersByTeacher(teacherId);
        List<Submission> submissions = repository.listSubmissionsByTeacher(teacherId);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courseCount", courses.size());
        stats.put("paperCount", papers.size());
        stats.put("submissionCount", submissions.size());
        stats.put("reviewedCount", submissions.stream().filter(item -> item.status() == SubmissionStatus.REVIEWED).count());
        stats.put("pendingCount", submissions.stream().filter(item -> item.status() == SubmissionStatus.PENDING_REVIEW).count());

        return new ApiModels.TeacherDashboardView(
                toTeacherView(teacher),
                courses.stream().map(this::toCourseView).toList(),
                papers.stream().map(this::toPaperView).toList(),
                submissions.stream().map(this::toSubmissionView).toList(),
                stats
        );
    }

    public ApiModels.TeacherView updateTeacher(UUID teacherId, ApiModels.UpdateTeacherRequest request) {
        Teacher existing = getTeacher(teacherId);
        Teacher updated = repository.saveTeacher(new Teacher(
                existing.id(),
                valueOrExisting(request.name(), existing.name()),
                existing.username(),
                existing.password(),
                valueOrExisting(request.school(), existing.school()),
                valueOrExisting(request.taughtCourse(), existing.taughtCourse()),
                existing.createdAt()
        ));
        return toTeacherView(updated);
    }

    public ApiModels.CourseView createCourse(UUID teacherId, ApiModels.CourseRequest request) {
        getTeacher(teacherId);
        requireNonBlank(request.name(), "课程名称不能为空");
        Course course = repository.saveCourse(new Course(
                UUID.randomUUID(),
                teacherId,
                request.name().trim(),
                trimmed(request.description()),
                Instant.now()
        ));
        return toCourseView(course);
    }

    public void deleteCourse(UUID teacherId, UUID courseId) {
        getTeacher(teacherId);
        Course course = repository.findCourse(courseId)
                .filter(item -> item.teacherId().equals(teacherId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到课程"));
        if (!repository.listPapersByCourse(course.id()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "请先删除该课程下的试卷再删除课程");
        }
        repository.deleteCourse(course.id());
    }

    public ApiModels.PaperView createPaper(UUID teacherId, ApiModels.PaperRequest request) {
        return savePaper(null, teacherId, request);
    }

    public ApiModels.PaperView updatePaper(UUID teacherId, UUID paperId, ApiModels.PaperRequest request) {
        return savePaper(paperId, teacherId, request);
    }

    public void deletePaper(UUID teacherId, UUID paperId) {
        ExamPaper paper = getPaperOwnedByTeacher(teacherId, paperId);
        repository.deletePaper(paper.id());
    }

    public ApiModels.StudentPaperView getStudentPaper(String shareCode) {
        ExamPaper paper = repository.findPaperByShareCode(trimmed(shareCode))
                .filter(ExamPaper::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到试卷或试卷已停用"));
        return toStudentPaperView(paper);
    }

    public ApiModels.SubmissionView submitAnswers(String shareCode, ApiModels.SubmissionRequest request) {
        requireNonBlank(request.studentId(), "学号不能为空");
        ExamPaper paper = repository.findPaperByShareCode(trimmed(shareCode))
                .filter(ExamPaper::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "分享码无效"));
        Course course = repository.findCourse(paper.courseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到课程"));

        Map<UUID, String> answerMap = new HashMap<>();
        if (request.answers() != null) {
            request.answers().forEach(answer -> {
                if (answer.questionId() != null) {
                    answerMap.put(answer.questionId(), trimmed(answer.answerText()));
                }
            });
        }

        List<StudentAnswer> answers = paper.questions().stream()
                .map(question -> new StudentAnswer(question.id(), answerMap.getOrDefault(question.id(), "")))
                .toList();

        List<QuestionScore> autoScores = paper.questions().stream()
                .map(question -> scoringService.scoreQuestion(course.name(), question, answerMap.getOrDefault(question.id(), "")))
                .toList();

        double autoTotal = round(autoScores.stream().mapToDouble(QuestionScore::score).sum());
        Submission submission = repository.saveSubmission(new Submission(
                UUID.randomUUID(),
                paper.id(),
                paper.teacherId(),
                paper.courseId(),
                paper.shareCode(),
                request.studentId().trim(),
                defaultIfBlank(request.studentName(), "未命名学生"),
                answers,
                autoScores,
                autoScores,
                autoTotal,
                autoTotal,
                scoringService.buildOverallFeedback(autoTotal, paper.questions().stream().mapToDouble(Question::maxScore).sum()),
                SubmissionStatus.PENDING_REVIEW,
                Instant.now(),
                null
        ));
        return toSubmissionView(submission);
    }

    public ApiModels.StudentResultView getStudentResult(String shareCode, String studentId) {
        Submission submission = repository.findLatestSubmission(trimmed(shareCode), trimmed(studentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到提交记录"));
        ExamPaper paper = repository.findPaper(submission.paperId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到试卷"));

        if (submission.status() != SubmissionStatus.REVIEWED) {
            return new ApiModels.StudentResultView(
                    "已收到提交，教师尚未发布复核后的成绩。",
                    submission.status(),
                    submission.finalTotal(),
                    submission.overallFeedback(),
                    toStudentPaperView(paper),
                    List.of()
            );
        }

        return new ApiModels.StudentResultView(
                "复核已完成，最终成绩已公布。",
                submission.status(),
                submission.finalTotal(),
                submission.overallFeedback(),
                toStudentPaperView(paper),
                submission.finalScores().stream().map(this::toQuestionScoreView).toList()
        );
    }

    public ApiModels.SubmissionView reviewSubmission(UUID teacherId, UUID submissionId, ApiModels.ReviewSubmissionRequest request) {
        Submission submission = repository.findSubmission(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到提交记录"));
        if (!submission.teacherId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权复核该提交");
        }
        ExamPaper paper = repository.findPaper(submission.paperId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到试卷"));

        Map<UUID, ApiModels.QuestionReviewRequest> reviewMap = new HashMap<>();
        if (request.questionReviews() != null) {
            request.questionReviews().forEach(review -> {
                if (review.questionId() != null) {
                    reviewMap.put(review.questionId(), review);
                }
            });
        }

        Map<UUID, Question> questionMap = new LinkedHashMap<>();
        paper.questions().forEach(question -> questionMap.put(question.id(), question));

        List<QuestionScore> reviewedScores = submission.autoScores().stream()
                .map(score -> applyReview(score, questionMap.get(score.questionId()), reviewMap.get(score.questionId())))
                .toList();

        double finalTotal = round(reviewedScores.stream().mapToDouble(QuestionScore::score).sum());
        String overallFeedback = hasText(request.overallFeedback())
                ? request.overallFeedback().trim()
                : scoringService.buildOverallFeedback(finalTotal, paper.questions().stream().mapToDouble(Question::maxScore).sum());

        Submission reviewed = repository.saveSubmission(new Submission(
                submission.id(),
                submission.paperId(),
                submission.teacherId(),
                submission.courseId(),
                submission.shareCode(),
                submission.studentId(),
                submission.studentName(),
                submission.answers(),
                submission.autoScores(),
                reviewedScores,
                submission.autoTotal(),
                finalTotal,
                overallFeedback,
                SubmissionStatus.REVIEWED,
                submission.submittedAt(),
                Instant.now()
        ));
        return toSubmissionView(reviewed);
    }

    private ApiModels.PaperView savePaper(UUID existingPaperId, UUID teacherId, ApiModels.PaperRequest request) {
        getTeacher(teacherId);
        requireNonBlank(request.title(), "试卷标题不能为空");
        if (request.courseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "试卷必须归属某个课程");
        }
        Course course = repository.findCourse(request.courseId())
                .filter(item -> item.teacherId().equals(teacherId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到课程"));

        List<Question> questions = mapQuestions(request.questions());
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "试卷至少包含一道题目");
        }

        ExamPaper existing = existingPaperId == null ? null : getPaperOwnedByTeacher(teacherId, existingPaperId);
        Instant now = Instant.now();
        String shareCode = existing == null ? generateShareCode(course.name(), request.title()) : existing.shareCode();
        ExamPaper paper = repository.savePaper(new ExamPaper(
                existing == null ? UUID.randomUUID() : existing.id(),
                teacherId,
                course.id(),
                request.title().trim(),
                trimmed(request.description()),
                shareCode,
                request.active() == null || request.active(),
                questions,
                existing == null ? now : existing.createdAt(),
                now
        ));
        return toPaperView(paper);
    }

    private QuestionScore applyReview(QuestionScore autoScore, Question question, ApiModels.QuestionReviewRequest review) {
        if (review == null) {
            return autoScore;
        }
        double maxScore = question == null ? autoScore.maxScore() : question.maxScore();
        double score = review.score() == null ? autoScore.score() : Math.max(0, Math.min(review.score(), maxScore));
        String comment = hasText(review.comment()) ? review.comment().trim() : autoScore.comment();
        String rationale = hasText(review.rationale()) ? review.rationale().trim() : autoScore.rationale();

        return new QuestionScore(
                autoScore.questionId(),
                round(score),
                autoScore.maxScore(),
                autoScore.matchedPoints(),
                autoScore.missingPoints(),
                comment,
                rationale,
                true
        );
    }

    private List<Question> mapQuestions(List<ApiModels.QuestionRequest> questionRequests) {
        if (questionRequests == null) {
            return List.of();
        }
        List<Question> questions = new ArrayList<>();
        for (ApiModels.QuestionRequest request : questionRequests) {
            if (request == null) {
                continue;
            }
            requireNonBlank(request.text(), "题干不能为空");
            double maxScore = request.maxScore() == null ? 0 : request.maxScore();
            if (maxScore <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题目满分必须大于 0");
            }

            List<ScoringPoint> scoringPoints = new ArrayList<>();
            if (request.scoringPoints() != null) {
                for (ApiModels.ScoringPointRequest pointRequest : request.scoringPoints()) {
                    if (pointRequest == null || !hasText(pointRequest.keyword())) {
                        continue;
                    }
                    double pointScore = pointRequest.score() == null ? 0 : pointRequest.score();
                    if (pointScore <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "采分点分值必须大于 0");
                    }
                    scoringPoints.add(new ScoringPoint(
                            UUID.randomUUID(),
                            pointRequest.keyword().trim(),
                            pointScore,
                            defaultIfBlank(pointRequest.description(), pointRequest.keyword())
                    ));
                }
            }

            questions.add(new Question(
                    UUID.randomUUID(),
                    request.type() == null ? QuestionType.SHORT_ANSWER : request.type(),
                    request.text().trim(),
                    trimmed(request.referenceAnswer()),
                    maxScore,
                    scoringPoints
            ));
        }
        return questions;
    }

    private Teacher getTeacher(UUID teacherId) {
        return repository.findTeacher(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到教师"));
    }

    private ExamPaper getPaperOwnedByTeacher(UUID teacherId, UUID paperId) {
        return repository.findPaper(paperId)
                .filter(paper -> paper.teacherId().equals(teacherId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到试卷"));
    }

    private ApiModels.TeacherView toTeacherView(Teacher teacher) {
        return new ApiModels.TeacherView(
                teacher.id(),
                teacher.name(),
                teacher.username(),
                teacher.school(),
                teacher.taughtCourse()
        );
    }

    private ApiModels.CourseView toCourseView(Course course) {
        return new ApiModels.CourseView(
                course.id(),
                course.teacherId(),
                course.name(),
                course.description(),
                course.createdAt()
        );
    }

    private ApiModels.PaperView toPaperView(ExamPaper paper) {
        return new ApiModels.PaperView(
                paper.id(),
                paper.teacherId(),
                paper.courseId(),
                paper.title(),
                paper.description(),
                paper.shareCode(),
                paper.active(),
                paper.questions().stream().map(this::toQuestionView).toList(),
                paper.createdAt(),
                paper.updatedAt()
        );
    }

    private ApiModels.StudentPaperView toStudentPaperView(ExamPaper paper) {
        return new ApiModels.StudentPaperView(
                paper.id(),
                paper.courseId(),
                paper.title(),
                paper.description(),
                paper.shareCode(),
                paper.questions().stream()
                        .map(question -> new ApiModels.StudentQuestionView(
                                question.id(),
                                question.type(),
                                question.text(),
                                question.maxScore()
                        ))
                        .toList()
        );
    }

    private ApiModels.QuestionView toQuestionView(Question question) {
        return new ApiModels.QuestionView(
                question.id(),
                question.type(),
                question.text(),
                question.referenceAnswer(),
                question.maxScore(),
                question.scoringPoints().stream()
                        .map(point -> new ApiModels.ScoringPointView(point.id(), point.keyword(), point.score(), point.description()))
                        .toList()
        );
    }

    private ApiModels.SubmissionView toSubmissionView(Submission submission) {
        return new ApiModels.SubmissionView(
                submission.id(),
                submission.paperId(),
                submission.teacherId(),
                submission.courseId(),
                submission.shareCode(),
                submission.studentId(),
                submission.studentName(),
                submission.answers().stream()
                        .map(answer -> new ApiModels.StudentAnswerView(answer.questionId(), answer.answerText()))
                        .toList(),
                submission.autoScores().stream().map(this::toQuestionScoreView).toList(),
                submission.finalScores().stream().map(this::toQuestionScoreView).toList(),
                submission.autoTotal(),
                submission.finalTotal(),
                submission.overallFeedback(),
                submission.status(),
                submission.submittedAt(),
                submission.reviewedAt()
        );
    }

    private ApiModels.QuestionScoreView toQuestionScoreView(QuestionScore score) {
        return new ApiModels.QuestionScoreView(
                score.questionId(),
                score.score(),
                score.maxScore(),
                score.matchedPoints(),
                score.missingPoints(),
                score.comment(),
                score.rationale(),
                score.overridden()
        );
    }

    private String generateShareCode(String courseName, String title) {
        String courseToken = initials(courseName);
        String titleToken = initials(title);
        String randomToken = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String shareCode = (courseToken + "-" + titleToken + "-" + randomToken).replaceAll("-{2,}", "-");
        return shareCode.startsWith("-") ? shareCode.substring(1) : shareCode;
    }

    private String initials(String value) {
        return trimmed(value)
                .replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "")
                .chars()
                .limit(4)
                .collect(StringBuilder::new, (builder, character) -> builder.append((char) character), StringBuilder::append)
                .toString()
                .toUpperCase();
    }

    private void requireNonBlank(String value, String message) {
        if (!hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String valueOrExisting(String value, String existing) {
        return hasText(value) ? value.trim() : existing;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
