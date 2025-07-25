package com.sahayak.service;

import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Mock implementation of the ExamCreationService for testing purposes.
 * This service returns a sample response without calling the Gemini API.
 */
@Service
public class MockExamCreationService extends ExamCreationService {

    private static final Logger logger = LoggerFactory.getLogger(MockExamCreationService.class);

    public MockExamCreationService() {
        // Call the parent constructor with null parameters since we won't be using them
        super(null, null);
    }

    @Override
    public ExamCreationResponse createExam(ExamCreationRequest request) {
        logger.info("Creating mock exam with request: {}", request);

        try {
            // Create a sample exam response based on the request
            ExamCreationResponse.ExamData examData = createMockExamData(request);
            
            ExamCreationResponse response = new ExamCreationResponse("success", "Exam created successfully", examData);
            response.setRawResponse("This is a mock response. In a real implementation, this would be the raw response from the Gemini API.");
            
            return response;
        } catch (Exception e) {
            logger.error("Error creating mock exam", e);
            return new ExamCreationResponse("error", "Failed to create mock exam: " + e.getMessage());
        }
    }

    /**
     * Creates a mock exam data based on the request parameters
     * 
     * @param request The exam creation request
     * @return The mock exam data
     */
    private ExamCreationResponse.ExamData createMockExamData(ExamCreationRequest request) {
        ExamCreationResponse.ExamData examData = new ExamCreationResponse.ExamData();
        examData.setSubject(request.getSubject());
        examData.setGradeLevel(request.getGradeLevel());
        examData.setExamType(request.getExamType());
        
        List<ExamCreationResponse.Question> questions = new ArrayList<>();
        
        // Create sample questions based on the subject
        if ("Mathematics".equalsIgnoreCase(request.getSubject())) {
            questions.addAll(createMathQuestions(request.getNumberOfQuestions()));
        } else if ("Science".equalsIgnoreCase(request.getSubject())) {
            questions.addAll(createScienceQuestions(request.getNumberOfQuestions()));
        } else if ("History".equalsIgnoreCase(request.getSubject())) {
            questions.addAll(createHistoryQuestions(request.getNumberOfQuestions()));
        } else {
            questions.addAll(createGenericQuestions(request.getNumberOfQuestions(), request.getSubject()));
        }
        
        examData.setQuestions(questions);
        
        return examData;
    }

    /**
     * Creates sample math questions
     * 
     * @param count The number of questions to create
     * @return The list of questions
     */
    private List<ExamCreationResponse.Question> createMathQuestions(int count) {
        List<ExamCreationResponse.Question> questions = new ArrayList<>();
        
        String[] questionTexts = {
            "Solve for x: 2x + 5 = 15",
            "Find the area of a circle with radius 7 cm. Use π = 3.14.",
            "Simplify the expression: 3(x + 2) - 2(x - 1)",
            "If a triangle has sides of lengths 3, 4, and 5, what is its area?",
            "Solve the quadratic equation: x² - 5x + 6 = 0",
            "Find the value of y in the equation: 2y - 3 = 7",
            "What is the slope of the line passing through the points (2, 3) and (4, 7)?",
            "Find the value of x if 3x + 2 = 14",
            "Calculate the perimeter of a rectangle with length 8 cm and width 5 cm",
            "Solve for x: 3(x - 2) = 15"
        };
        
        String[][] options = {
            {"x = 5", "x = 10", "x = 7.5", "x = 4"},
            {"153.86 cm²", "43.96 cm²", "21.98 cm²", "153.86 cm"},
            {"x + 8", "x + 4", "5x + 1", "5x - 1"},
            {"6 square units", "12 square units", "7.5 square units", "6.5 square units"},
            {"x = 2 and x = 3", "x = -2 and x = -3", "x = 2 and x = -3", "x = -2 and x = 3"},
            {"y = 5", "y = 2", "y = 4", "y = 10"},
            {"2", "4", "1/2", "2.5"},
            {"x = 4", "x = 6", "x = 12", "x = 3"},
            {"26 cm", "13 cm", "40 cm²", "30 cm"},
            {"x = 7", "x = 5", "x = 9", "x = 3"}
        };
        
        String[] correctAnswers = {
            "x = 5",
            "153.86 cm²",
            "x + 8",
            "6 square units",
            "x = 2 and x = 3",
            "y = 5",
            "2",
            "x = 4",
            "26 cm",
            "x = 7"
        };
        
        String[] explanations = {
            "2x + 5 = 15\n2x = 15 - 5\n2x = 10\nx = 5",
            "Area of a circle = πr²\nArea = 3.14 × 7² = 3.14 × 49 = 153.86 cm²",
            "3(x + 2) - 2(x - 1) = 3x + 6 - 2x + 2 = 3x - 2x + 6 + 2 = x + 8",
            "Using the Pythagorean theorem, we can confirm this is a right triangle. Area = (1/2) × base × height = (1/2) × 3 × 4 = 6 square units",
            "x² - 5x + 6 = 0\n(x - 2)(x - 3) = 0\nx = 2 or x = 3",
            "2y - 3 = 7\n2y = 7 + 3\n2y = 10\ny = 5",
            "Slope = (y₂ - y₁)/(x₂ - x₁) = (7 - 3)/(4 - 2) = 4/2 = 2",
            "3x + 2 = 14\n3x = 14 - 2\n3x = 12\nx = 4",
            "Perimeter of a rectangle = 2(length + width) = 2(8 + 5) = 2(13) = 26 cm",
            "3(x - 2) = 15\n3x - 6 = 15\n3x = 15 + 6\n3x = 21\nx = 7"
        };
        
        // Create questions based on the count
        for (int i = 0; i < Math.min(count, questionTexts.length); i++) {
            ExamCreationResponse.Question question = new ExamCreationResponse.Question();
            question.setQuestionText(questionTexts[i]);
            question.setOptions(Arrays.asList(options[i]));
            question.setCorrectAnswer(correctAnswers[i]);
            question.setExplanation(explanations[i]);
            
            questions.add(question);
        }
        
        return questions;
    }

