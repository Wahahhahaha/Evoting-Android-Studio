package com.example.e_voting

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
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
import java.util.Calendar
import java.util.Locale
import kotlin.concurrent.thread

class AddPeriod : AppCompatActivity() {

    private lateinit var titleInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_period_activity)

        titleInput = findViewById(R.id.editPeriodTitle)
        startDateInput = findViewById(R.id.editStartDate)
        endDateInput = findViewById(R.id.editEndDate)
        saveButton = findViewById(R.id.btnSavePeriod)

        setupDatePicker(startDateInput)
        setupDatePicker(endDateInput)

        saveButton.setOnClickListener {
            submitPeriod()
        }
    }

    private fun submitPeriod() {
        val title = titleInput.text?.toString()?.trim().orEmpty()
        val startDate = startDateInput.text?.toString()?.trim().orEmpty()
        val endDate = endDateInput.text?.toString()?.trim().orEmpty()

        if (title.isBlank() || startDate.isBlank() || endDate.isBlank()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (startDate > endDate) {
            Toast.makeText(this, "Start date cannot be later than end date", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.PERIOD_ADD_URL).openConnection() as HttpURLConnection).apply {
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
                    append("title=")
                    append(URLEncoder.encode(title, "UTF-8"))
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
                    Toast.makeText(this, "Failed to save period: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupDatePicker(input: TextInputEditText) {
        input.setShowSoftInputOnFocus(false)
        input.setOnClickListener {
            showDatePicker(input)
        }
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker(input)
            }
        }
    }

    private fun showDatePicker(targetInput: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val existing = targetInput.text?.toString()?.trim().orEmpty()
        val parts = existing.split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val day = parts[2].toIntOrNull()
            if (year != null && month != null && day != null) {
                calendar.set(year, month - 1, day)
            }
        }

        DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
                targetInput.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
