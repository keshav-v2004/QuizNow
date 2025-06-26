package com.example.quiznow.entities

import java.util.UUID

data class QuizResult(
    val score: Int,
    val totalQuestions: Int,
    val topic: String,
    val difficulty: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val id: String = UUID.randomUUID().toString()
)
