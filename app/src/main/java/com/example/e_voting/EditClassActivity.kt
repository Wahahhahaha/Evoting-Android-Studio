package com.example.e_voting

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class EditClassActivity : AppCompatActivity() {

    private lateinit var classNameInput: TextInputEditText
    private lateinit var gradeSpinner: Spinner
    private lateinit var majorSpinner: Spinner
    private lateinit var saveButton: MaterialButton

    private var classId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_class_activity)

        classId = intent.getIntExtra(EXTRA_CLASS_ID, -1)

        classNameInput = findViewById(R.id.etClassName)
        gradeSpinner = findViewById(R.id.spinnerGrade)
        majorSpinner = findViewById(R.id.spinnerMajor)
        saveButton = findViewById(R.id.btnSaveClass)

        classNameInput.setText(intent.getStringExtra(EXTRA_CLASS_NAME).orEmpty())

        setupSpinner(gradeSpinner, BATCH_OPTIONS)
        setupSpinner(majorSpinner, MAJOR_OPTIONS)

        selectSpinnerValue(gradeSpinner, intent.getStringExtra(EXTRA_BATCH_NAME).orEmpty())
        selectSpinnerValue(majorSpinner, intent.getStringExtra(EXTRA_MAJOR_NAME).orEmpty())

        saveButton.setOnClickListener {
            updateClass()
        }
    }

    private fun setupSpinner(spinner: Spinner, values: List<String>) {
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            values
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun selectSpinnerValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter ?: return
        for (index in 0 until adapter.count) {
            val item = adapter.getItem(index)?.toString().orEmpty()
            if (item.equals(value, ignoreCase = true)) {
                spinner.setSelection(index)
                break
            }
        }
    }

    private fun updateClass() {
        val className = classNameInput.text?.toString()?.trim().orEmpty()
        val batchName = gradeSpinner.selectedItem?.toString()?.trim().orEmpty()
        val majorName = majorSpinner.selectedItem?.toString()?.trim().orEmpty()

        if (classId <= 0 || className.isBlank() || batchName.isBlank() || majorName.isBlank()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.CLASS_EDIT_URL).openConnection() as HttpURLConnection).apply {
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
                    append(URLEncoder.encode(classId.toString(), "UTF-8"))
                    append("&classname=")
                    append(URLEncoder.encode(className, "UTF-8"))
                    append("&angkatan=")
                    append(URLEncoder.encode(batchName, "UTF-8"))
                    append("&jurusan=")
                    append(URLEncoder.encode(majorName, "UTF-8"))
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
                    Toast.makeText(this, "Failed to update class: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_CLASS_NAME = "extra_class_name"
        const val EXTRA_BATCH_NAME = "extra_batch_name"
        const val EXTRA_MAJOR_NAME = "extra_major_name"

        private val BATCH_OPTIONS = listOf("X", "XI", "XII")
        private val MAJOR_OPTIONS = listOf("AKL", "BDP", "RPL")
    }
}
