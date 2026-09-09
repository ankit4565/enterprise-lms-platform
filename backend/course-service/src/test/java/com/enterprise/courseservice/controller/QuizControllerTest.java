package com.enterprise.courseservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.courseservice.dto.request.*;
import com.enterprise.courseservice.dto.response.*;
import com.enterprise.courseservice.entity.AttemptStatus;
import com.enterprise.courseservice.entity.QuestionType;
import com.enterprise.courseservice.entity.ShowAnswersPolicy;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.QuizService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID testUserId;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .userId(testUserId)
                .email("instructor@enterprise.com")
                .roles(List.of("INSTRUCTOR"))
                .permissions(Collections.emptyList())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .build();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return testPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(quizController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createQuiz_Success() throws Exception {
        UUID lessonId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        CreateQuizRequest req = CreateQuizRequest.builder()
                .title("Midterm Quiz")
                .description("Midterm examination")
                .timeLimitMinutes(60)
                .maxAttempts(1)
                .passPercent(new BigDecimal("70.00"))
                .build();

        QuizSummaryResponse res = QuizSummaryResponse.builder()
                .id(quizId)
                .lessonId(lessonId)
                .title("Midterm Quiz")
                .timeLimitMinutes(60)
                .maxAttempts(1)
                .passPercent(new BigDecimal("70.00"))
                .build();

        when(quizService.createQuiz(eq(lessonId), eq(testUserId), any(CreateQuizRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/quizzes", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(quizId.toString()))
                .andExpect(jsonPath("$.data.title").value("Midterm Quiz"));
    }

    @Test
    void addQuestion_Success() throws Exception {
        UUID quizId = UUID.randomUUID();
        UUID qId = UUID.randomUUID();

        CreateQuestionRequest req = CreateQuestionRequest.builder()
                .type(QuestionType.SINGLE_CHOICE)
                .text("Is Java object-oriented?")
                .marks(new BigDecimal("2.00"))
                .options(List.of(
                        CreateOptionRequest.builder().text("Yes").isCorrect(true).build(),
                        CreateOptionRequest.builder().text("No").isCorrect(false).build()
                ))
                .build();

        QuizQuestionAdminResponse res = QuizQuestionAdminResponse.builder()
                .id(qId)
                .quizId(quizId)
                .type(QuestionType.SINGLE_CHOICE)
                .text("Is Java object-oriented?")
                .marks(new BigDecimal("2.00"))
                .options(Collections.emptyList())
                .build();

        when(quizService.addQuestion(eq(quizId), eq(testUserId), any(CreateQuestionRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/v1/quizzes/{id}/questions", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(qId.toString()))
                .andExpect(jsonPath("$.data.text").value("Is Java object-oriented?"));
    }

    @Test
    void startAttempt_Success() throws Exception {
        UUID quizId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        ActiveQuizAttemptResponse res = ActiveQuizAttemptResponse.builder()
                .attemptId(attemptId)
                .quizId(quizId)
                .quizTitle("Midterm Quiz")
                .attemptNo(1)
                .startedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .timeLimitMinutes(30)
                .questions(Collections.emptyList())
                .build();

        when(quizService.startAttempt(eq(quizId), eq(testUserId))).thenReturn(res);

        mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.data.attemptNo").value(1));
    }

    @Test
    void autosaveAnswer_Success() throws Exception {
        UUID attemptId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        AutosaveAnswerRequest req = AutosaveAnswerRequest.builder()
                .questionId(questionId)
                .selectedOptionIds(List.of(UUID.randomUUID()))
                .build();

        mockMvc.perform(post("/api/v1/quizzes/attempts/{attemptId}/answers", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submitAttempt_Success() throws Exception {
        UUID attemptId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        QuizResultResponse res = QuizResultResponse.builder()
                .attemptId(attemptId)
                .quizId(quizId)
                .quizTitle("Midterm Quiz")
                .attemptNo(1)
                .status(AttemptStatus.SUBMITTED)
                .score(new BigDecimal("8.00"))
                .totalMarks(new BigDecimal("10.00"))
                .percentage(new BigDecimal("80.00"))
                .passed(true)
                .correctCount(4)
                .wrongCount(1)
                .unansweredCount(0)
                .showAnswersPolicy(ShowAnswersPolicy.AFTER_SUBMIT)
                .build();

        when(quizService.submitAttempt(eq(attemptId), eq(testUserId), any()))
                .thenReturn(res);

        mockMvc.perform(post("/api/v1/quizzes/attempts/{attemptId}/submit", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitQuizRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(8.00))
                .andExpect(jsonPath("$.data.percentage").value(80.00))
                .andExpect(jsonPath("$.data.passed").value(true));
    }

    @Test
    void getAttemptResult_Success() throws Exception {
        UUID attemptId = UUID.randomUUID();

        QuizResultResponse res = QuizResultResponse.builder()
                .attemptId(attemptId)
                .status(AttemptStatus.SUBMITTED)
                .score(new BigDecimal("10.00"))
                .passed(true)
                .build();

        when(quizService.getAttemptResult(eq(attemptId), eq(testUserId), eq(true)))
                .thenReturn(res);

        mockMvc.perform(get("/api/v1/quizzes/attempts/{attemptId}/result", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(10.00))
                .andExpect(jsonPath("$.data.passed").value(true));
    }
}
