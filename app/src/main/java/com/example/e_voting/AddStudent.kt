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

class AddStudent : AppCompatActivity() {

    private lateinit var usernameInput: TextInputEditText
    private lateinit var nameInput: TextInputEditText
    private lateinit var classSpinner: Spinner
    private lateinit var saveButton: Button

    private val classItems = mutableListOf<ClassItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_student_activity)

        usernameInput = findViewById(R.id.usn)
        nameInput = findViewById(R.id.name)
        classSpinner = findViewById(R.id.cls)
        saveButton = findViewById(R.id.btnSave)

        saveButton.setOnClickListener {
            submitStudent()
        }

        loadClassOptions()
    }

    private fun loadClassOptions() {
        saveButton.isEnabled = false
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
                parseClassOptions(response)
            }

            runOnUiThread {
                result.onSuccess { items ->
                    classItems.clear()
                    classItems.addAll(items)
                    classSpinner.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        classItems.map { it.className }
                    )
                    saveButton.isEnabled = classItems.isNotEmpty()
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Failed to load classes: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun submitStudent() {
        val username = usernameInput.text?.toString()?.trim().orEmpty()
        val name = nameInput.text?.toString()?.trim().orEmpty()
        val selectedClass = classItems.getOrNull(classSpinner.selectedItemPosition)

        if (username.isBlank() || name.isBlank() || selectedClass == null) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false

        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.STUDENT_ADD_URL).openConnection() as HttpURLConnection).apply {
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
                    append("username=")
                    append(URLEncoder.encode(username, "UTF-8"))
                    append("&name=")
                    append(URLEncoder.encode(name, "UTF-8"))
                    append("&password=")
                    append(URLEncoder.encode(username, "UTF-8"))
                    append("&classid=")
                    append(URLEncoder.encode(selectedClass.classId.toString(), "UTF-8"))
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
                saveButton.isEnabled = true
                result.onSuccess { response ->
                    val success = response.optBoolean("success", false)
                    Toast.makeText(
                        this,
                        response.optString("message", if (success) "Success" else "Failed"),
                        Toast.LENGTH_SHORT
                    ).show()

                    if (success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Failed to save student: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseClassOptions(response: String): List<ClassItem> {
        val array = JSONArray(response)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    ClassItem(
                        classId = item.optInt("classid"),
                        className = item.optString("classname")
                    )
                )
            }
        }
    }
}
