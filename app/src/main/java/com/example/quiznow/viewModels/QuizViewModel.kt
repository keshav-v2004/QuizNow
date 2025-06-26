
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quiznow.R
import com.example.quiznow.constants.ApiKey
import com.example.quiznow.entities.Question
import com.example.quiznow.entities.QuizResult
import com.example.quiznow.entities.Topic
import com.example.quiznow.firebase_gemini.FirebaseRepository
import com.example.quiznow.firebase_gemini.GeminiQuestionResponse
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizViewModel(private val context: Context) : ViewModel() {

    private val _topics = mutableStateOf(getTopics())
    val topics: State<List<Topic>> = _topics

    private val _bookmarkedQuestions = mutableStateOf<List<Question>>(emptyList())
    val bookmarkedQuestions: State<List<Question>> = _bookmarkedQuestions

    private val _pastQuizzes = mutableStateOf<List<QuizResult>>(emptyList())
    val pastQuizzes: State<List<QuizResult>> = _pastQuizzes

    private val _currentQuestions = mutableStateOf<List<Question>>(emptyList())
    val currentQuestions: State<List<Question>> = _currentQuestions

    internal val _currentQuestionIndex = mutableStateOf(0)
    val currentQuestionIndex: State<Int> = _currentQuestionIndex

    private val _selectedAnswers = mutableStateOf<MutableMap<Int, Int>>(mutableMapOf())
    val selectedAnswers: State<MutableMap<Int, Int>> = _selectedAnswers

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // Initialize UserManager and FirebaseRepository
    private val userManager = UserManager(context)
    private val firebaseRepository = FirebaseRepository(userManager)

    // Gemini SDK instance
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = ApiKey.apiKey
    )

    private val gson = Gson()

    init {
        if (userManager.isUserLoggedIn()) {
            loadBookmarkedQuestions()
            loadPastQuizzes()
        }
    }

    fun refreshUserData() {
        if (userManager.isUserLoggedIn()) {
            loadBookmarkedQuestions()
            loadPastQuizzes()
        } else {
            _bookmarkedQuestions.value = emptyList()
            _pastQuizzes.value = emptyList()
        }
    }

    private fun loadBookmarkedQuestions() {
        viewModelScope.launch {
            firebaseRepository.getBookmarkedQuestions { questions ->
                _bookmarkedQuestions.value = questions
            }
        }
    }

    private fun loadPastQuizzes() {
        viewModelScope.launch {
            firebaseRepository.getPastQuizzes { quizzes ->
                _pastQuizzes.value = quizzes
            }
        }
    }

    // Generate questions using Gemini SDK
    private suspend fun generateQuestionsFromGemini(topic: String, count: Int, difficulty: String): List<Question> = withContext(Dispatchers.IO) {
        val prompt = createPrompt(topic, count, difficulty)

        try {
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: throw Exception("Empty response from Gemini")

            val cleanedResponse = cleanJsonResponse(responseText)

            val geminiQuestions: Array<GeminiQuestionResponse> = try {
                gson.fromJson(cleanedResponse, Array<GeminiQuestionResponse>::class.java)
            } catch (e: JsonSyntaxException) {
                throw Exception("Failed to parse JSON response: ${e.message}")
            }

            geminiQuestions.mapIndexed { index, geminiQuestion ->
                val questionId = "${topic}_${System.currentTimeMillis()}_$index"
                Question(
                    id = questionId,
                    question = geminiQuestion.question,
                    options = geminiQuestion.options,
                    correctAnswer = geminiQuestion.correctAnswer,
                    topic = topic,
                    difficulty = difficulty,
                    isBookmarked = isQuestionBookmarked(questionId)
                )
            }
        } catch (e: Exception) {
            throw Exception("Gemini API error: ${e.message}")
        }
    }

    private fun createPrompt(topic: String, count: Int, difficulty: String): String {
        return """
            Generate exactly $count multiple choice questions about $topic with $difficulty difficulty level.
            Try not to generate repetitive questions.
            Requirements:
            - Each question should have exactly 4 options
            - Only one correct answer per question
            - correctAnswer should be the index (0-3) of the correct option
            - Questions should be appropriate for $difficulty level
            - Avoid overly complex or ambiguous questions
            
            Return ONLY a JSON array in this exact format:
            [
                {
                    "question": "What is the time complexity of binary search?",
                    "options": ["O(n)", "O(log n)", "O(n²)", "O(1)"],
                    "correctAnswer": 1,
                    "explanation": "Binary search divides the search space in half with each iteration"
                },
                {
                    "question": "Which data structure follows LIFO principle?",
                    "options": ["Queue", "Stack", "Array", "Linked List"],
                    "correctAnswer": 1,
                    "explanation": "Stack follows Last In First Out (LIFO) principle"
                }
            ]
            
            Topic: $topic
            Difficulty: $difficulty
            Count: $count
        """.trimIndent()
    }

    private fun cleanJsonResponse(responseText: String): String {
        // Remove markdown code blocks if present
        var cleaned = responseText.replace("```json", "").replace("```", "")

        // Find the JSON array start and end
        val startIndex = cleaned.indexOf('[')
        val endIndex = cleaned.lastIndexOf(']')

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1)
        }

        // Remove any extra whitespace and newlines
        return cleaned.trim()
    }

    fun generateQuestions(topic: String, count: Int, difficulty: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val questions = generateQuestionsFromGemini(topic, count, difficulty)
                _currentQuestions.value = questions
                _currentQuestionIndex.value = 0
                _selectedAnswers.value.clear()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to generate questions: ${e.message}"
                // Fallback to sample questions
                val questions = generateSampleQuestions(topic, count, difficulty)
                _currentQuestions.value = questions
                _currentQuestionIndex.value = 0
                _selectedAnswers.value.clear()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAnswer(questionIndex: Int, answerIndex: Int) {
        _selectedAnswers.value[questionIndex] = answerIndex
    }

    fun toggleBookmark(question: Question) {
        viewModelScope.launch {
            if (question.isBookmarked) {
                firebaseRepository.removeBookmarkedQuestion(question.id) { success ->
                    if (success) {
                        _bookmarkedQuestions.value = _bookmarkedQuestions.value.filter { it.id != question.id }
                        val updatedQuestions = _currentQuestions.value.map {
                            if (it.id == question.id) it.copy(isBookmarked = false) else it
                        }
                        _currentQuestions.value = updatedQuestions
                    }
                }
            } else {
                val bookmarkedQuestion = question.copy(isBookmarked = true)
                firebaseRepository.saveBookmarkedQuestion(bookmarkedQuestion) { success ->
                    if (success) {
                        _bookmarkedQuestions.value = bookmarkedQuestions.value + bookmarkedQuestion
                        val updatedQuestions = _currentQuestions.value.map {
                            if (it.id == question.id) it.copy(isBookmarked = true) else it
                        }
                        _currentQuestions.value = updatedQuestions
                    }
                }
            }
        }
    }
    fun submitQuiz(topic: String, difficulty: String): QuizResult {
        val questions = _currentQuestions.value
        var score = 0

        questions.forEachIndexed { index, question ->
            val selectedAnswer = _selectedAnswers.value[index]
            if (selectedAnswer == question.correctAnswer) {
                score++
            }
        }

        val result = QuizResult(score, questions.size, topic, difficulty)

        viewModelScope.launch {
            firebaseRepository.savePastQuiz(result) { success ->
                if (success) {
                    _pastQuizzes.value = listOf(result) + _pastQuizzes.value
                }
            }
        }

        return result
    }
    private fun isQuestionBookmarked(questionId: String): Boolean {
        return _bookmarkedQuestions.value.any { it.id == questionId }
    }

    private fun getTopics(): List<Topic> {
        return listOf(
            Topic("dsa", "DSA", R.drawable.dsa, Color(0xFF4CAF50)),
            Topic("html", "HTML", R.drawable.html5_logo_and_wordmark_svg, Color(0xFF2196F3)),
            Topic("css", "CSS", R.drawable.css_logo, Color(0xFF9C27B0)),
            Topic("blockchain", "Blockchain", R.drawable.blockchain_icon_design_cryptocurrency_digital_logo_vector, Color(0xFF795548)),
            Topic("js", "JavaScript", R.drawable.javascript_logo_javascript_icon_transparent_free_png, Color(0xFFFF9800)),
            Topic("devops", "DevOps", R.drawable.istockphoto_1438577807_612x612, Color(0xFF607D8B)),
            Topic("github", "GitHub", R.drawable.git_icon_2048x2048_juzdf1l5, Color(0xFF424242)),
            Topic("system_design", "System Design", R.drawable.systemdevelopmentinfographic1, Color(0xFFE91E63)),
            Topic("spring_boot", "Spring Boot", R.drawable.springboot, Color(0xFF4CAF50)),
            Topic("mern", "MERN", R.drawable.mernlogo, Color(0xFF00BCD4)),
            Topic("aws", "AWS", R.drawable.aws, Color(0xFFFF5722)),
            Topic("api", "API", R.drawable.springboot, Color(0xFF3F51B5)),
            Topic("vcs", "VCS (Git)", R.drawable.git_icon_2048x2048_juzdf1l5, Color(0xFF009688)),
            Topic("ml", "Machine Learning", R.drawable.machinelearning, Color(0xFFE91E63)),
            Topic("ai", "AI", R.drawable.machinelearning, Color(0xFF673AB7)),
            Topic("python", "Python",R.drawable.python, Color(0xFF4CAF50)),
            Topic("data_science", "Data Science", R.drawable.data_science, Color(0xFF2196F3)),
            Topic("oops", "OOPs", R.drawable.oops, Color(0xFFFF9800)),
            Topic("iot", "IoT", R.drawable.iot, Color(0xFF795548)),
            Topic("kotlin", "Kotlin", R.drawable.kotlin_icon, Color(0xFF4CAF50))
        )
    }

    private fun generateSampleQuestions(topic: String, count: Int, difficulty: String): List<Question> {
        val sampleQuestions = when (topic.lowercase()) {
            "dsa" -> listOf(
                Question("dsa_1", "What is the time complexity of binary search?",
                    listOf("O(n)", "O(log n)", "O(n²)", "O(1)"), 1, topic, difficulty),
                Question("dsa_2", "Which data structure uses LIFO principle?",
                    listOf("Queue", "Stack", "Array", "Linked List"), 1, topic, difficulty),
                Question("dsa_3", "What is the space complexity of merge sort?",
                    listOf("O(1)", "O(log n)", "O(n)", "O(n log n)"), 2, topic, difficulty)
            )
            "javascript", "js" -> listOf(
                Question("js_1", "What does 'this' keyword refer to in JavaScript?",
                    listOf("Global object", "Current function", "Current object", "Depends on context"), 3, topic, difficulty),
                Question("js_2", "Which method is used to add elements to the end of an array?",
                    listOf("push()", "pop()", "shift()", "unshift()"), 0, topic, difficulty),
                Question("js_3", "What is closure in JavaScript?",
                    listOf("A loop", "A function with access to outer scope", "An object", "A variable"), 1, topic, difficulty)
            )
            else -> listOf(
                Question("${topic}_1", "Sample question for $topic?",
                    listOf("Option A", "Option B", "Option C", "Option D"), 1, topic, difficulty),
                Question("${topic}_2", "Another question for $topic?",
                    listOf("Choice 1", "Choice 2", "Choice 3", "Choice 4"), 2, topic, difficulty)
            )
        }

        // Check bookmark status for each question
        return sampleQuestions.take(count).map { question ->
            question.copy(isBookmarked = isQuestionBookmarked(question.id))
        }
    }
}