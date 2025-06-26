package com.example.quiznow.firebase_gemini

import UserManager
import android.util.Log
import com.example.quiznow.entities.Question
import com.example.quiznow.entities.QuizResult
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FirebaseRepository(private val userManager: UserManager) {

    private val db = Firebase.firestore

    suspend fun saveBookmarkedQuestion(question: Question, onComplete: (Boolean) -> Unit) {

        val userId = userManager.getCurrentUserId()
        val bookmarkedQuestion = question.copy(userId = userId.toString())

        db.collection("bookmarkedQuestions")
            .document(question.id)
            .set(bookmarkedQuestion)
            .addOnSuccessListener {
                onComplete(true)
                Log.i("FirebaseRepository", "Bookmarked question saved successfully")
            }
            .addOnFailureListener {
                onComplete(false)
                Log.i("FirebaseRepository", "Failed to save bookmarked question")
            }


    }

    fun removeBookmarkedQuestion(questionId: String, onComplete: (Boolean) -> Unit) {
        db.collection("bookmarkedQuestions")
            .document(questionId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    suspend fun getBookmarkedQuestions(onComplete: (List<Question>) -> Unit) {
        val userId = userManager.getCurrentUserId()
        db.collection("bookmarkedQuestions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val questions = documents.mapNotNull { doc ->
                    try {
                        Question(
                            id = doc.getString("id") ?: "",
                            question = doc.getString("question") ?: "",
                            options = doc.get("options") as? List<String> ?: emptyList(),
                            correctAnswer = doc.getLong("correctAnswer")?.toInt() ?: 0,
                            topic = doc.getString("topic") ?: "",
                            difficulty = doc.getString("difficulty") ?: "",
                            isBookmarked = true,
                            userId = doc.getString("userId") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onComplete(questions)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    suspend fun savePastQuiz(quizResult: QuizResult, onComplete: (Boolean) -> Unit) {
        val userId = userManager.getCurrentUserId()
        val result = quizResult.copy(userId = userId.toString())
        db.collection("pastQuizzes")
            .document(result.id)
            .set(result)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    suspend fun getPastQuizzes(onComplete: (List<QuizResult>) -> Unit) {
        val userId = userManager.getCurrentUserId()
        db.collection("pastQuizzes")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val quizzes = documents.mapNotNull { doc ->
                    try {
                        QuizResult(
                            score = doc.getLong("score")?.toInt() ?: 0,
                            totalQuestions = doc.getLong("totalQuestions")?.toInt() ?: 0,
                            topic = doc.getString("topic") ?: "",
                            difficulty = doc.getString("difficulty") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            userId = doc.getString("userId") ?: "",
                            id = doc.getString("id") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onComplete(quizzes)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
}