    /**
     * Creates sample science questions
     * 
     * @param count The number of questions to create
     * @return The list of questions
     */
    private List<ExamCreationResponse.Question> createScienceQuestions(int count) {
        List<ExamCreationResponse.Question> questions = new ArrayList<>();
        
        String[] questionTexts = {
            "What is the chemical symbol for gold?",
            "Which of the following is NOT a state of matter?",
            "What is the process by which plants make their own food called?",
            "Which planet is known as the Red Planet?",
            "What is the smallest unit of matter?",
            "Which of the following is a renewable energy source?",
            "What is the main function of the mitochondria in a cell?",
            "Which of the following is NOT a type of rock?",
            "What is the speed of light in a vacuum?",
            "Which of the following is NOT a greenhouse gas?"
        };
        
        String[][] options = {
            {"Au", "Ag", "Fe", "Cu"},
            {"Solid", "Liquid", "Gas", "Energy"},
            {"Respiration", "Photosynthesis", "Digestion", "Excretion"},
            {"Venus", "Mars", "Jupiter", "Mercury"},
            {"Atom", "Molecule", "Cell", "Proton"},
            {"Coal", "Natural gas", "Solar energy", "Oil"},
            {"Protein synthesis", "Cell division", "Energy production", "Waste removal"},
            {"Igneous", "Sedimentary", "Metamorphic", "Metallic"},
            {"300,000 km/s", "150,000 km/s", "3,000,000 km/s", "30,000 km/s"},
            {"Carbon dioxide", "Methane", "Water vapor", "Nitrogen"}
        };
        
        String[] correctAnswers = {
            "Au",
            "Energy",
            "Photosynthesis",
            "Mars",
            "Atom",
            "Solar energy",
            "Energy production",
            "Metallic",
            "300,000 km/s",
            "Nitrogen"
        };
        
        String[] explanations = {
            "The chemical symbol for gold is Au, which comes from the Latin word 'aurum'.",
            "The three states of matter are solid, liquid, and gas. Energy is a form of power, not a state of matter.",
            "Photosynthesis is the process by which plants use sunlight, water, and carbon dioxide to create oxygen and energy in the form of sugar.",
            "Mars is known as the Red Planet due to its reddish appearance, which is caused by iron oxide (rust) on its surface.",
            "An atom is the smallest unit of matter that retains the properties of an element.",
            "Solar energy is renewable as it comes from the sun, which is a virtually inexhaustible source. Coal, natural gas, and oil are fossil fuels, which are non-renewable.",
            "Mitochondria are often referred to as the 'powerhouse of the cell' because they generate most of the cell's supply of ATP, which is used as a source of chemical energy.",
            "The three main types of rocks are igneous, sedimentary, and metamorphic. Metallic is not a type of rock but refers to materials containing metals.",
            "The speed of light in a vacuum is approximately 300,000 kilometers per second (or 186,282 miles per second).",
            "Carbon dioxide, methane, and water vapor are all greenhouse gases that trap heat in Earth's atmosphere. Nitrogen, which makes up about 78% of Earth's atmosphere, is not a greenhouse gas."
        };
        
        // Create questions based on the count
        for (int i = 0; i < Math.min(count, questionTexts.length); i++) {
            ExamCreationResponse.Question question = new ExamCreationResponse.Question();
            question.setQuestionText(questionTexts[i]);
            question.setOptions(Arrays.asList(options[i]));
            question.setCorrectAnswer(correctAnswers[i]);
            question.setExplanation(explanations[i]);
            
            questions.add(question);
        }
        
        return questions;
    }

