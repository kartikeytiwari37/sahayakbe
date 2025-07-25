package com.sahayak.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory class to create the appropriate exam type strategy based on the exam type.
 */
@Component
public class ExamTypeStrategyFactory {

    private static final Logger logger = LoggerFactory.getLogger(ExamTypeStrategyFactory.class);

    /**
     * Enum for supported exam types
     */
    public enum ExamType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        ESSAY,
        MIXED,
        SHORT_ANSWER
    }

    /**
     * Creates the appropriate exam type strategy based on the exam type.
     * 
     * @param examType The type of exam
     * @return The appropriate exam type strategy
     */
    public ExamTypeStrategy createStrategy(String examType) {
        if (examType == null) {
            logger.warn("Exam type is null, defaulting to MULTIPLE_CHOICE");
            return new MultipleChoiceExamStrategy();
        }

        try {
            ExamType type = ExamType.valueOf(examType.toUpperCase().replace(" ", "_"));
            
            switch (type) {
                case MULTIPLE_CHOICE:
                    logger.info("Creating Multiple Choice exam strategy");
                    return new MultipleChoiceExamStrategy();
                case TRUE_FALSE:
                    logger.info("Creating True/False exam strategy");
                    return new TrueFalseExamStrategy();
                case ESSAY:
                    logger.info("Creating Essay exam strategy");
                    return new EssayExamStrategy();
                case MIXED:
                    logger.info("Creating Mixed exam strategy");
                    return new MixedExamStrategy();
                case SHORT_ANSWER:
                    logger.info("Creating Short Answer exam strategy");
                    return new ShortAnswerExamStrategy();
                default:
                    logger.warn("Unknown exam type: {}, defaulting to Multiple Choice", examType);
                    return new MultipleChoiceExamStrategy();
            }
        } catch (IllegalArgumentException e) {
            // Handle legacy or non-standard exam type formats
            String normalizedType = examType.toLowerCase();
            
            if (normalizedType.contains("multiple") && normalizedType.contains("choice")) {
                logger.info("Creating Multiple Choice exam strategy from non-standard format");
                return new MultipleChoiceExamStrategy();
            } else if (normalizedType.contains("true") && (normalizedType.contains("false") || normalizedType.contains("/false"))) {
                logger.info("Creating True/False exam strategy from non-standard format");
                return new TrueFalseExamStrategy();
            } else {
                logger.warn("Unrecognized exam type format: {}, defaulting to Multiple Choice", examType);
                return new MultipleChoiceExamStrategy();
            }
        }
    }
}
