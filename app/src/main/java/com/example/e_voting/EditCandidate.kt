package com.example.e_voting

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
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
import java.util.UUID
import kotlin.concurrent.thread

class EditCandidate : AppCompatActivity() {

    private lateinit var presidentSpinner: Spinner
    private lateinit var viceSpinner: Spinner
    private lateinit var periodSpinner: Spinner
    private lateinit var visionInput: TextInputEditText
    private lateinit var missionInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var changePresidentPhotoButton: MaterialButton
    private lateinit var changeVicePhotoButton: MaterialButton
    private lateinit var presidentImageView: ShapeableImageView
    private lateinit var viceImageView: ShapeableImageView

    private val studentOptions = mutableListOf<StudentOption>()
    private val periodOptions = mutableListOf<PeriodItem>()
    private var candidateId: Int = -1
    private var periodId: Int = -1
    private var selectedPresidentId: Int = -1
    private var selectedViceId: Int = -1
    private var studentsReady = false
    private var periodsReady = false
    private var selectedPeriodId = 0
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
        periodSpinner = findViewById(R.id.spinner_edit_period)
        visionInput = findViewById(R.id.editvision)
        missionInput = findViewById(R.id.editmission)
        saveButton = findViewById(R.id.btnUpdateCandidate)
        changePresidentPhotoButton = findViewById(R.id.btn_change_chairman_photo)
        changeVicePhotoButton = findViewById(R.id.btn_change_vice_photo)
        presidentImageView = findViewById(R.id.img_edit_chairman)
        viceImageView = findViewById(R.id.img_edit_vice)

        visionInput.setText(intent.getStringExtra(EXTRA_VISION).orEmpty())
        missionInput.setText(intent.getStringExtra(EXTRA_MISSION).orEmpty())
        CandidateImageLoader.loadInto(
            presidentImageView,
            intent.getStringExtra(EXTRA_PRESIDENT_PICTURE).orEmpty()
        )
        CandidateImageLoader.loadInto(
            viceImageView,
            intent.getStringExtra(EXTRA_VICE_PICTURE).orEmpty()
        )

        changePresidentPhotoButton.setOnClickListener {
            presidentImagePickerLauncher.launch("image/*")
        }

        changeVicePhotoButton.setOnClickListener {
            viceImagePickerLauncher.launch("image/*")
        }

        saveButton.setOnClickListener {
            updateCandidate()
        }

