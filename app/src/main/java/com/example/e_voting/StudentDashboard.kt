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
    private var studentId: Int = -1
    private var studentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_dashboard)

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

                    val hasAnyVotedPeriod = items.any { it.hasVoted }
                    findViewById<TextView>(R.id.txtStatusMessage).text =
                        if (hasAnyVotedPeriod) {
                            "Kamu sudah vote di periode aktif yang tersedia."
                        } else {
                            "Pilih kandidat aktif untuk periode yang sedang berjalan."
                        }

                    findViewById<View>(R.id.txtEmptyStateStudent).visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                }.onFailure {
                    Toast.makeText(this, "Gagal memuat kandidat: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmVote(candidate: CandidateItem) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi vote")
            .setMessage("Vote ${candidate.studentName} untuk periode ${candidate.periodTitle}? Kamu hanya bisa vote 1 kali di periode ini.")
            .setPositiveButton("Vote") { _, _ ->
                submitVote(candidate)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun submitVote(candidate: CandidateItem) {
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
                    Toast.makeText(this, response.optString("message", "Proses vote selesai"), Toast.LENGTH_SHORT).show()
                    loadCandidates()
                }.onFailure {
                    Toast.makeText(this, "Gagal mengirim vote: ${it.message}", Toast.LENGTH_LONG).show()
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
                        picture = item.optString("picture"),
                        studentId = item.optInt("studentid"),
                        periodId = item.optInt("periodid"),
                        studentName = item.optString("student_name"),
                        vision = item.optString("vision"),
                        mission = item.optString("mission"),
                        periodTitle = item.optString("period_title"),
                        startDate = item.optString("startdate"),
                        endDate = item.optString("enddate"),
                        hasVoted = item.optInt("has_voted", 0) == 1
                    )
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findViewById<MaterialButton>(R.id.logout).isEnabled = !isLoading
    }
}
