package com.example.e_voting

data class ClassItem(
    val classId: Int,
    val className: String,
    val majorName: String = "",
    val batchName: String = "",
    val displayName: String = className
)
