package com.example.quiznow.entities

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

data class Topic(
    val id: String,
    val name: String,

    @DrawableRes
    val icon: Int,
    val color: Color

)
