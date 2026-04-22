package com.example.e_voting

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class PeriodData : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PeriodAdapter
    private val periods = mutableListOf<PeriodItem>()
    private val allPeriods = mutableListOf<PeriodItem>()
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var txtPageIndicator: TextView
    private var currentPage: Int = 1

    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadPeriods()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.period_data_activity)

        recyclerView = findViewById(R.id.rvPeriod)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        txtPageIndicator = findViewById(R.id.txtPageIndicator)
        adapter = PeriodAdapter(
            items = periods,
            onEdit = { period ->
                val intent = Intent(this, EditPeriod::class.java).apply {
                    putExtra(EditPeriod.EXTRA_PERIOD_ID, period.periodId)
                    putExtra(EditPeriod.EXTRA_TITLE, period.title)
                    putExtra(EditPeriod.EXTRA_START_DATE, period.startDate)
                    putExtra(EditPeriod.EXTRA_END_DATE, period.endDate)
                }
                formLauncher.launch(intent)
            },
            onDelete = { period ->
                confirmDelete(period)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        setupPaginationControls()

        findViewById<FloatingActionButton>(R.id.btnAddPeriod).setOnClickListener {
            formLauncher.launch(Intent(this, AddPeriod::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadPeriods()
    }

    private fun loadPeriods() {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.PERIOD_LIST_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val response = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use {
                    it.readText()
                }
                connection.disconnect()
                parsePeriodResponse(response)
            }

            runOnUiThread {
                setLoading(false)
                result.onSuccess { items ->
                    allPeriods.clear()
                    allPeriods.addAll(items)
                    currentPage = 1
                    renderCurrentPage()
                }.onFailure {
                    Toast.makeText(this, "Failed to load periods: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmDelete(period: PeriodItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete period")
            .setMessage("Delete period ${period.title}?")
            .setPositiveButton("Delete") { _, _ ->
                deletePeriod(period)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePeriod(period: PeriodItem) {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.PERIOD_DELETE_URL).openConnection() as HttpURLConnection).apply {
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
                    append("periodid=")
                    append(URLEncoder.encode(period.periodId.toString(), "UTF-8"))
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
                    Toast.makeText(
                        this,
                        response.optString("message", if (success) "Success" else "Failed"),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (success) {
                        loadPeriods()
                    }
                }.onFailure {
                    Toast.makeText(this, "Failed to delete period: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parsePeriodResponse(response: String): List<PeriodItem> {
        val root = extractJsonPayload(response)
        val array = when {
            root.startsWith("[") -> JSONArray(root)
            root.startsWith("{") -> JSONObject(root).optJSONArray("data") ?: JSONArray()
            else -> JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    PeriodItem(
                        periodId = firstPositiveInt(
                            item.optInt("periodid"),
                            item.optInt("period_id"),
                            item.optInt("id")
                        ),
                        title = firstNonBlank(
                            item.optString("title"),
                            item.optString("period_title"),
                            item.optString("period")
                        ),
                        startDate = firstNonBlank(
                            item.optString("startdate"),
                            item.optString("start_date")
                        ),
                        endDate = firstNonBlank(
                            item.optString("enddate"),
                            item.optString("end_date")
                        )
                    )
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findViewById<FloatingActionButton>(R.id.btnAddPeriod).isEnabled = !isLoading
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
        val endIndex = minOf(startIndex + PAGE_SIZE, allPeriods.size)
        val pageItems = if (allPeriods.isEmpty()) {
            emptyList()
        } else {
            allPeriods.subList(startIndex, endIndex)
        }

        periods.clear()
        periods.addAll(pageItems)
        adapter.notifyDataSetChanged()

        findViewById<View>(R.id.txtEmptyState).visibility =
            if (allPeriods.isEmpty()) View.VISIBLE else View.GONE
        txtPageIndicator.text = "Page $currentPage of $totalPages"
        btnPrev.isEnabled = currentPage > 1 && allPeriods.isNotEmpty()
        btnNext.isEnabled = currentPage < totalPages && allPeriods.isNotEmpty()
        btnPrev.alpha = if (btnPrev.isEnabled) 1f else 0.4f
        btnNext.alpha = if (btnNext.isEnabled) 1f else 0.4f
    }

    private fun calculateTotalPages(): Int {
        if (allPeriods.isEmpty()) return 1
        return (allPeriods.size + PAGE_SIZE - 1) / PAGE_SIZE
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() } ?: ""
    }

    private fun firstPositiveInt(vararg values: Int): Int {
        return values.firstOrNull { it > 0 } ?: 0
    }

    private fun extractJsonPayload(rawResponse: String): String {
        val response = rawResponse.removePrefix("\uFEFF").trim()
        if (response.startsWith("{") || response.startsWith("[")) {
            return response
        }

        val firstObject = response.indexOf('{')
        val firstArray = response.indexOf('[')
        val start = listOf(firstObject, firstArray).filter { it >= 0 }.minOrNull() ?: return response
        val endObject = response.lastIndexOf('}')
        val endArray = response.lastIndexOf(']')
        val end = maxOf(endObject, endArray)
        return if (end >= start) response.substring(start, end + 1).trim() else response
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
