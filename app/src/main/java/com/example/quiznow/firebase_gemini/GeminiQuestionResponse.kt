package com.example.quiznow.firebase_gemini

import com.google.gson.annotations.SerializedName

data class GeminiQuestionResponse(
    @SerializedName ("question") val question: String,
    @SerializedName ("options") val options: List<String>,
    @SerializedName ("correctAnswer") val correctAnswer: Int,
    @SerializedName ("explanation") val explanation: String? = null
)
