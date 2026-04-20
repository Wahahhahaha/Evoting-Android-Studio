package com.example.e_voting

data class VotingResultItem(
    val candidateId: Int,
    val periodId: Int,
    val periodTitle: String,
    val presidentName: String,
    val viceName: String,
    val voteCount: Int,
    val percentage: Int,
    val isWinner: Boolean
)
