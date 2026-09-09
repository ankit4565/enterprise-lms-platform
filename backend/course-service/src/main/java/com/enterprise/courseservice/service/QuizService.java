package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.courseservice.dto.request.*;
import com.enterprise.courseservice.dto.response.*;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final LessonProgressService lessonProgressService;

    // =========================================================================
    // Instructor Authoring
    // =========================================================================

    @Transactional
    public QuizSummaryResponse createQuiz(UUID lessonId, UUID instructorId, CreateQuizRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found: " + lessonId));

        Course course = courseRepository.findById(lesson.getCourseId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found: " + lesson.getCourseId()));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can create quizzes");
        }

        if (quizRepository.existsByLessonId(lessonId)) {
            throw new ApiException(ErrorCode.CONFLICT, "A quiz already exists for this lesson");
        }

        if (lesson.getType() != LessonType.QUIZ) {
            lesson.setType(LessonType.QUIZ);
            lessonRepository.save(lesson);
        }

        Quiz quiz = Quiz.builder()
                .lesson(lesson)
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1)
                .passPercent(request.getPassPercent() != null ? request.getPassPercent() : new BigDecimal("60.00"))
                .shuffleQuestions(request.getShuffleQuestions() != null ? request.getShuffleQuestions() : false)
                .shuffleOptions(request.getShuffleOptions() != null ? request.getShuffleOptions() : false)
                .showAnswersPolicy(request.getShowAnswersPolicy() != null ? request.getShowAnswersPolicy() : ShowAnswersPolicy.AFTER_SUBMIT)
                .totalMarks(BigDecimal.ZERO)
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
                .build();

        Quiz saved = quizRepository.save(quiz);
        log.info("Quiz created with id {} for lesson {} in course {}", saved.getId(), lessonId, course.getId());
        return QuizSummaryResponse.from(saved, 0);
    }

    @Transactional
    public QuizSummaryResponse updateQuiz(UUID quizId, UUID instructorId, UpdateQuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Quiz not found: " + quizId));

        if (!quiz.getCourse().getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can update this quiz");
        }

        if (request.getTitle() != null) quiz.setTitle(request.getTitle());
        if (request.getDescription() != null) quiz.setDescription(request.getDescription());
        if (request.getTimeLimitMinutes() != null) quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        if (request.getMaxAttempts() != null) quiz.setMaxAttempts(request.getMaxAttempts());
        if (request.getPassPercent() != null) quiz.setPassPercent(request.getPassPercent());
        if (request.getShuffleQuestions() != null) quiz.setShuffleQuestions(request.getShuffleQuestions());
        if (request.getShuffleOptions() != null) quiz.setShuffleOptions(request.getShuffleOptions());
        if (request.getShowAnswersPolicy() != null) quiz.setShowAnswersPolicy(request.getShowAnswersPolicy());
        if (request.getIsPublished() != null) quiz.setIsPublished(request.getIsPublished());

        Quiz saved = quizRepository.save(quiz);
        int questionCount = (int) questionRepository.countByQuizId(quizId);
        return QuizSummaryResponse.from(saved, questionCount);
    }

    @Transactional
    public QuizQuestionAdminResponse addQuestion(UUID quizId, UUID instructorId, CreateQuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Quiz not found: " + quizId));

        if (!quiz.getCourse().getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can add questions");
        }

        validateQuestionRequest(request);

        int nextPosition = (request.getPosition() != null && request.getPosition() > 0)
                ? request.getPosition()
                : (int) questionRepository.countByQuizId(quizId) + 1;

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .type(request.getType())
                .text(request.getText())
                .marks(request.getMarks() != null ? request.getMarks() : BigDecimal.ONE)
                .negativeMarks(request.getNegativeMarks() != null ? request.getNegativeMarks() : BigDecimal.ZERO)
                .explanation(request.getExplanation())
                .position(nextPosition)
                .correctText(request.getCorrectText())
                .numericAnswer(request.getNumericAnswer())
                .tolerance(request.getTolerance() != null ? request.getTolerance() : BigDecimal.ZERO)
                .build();

        QuizQuestion savedQuestion = questionRepository.save(question);

        // Save options for choice questions
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            int optPos = 1;
            List<QuizOption> options = new ArrayList<>();
            for (CreateOptionRequest optReq : request.getOptions()) {
                QuizOption opt = QuizOption.builder()
                        .question(savedQuestion)
                        .text(optReq.getText())
                        .isCorrect(optReq.getIsCorrect() != null ? optReq.getIsCorrect() : false)
                        .position(optReq.getPosition() != null && optReq.getPosition() > 0 ? optReq.getPosition() : optPos++)
                        .build();
                options.add(opt);
            }
            List<QuizOption> savedOptions = optionRepository.saveAll(options);
            savedQuestion.setOptions(savedOptions);
        }

        // Recalculate quiz total marks
        recalculateQuizTotalMarks(quiz);

        return QuizQuestionAdminResponse.from(savedQuestion);
    }

    @Transactional
    public void deleteQuestion(UUID questionId, UUID instructorId) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Question not found: " + questionId));

        Quiz quiz = question.getQuiz();
        if (!quiz.getCourse().getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can delete questions");
        }

        questionRepository.delete(question);
        recalculateQuizTotalMarks(quiz);
        log.info("Question {} deleted from quiz {}", questionId, quiz.getId());
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizInstructorView(UUID quizId, UUID instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Quiz not found: " + quizId));

        if (!quiz.getCourse().getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can access the full quiz view");
        }

        return QuizDetailResponse.from(quiz);
    }

    @Transactional(readOnly = true)
    public QuizSummaryResponse getQuizSummary(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Quiz not found: " + quizId));
        int count = (int) questionRepository.countByQuizId(quizId);
        return QuizSummaryResponse.from(quiz, count);
    }

    @Transactional(readOnly = true)
    public QuizSummaryResponse getQuizByLessonId(UUID lessonId) {
        Quiz quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Quiz not found for lesson: " + lessonId));
        int count = (int) questionRepository.countByQuizId(quiz.getId());
        return QuizSummaryResponse.from(quiz, count);
    }

    // =========================================================================
    // Student Quiz Taking & Anti-Cheating Engine
    // =========================================================================

    @Transactional
    public ActiveQuizAttemptResponse startAttempt(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Quiz not found: " + quizId));

        verifyEnrolment(quiz.getCourse().getId(), studentId);

        // Check if there is an existing IN_PROGRESS attempt
        Optional<QuizAttempt> existingAttemptOpt = attemptRepository.findByQuizIdAndStudentIdAndStatus(
                quizId, studentId, AttemptStatus.IN_PROGRESS);

        if (existingAttemptOpt.isPresent()) {
            QuizAttempt existing = existingAttemptOpt.get();
            // Check if timed out
            if (existing.getExpiresAt() != null && Instant.now().isAfter(existing.getExpiresAt())) {
                log.info("Active attempt {} has expired, auto-submitting...", existing.getId());
                autoSubmitAndGrade(existing);
            } else {
                // Resume active attempt
                return buildActiveAttemptResponse(existing, quiz);
            }
        }

        // Check max attempt limit
        long totalAttempts = attemptRepository.countByQuizIdAndStudentId(quizId, studentId);
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0 && totalAttempts >= quiz.getMaxAttempts()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Maximum attempt limit reached for this quiz (" + quiz.getMaxAttempts() + ")");
        }

        int nextAttemptNo = (int) totalAttempts + 1;
        Instant now = Instant.now();
        Instant expiresAt = null;
        if (quiz.getTimeLimitMinutes() != null && quiz.getTimeLimitMinutes() > 0) {
            expiresAt = now.plus(Duration.ofMinutes(quiz.getTimeLimitMinutes()));
        }

        QuizAttempt newAttempt = QuizAttempt.builder()
                .quiz(quiz)
                .studentId(studentId)
                .attemptNo(nextAttemptNo)
                .startedAt(now)
                .expiresAt(expiresAt)
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        QuizAttempt savedAttempt = attemptRepository.save(newAttempt);
        log.info("Student {} started attempt #{} for quiz {}", studentId, nextAttemptNo, quizId);
        return buildActiveAttemptResponse(savedAttempt, quiz);
    }

    @Transactional(readOnly = true)
    public ActiveQuizAttemptResponse getActiveAttempt(UUID attemptId, UUID studentId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You cannot access another student's attempt");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "This attempt is no longer active (status: " + attempt.getStatus() + ")");
        }

        if (attempt.getExpiresAt() != null && Instant.now().isAfter(attempt.getExpiresAt())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Quiz time limit has expired for this attempt");
        }

        return buildActiveAttemptResponse(attempt, attempt.getQuiz());
    }

    @Transactional
    public void autosaveAnswer(UUID attemptId, UUID studentId, AutosaveAnswerRequest request) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You cannot modify another student's attempt");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Cannot autosave: attempt is " + attempt.getStatus());
        }

        if (attempt.getExpiresAt() != null && Instant.now().isAfter(attempt.getExpiresAt())) {
            autoSubmitAndGrade(attempt);
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Quiz time limit has expired and attempt has been submitted");
        }

        QuizQuestion question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Question not found: " + request.getQuestionId()));

        if (!question.getQuiz().getId().equals(attempt.getQuiz().getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Question does not belong to this quiz");
        }

        saveSingleAnswer(attempt, question, request);
    }

    @Transactional
    public QuizResultResponse submitAttempt(UUID attemptId, UUID studentId, SubmitQuizRequest request) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You cannot submit another student's attempt");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Attempt has already been submitted or closed");
        }

        // Save batch answers if provided in the submit payload
        if (request != null && request.getAnswers() != null) {
            for (AutosaveAnswerRequest ansReq : request.getAnswers()) {
                questionRepository.findById(ansReq.getQuestionId()).ifPresent(q -> {
                    if (q.getQuiz().getId().equals(attempt.getQuiz().getId())) {
                        saveSingleAnswer(attempt, q, ansReq);
                    }
                });
            }
        }

        boolean isTimedOut = attempt.getExpiresAt() != null && Instant.now().isAfter(attempt.getExpiresAt());
        attempt.setStatus(isTimedOut ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);

        return gradeAndFinalizeAttempt(attempt);
    }

    @Transactional(readOnly = true)
    public QuizResultResponse getAttemptResult(UUID attemptId, UUID userId, boolean isInstructor) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Attempt not found: " + attemptId));

        boolean isStudentOwner = attempt.getStudentId().equals(userId);
        if (!isStudentOwner && !isInstructor) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You do not have permission to view this attempt result");
        }

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Attempt is still in progress and not yet evaluated");
        }

        return buildResultResponse(attempt, isInstructor);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptSummaryResponse> getMyAttempts(UUID quizId, UUID studentId) {
        return attemptRepository.findAllByQuizIdAndStudentIdOrderByAttemptNoAsc(quizId, studentId)
                .stream()
                .map(QuizAttemptSummaryResponse::from)
                .toList();
    }

    // =========================================================================
    // Auto-Grading & Evaluation Engine
    // =========================================================================

    private void autoSubmitAndGrade(QuizAttempt attempt) {
        attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
        gradeAndFinalizeAttempt(attempt);
    }

    private QuizResultResponse gradeAndFinalizeAttempt(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        Instant submittedAt = Instant.now();
        attempt.setSubmittedAt(submittedAt);

        long timeTaken = Duration.between(attempt.getStartedAt(), submittedAt).toSeconds();
        if (quiz.getTimeLimitMinutes() != null && quiz.getTimeLimitMinutes() > 0) {
            long maxAllowed = quiz.getTimeLimitMinutes() * 60L;
            timeTaken = Math.min(timeTaken, maxAllowed);
        }
        attempt.setTimeTakenSeconds((int) timeTaken);

        List<QuizQuestion> questions = questionRepository.findAllByQuizIdOrderByPositionAsc(quiz.getId());
        Map<UUID, AttemptAnswer> answerMap = answerRepository.findAllByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (k1, k2) -> k1));

        BigDecimal totalEarnedMarks = BigDecimal.ZERO;
        int correctCount = 0;
        int wrongCount = 0;
        int unansweredCount = 0;

        for (QuizQuestion question : questions) {
            AttemptAnswer answer = answerMap.get(question.getId());
            boolean isAttempted = isQuestionAttempted(question.getType(), answer);

            if (!isAttempted) {
                unansweredCount++;
                if (answer == null) {
                    answer = AttemptAnswer.builder()
                            .attempt(attempt)
                            .question(question)
                            .isCorrect(false)
                            .marksAwarded(BigDecimal.ZERO)
                            .answeredAt(submittedAt)
                            .build();
                } else {
                    answer.setIsCorrect(false);
                    answer.setMarksAwarded(BigDecimal.ZERO);
                }
                answerRepository.save(answer);
            } else {
                boolean correct = evaluateQuestionCorrectness(question, answer);
                BigDecimal marksAwarded;
                if (correct) {
                    correctCount++;
                    marksAwarded = question.getMarks();
                    totalEarnedMarks = totalEarnedMarks.add(marksAwarded);
                } else {
                    wrongCount++;
                    // Apply negative marking
                    marksAwarded = question.getNegativeMarks().negate();
                    totalEarnedMarks = totalEarnedMarks.subtract(question.getNegativeMarks());
                }
                answer.setIsCorrect(correct);
                answer.setMarksAwarded(marksAwarded);
                answerRepository.save(answer);
            }
        }

        // Floor total score at 0 (FR-QZ-03)
        BigDecimal finalScore = (totalEarnedMarks.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalEarnedMarks)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (quiz.getTotalMarks() != null && quiz.getTotalMarks().compareTo(BigDecimal.ZERO) > 0) {
            percentage = finalScore.divide(quiz.getTotalMarks(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        boolean passed = percentage.compareTo(quiz.getPassPercent()) >= 0;

        attempt.setScore(finalScore);
        attempt.setPercentage(percentage);
        attempt.setPassed(passed);
        attempt.setCorrectCount(correctCount);
        attempt.setWrongCount(wrongCount);
        attempt.setUnansweredCount(unansweredCount);

        QuizAttempt evaluated = attemptRepository.save(attempt);
        log.info("Attempt {} evaluated: score={}, percentage={}%, passed={}, correct={}, wrong={}, unanswered={}",
                evaluated.getId(), finalScore, percentage, passed, correctCount, wrongCount, unansweredCount);

        // Update lesson progress if passed (FR-QZ-06)
        if (passed && quiz.getLesson() != null) {
            try {
                lessonProgressService.recordProgress(
                        quiz.getCourse().getId(),
                        quiz.getLesson().getId(),
                        attempt.getStudentId(),
                        UpdateProgressRequest.builder().isCompleted(true).build()
                );
                log.info("Lesson {} marked completed for student {} upon passing quiz {}",
                        quiz.getLesson().getId(), attempt.getStudentId(), quiz.getId());
            } catch (Exception e) {
                log.warn("Could not automatically update lesson progress for quiz {}: {}", quiz.getId(), e.getMessage());
            }
        }

        return buildResultResponse(evaluated, false);
    }

    private boolean isQuestionAttempted(QuestionType type, AttemptAnswer answer) {
        if (answer == null) return false;
        return switch (type) {
            case SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE ->
                    answer.getSelectedOptionIds() != null && !answer.getSelectedOptionIds().trim().isEmpty();
            case SHORT_ANSWER ->
                    answer.getTextAnswer() != null && !answer.getTextAnswer().trim().isEmpty();
            case NUMERIC ->
                    answer.getNumericAnswer() != null;
        };
    }

    private boolean evaluateQuestionCorrectness(QuizQuestion question, AttemptAnswer answer) {
        return switch (question.getType()) {
            case SINGLE_CHOICE, TRUE_FALSE -> {
                UUID correctOptionId = question.getOptions().stream()
                        .filter(QuizOption::getIsCorrect)
                        .map(QuizOption::getId)
                        .findFirst()
                        .orElse(null);
                if (correctOptionId == null || answer.getSelectedOptionIds() == null) {
                    yield false;
                }
                List<UUID> selectedIds = parseSelectedOptionIds(answer.getSelectedOptionIds());
                yield selectedIds.size() == 1 && selectedIds.getFirst().equals(correctOptionId);
            }
            case MULTI_CHOICE -> {
                Set<UUID> correctIds = question.getOptions().stream()
                        .filter(QuizOption::getIsCorrect)
                        .map(QuizOption::getId)
                        .collect(Collectors.toSet());
                List<UUID> selectedIds = parseSelectedOptionIds(answer.getSelectedOptionIds());
                Set<UUID> studentIds = new HashSet<>(selectedIds);
                yield correctIds.equals(studentIds);
            }
            case SHORT_ANSWER -> {
                if (question.getCorrectText() == null || answer.getTextAnswer() == null) {
                    yield false;
                }
                String correctText = question.getCorrectText().trim().toLowerCase();
                String studentText = answer.getTextAnswer().trim().toLowerCase();
                yield correctText.equals(studentText);
            }
            case NUMERIC -> {
                if (question.getNumericAnswer() == null || answer.getNumericAnswer() == null) {
                    yield false;
                }
                BigDecimal diff = answer.getNumericAnswer().subtract(question.getNumericAnswer()).abs();
                BigDecimal tol = question.getTolerance() != null ? question.getTolerance() : BigDecimal.ZERO;
                yield diff.compareTo(tol) <= 0;
            }
        };
    }

    private void saveSingleAnswer(QuizAttempt attempt, QuizQuestion question, AutosaveAnswerRequest request) {
        AttemptAnswer answer = answerRepository.findByAttemptIdAndQuestionId(attempt.getId(), question.getId())
                .orElseGet(() -> AttemptAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .build());

        if (request.getSelectedOptionIds() != null) {
            String joined = request.getSelectedOptionIds().stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));
            answer.setSelectedOptionIds(joined);
        } else {
            answer.setSelectedOptionIds(null);
        }

        answer.setTextAnswer(request.getTextAnswer());
        answer.setNumericAnswer(request.getNumericAnswer());
        answer.setAnsweredAt(Instant.now());

        answerRepository.save(answer);
    }

    // =========================================================================
    // Sanitized DTO Builders (Anti-Cheating Compliance)
    // =========================================================================

    private ActiveQuizAttemptResponse buildActiveAttemptResponse(QuizAttempt attempt, Quiz quiz) {
        List<QuizQuestion> questions = new ArrayList<>(questionRepository.findAllByQuizIdOrderByPositionAsc(quiz.getId()));
        if (Boolean.TRUE.equals(quiz.getShuffleQuestions())) {
            Collections.shuffle(questions);
        }

        Map<UUID, AttemptAnswer> savedAnswersMap = answerRepository.findAllByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (k1, k2) -> k1));

        List<ActiveQuizAttemptResponse.ActiveQuestionDto> questionDtos = new ArrayList<>();
        for (QuizQuestion q : questions) {
            List<QuizOption> options = new ArrayList<>(q.getOptions() != null ? q.getOptions() : Collections.emptyList());
            if (Boolean.TRUE.equals(quiz.getShuffleOptions())) {
                Collections.shuffle(options);
            }

            // Strictly sanitize options (no isCorrect!)
            List<ActiveQuizAttemptResponse.ActiveOptionDto> optionDtos = options.stream()
                    .map(opt -> ActiveQuizAttemptResponse.ActiveOptionDto.builder()
                            .id(opt.getId())
                            .text(opt.getText())
                            .position(opt.getPosition())
                            .build())
                    .toList();

            ActiveQuizAttemptResponse.SavedAnswerDto savedAnswerDto = null;
            AttemptAnswer saved = savedAnswersMap.get(q.getId());
            if (saved != null) {
                savedAnswerDto = ActiveQuizAttemptResponse.SavedAnswerDto.builder()
                        .selectedOptionIds(parseSelectedOptionIds(saved.getSelectedOptionIds()))
                        .textAnswer(saved.getTextAnswer())
                        .numericAnswer(saved.getNumericAnswer())
                        .build();
            }

            // Strictly sanitized question (no explanation, no correctText, no numericAnswer, no tolerance!)
            questionDtos.add(ActiveQuizAttemptResponse.ActiveQuestionDto.builder()
                    .id(q.getId())
                    .type(q.getType())
                    .text(q.getText())
                    .marks(q.getMarks())
                    .negativeMarks(q.getNegativeMarks())
                    .position(q.getPosition())
                    .options(optionDtos)
                    .savedAnswer(savedAnswerDto)
                    .build());
        }

        return ActiveQuizAttemptResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .attemptNo(attempt.getAttemptNo())
                .startedAt(attempt.getStartedAt())
                .expiresAt(attempt.getExpiresAt())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .totalMarks(quiz.getTotalMarks())
                .questions(questionDtos)
                .build();
    }

    private QuizResultResponse buildResultResponse(QuizAttempt attempt, boolean isInstructor) {
        Quiz quiz = attempt.getQuiz();
        boolean revealAnswers = isInstructor || quiz.getShowAnswersPolicy() != ShowAnswersPolicy.NEVER;

        List<QuizResultResponse.QuestionResultDto> resultQuestionList = null;

        if (revealAnswers) {
            List<QuizQuestion> questions = questionRepository.findAllByQuizIdOrderByPositionAsc(quiz.getId());
            Map<UUID, AttemptAnswer> answerMap = answerRepository.findAllByAttemptId(attempt.getId())
                    .stream()
                    .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (k1, k2) -> k1));

            resultQuestionList = new ArrayList<>();
            for (QuizQuestion q : questions) {
                AttemptAnswer ans = answerMap.get(q.getId());
                List<UUID> studentSelectedOptionIds = ans != null ? parseSelectedOptionIds(ans.getSelectedOptionIds()) : Collections.emptyList();
                String studentText = ans != null ? ans.getTextAnswer() : null;
                BigDecimal studentNumeric = ans != null ? ans.getNumericAnswer() : null;
                Boolean isCorrect = ans != null ? ans.getIsCorrect() : false;
                BigDecimal marksAwarded = ans != null ? ans.getMarksAwarded() : BigDecimal.ZERO;

                List<UUID> correctOptionIds = q.getOptions().stream()
                        .filter(QuizOption::getIsCorrect)
                        .map(QuizOption::getId)
                        .toList();

                List<QuizResultResponse.OptionResultDto> options = q.getOptions().stream()
                        .map(opt -> QuizResultResponse.OptionResultDto.builder()
                                .id(opt.getId())
                                .text(opt.getText())
                                .isCorrect(opt.getIsCorrect())
                                .build())
                        .toList();

                resultQuestionList.add(QuizResultResponse.QuestionResultDto.builder()
                        .questionId(q.getId())
                        .type(q.getType())
                        .text(q.getText())
                        .marks(q.getMarks())
                        .negativeMarks(q.getNegativeMarks())
                        .isCorrect(isCorrect)
                        .marksAwarded(marksAwarded)
                        .studentSelectedOptionIds(studentSelectedOptionIds)
                        .studentTextAnswer(studentText)
                        .studentNumericAnswer(studentNumeric)
                        .correctOptionIds(correctOptionIds)
                        .correctText(q.getCorrectText())
                        .numericAnswer(q.getNumericAnswer())
                        .tolerance(q.getTolerance())
                        .explanation(q.getExplanation())
                        .options(options)
                        .build());
            }
        }

        return QuizResultResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .studentId(attempt.getStudentId())
                .attemptNo(attempt.getAttemptNo())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .status(attempt.getStatus())
                .score(attempt.getScore())
                .totalMarks(quiz.getTotalMarks())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .passPercent(quiz.getPassPercent())
                .correctCount(attempt.getCorrectCount())
                .wrongCount(attempt.getWrongCount())
                .unansweredCount(attempt.getUnansweredCount())
                .timeTakenSeconds(attempt.getTimeTakenSeconds())
                .showAnswersPolicy(quiz.getShowAnswersPolicy())
                .questionResults(resultQuestionList)
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void recalculateQuizTotalMarks(Quiz quiz) {
        List<QuizQuestion> questions = questionRepository.findAllByQuizIdOrderByPositionAsc(quiz.getId());
        BigDecimal total = questions.stream()
                .map(QuizQuestion::getMarks)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        quiz.setTotalMarks(total);
        quizRepository.save(quiz);
    }

    private void validateQuestionRequest(CreateQuestionRequest request) {
        switch (request.getType()) {
            case SINGLE_CHOICE, TRUE_FALSE -> {
                if (request.getOptions() == null || request.getOptions().size() < 2) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            request.getType() + " question must have at least 2 options");
                }
                long correctCount = request.getOptions().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .count();
                if (correctCount != 1) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            request.getType() + " question must have exactly one correct option");
                }
            }
            case MULTI_CHOICE -> {
                if (request.getOptions() == null || request.getOptions().size() < 2) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            "MULTI_CHOICE question must have at least 2 options");
                }
                long correctCount = request.getOptions().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .count();
                if (correctCount < 1) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            "MULTI_CHOICE question must have at least one correct option");
                }
            }
            case SHORT_ANSWER -> {
                if (request.getCorrectText() == null || request.getCorrectText().trim().isEmpty()) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            "SHORT_ANSWER question requires correctText to be specified");
                }
            }
            case NUMERIC -> {
                if (request.getNumericAnswer() == null) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST,
                            "NUMERIC question requires numericAnswer to be specified");
                }
            }
        }
    }

    private List<UUID> parseSelectedOptionIds(String joinedIds) {
        if (joinedIds == null || joinedIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(joinedIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
    }

    private void verifyEnrolment(UUID courseId, UUID studentId) {
        Enrolment enrolment = enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "You must be enrolled in this course to access quizzes"));

        if (enrolment.getStatus() != EnrolmentStatus.ACTIVE && enrolment.getStatus() != EnrolmentStatus.COMPLETED) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Enrolment is no longer active (status: " + enrolment.getStatus() + ")");
        }
    }
}
