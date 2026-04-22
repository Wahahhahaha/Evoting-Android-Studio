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

class StudentData : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAdapter
    private val students = mutableListOf<StudentItem>()
    private val allStudents = mutableListOf<StudentItem>()
    private val filteredStudents = mutableListOf<StudentItem>()
    private lateinit var searchInput: EditText
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var txtPageIndicator: TextView
    private var searchQuery: String = ""
    private var currentPage: Int = 1

    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadStudents()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_data_activity)

        recyclerView = findViewById(R.id.rvStudent)
        searchInput = findViewById(R.id.etSearchStudent)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        txtPageIndicator = findViewById(R.id.txtPageIndicator)
        adapter = StudentAdapter(
            items = students,
            onEdit = { student ->
                val intent = Intent(this, EditStudent::class.java).apply {
                    putExtra(EditStudent.EXTRA_STUDENT_ID, student.studentId)
                    putExtra(EditStudent.EXTRA_USER_ID, student.userId)
                    putExtra(EditStudent.EXTRA_NAME, student.name)
                    putExtra(EditStudent.EXTRA_USERNAME, student.username)
                    putExtra(EditStudent.EXTRA_CLASS_ID, student.classId)
                }
                formLauncher.launch(intent)
            },
            onDelete = { student ->
                confirmDelete(student)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        setupSearch()
        setupPaginationControls()

        findViewById<ExtendedFloatingActionButton>(R.id.btnAddStudent).setOnClickListener {
            formLauncher.launch(Intent(this, AddStudent::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadStudents()
    }

    private fun loadStudents() {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.STUDENT_LIST_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val response = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use {
                    it.readText()
                }
                connection.disconnect()

                parseStudentResponse(response)
            }

            runOnUiThread {
                setLoading(false)
                result.onSuccess { items ->
                    allStudents.clear()
                    allStudents.addAll(items)
                    applySearchAndPagination()
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Failed to load students: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun confirmDelete(student: StudentItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Delete ${student.name} from student list?")
            .setPositiveButton("Delete") { _, _ ->
                deleteStudent(student)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStudent(student: StudentItem) {
        setLoading(true)
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.STUDENT_DELETE_URL).openConnection() as HttpURLConnection).apply {
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
                    append(URLEncoder.encode(student.studentId.toString(), "UTF-8"))
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
                        loadStudents()
                    }
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Failed to delete student: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseStudentResponse(response: String): List<StudentItem> {
        val root = response.trim()
        val array = when {
            root.startsWith("[") -> JSONArray(root)
            root.startsWith("{") -> JSONObject(root).optJSONArray("data") ?: JSONArray()
            else -> JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    StudentItem(
                        studentId = item.optInt("studentid"),
                        userId = item.optInt("userid"),
                        name = item.optString("name"),
                        username = item.optString("username"),
                        classId = item.optInt("classid"),
                        className = item.optString("classname")
                    )
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findViewById<ExtendedFloatingActionButton>(R.id.btnAddStudent).isEnabled = !isLoading
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
            allStudents
        } else {
            allStudents.filter { item ->
                item.name.lowercase().contains(query) ||
                    item.username.lowercase().contains(query) ||
                    item.studentId.toString().contains(query) ||
                    item.className.lowercase().contains(query)
            }
        }

        filteredStudents.clear()
        filteredStudents.addAll(matched)
        currentPage = 1
        renderCurrentPage()
    }

    private fun renderCurrentPage() {
        val totalPages = calculateTotalPages()
        if (currentPage > totalPages) {
            currentPage = totalPages
        }

        val startIndex = (currentPage - 1) * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, filteredStudents.size)
        val pageItems = if (filteredStudents.isEmpty()) {
            emptyList()
        } else {
            filteredStudents.subList(startIndex, endIndex)
        }

        students.clear()
        students.addAll(pageItems)
        adapter.notifyDataSetChanged()

        findViewById<View>(R.id.txtEmptyState).visibility =
            if (filteredStudents.isEmpty()) View.VISIBLE else View.GONE
        txtPageIndicator.text = "Page $currentPage of $totalPages"
        btnPrev.isEnabled = currentPage > 1 && filteredStudents.isNotEmpty()
        btnNext.isEnabled = currentPage < totalPages && filteredStudents.isNotEmpty()
        btnPrev.alpha = if (btnPrev.isEnabled) 1f else 0.4f
        btnNext.alpha = if (btnNext.isEnabled) 1f else 0.4f
    }

    private fun calculateTotalPages(): Int {
        if (filteredStudents.isEmpty()) return 1
        return (filteredStudents.size + PAGE_SIZE - 1) / PAGE_SIZE
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