        saveButton.isEnabled = false
        setupEmptyStudentSpinners("Select voting period first")
        loadPeriodOptions()
    }

    private fun loadStudentOptions(periodId: Int) {
        studentsReady = false
        updateSaveButtonState()
        thread {
            val result = runCatching {
                val url = URL("${ApiConfig.STUDENT_OPTIONS_URL}?periodid=$periodId&candidateid=$candidateId")
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

                    studentsReady = studentOptions.size >= 2
                    if (studentOptions.isEmpty()) {
                        setupEmptyStudentSpinners("No available students for this period")
                    }
                    updateSaveButtonState()
                }.onFailure {
                    studentsReady = false
                    updateSaveButtonState()
                    Toast.makeText(this, "Failed to load students: ${it.message}", Toast.LENGTH_LONG).show()
                    setupEmptyStudentSpinners("Failed to load students")
                }
            }
        }
    }

    private fun loadPeriodOptions() {
        periodsReady = false
        updateSaveButtonState()
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
                parsePeriodOptions(response)
            }

            runOnUiThread {
                result.onSuccess { items ->
                    periodOptions.clear()
                    periodOptions.addAll(items)
                    val labels = mutableListOf("Select voting period").apply {
                        addAll(periodOptions.map { "${it.title} (${it.startDate} - ${it.endDate})" })
                    }
                    periodSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

                    periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position <= 0) {
                                selectedPeriodId = 0
                                setupEmptyStudentSpinners("Select voting period first")
                                studentsReady = false
                                updateSaveButtonState()
                                return
                            }

                            val selected = periodOptions[position - 1]
                            selectedPeriodId = selected.periodId
                            loadStudentOptions(selectedPeriodId)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            selectedPeriodId = 0
                            setupEmptyStudentSpinners("Select voting period first")
                            studentsReady = false
                            updateSaveButtonState()
                        }
                    }

                    val periodIndex = periodOptions.indexOfFirst { it.periodId == periodId }
                    if (periodIndex >= 0) {
                        periodSpinner.setSelection(periodIndex + 1)
                    } else {
                        periodSpinner.setSelection(0)
                    }

                    periodsReady = periodOptions.isNotEmpty()
                    updateSaveButtonState()
                }.onFailure {
                    periodsReady = false
                    updateSaveButtonState()
                    Toast.makeText(this, "Failed to load periods: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateCandidate() {
        val selectedPresident = studentOptions.getOrNull(presidentSpinner.selectedItemPosition)
        val selectedVice = studentOptions.getOrNull(viceSpinner.selectedItemPosition)
        val selectedPeriod = periodOptions.firstOrNull { it.periodId == selectedPeriodId }
        val vision = visionInput.text?.toString()?.trim().orEmpty()
        val mission = missionInput.text?.toString()?.trim().orEmpty()

        if (candidateId <= 0 || selectedPresident == null || selectedVice == null || selectedPeriod == null || vision.isBlank() || mission.isBlank()) {
            Toast.makeText(this, "Incomplete candidate data", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPresident.studentId == selectedVice.studentId) {
            Toast.makeText(this, "President and vice president must be different", Toast.LENGTH_SHORT).show()
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
                    writeFormField(output, boundary, "periodid", selectedPeriod.periodId.toString())
                    writeFormField(output, boundary, "presidentid", selectedPresident.studentId.toString())
                    writeFormField(output, boundary, "viceid", selectedVice.studentId.toString())
                    writeFormField(output, boundary, "vision", vision)
                    writeFormField(output, boundary, "mission", mission)

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
                changePresidentPhotoButton.isEnabled = true
                changeVicePhotoButton.isEnabled = true
                updateSaveButtonState()
                result.onSuccess { response ->
                    val success = response.optBoolean("success", false)
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                    if (success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }.onFailure {
                    Toast.makeText(this, "Failed to update candidate: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupEmptyStudentSpinners(message: String) {
        val placeholder = listOf(message)
        presidentSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, placeholder)
        viceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, placeholder)
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
            ?: throw IllegalArgumentException("Photo file cannot be read")
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

    private fun parseStudentOptions(response: String): List<StudentOption> {
        val root = extractJsonPayload(response)
        val array = when {
            root.startsWith("[") -> JSONArray(root)
            root.startsWith("{") -> JSONObject(root).optJSONArray("data") ?: JSONArray()
            else -> JSONArray()
        }

        return buildList {
            val seenIds = mutableSetOf<Int>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val studentId = item.optInt("studentid", item.optInt("id", 0))
                val name = item.optString("name", item.optString("student_name"))
                if (studentId <= 0 || name.isBlank() || !seenIds.add(studentId)) continue
                add(StudentOption(studentId, name))
            }
        }
    }

    private fun extractJsonPayload(rawResponse: String): String {
        val response = rawResponse.removePrefix("\uFEFF").trim()
        if (response.startsWith("{") || response.startsWith("[")) return response

        val firstObject = response.indexOf('{')
        val firstArray = response.indexOf('[')
        val start = listOf(firstObject, firstArray).filter { it >= 0 }.minOrNull() ?: return response
        val endObject = response.lastIndexOf('}')
        val endArray = response.lastIndexOf(']')
        val end = maxOf(endObject, endArray)
        return if (end >= start) response.substring(start, end + 1).trim() else response
    }

    private fun parsePeriodOptions(response: String): List<PeriodItem> {
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
                    PeriodItem(
                        periodId = item.optInt("periodid"),
                        title = item.optString("title"),
                        startDate = item.optString("startdate"),
                        endDate = item.optString("enddate")
                    )
                )
            }
        }
    }

    private fun updateSaveButtonState() {
        saveButton.isEnabled = studentsReady && periodsReady && selectedPeriodId > 0
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
