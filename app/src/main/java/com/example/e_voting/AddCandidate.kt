package com.example.e_voting

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class AddCandidate : AppCompatActivity() {

    private lateinit var studentSpinner: Spinner
    private lateinit var visionInput: TextInputEditText
    private lateinit var missionInput: TextInputEditText
    private lateinit var periodTitleInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var saveButton: Button

    private val studentOptions = mutableListOf<StudentOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_candidate_activity)

        studentSpinner = findViewById(R.id.spinner_student)
        visionInput = findViewById(R.id.editvision)
        missionInput = findViewById(R.id.editmission)
        periodTitleInput = findViewById(R.id.editPeriodTitle)
        startDateInput = findViewById(R.id.editdatestart)
        endDateInput = findViewById(R.id.editdateend)
        saveButton = findViewById(R.id.button)

        saveButton.setOnClickListener {
            submitCandidate()
        }

        loadStudentOptions()
    }

    private fun loadStudentOptions() {
        saveButton.isEnabled = false
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.STUDENT_OPTIONS_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val response = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use {
                    it.readText()
                }
                connection.disconnect()
                parseStudentOptions(response)
            }

            runOnUiThread {
                result.onSuccess { items ->
                    studentOptions.clear()
                    studentOptions.addAll(items)
                    studentSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, studentOptions.map { it.name })
                    saveButton.isEnabled = studentOptions.isNotEmpty()
                }.onFailure {
                    Toast.makeText(this, "Gagal memuat siswa: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun submitCandidate() {
        val selectedStudent = studentOptions.getOrNull(studentSpinner.selectedItemPosition)
        val vision = visionInput.text?.toString()?.trim().orEmpty()
        val mission = missionInput.text?.toString()?.trim().orEmpty()
        val periodTitle = periodTitleInput.text?.toString()?.trim().orEmpty()
        val startDate = startDateInput.text?.toString()?.trim().orEmpty()
        val endDate = endDateInput.text?.toString()?.trim().orEmpty()

        if (selectedStudent == null || vision.isBlank() || mission.isBlank() || periodTitle.isBlank() || startDate.isBlank() || endDate.isBlank()) {
            Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.CANDIDATE_ADD_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                }

                val payload = buildString {
                    append("studentid=")
                    append(URLEncoder.encode(selectedStudent.studentId.toString(), "UTF-8"))
                    append("&vision=")
                    append(URLEncoder.encode(vision, "UTF-8"))
                    append("&mission=")
                    append(URLEncoder.encode(mission, "UTF-8"))
                    append("&title=")
                    append(URLEncoder.encode(periodTitle, "UTF-8"))
                    append("&startdate=")
                    append(URLEncoder.encode(startDate, "UTF-8"))
                    append("&enddate=")
                    append(URLEncoder.encode(endDate, "UTF-8"))
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                    it.write(payload)
                    it.flush()
                }

                val response = BufferedReader(
                    if (connection.responseCode in 200..299) connection.inputStream.reader(Charsets.UTF_8)
                    else connection.errorStream?.reader(Charsets.UTF_8) ?: connection.inputStream.reader(Charsets.UTF_8)
                ).use { it.readText() }

                connection.disconnect()
                JSONObject(response)
            }

            runOnUiThread {
                saveButton.isEnabled = true
                result.onSuccess { response ->
                    val success = response.optBoolean("success", false)
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                    if (success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }.onFailure {
                    Toast.makeText(this, "Gagal menyimpan kandidat: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseStudentOptions(response: String): List<StudentOption> {
        val array = JSONArray(response)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(StudentOption(item.optInt("studentid"), item.optString("name")))
            }
        }
    }
}
