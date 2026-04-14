package com.example.e_voting

data class CandidateItem(
    val candidateId: Int,
    val studentId: Int,
    val periodId: Int,
    val studentName: String,
    val vision: String,
    val mission: String,
    val periodTitle: String,
    val startDate: String,
    val endDate: String,
    val hasVoted: Boolean = false
)
