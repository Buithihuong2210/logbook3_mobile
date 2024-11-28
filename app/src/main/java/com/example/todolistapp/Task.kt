package com.example.todolistapp

import java.util.*

data class Task(
    val id: Int? = null,
    val name: String,
    var isCompleted: Boolean = false
)
