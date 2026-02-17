package com.example.chatapp

data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val members: Map<String, Boolean> = emptyMap(),
    val adminId: String = "",
    val groupImageUrl: String = "",
    val chatColor: String = "#FFFFFF"
)