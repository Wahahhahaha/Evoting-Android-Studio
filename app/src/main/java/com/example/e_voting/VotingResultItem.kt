package com.example.e_voting

data class VotingResultItem(
    val periodId: Int,
    val periodTitle: String,
    val totalVotes: Int,
    val candidates: List<CandidateResultItem>
)

data class CandidateResultItem(
    val candidateId: Int,
    val presidentName: String,
    val viceName: String,
    val voteCount: Int,
    val percentage: Int,
    val isWinner: Boolean
)
