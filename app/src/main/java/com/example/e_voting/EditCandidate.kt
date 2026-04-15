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
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.concurrent.thread

class EditCandidate : AppCompatActivity() {

    private lateinit var presidentSpinner: Spinner
    private lateinit var viceSpinner: Spinner
    private lateinit var visionInput: TextInputEditText
    private lateinit var missionInput: TextInputEditText
    private lateinit var periodTitleInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var changePresidentPhotoButton: MaterialButton
    private lateinit var changeVicePhotoButton: MaterialButton
    private lateinit var presidentImageView: ShapeableImageView
    private lateinit var viceImageView: ShapeableImageView

    private val studentOptions = mutableListOf<StudentOption>()
    private var candidateId: Int = -1
    private var periodId: Int = -1
    private var selectedPresidentId: Int = -1
    private var selectedViceId: Int = -1
    private var selectedPresidentPhotoUri: Uri? = null
    private var selectedVicePhotoUri: Uri? = null

    private val presidentImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedPresidentPhotoUri = uri
                presidentImageView.setImageURI(uri)
            }
        }

    private val viceImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedVicePhotoUri = uri
                viceImageView.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_candidate_activity)

        candidateId = intent.getIntExtra(EXTRA_CANDIDATE_ID, -1)
        periodId = intent.getIntExtra(EXTRA_PERIOD_ID, -1)
        selectedPresidentId = intent.getIntExtra(EXTRA_PRESIDENT_ID, -1)
        selectedViceId = intent.getIntExtra(EXTRA_VICE_ID, -1)

        presidentSpinner = findViewById(R.id.spinner_edit_chairman)
        viceSpinner = findViewById(R.id.spinner_edit_vice)
        visionInput = findViewById(R.id.editvision)
        missionInput = findViewById(R.id.editmission)
        periodTitleInput = findViewById(R.id.editPeriodTitle)
        startDateInput = findViewById(R.id.editdatestart)
        endDateInput = findViewById(R.id.editdateend)
        saveButton = findViewById(R.id.btnUpdateCandidate)
        changePresidentPhotoButton = findViewById(R.id.btn_change_chairman_photo)
        changeVicePhotoButton = findViewById(R.id.btn_change_vice_photo)
        presidentImageView = findViewById(R.id.img_edit_chairman)
        viceImageView = findViewById(R.id.img_edit_vice)

        visionInput.setText(intent.getStringExtra(EXTRA_VISION).orEmpty())
        missionInput.setText(intent.getStringExtra(EXTRA_MISSION).orEmpty())
        periodTitleInput.setText(intent.getStringExtra(EXTRA_PERIOD_TITLE).orEmpty())
        startDateInput.setText(intent.getStringExtra(EXTRA_START_DATE).orEmpty())
        endDateInput.setText(intent.getStringExtra(EXTRA_END_DATE).orEmpty())
        CandidateImageLoader.loadInto(
            presidentImageView,
            intent.getStringExtra(EXTRA_PRESIDENT_PICTURE).orEmpty()
        )
        CandidateImageLoader.loadInto(
            viceImageView,
            intent.getStringExtra(EXTRA_VICE_PICTURE).orEmpty()
        )

        setupDatePicker(startDateInput)
        setupDatePicker(endDateInput)

        changePresidentPhotoButton.setOnClickListener {
            presidentImagePickerLauncher.launch("image/*")
        }

        changeVicePhotoButton.setOnClickListener {
            viceImagePickerLauncher.launch("image/*")
        }

        saveButton.setOnClickListener {
            updateCandidate()
        }

        loadStudentOptions()
    }

    private fun loadStudentOptions() {
        saveButton.isEnabled = false
        thread {
            val result = runCatching {
                val url = URL("${ApiConfig.STUDENT_OPTIONS_URL}?candidateid=$candidateId")
                val connection = (url.openConnection() as HttpURLConnection).apply {
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
                    presidentSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
                    viceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)

                    val presidentIndex = studentOptions.indexOfFirst { it.studentId == selectedPresidentId }
                    val viceIndex = studentOptions.indexOfFirst { it.studentId == selectedViceId }
                    if (presidentIndex >= 0) {
                        presidentSpinner.setSelection(presidentIndex)
                    }
                    if (viceIndex >= 0) {
                        viceSpinner.setSelection(viceIndex)
                    }

                    saveButton.isEnabled = studentOptions.size >= 2
                }.onFailure {
                    Toast.makeText(this, "Gagal memuat siswa: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateCandidate() {
        val selectedPresident = studentOptions.getOrNull(presidentSpinner.selectedItemPosition)
        val selectedVice = studentOptions.getOrNull(viceSpinner.selectedItemPosition)
        val vision = visionInput.text?.toString()?.trim().orEmpty()
        val mission = missionInput.text?.toString()?.trim().orEmpty()
        val periodTitle = periodTitleInput.text?.toString()?.trim().orEmpty()
        val startDate = startDateInput.text?.toString()?.trim().orEmpty()
        val endDate = endDateInput.text?.toString()?.trim().orEmpty()

        if (candidateId <= 0 || periodId <= 0 || selectedPresident == null || selectedVice == null || vision.isBlank() || mission.isBlank() || periodTitle.isBlank() || startDate.isBlank() || endDate.isBlank()) {
            Toast.makeText(this, "Data kandidat tidak lengkap", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPresident.studentId == selectedVice.studentId) {
            Toast.makeText(this, "Ketua dan wakil harus berbeda", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        changePresidentPhotoButton.isEnabled = false
        changeVicePhotoButton.isEnabled = false

        thread {
            val result = runCatching {
                val boundary = "Boundary-${UUID.randomUUID()}"
                val connection = (URL(ApiConfig.CANDIDATE_EDIT_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                DataOutputStream(connection.outputStream).use { output ->
                    writeFormField(output, boundary, "candidateid", candidateId.toString())
                    writeFormField(output, boundary, "periodid", periodId.toString())
                    writeFormField(output, boundary, "presidentid", selectedPresident.studentId.toString())
                    writeFormField(output, boundary, "viceid", selectedVice.studentId.toString())
                    writeFormField(output, boundary, "vision", vision)
                    writeFormField(output, boundary, "mission", mission)
                    writeFormField(output, boundary, "title", periodTitle)
                    writeFormField(output, boundary, "startdate", startDate)
                    writeFormField(output, boundary, "enddate", endDate)

                    selectedPresidentPhotoUri?.let {
                        writePhotoField(output, boundary, "photo_president", it, "president")
                    }
                    selectedVicePhotoUri?.let {
                        writePhotoField(output, boundary, "photo_vice", it, "vice")
                    }

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
                changePresidentPhotoButton.isEnabled = true
                changeVicePhotoButton.isEnabled = true
                result.onSuccess { response ->
                    val success = response.optBoolean("success", false)
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                    if (success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }.onFailure {
                    Toast.makeText(this, "Gagal memperbarui kandidat: ${it.message}", Toast.LENGTH_LONG).show()
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

    companion object {
        const val EXTRA_CANDIDATE_ID = "extra_candidate_id"
        const val EXTRA_PERIOD_ID = "extra_period_id"
        const val EXTRA_PRESIDENT_ID = "extra_president_id"
        const val EXTRA_VICE_ID = "extra_vice_id"
        const val EXTRA_VISION = "extra_vision"
        const val EXTRA_MISSION = "extra_mission"
        const val EXTRA_PERIOD_TITLE = "extra_period_title"
        const val EXTRA_START_DATE = "extra_start_date"
        const val EXTRA_END_DATE = "extra_end_date"
        const val EXTRA_PRESIDENT_PICTURE = "extra_president_picture"
        const val EXTRA_VICE_PICTURE = "extra_vice_picture"
    }
}