    /**
     * Creates sample history questions
     * 
     * @param count The number of questions to create
     * @return The list of questions
     */
    private List<ExamCreationResponse.Question> createHistoryQuestions(int count) {
        List<ExamCreationResponse.Question> questions = new ArrayList<>();
        
        String[] questionTexts = {
            "Who was the first President of the United States?",
            "In which year did World War II end?",
            "Which ancient civilization built the pyramids at Giza?",
            "Who wrote the Declaration of Independence?",
            "Which empire was ruled by Genghis Khan?",
            "The Renaissance period began in which country?",
            "Who was the first woman to fly solo across the Atlantic Ocean?",
            "Which treaty ended World War I?",
            "Who was the leader of the Soviet Union during most of World War II?",
            "Which event marked the beginning of World War I?"
        };
        
        String[][] options = {
            {"Thomas Jefferson", "George Washington", "John Adams", "Benjamin Franklin"},
            {"1943", "1944", "1945", "1946"},
            {"Roman", "Greek", "Egyptian", "Mesopotamian"},
            {"George Washington", "Benjamin Franklin", "Thomas Jefferson", "John Adams"},
            {"Roman Empire", "Ottoman Empire", "Mongol Empire", "Byzantine Empire"},
            {"England", "France", "Italy", "Spain"},
            {"Amelia Earhart", "Bessie Coleman", "Harriet Quimby", "Jacqueline Cochran"},
            {"Treaty of Paris", "Treaty of Versailles", "Treaty of London", "Treaty of Rome"},
            {"Vladimir Lenin", "Joseph Stalin", "Leon Trotsky", "Nikita Khrushchev"},
            {"The assassination of Archduke Franz Ferdinand", "The invasion of Poland", "The sinking of the Lusitania", "The Battle of Verdun"}
        };
        
        String[] correctAnswers = {
            "George Washington",
            "1945",
            "Egyptian",
            "Thomas Jefferson",
            "Mongol Empire",
            "Italy",
            "Amelia Earhart",
            "Treaty of Versailles",
            "Joseph Stalin",
            "The assassination of Archduke Franz Ferdinand"
        };
        
        String[] explanations = {
            "George Washington served as the first President of the United States from 1789 to 1797.",
            "World War II ended in 1945 with the surrender of Germany in May and Japan in September.",
            "The ancient Egyptians built the pyramids at Giza, with the Great Pyramid being constructed around 2560 BCE.",
            "Thomas Jefferson was the principal author of the Declaration of Independence, which was adopted by the Continental Congress on July 4, 1776.",
            "Genghis Khan founded and ruled the Mongol Empire, which became the largest contiguous empire in history after his death.",
            "The Renaissance began in Italy in the late 13th century before spreading to the rest of Europe.",
            "Amelia Earhart became the first woman to fly solo across the Atlantic Ocean in 1932.",
            "The Treaty of Versailles, signed on June 28, 1919, formally ended World War I between Germany and the Allied Powers.",
            "Joseph Stalin was the leader of the Soviet Union from the mid-1920s until his death in 1953, including during most of World War II.",
            "The assassination of Archduke Franz Ferdinand of Austria on June 28, 1914, is considered the immediate cause of World War I."
        };
        
        // Create questions based on the count
        for (int i = 0; i < Math.min(count, questionTexts.length); i++) {
            ExamCreationResponse.Question question = new ExamCreationResponse.Question();
            question.setQuestionText(questionTexts[i]);
            question.setOptions(Arrays.asList(options[i]));
            question.setCorrectAnswer(correctAnswers[i]);
            question.setExplanation(explanations[i]);
            
            questions.add(question);
        }
        
        return questions;
    }

    /**
     * Creates generic questions for any subject
     * 
     * @param count The number of questions to create
     * @param subject The subject of the questions
     * @return The list of questions
     */
    private List<ExamCreationResponse.Question> createGenericQuestions(int count, String subject) {
        List<ExamCreationResponse.Question> questions = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            ExamCreationResponse.Question question = new ExamCreationResponse.Question();
            question.setQuestionText("Sample " + subject + " question " + i);
            question.setOptions(Arrays.asList("Option A", "Option B", "Option C", "Option D"));
            question.setCorrectAnswer("Option A");
            question.setExplanation("This is a sample explanation for question " + i);
            
            questions.add(question);
        }
        
        return questions;
    }
}
