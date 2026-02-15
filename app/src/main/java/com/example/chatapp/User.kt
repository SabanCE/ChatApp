package com.example.chatapp

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val shortId: String = "",
    val profileImageUrl: String = "",
    val chatColor: String = "#FFFFFF" // Varsayılan renk Beyaz
)