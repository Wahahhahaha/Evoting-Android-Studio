package com.example.e_voting

import android.os.Bundle
import android.view.View
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VotingResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_data_activity)

        recyclerView = findViewById(R.id.rvVotingResult)
        adapter = VotingResultAdapter(items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
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
                    items.clear()
                    items.addAll(parsed)
                    adapter.notifyDataSetChanged()
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

        return rawRows.map { row ->
            val periodTotal = totalVotesByPeriod[row.periodId] ?: 0
            val percent = if (periodTotal > 0) {
                ((row.voteCount.toDouble() / periodTotal.toDouble()) * 100.0).toInt()
            } else {
                0
            }

            VotingResultItem(
                candidateId = row.candidateId,
                periodId = row.periodId,
                periodTitle = row.periodTitle,
                presidentName = row.presidentName,
                viceName = row.viceName,
                voteCount = row.voteCount,
                percentage = percent,
                isWinner = row.isWinner
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        recyclerView.alpha = if (isLoading) 0.5f else 1f
        recyclerView.isEnabled = !isLoading
        if (isLoading && items.isEmpty()) {
            recyclerView.visibility = View.VISIBLE
        }
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
}
