package com.example.chatapp

data class Message(
    val message: String = "",
    val senderId: String = "",
    val timestamp: Long = 0
)