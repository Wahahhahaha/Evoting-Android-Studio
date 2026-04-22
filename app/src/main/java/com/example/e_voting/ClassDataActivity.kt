package com.example.e_voting

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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

class ClassDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClassDataAdapter
    private val classes = mutableListOf<ClassDataItem>()
    private val allClasses = mutableListOf<ClassDataItem>()
    private val filteredClasses = mutableListOf<ClassDataItem>()
    private lateinit var searchInput: EditText
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var txtPageIndicator: TextView
    private var searchQuery: String = ""
    private var currentPage: Int = 1

    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadClasses()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.class_activity)

        recyclerView = findViewById(R.id.rvClass)
        searchInput = findViewById(R.id.etSearchClass)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        txtPageIndicator = findViewById(R.id.txtPageIndicator)

        adapter = ClassDataAdapter(
            items = classes,
            onEdit = { classItem ->
                val intent = Intent(this, EditClassActivity::class.java).apply {
                    putExtra(EditClassActivity.EXTRA_CLASS_ID, classItem.classId)
                    putExtra(EditClassActivity.EXTRA_CLASS_NAME, classItem.className)
                    putExtra(EditClassActivity.EXTRA_BATCH_NAME, classItem.batchName)
                    putExtra(EditClassActivity.EXTRA_MAJOR_NAME, classItem.majorName)
                }
                formLauncher.launch(intent)
            },
            onDelete = { classItem ->
                confirmDelete(classItem)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupSearch()
        setupPaginationControls()

        findViewById<ExtendedFloatingActionButton>(R.id.btnAddClass).setOnClickListener {
            formLauncher.launch(Intent(this, AddClassActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadClasses()
    }

    private fun loadClasses() {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.CLASS_LIST_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val response = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use {
                    it.readText()
                }
                connection.disconnect()
                parseClassResponse(response)
            }

            runOnUiThread {
                setLoading(false)
                result.onSuccess { items ->
                    allClasses.clear()
                    allClasses.addAll(items)
                    applySearchAndPagination()
                }.onFailure {
                    Toast.makeText(this, "Failed to load classes: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmDelete(classItem: ClassDataItem) {
        val classLabel = listOf(
            classItem.majorName.takeIf { it.isNotBlank() },
            classItem.batchName.takeIf { it.isNotBlank() },
            classItem.className.takeIf { it.isNotBlank() }
        ).filterNotNull().joinToString(" ").ifBlank { "this class" }

        AlertDialog.Builder(this)
            .setTitle("Delete class")
            .setMessage("Are you sure you want to delete $classLabel?")
            .setPositiveButton("Delete") { _, _ ->
                deleteClass(classItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteClass(classItem: ClassDataItem) {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.CLASS_DELETE_URL).openConnection() as HttpURLConnection).apply {
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
                    append("classid=")
                    append(URLEncoder.encode(classItem.classId.toString(), "UTF-8"))
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
                        loadClasses()
                    }
                }.onFailure {
                    Toast.makeText(this, "Failed to delete class: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseClassResponse(response: String): List<ClassDataItem> {
        val root = extractJsonPayload(response)
        val array = when {
            root.startsWith("[") -> JSONArray(root)
            root.startsWith("{") -> JSONObject(root).optJSONArray("data") ?: JSONArray()
            else -> JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val className = item.optString("classname").trim()
                if (className.isBlank()) continue

                val majorName = firstNonBlank(
                    item.optString("jurusan"),
                    item.optString("jurusanname"),
                    item.optString("nama_jurusan"),
                    item.optString("major"),
                    item.optString("majorname")
                )

                val batchName = firstNonBlank(
                    item.optString("angkatan"),
                    item.optString("angkatanname"),
                    item.optString("tahun_angkatan"),
                    item.optString("batch"),
                    item.optString("batchyear")
                )

                val studentCount = when {
                    item.has("studentcount") -> item.optInt("studentcount", -1)
                    item.has("student_count") -> item.optInt("student_count", -1)
                    item.has("totalstudent") -> item.optInt("totalstudent", -1)
                    item.has("total_students") -> item.optInt("total_students", -1)
                    else -> -1
                }

                add(
                    ClassDataItem(
                        classId = item.optInt("classid"),
                        className = className,
                        majorName = majorName,
                        batchName = batchName,
                        studentCount = studentCount
                    )
                )
            }
        }
    }

    private fun firstNonBlank(vararg values: String): String {
        for (value in values) {
            val normalized = value.trim()
            if (normalized.isNotBlank() && !normalized.equals("null", ignoreCase = true)) {
                return normalized
            }
        }
        return ""
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

    private fun setLoading(isLoading: Boolean) {
        findViewById<ExtendedFloatingActionButton>(R.id.btnAddClass).isEnabled = !isLoading
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty().trim()
                currentPage = 1
                applySearchAndPagination()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
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

    private fun applySearchAndPagination() {
        val query = searchQuery.lowercase()
        val matched = if (query.isBlank()) {
            allClasses
        } else {
            allClasses.filter { item ->
                item.className.lowercase().contains(query) ||
                    item.majorName.lowercase().contains(query) ||
                    item.batchName.lowercase().contains(query) ||
                    item.classId.toString().contains(query)
            }
        }

        filteredClasses.clear()
        filteredClasses.addAll(matched)
        currentPage = 1
        renderCurrentPage()
    }

    private fun renderCurrentPage() {
        val totalPages = calculateTotalPages()
        if (currentPage > totalPages) {
            currentPage = totalPages
        }

        val startIndex = (currentPage - 1) * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, filteredClasses.size)
        val pageItems = if (filteredClasses.isEmpty()) {
            emptyList()
        } else {
            filteredClasses.subList(startIndex, endIndex)
        }

        classes.clear()
        classes.addAll(pageItems)
        adapter.notifyDataSetChanged()

        findViewById<View>(R.id.txtEmptyState).visibility =
            if (filteredClasses.isEmpty()) View.VISIBLE else View.GONE
        txtPageIndicator.text = "Page $currentPage of $totalPages"
        btnPrev.isEnabled = currentPage > 1 && filteredClasses.isNotEmpty()
        btnNext.isEnabled = currentPage < totalPages && filteredClasses.isNotEmpty()
        btnPrev.alpha = if (btnPrev.isEnabled) 1f else 0.4f
        btnNext.alpha = if (btnNext.isEnabled) 1f else 0.4f
    }

    private fun calculateTotalPages(): Int {
        if (filteredClasses.isEmpty()) return 1
        return (filteredClasses.size + PAGE_SIZE - 1) / PAGE_SIZE
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
