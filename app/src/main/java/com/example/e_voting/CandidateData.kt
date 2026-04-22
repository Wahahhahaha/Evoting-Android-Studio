package com.example.e_voting

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
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
    private val allCandidates = mutableListOf<CandidateItem>()
    private val filteredCandidates = mutableListOf<CandidateItem>()
    private lateinit var periodFilterSpinner: Spinner
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var txtPageIndicator: TextView
    private var selectedPeriodTitle: String = ALL_PERIODS
    private var currentPage: Int = 1

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
        periodFilterSpinner = findViewById(R.id.spinnerFilterPeriod)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        txtPageIndicator = findViewById(R.id.txtPageIndicator)
        adapter = CandidateAdapter(
            items = candidates,
            onEdit = { candidate ->
                val intent = Intent(this, EditCandidate::class.java).apply {
                    putExtra(EditCandidate.EXTRA_CANDIDATE_ID, candidate.candidateId)
                    putExtra(EditCandidate.EXTRA_PERIOD_ID, candidate.periodId)
                    putExtra(EditCandidate.EXTRA_PRESIDENT_ID, candidate.presidentStudentId)
                    putExtra(EditCandidate.EXTRA_VICE_ID, candidate.viceStudentId)
                    putExtra(EditCandidate.EXTRA_PRESIDENT_PICTURE, candidate.presidentPicture)
                    putExtra(EditCandidate.EXTRA_VICE_PICTURE, candidate.vicePicture)
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
        setupPeriodFilterSpinner()
        setupPaginationControls()

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
                    allCandidates.clear()
                    allCandidates.addAll(items)
                    updatePeriodFilterOptions(items)
                    applyPeriodFilter()
                }.onFailure {
                    Toast.makeText(this, "Failed to load candidates: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmDelete(candidate: CandidateItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Candidate")
            .setMessage("Delete pair ${candidate.presidentName} & ${candidate.viceName}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCandidate(candidate)
            }
            .setNegativeButton("Cancel", null)
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
                    Toast.makeText(this, response.optString("message", if (success) "Success" else "Failed"), Toast.LENGTH_SHORT).show()
                    if (success) {
                        loadCandidates()
                    }
                }.onFailure {
                    Toast.makeText(this, "Failed to delete candidate: ${it.message}", Toast.LENGTH_LONG).show()
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
                        periodTitle = firstNonBlank(
                            item.optString("period_title"),
                            item.optString("title"),
                            item.optString("period")
                        ),
                        startDate = firstNonBlank(
                            item.optString("startdate"),
                            item.optString("start_date")
                        ),
                        endDate = firstNonBlank(
                            item.optString("enddate"),
                            item.optString("end_date")
                        ),
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
        findViewById<ExtendedFloatingActionButton>(R.id.btnAddCandidate).isEnabled = !isLoading
    }

    private fun setupPeriodFilterSpinner() {
        periodFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position)?.toString().orEmpty()
                selectedPeriodTitle = selected.ifBlank { ALL_PERIODS }
                applyPeriodFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun updatePeriodFilterOptions(items: List<CandidateItem>) {
        val options = buildList {
            add(ALL_PERIODS)
            addAll(
                items.map { it.periodTitle.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
            )
        }

        val currentSelection = selectedPeriodTitle.takeIf { options.contains(it) } ?: ALL_PERIODS
        selectedPeriodTitle = currentSelection

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        periodFilterSpinner.adapter = spinnerAdapter
        periodFilterSpinner.setSelection(options.indexOf(currentSelection))
    }

    private fun applyPeriodFilter() {
        val filteredItems = if (selectedPeriodTitle == ALL_PERIODS) {
            allCandidates
        } else {
            allCandidates.filter { it.periodTitle.trim() == selectedPeriodTitle }
        }

        filteredCandidates.clear()
        filteredCandidates.addAll(filteredItems)
        currentPage = 1
        renderCurrentPage()
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
        val endIndex = minOf(startIndex + PAGE_SIZE, filteredCandidates.size)
        val pageItems = if (filteredCandidates.isEmpty()) {
            emptyList()
        } else {
            filteredCandidates.subList(startIndex, endIndex)
        }

        candidates.clear()
        candidates.addAll(pageItems)
        adapter.notifyDataSetChanged()
        findViewById<View>(R.id.txtEmptyState).visibility =
            if (filteredCandidates.isEmpty()) View.VISIBLE else View.GONE

        txtPageIndicator.text = "Page $currentPage of $totalPages"
        btnPrev.isEnabled = currentPage > 1 && filteredCandidates.isNotEmpty()
        btnNext.isEnabled = currentPage < totalPages && filteredCandidates.isNotEmpty()
        btnPrev.alpha = if (btnPrev.isEnabled) 1f else 0.4f
        btnNext.alpha = if (btnNext.isEnabled) 1f else 0.4f
    }

    private fun calculateTotalPages(): Int {
        if (filteredCandidates.isEmpty()) return 1
        return (filteredCandidates.size + PAGE_SIZE - 1) / PAGE_SIZE
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() } ?: ""
    }

    companion object {
        private const val ALL_PERIODS = "All Periods"
        private const val PAGE_SIZE = 10
    }
}
