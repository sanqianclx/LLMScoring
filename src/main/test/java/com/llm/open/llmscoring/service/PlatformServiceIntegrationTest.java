package com.llm.open.llmscoring.service;

import com.llm.open.llmscoring.dto.ApiModels;
import com.llm.open.llmscoring.dto.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PlatformServiceIntegrationTest {

    @Autowired
    private PlatformService platformService;

    @Test
    void bootstrapExposesDemoTeacherAndShareCode() {
        ApiModels.BootstrapView bootstrap = platformService.bootstrap();

        assertThat(bootstrap.demoTeacher()).isNotNull();
        assertThat(bootstrap.demoTeacher().username()).isEqualTo("teacher");
        assertThat(bootstrap.demoShareCode()).isEqualTo("BIO-2026");
        assertThat(bootstrap.teachers()).isNotEmpty();
    }

    @Test
    void studentSubmissionCanBeReviewedAndPublishedEndToEnd() {
        ApiModels.BootstrapView bootstrap = platformService.bootstrap();
        ApiModels.StudentPaperView paper = platformService.getStudentPaper(bootstrap.demoShareCode());

        ApiModels.StudentQuestionView fillBlankQuestion = paper.questions().get(0);
        ApiModels.StudentQuestionView shortAnswerQuestion = paper.questions().get(1);

        ApiModels.SubmissionView submission = platformService.submitAnswers(
                bootstrap.demoShareCode(),
                new ApiModels.SubmissionRequest(
                        "2026001",
                        "汪云松",
                        List.of(
                                new ApiModels.StudentAnswerRequest(fillBlankQuestion.id(), "叶绿体"),
                                new ApiModels.StudentAnswerRequest(shortAnswerQuestion.id(), "光合作用需要光照和水，最后会释放氧气。")
                        )
                )
        );

        assertThat(submission.status()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
        assertThat(submission.autoScores()).hasSize(2);
        assertThat(submission.autoTotal()).isEqualTo(22.0);
        assertThat(submission.finalTotal()).isEqualTo(22.0);
        assertThat(submission.autoScores().get(0).comment()).isNotBlank();
        assertThat(submission.autoScores().get(1).missingPoints()).hasSize(2);

        ApiModels.StudentResultView pendingResult = platformService.getStudentResult(bootstrap.demoShareCode(), "2026001");
        assertThat(pendingResult.status()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
        assertThat(pendingResult.scores()).isEmpty();

        ApiModels.SubmissionView reviewedSubmission = platformService.reviewSubmission(
                bootstrap.demoTeacher().id(),
                submission.id(),
                new ApiModels.ReviewSubmissionRequest(
                        List.of(
                                new ApiModels.QuestionReviewRequest(
                                        fillBlankQuestion.id(),
                                        9.5,
                                        "教师复核：答案正确，提醒注意书写规范。",
                                        "答案与标准答案一致，本次按教师规则保留 0.5 分书写提醒。"
                                )
                        ),
                        "人工复核通过，填空题准确，简答题还可以继续补充反应物和产物。"
                )
        );

        assertThat(reviewedSubmission.status()).isEqualTo(SubmissionStatus.REVIEWED);
        assertThat(reviewedSubmission.finalTotal()).isEqualTo(21.5);
        assertThat(reviewedSubmission.finalScores().get(0).overridden()).isTrue();
        assertThat(reviewedSubmission.overallFeedback()).contains("人工复核通过");

        ApiModels.StudentResultView publishedResult = platformService.getStudentResult(bootstrap.demoShareCode(), "2026001");
        assertThat(publishedResult.status()).isEqualTo(SubmissionStatus.REVIEWED);
        assertThat(publishedResult.totalScore()).isEqualTo(21.5);
        assertThat(publishedResult.scores()).hasSize(2);
        assertThat(publishedResult.scores().get(0).comment()).contains("教师复核");
    }
}
