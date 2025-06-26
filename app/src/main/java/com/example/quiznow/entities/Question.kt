package com.example.quiznow.entities

data class Question(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val topic: String,
    val difficulty: String,
    var isBookmarked: Boolean = false,
    val userId: String = ""
)
