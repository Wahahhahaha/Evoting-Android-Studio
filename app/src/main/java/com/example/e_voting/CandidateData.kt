package com.example.e_voting

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class CandidateData : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CandidateAdapter
    private val candidates = mutableListOf<CandidateItem>()

    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadCandidates()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.candidate_data_activity)

        recyclerView = findViewById(R.id.rvCandidate)
        adapter = CandidateAdapter(
            items = candidates,
            onEdit = { candidate ->
                val intent = Intent(this, EditCandidate::class.java).apply {
                    putExtra(EditCandidate.EXTRA_CANDIDATE_ID, candidate.candidateId)
                    putExtra(EditCandidate.EXTRA_STUDENT_ID, candidate.studentId)
                    putExtra(EditCandidate.EXTRA_PERIOD_ID, candidate.periodId)
                    putExtra(EditCandidate.EXTRA_STUDENT_NAME, candidate.studentName)
                    putExtra(EditCandidate.EXTRA_VISION, candidate.vision)
                    putExtra(EditCandidate.EXTRA_MISSION, candidate.mission)
                    putExtra(EditCandidate.EXTRA_PERIOD_TITLE, candidate.periodTitle)
                    putExtra(EditCandidate.EXTRA_START_DATE, candidate.startDate)
                    putExtra(EditCandidate.EXTRA_END_DATE, candidate.endDate)
                }
                formLauncher.launch(intent)
            },
            onDelete = { candidate ->
                confirmDelete(candidate)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<ExtendedFloatingActionButton>(R.id.btnAddCandidate).setOnClickListener {
            formLauncher.launch(Intent(this, AddCandidate::class.java))
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
                val connection = (URL(ApiConfig.CANDIDATE_LIST_URL).openConnection() as HttpURLConnection).apply {
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
                    findViewById<View>(R.id.txtEmptyState).visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                }.onFailure {
                    Toast.makeText(this, "Gagal memuat kandidat: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmDelete(candidate: CandidateItem) {
        AlertDialog.Builder(this)
            .setTitle("Hapus kandidat")
            .setMessage("Hapus kandidat ${candidate.studentName}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteCandidate(candidate)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteCandidate(candidate: CandidateItem) {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.CANDIDATE_DELETE_URL).openConnection() as HttpURLConnection).apply {
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
                    append("candidateid=")
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
                    val success = response.optBoolean("success", response.optInt("status") == 1)
                    Toast.makeText(this, response.optString("message", if (success) "Berhasil" else "Gagal"), Toast.LENGTH_SHORT).show()
                    if (success) {
                        loadCandidates()
                    }
                }.onFailure {
                    Toast.makeText(this, "Gagal menghapus kandidat: ${it.message}", Toast.LENGTH_LONG).show()
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
        findViewById<ExtendedFloatingActionButton>(R.id.btnAddCandidate).isEnabled = !isLoading
    }
}
