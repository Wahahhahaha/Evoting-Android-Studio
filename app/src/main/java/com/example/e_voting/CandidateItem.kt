package com.example.e_voting

data class CandidateItem(
    val candidateId: Int,
    val presidentPicture: String,
    val vicePicture: String,
    val presidentStudentId: Int,
    val viceStudentId: Int,
    val periodId: Int,
    val presidentName: String,
    val viceName: String,
    val vision: String,
    val mission: String,
    val periodTitle: String,
    val startDate: String,
    val endDate: String,
    val hasVoted: Boolean = false,
    val isVotedCandidate: Boolean = false,
    val voteCount: Int = 0,
    val isWinner: Boolean = false,
    val displayMode: String = "voting"
)
