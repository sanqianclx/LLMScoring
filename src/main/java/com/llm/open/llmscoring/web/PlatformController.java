package com.llm.open.llmscoring.web;

import com.llm.open.llmscoring.service.PlatformService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/bootstrap")
    public ApiModels.BootstrapView bootstrap() {
        return platformService.bootstrap();
    }

    @PostMapping("/teachers/register")
    public ApiModels.TeacherView registerTeacher(@RequestBody ApiModels.RegisterTeacherRequest request) {
        return platformService.registerTeacher(request);
    }

    @PostMapping("/teachers/login")
    public ApiModels.TeacherDashboardView login(@RequestBody ApiModels.LoginRequest request) {
        return platformService.login(request);
    }

    @GetMapping("/teachers/{teacherId}/dashboard")
    public ApiModels.TeacherDashboardView getTeacherDashboard(@PathVariable UUID teacherId) {
        return platformService.getTeacherDashboard(teacherId);
    }

    @PutMapping("/teachers/{teacherId}")
    public ApiModels.TeacherView updateTeacher(@PathVariable UUID teacherId,
                                               @RequestBody ApiModels.UpdateTeacherRequest request) {
        return platformService.updateTeacher(teacherId, request);
    }

    @PostMapping("/teachers/{teacherId}/courses")
    public ApiModels.CourseView createCourse(@PathVariable UUID teacherId,
                                             @RequestBody ApiModels.CourseRequest request) {
        return platformService.createCourse(teacherId, request);
    }

    @PostMapping("/teachers/{teacherId}/papers")
    public ApiModels.PaperView createPaper(@PathVariable UUID teacherId,
                                           @RequestBody ApiModels.PaperRequest request) {
        return platformService.createPaper(teacherId, request);
    }

    @PutMapping("/teachers/{teacherId}/papers/{paperId}")
    public ApiModels.PaperView updatePaper(@PathVariable UUID teacherId,
                                           @PathVariable UUID paperId,
                                           @RequestBody ApiModels.PaperRequest request) {
        return platformService.updatePaper(teacherId, paperId, request);
    }

    @DeleteMapping("/teachers/{teacherId}/papers/{paperId}")
    public void deletePaper(@PathVariable UUID teacherId, @PathVariable UUID paperId) {
        platformService.deletePaper(teacherId, paperId);
    }

    @GetMapping("/student/papers/{shareCode}")
    public ApiModels.StudentPaperView getStudentPaper(@PathVariable String shareCode) {
        return platformService.getStudentPaper(shareCode);
    }

    @PostMapping("/student/papers/{shareCode}/submissions")
    public ApiModels.SubmissionView submitAnswers(@PathVariable String shareCode,
                                                  @RequestBody ApiModels.SubmissionRequest request) {
        return platformService.submitAnswers(shareCode, request);
    }

    @GetMapping("/student/results/{shareCode}")
    public ApiModels.StudentResultView getStudentResult(@PathVariable String shareCode,
                                                        @RequestParam String studentId) {
        return platformService.getStudentResult(shareCode, studentId);
    }

    @PutMapping("/teachers/{teacherId}/submissions/{submissionId}/review")
    public ApiModels.SubmissionView reviewSubmission(@PathVariable UUID teacherId,
                                                     @PathVariable UUID submissionId,
                                                     @RequestBody ApiModels.ReviewSubmissionRequest request) {
        return platformService.reviewSubmission(teacherId, submissionId, request);
    }
}
