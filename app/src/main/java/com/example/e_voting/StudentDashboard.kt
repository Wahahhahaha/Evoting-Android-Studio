package com.example.e_voting

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class StudentDashboard : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentCandidateAdapter
    private val candidates = mutableListOf<CandidateItem>()
    private var userId: Int = -1
    private var studentId: Int = -1
    private var studentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_dashboard)

        userId = intent.getIntExtra("userid", -1)
        studentId = intent.getIntExtra("studentid", -1)
        studentName = intent.getStringExtra("name").orEmpty()

        findViewById<TextView>(R.id.dashboardTitle).text =
            "Welcome,\n${if (studentName.isBlank()) "Student" else studentName}!"

        recyclerView = findViewById(R.id.rvKandidat)
        adapter = StudentCandidateAdapter(candidates) { candidate ->
            confirmVote(candidate)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<MaterialButton>(R.id.logout).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCandidates()
    }

    private fun loadCandidates() {
        setLoading(true)
        thread {
            val result = runCatching {
                val url = URL("${ApiConfig.ACTIVE_CANDIDATE_LIST_URL}?studentid=$studentId")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val response = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use {
                    it.readText()
                }
                connection.disconnect()
                parseCandidateResponse(response)
            }

            runOnUiThread {
                setLoading(false)
                result.onSuccess { items ->
                    candidates.clear()
                    candidates.addAll(items)
                    adapter.notifyDataSetChanged()

                    val statusText = when {
                        items.isEmpty() -> "No active voting or finished period results yet."
                        items.first().displayMode == "result" -> "Election results are now available."
                        items.any { it.hasVoted } -> "Your vote has been saved."
                        else -> "Choose a candidate pair to vote."
                    }
                    findViewById<TextView>(R.id.txtStatusMessage).text = statusText

                    findViewById<TextView>(R.id.txtEmptyStateStudent).text =
                        "No active voting or election results available."
                    findViewById<View>(R.id.txtEmptyStateStudent).visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                }.onFailure {
                    Toast.makeText(this, "Failed to load candidates: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmVote(candidate: CandidateItem) {
        AlertDialog.Builder(this)
            .setTitle("Vote Confirmation")
            .setMessage("Vote for ${candidate.presidentName} & ${candidate.viceName} in period ${candidate.periodTitle}? You can only vote once in this period.")
            .setPositiveButton("Vote") { _, _ ->
                submitVote(candidate)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitVote(candidate: CandidateItem) {
        if (candidate.candidateId <= 0) {
            Toast.makeText(this, "Invalid candidate data", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.CANDIDATE_VOTE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=UTF-8"
                    )
                }

                val payload = buildString {
                    append("studentid=")
                    append(URLEncoder.encode(studentId.toString(), "UTF-8"))
                    append("&userid=")
                    append(URLEncoder.encode(userId.toString(), "UTF-8"))
                    append("&candidateid=")
                    append(URLEncoder.encode(candidate.candidateId.toString(), "UTF-8"))
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                    it.write(payload)
                    it.flush()
                }

                val response = BufferedReader(
                    if (connection.responseCode in 200..299) {
                        connection.inputStream.reader(Charsets.UTF_8)
                    } else {
                        connection.errorStream?.reader(Charsets.UTF_8)
                            ?: connection.inputStream.reader(Charsets.UTF_8)
                    }
                ).use { it.readText() }

                connection.disconnect()
                JSONObject(response)
            }

            runOnUiThread {
                setLoading(false)
                result.onSuccess { response ->
                    Toast.makeText(this, response.optString("message", "Vote process completed"), Toast.LENGTH_SHORT).show()
                    loadCandidates()
                }.onFailure {
                    Toast.makeText(this, "Failed to submit vote: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseCandidateResponse(response: String): List<CandidateItem> {
        val array = JSONArray(response)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    CandidateItem(
                        candidateId = item.optInt("candidateid"),
                        presidentPicture = item.optString("picture"),
                        vicePicture = item.optString("vice_picture", item.optString("picture")),
                        presidentStudentId = item.optInt("president_studentid", item.optInt("studentid")),
                        viceStudentId = item.optInt("vice_studentid", item.optInt("studentid")),
                        periodId = item.optInt("periodid"),
                        presidentName = item.optString("president_name"),
                        viceName = item.optString("vice_name"),
                        vision = item.optString("vision"),
                        mission = item.optString("mission"),
                        periodTitle = item.optString("period_title"),
                        startDate = item.optString("startdate"),
                        endDate = item.optString("enddate"),
                        hasVoted = item.optInt("has_voted", 0) == 1,
                        isVotedCandidate = item.optInt("is_voted_candidate", 0) == 1,
                        voteCount = item.optInt("vote_count", 0),
                        isWinner = item.optInt("is_winner", 0) == 1,
                        displayMode = item.optString("display_mode", "voting")
                    )
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findViewById<MaterialButton>(R.id.logout).isEnabled = !isLoading
    }
}
