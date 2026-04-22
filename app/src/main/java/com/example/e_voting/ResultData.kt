package com.example.e_voting

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ResultData : AppCompatActivity() {

    private val items = mutableListOf<VotingResultItem>()
    private val allItems = mutableListOf<VotingResultItem>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VotingResultAdapter
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var txtPageIndicator: TextView
    private var currentPage: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_data_activity)

        recyclerView = findViewById(R.id.rvVotingResult)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        txtPageIndicator = findViewById(R.id.txtPageIndicator)
        adapter = VotingResultAdapter(items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        setupPaginationControls()
    }

    override fun onResume() {
        super.onResume()
        loadResults()
    }

    private fun loadResults() {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.VOTING_RESULT_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val response = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use {
                    it.readText()
                }
                connection.disconnect()
                parseResults(response)
            }

            runOnUiThread {
                setLoading(false)
                result.onSuccess { parsed ->
                    allItems.clear()
                    allItems.addAll(parsed)
                    currentPage = 1
                    renderCurrentPage()
                }.onFailure {
                    Toast.makeText(this, "Failed to load voting results: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseResults(response: String): List<VotingResultItem> {
        val array = JSONArray(response)
        val rawRows = mutableListOf<RawRow>()

        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            if (obj.optString("display_mode", "result") != "result") continue
            rawRows.add(
                RawRow(
                    candidateId = obj.optInt("candidateid"),
                    periodId = obj.optInt("periodid"),
                    periodTitle = obj.optString("period_title").ifBlank { "Unknown Period" },
                    presidentName = obj.optString("president_name"),
                    viceName = obj.optString("vice_name"),
                    voteCount = obj.optInt("vote_count", 0),
                    isWinner = obj.optInt("is_winner", 0) == 1
                )
            )
        }

        val totalVotesByPeriod = rawRows
            .groupBy { it.periodId }
            .mapValues { entry -> entry.value.sumOf { it.voteCount } }

        return rawRows
            .groupBy { it.periodId }
            .map { (periodId, periodRows) ->
                val periodTitle = periodRows.firstOrNull()?.periodTitle ?: "Unknown Period"
                val periodTotal = totalVotesByPeriod[periodId] ?: 0
                val candidates = periodRows
                    .map { row ->
                        val percent = if (periodTotal > 0) {
                            ((row.voteCount.toDouble() / periodTotal.toDouble()) * 100.0).toInt()
                        } else {
                            0
                        }
                        CandidateResultItem(
                            candidateId = row.candidateId,
                            presidentName = row.presidentName,
                            viceName = row.viceName,
                            voteCount = row.voteCount,
                            percentage = percent,
                            isWinner = row.isWinner
                        )
                    }
                    .sortedByDescending { it.voteCount }

                VotingResultItem(
                    periodId = periodId,
                    periodTitle = periodTitle,
                    totalVotes = periodTotal,
                    candidates = candidates
                )
            }
            .sortedByDescending { it.periodId }
    }

    private fun setLoading(isLoading: Boolean) {
        recyclerView.alpha = if (isLoading) 0.5f else 1f
        recyclerView.isEnabled = !isLoading
        if (isLoading && items.isEmpty()) {
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupPaginationControls() {
        btnPrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage -= 1
                renderCurrentPage()
            }
        }

        btnNext.setOnClickListener {
            val totalPages = calculateTotalPages()
            if (currentPage < totalPages) {
                currentPage += 1
                renderCurrentPage()
            }
        }
    }

    private fun renderCurrentPage() {
        val totalPages = calculateTotalPages()
        if (currentPage > totalPages) {
            currentPage = totalPages
        }

        val startIndex = (currentPage - 1) * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, allItems.size)
        val pageItems = if (allItems.isEmpty()) {
            emptyList()
        } else {
            allItems.subList(startIndex, endIndex)
        }

        items.clear()
        items.addAll(pageItems)
        adapter.notifyDataSetChanged()

        findViewById<View>(R.id.txtEmptyState).visibility =
            if (allItems.isEmpty()) View.VISIBLE else View.GONE
        txtPageIndicator.text = "Page $currentPage of $totalPages"
        btnPrev.isEnabled = currentPage > 1 && allItems.isNotEmpty()
        btnNext.isEnabled = currentPage < totalPages && allItems.isNotEmpty()
        btnPrev.alpha = if (btnPrev.isEnabled) 1f else 0.4f
        btnNext.alpha = if (btnNext.isEnabled) 1f else 0.4f
    }

    private fun calculateTotalPages(): Int {
        if (allItems.isEmpty()) return 1
        return (allItems.size + PAGE_SIZE - 1) / PAGE_SIZE
    }

    private data class RawRow(
        val candidateId: Int,
        val periodId: Int,
        val periodTitle: String,
        val presidentName: String,
        val viceName: String,
        val voteCount: Int,
        val isWinner: Boolean
    )

    companion object {
        private const val PAGE_SIZE = 10
    }
}
