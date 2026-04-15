package com.example.e_voting

import android.app.Activity
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.concurrent.thread

class AddCandidate : AppCompatActivity() {

    private lateinit var presidentSpinner: Spinner
    private lateinit var viceSpinner: Spinner
    private lateinit var visionInput: TextInputEditText
    private lateinit var missionInput: TextInputEditText
    private lateinit var periodTitleInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var choosePresidentPictureButton: MaterialButton
    private lateinit var chooseVicePictureButton: MaterialButton
    private lateinit var presidentImagePreview: ShapeableImageView
    private lateinit var viceImagePreview: ShapeableImageView

    private val studentOptions = mutableListOf<StudentOption>()
    private var selectedPresidentPhotoUri: Uri? = null
    private var selectedVicePhotoUri: Uri? = null

    private val presidentImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedPresidentPhotoUri = uri
                presidentImagePreview.setImageURI(uri)
            }
        }

    private val viceImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedVicePhotoUri = uri
                viceImagePreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_candidate_activity)

        presidentSpinner = findViewById(R.id.spinner_chairman)
        viceSpinner = findViewById(R.id.spinner_vice)
        visionInput = findViewById(R.id.editvision)
        missionInput = findViewById(R.id.editmission)
        periodTitleInput = findViewById(R.id.editPeriodTitle)
        startDateInput = findViewById(R.id.editdatestart)
        endDateInput = findViewById(R.id.editdateend)
        saveButton = findViewById(R.id.btnSaveCandidate)
        choosePresidentPictureButton = findViewById(R.id.btn_choose_chairman)
        chooseVicePictureButton = findViewById(R.id.btn_choose_vice)
        presidentImagePreview = findViewById(R.id.img_preview_chairman)
        viceImagePreview = findViewById(R.id.img_preview_vice)

        choosePresidentPictureButton.setOnClickListener {
            presidentImagePickerLauncher.launch("image/*")
        }

        chooseVicePictureButton.setOnClickListener {
            viceImagePickerLauncher.launch("image/*")
        }

        setupDatePicker(startDateInput)
        setupDatePicker(endDateInput)

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
                    val names = studentOptions.map { it.name }
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
                    presidentSpinner.adapter = adapter
                    viceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
                    saveButton.isEnabled = studentOptions.size >= 2
                }.onFailure {
                    Toast.makeText(this, "Gagal memuat siswa: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun submitCandidate() {
        val selectedPresident = studentOptions.getOrNull(presidentSpinner.selectedItemPosition)
        val selectedVice = studentOptions.getOrNull(viceSpinner.selectedItemPosition)
        val vision = visionInput.text?.toString()?.trim().orEmpty()
        val mission = missionInput.text?.toString()?.trim().orEmpty()
        val periodTitle = periodTitleInput.text?.toString()?.trim().orEmpty()
        val startDate = startDateInput.text?.toString()?.trim().orEmpty()
        val endDate = endDateInput.text?.toString()?.trim().orEmpty()
        val presidentPhotoUri = selectedPresidentPhotoUri
        val vicePhotoUri = selectedVicePhotoUri

        if (selectedPresident == null || selectedVice == null || vision.isBlank() || mission.isBlank() || periodTitle.isBlank() || startDate.isBlank() || endDate.isBlank() || presidentPhotoUri == null || vicePhotoUri == null) {
            Toast.makeText(this, "Semua field wajib diisi termasuk 2 foto", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPresident.studentId == selectedVice.studentId) {
            Toast.makeText(this, "Ketua dan wakil harus berbeda", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        choosePresidentPictureButton.isEnabled = false
        chooseVicePictureButton.isEnabled = false

        thread {
            val result = runCatching {
                val boundary = "Boundary-${UUID.randomUUID()}"
                val connection = (URL(ApiConfig.CANDIDATE_ADD_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                DataOutputStream(connection.outputStream).use { output ->
                    writeFormField(output, boundary, "presidentid", selectedPresident.studentId.toString())
                    writeFormField(output, boundary, "viceid", selectedVice.studentId.toString())
                    writeFormField(output, boundary, "vision", vision)
                    writeFormField(output, boundary, "mission", mission)
                    writeFormField(output, boundary, "title", periodTitle)
                    writeFormField(output, boundary, "startdate", startDate)
                    writeFormField(output, boundary, "enddate", endDate)
                    writePhotoField(output, boundary, "photo_president", presidentPhotoUri, "president")
                    writePhotoField(output, boundary, "photo_vice", vicePhotoUri, "vice")
                    output.writeBytes("--$boundary--\r\n")
                    output.flush()
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
                choosePresidentPictureButton.isEnabled = true
                chooseVicePictureButton.isEnabled = true
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

    private fun writeFormField(output: DataOutputStream, boundary: String, key: String, value: String) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$key\"\r\n\r\n")
        output.write(value.toByteArray(Charsets.UTF_8))
        output.writeBytes("\r\n")
    }

    private fun writePhotoField(output: DataOutputStream, boundary: String, key: String, uri: Uri, prefix: String) {
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "${prefix}_${System.currentTimeMillis()}.$extension"

        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$key\"; filename=\"$fileName\"\r\n")
        output.writeBytes("Content-Type: $mimeType\r\n\r\n")
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("File foto tidak bisa dibaca")
        inputStream.use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
        }
        output.writeBytes("\r\n")
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
                val formatted = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year,
                    monthOfYear + 1,
                    dayOfMonth
                )
                targetInput.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
