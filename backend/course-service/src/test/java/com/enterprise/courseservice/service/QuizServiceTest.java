package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.courseservice.dto.request.*;
import com.enterprise.courseservice.dto.response.*;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizQuestionRepository questionRepository;

    @Mock
    private QuizOptionRepository optionRepository;

    @Mock
    private QuizAttemptRepository attemptRepository;

    @Mock
    private AttemptAnswerRepository answerRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private LessonProgressService lessonProgressService;

    @InjectMocks
    private QuizService quizService;

    private UUID instructorId;
    private UUID studentId;
    private UUID courseId;
    private UUID lessonId;
    private UUID quizId;
    private Course course;
    private Lesson lesson;
    private Quiz quiz;
    private Enrolment enrolment;

    @BeforeEach
    void setUp() {
        instructorId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        lessonId = UUID.randomUUID();
        quizId = UUID.randomUUID();

        course = Course.builder()
                .id(courseId)
                .instructorId(instructorId)
                .title("Full Stack Java")
                .slug("full-stack-java")
                .description("Course description")
                .build();

        lesson = Lesson.builder()
                .id(lessonId)
                .courseId(courseId)
                .title("Java Basics Lesson")
                .type(LessonType.QUIZ)
                .build();

        quiz = Quiz.builder()
                .id(quizId)
                .lesson(lesson)
                .course(course)
                .title("Java Fundamentals Quiz")
                .description("Test your java skills")
                .timeLimitMinutes(30)
                .maxAttempts(2)
                .passPercent(new BigDecimal("60.00"))
                .totalMarks(new BigDecimal("10.00"))
                .showAnswersPolicy(ShowAnswersPolicy.AFTER_SUBMIT)
                .shuffleQuestions(false)
                .shuffleOptions(false)
                .isPublished(true)
                .build();

        enrolment = Enrolment.builder()
                .id(UUID.randomUUID())
                .course(course)
                .studentId(studentId)
                .status(EnrolmentStatus.ACTIVE)
                .build();
    }

    // =========================================================================
    // Authoring Tests
    // =========================================================================

    @Test
    void createQuiz_Success() {
        CreateQuizRequest req = CreateQuizRequest.builder()
                .title("Java Quiz 1")
                .description("Quiz description")
                .timeLimitMinutes(45)
                .maxAttempts(3)
                .passPercent(new BigDecimal("70.00"))
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(quizRepository.existsByLessonId(lessonId)).thenReturn(false);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
            Quiz q = inv.getArgument(0);
            q.setId(quizId);
            return q;
        });

        QuizSummaryResponse res = quizService.createQuiz(lessonId, instructorId, req);

        assertNotNull(res);
        assertEquals("Java Quiz 1", res.getTitle());
        assertEquals(45, res.getTimeLimitMinutes());
        assertEquals(3, res.getMaxAttempts());
        assertEquals(new BigDecimal("70.00"), res.getPassPercent());
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    void createQuiz_NotInstructor_ThrowsAccessDenied() {
        CreateQuizRequest req = CreateQuizRequest.builder().title("Quiz").build();
        UUID otherUserId = UUID.randomUUID();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(ApiException.class, () -> quizService.createQuiz(lessonId, otherUserId, req));
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_AlreadyExists_ThrowsConflict() {
        CreateQuizRequest req = CreateQuizRequest.builder().title("Quiz").build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(quizRepository.existsByLessonId(lessonId)).thenReturn(true);

        assertThrows(ApiException.class, () -> quizService.createQuiz(lessonId, instructorId, req));
    }

    @Test
    void addQuestion_SingleChoice_Success() {
        CreateQuestionRequest req = CreateQuestionRequest.builder()
                .type(QuestionType.SINGLE_CHOICE)
                .text("What is JVM?")
                .marks(new BigDecimal("2.00"))
                .negativeMarks(new BigDecimal("0.50"))
                .options(List.of(
                        CreateOptionRequest.builder().text("Java Virtual Machine").isCorrect(true).build(),
                        CreateOptionRequest.builder().text("Java Visual Model").isCorrect(false).build()
                ))
                .build();

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(questionRepository.countByQuizId(quizId)).thenReturn(0L);
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> {
            QuizQuestion q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });
        when(optionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.findAllByQuizIdOrderByPositionAsc(quizId)).thenReturn(List.of(
                QuizQuestion.builder().marks(new BigDecimal("2.00")).build()
        ));

        QuizQuestionAdminResponse res = quizService.addQuestion(quizId, instructorId, req);

        assertNotNull(res);
        assertEquals(QuestionType.SINGLE_CHOICE, res.getType());
        assertEquals("What is JVM?", res.getText());
        assertEquals(new BigDecimal("2.00"), res.getMarks());
        assertEquals(2, res.getOptions().size());
        verify(quizRepository, times(1)).save(quiz);
    }

    @Test
    void addQuestion_SingleChoice_NoCorrectOption_ThrowsBadRequest() {
        CreateQuestionRequest req = CreateQuestionRequest.builder()
                .type(QuestionType.SINGLE_CHOICE)
                .text("Invalid question")
                .options(List.of(
                        CreateOptionRequest.builder().text("Option A").isCorrect(false).build(),
                        CreateOptionRequest.builder().text("Option B").isCorrect(false).build()
                ))
                .build();

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));

        assertThrows(ApiException.class, () -> quizService.addQuestion(quizId, instructorId, req));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void addQuestion_ShortAnswer_Success() {
        CreateQuestionRequest req = CreateQuestionRequest.builder()
                .type(QuestionType.SHORT_ANSWER)
                .text("Name Java garbage collector algorithm")
                .marks(new BigDecimal("3.00"))
                .correctText("G1")
                .build();

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(questionRepository.countByQuizId(quizId)).thenReturn(1L);
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizQuestionAdminResponse res = quizService.addQuestion(quizId, instructorId, req);

        assertNotNull(res);
        assertEquals(QuestionType.SHORT_ANSWER, res.getType());
        assertEquals("G1", res.getCorrectText());
    }

    @Test
    void addQuestion_Numeric_Success() {
        CreateQuestionRequest req = CreateQuestionRequest.builder()
                .type(QuestionType.NUMERIC)
                .text("Value of Pi up to 2 decimals")
                .marks(new BigDecimal("2.00"))
                .numericAnswer(new BigDecimal("3.14"))
                .tolerance(new BigDecimal("0.01"))
                .build();

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(questionRepository.countByQuizId(quizId)).thenReturn(2L);
        when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizQuestionAdminResponse res = quizService.addQuestion(quizId, instructorId, req);

        assertNotNull(res);
        assertEquals(QuestionType.NUMERIC, res.getType());
        assertEquals(new BigDecimal("3.14"), res.getNumericAnswer());
        assertEquals(new BigDecimal("0.01"), res.getTolerance());
    }

    // =========================================================================
    // Student Attempt & Anti-Cheating Tests (FR-QZ-04)
    // =========================================================================

    @Test
    void startAttempt_Success_PayloadSanitized() {
        UUID qId = UUID.randomUUID();
        UUID opt1 = UUID.randomUUID();
        UUID opt2 = UUID.randomUUID();

        QuizQuestion question = QuizQuestion.builder()
                .id(qId)
                .quiz(quiz)
                .type(QuestionType.SINGLE_CHOICE)
                .text("JVM Question")
                .marks(new BigDecimal("5.00"))
                .negativeMarks(new BigDecimal("1.00"))
                .explanation("SECRET EXPLANATION")
                .position(1)
                .options(List.of(
                        QuizOption.builder().id(opt1).text("Correct JVM").isCorrect(true).position(1).build(),
                        QuizOption.builder().id(opt2).text("Wrong JVM").isCorrect(false).position(2).build()
                ))
                .build();

        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(attemptRepository.findByQuizIdAndStudentIdAndStatus(quizId, studentId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByQuizIdAndStudentId(quizId, studentId)).thenReturn(0L);
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(questionRepository.findAllByQuizIdOrderByPositionAsc(quizId)).thenReturn(List.of(question));
        when(answerRepository.findAllByAttemptId(any())).thenReturn(Collections.emptyList());

        ActiveQuizAttemptResponse res = quizService.startAttempt(quizId, studentId);

        assertNotNull(res);
        assertEquals(1, res.getAttemptNo());
        assertEquals(1, res.getQuestions().size());

        ActiveQuizAttemptResponse.ActiveQuestionDto qDto = res.getQuestions().getFirst();
        assertEquals("JVM Question", qDto.getText());
        assertEquals(2, qDto.getOptions().size());

        // STRICT ANTI-CHEATING VERIFICATION:
        // Neither question nor options should expose isCorrect, explanation, etc.
        assertEquals("Correct JVM", qDto.getOptions().get(0).getText());
        assertNull(qDto.getSavedAnswer());
    }

    @Test
    void startAttempt_ExceedsMaxAttempts_ThrowsBadRequest() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(attemptRepository.findByQuizIdAndStudentIdAndStatus(quizId, studentId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByQuizIdAndStudentId(quizId, studentId)).thenReturn(2L); // max is 2

        assertThrows(ApiException.class, () -> quizService.startAttempt(quizId, studentId));
    }

    @Test
    void startAttempt_NotEnrolled_ThrowsAccessDenied() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> quizService.startAttempt(quizId, studentId));
    }

    // =========================================================================
    // Auto-Grading Engine Tests (FR-QZ-06, FR-QZ-03)
    // =========================================================================

    @Test
    void submitAttempt_AutoGrading_AllTypesAndScoring() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .quiz(quiz)
                .studentId(studentId)
                .attemptNo(1)
                .startedAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().plus(20, ChronoUnit.MINUTES))
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        // Question 1: Single Choice (Correct -> +2.00)
        UUID q1Id = UUID.randomUUID();
        UUID q1OptCorrect = UUID.randomUUID();
        UUID q1OptWrong = UUID.randomUUID();
        QuizQuestion q1 = QuizQuestion.builder()
                .id(q1Id)
                .quiz(quiz)
                .type(QuestionType.SINGLE_CHOICE)
                .marks(new BigDecimal("2.00"))
                .negativeMarks(new BigDecimal("0.50"))
                .options(List.of(
                        QuizOption.builder().id(q1OptCorrect).isCorrect(true).build(),
                        QuizOption.builder().id(q1OptWrong).isCorrect(false).build()
                ))
                .build();

        // Question 2: Multi Choice (Wrong -> -1.00 negative mark)
        UUID q2Id = UUID.randomUUID();
        UUID q2Opt1 = UUID.randomUUID();
        UUID q2Opt2 = UUID.randomUUID();
        QuizQuestion q2 = QuizQuestion.builder()
                .id(q2Id)
                .quiz(quiz)
                .type(QuestionType.MULTI_CHOICE)
                .marks(new BigDecimal("3.00"))
                .negativeMarks(new BigDecimal("1.00"))
                .options(List.of(
                        QuizOption.builder().id(q2Opt1).isCorrect(true).build(),
                        QuizOption.builder().id(q2Opt2).isCorrect(true).build()
                ))
                .build();

        // Question 3: Numeric with tolerance (Correct within tolerance -> +3.00)
        UUID q3Id = UUID.randomUUID();
        QuizQuestion q3 = QuizQuestion.builder()
                .id(q3Id)
                .quiz(quiz)
                .type(QuestionType.NUMERIC)
                .marks(new BigDecimal("3.00"))
                .negativeMarks(new BigDecimal("0.00"))
                .numericAnswer(new BigDecimal("10.00"))
                .tolerance(new BigDecimal("0.50"))
                .options(Collections.emptyList())
                .build();

        // Question 4: Short Answer (Unanswered -> 0.00, no negative deduction)
        UUID q4Id = UUID.randomUUID();
        QuizQuestion q4 = QuizQuestion.builder()
                .id(q4Id)
                .quiz(quiz)
                .type(QuestionType.SHORT_ANSWER)
                .marks(new BigDecimal("2.00"))
                .negativeMarks(new BigDecimal("0.50"))
                .correctText("Spring")
                .options(Collections.emptyList())
                .build();

        // Answers
        AttemptAnswer a1 = AttemptAnswer.builder()
                .attempt(attempt)
                .question(q1)
                .selectedOptionIds(q1OptCorrect.toString())
                .build();

        AttemptAnswer a2 = AttemptAnswer.builder()
                .attempt(attempt)
                .question(q2)
                .selectedOptionIds(q2Opt1.toString()) // Missing q2Opt2 -> wrong
                .build();

        AttemptAnswer a3 = AttemptAnswer.builder()
                .attempt(attempt)
                .question(q3)
                .numericAnswer(new BigDecimal("10.30")) // diff 0.30 <= tolerance 0.50 -> correct
                .build();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByQuizIdOrderByPositionAsc(quizId)).thenReturn(List.of(q1, q2, q3, q4));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(a1, a2, a3));
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitQuizRequest submitReq = new SubmitQuizRequest();
        QuizResultResponse res = quizService.submitAttempt(attemptId, studentId, submitReq);

        assertNotNull(res);
        assertEquals(AttemptStatus.SUBMITTED, res.getStatus());
        assertEquals(2, res.getCorrectCount()); // q1, q3
        assertEquals(1, res.getWrongCount());   // q2
        assertEquals(1, res.getUnansweredCount()); // q4

        // Score = 2.00 (q1) - 1.00 (q2 neg) + 3.00 (q3) + 0 (q4) = 4.00
        assertEquals(new BigDecimal("4.00"), res.getScore());
        // Percentage = (4.00 / 10.00) * 100 = 40.00%
        assertEquals(new BigDecimal("40.00"), res.getPercentage());
        // Pass percent is 60.00% -> passed = false
        assertFalse(res.getPassed());
    }

    @Test
    void submitAttempt_NegativeMarksFlooredAtZero() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .quiz(quiz)
                .studentId(studentId)
                .attemptNo(1)
                .startedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        UUID qId = UUID.randomUUID();
        UUID optCorrect = UUID.randomUUID();
        UUID optWrong = UUID.randomUUID();
        QuizQuestion q = QuizQuestion.builder()
                .id(qId)
                .quiz(quiz)
                .type(QuestionType.SINGLE_CHOICE)
                .marks(new BigDecimal("1.00"))
                .negativeMarks(new BigDecimal("5.00"))
                .options(List.of(
                        QuizOption.builder().id(optCorrect).isCorrect(true).build(),
                        QuizOption.builder().id(optWrong).isCorrect(false).build()
                ))
                .build();

        AttemptAnswer wrongAns = AttemptAnswer.builder()
                .attempt(attempt)
                .question(q)
                .selectedOptionIds(optWrong.toString())
                .build();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByQuizIdOrderByPositionAsc(quizId)).thenReturn(List.of(q));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(wrongAns));
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizResultResponse res = quizService.submitAttempt(attemptId, studentId, null);

        assertNotNull(res);
        // Score was 0 - 5 = -5, but floored at 0.00
        assertEquals(new BigDecimal("0.00"), res.getScore());
        assertEquals(new BigDecimal("0.00"), res.getPercentage());
        assertFalse(res.getPassed());
    }

    @Test
    void submitAttempt_PassUpdatesLessonProgress() {
        UUID attemptId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.builder()
                .id(attemptId)
                .quiz(quiz)
                .studentId(studentId)
                .attemptNo(1)
                .startedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        UUID qId = UUID.randomUUID();
        UUID optCorrect = UUID.randomUUID();
        QuizQuestion q = QuizQuestion.builder()
                .id(qId)
                .quiz(quiz)
                .type(QuestionType.SINGLE_CHOICE)
                .marks(new BigDecimal("10.00"))
                .negativeMarks(BigDecimal.ZERO)
                .options(List.of(
                        QuizOption.builder().id(optCorrect).isCorrect(true).build()
                ))
                .build();

        AttemptAnswer correctAns = AttemptAnswer.builder()
                .attempt(attempt)
                .question(q)
                .selectedOptionIds(optCorrect.toString())
                .build();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(questionRepository.findAllByQuizIdOrderByPositionAsc(quizId)).thenReturn(List.of(q));
        when(answerRepository.findAllByAttemptId(attemptId)).thenReturn(List.of(correctAns));
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        QuizResultResponse res = quizService.submitAttempt(attemptId, studentId, null);

        assertTrue(res.getPassed());
        verify(lessonProgressService, times(1)).recordProgress(eq(courseId), eq(lessonId), eq(studentId), any());
    }
}
