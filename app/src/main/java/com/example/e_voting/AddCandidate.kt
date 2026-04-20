package com.example.e_voting

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

class AddCandidate : AppCompatActivity() {

    private lateinit var presidentSpinner: Spinner
    private lateinit var viceSpinner: Spinner
    private lateinit var periodSpinner: Spinner
    private lateinit var visionInput: TextInputEditText
    private lateinit var missionInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var choosePresidentPictureButton: MaterialButton
    private lateinit var chooseVicePictureButton: MaterialButton
    private lateinit var presidentImagePreview: ShapeableImageView
    private lateinit var viceImagePreview: ShapeableImageView

    private val studentOptions = mutableListOf<StudentOption>()
    private val periodOptions = mutableListOf<PeriodItem>()
    private var studentsReady = false
    private var periodsReady = false
    private var selectedPeriodId = 0
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
        periodSpinner = findViewById(R.id.spinner_period)
        visionInput = findViewById(R.id.editvision)
        missionInput = findViewById(R.id.editmission)
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

        saveButton.setOnClickListener {
            submitCandidate()
        }

        saveButton.isEnabled = false
        setupStudentSelectionListeners()
        setupEmptyStudentSpinners("Choose student")
        loadPeriodOptions()
    }

    private fun loadStudentOptions(periodId: Int) {
        studentsReady = false
        updateSaveButtonState()
        thread {
            val result = runCatching {
                fetchStudentOptions("${ApiConfig.STUDENT_OPTIONS_URL}?periodid=$periodId")
            }

            runOnUiThread {
                result.onSuccess { items ->
                    studentOptions.clear()
                    studentOptions.addAll(items)
                    val labels = mutableListOf("Choose student").apply {
                        addAll(studentOptions.map { it.name })
                    }
                    presidentSpinner.adapter = buildSpinnerAdapter(labels)
                    viceSpinner.adapter = buildSpinnerAdapter(labels)
                    presidentSpinner.setSelection(0, false)
                    viceSpinner.setSelection(0, false)
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
                    val labels = mutableListOf("Choose period").apply {
                        addAll(periodOptions.map { "${it.title} (${it.startDate} - ${it.endDate})" })
                    }
                    periodSpinner.adapter = buildSpinnerAdapter(labels)
                    periodsReady = periodOptions.isNotEmpty()
                    periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position <= 0) {
                                selectedPeriodId = 0
                                setupEmptyStudentSpinners("Choose student")
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
                            setupEmptyStudentSpinners("Choose student")
                            studentsReady = false
                            updateSaveButtonState()
                        }
                    }
                    periodSpinner.setSelection(0, false)
                    updateSaveButtonState()
                }.onFailure {
                    periodsReady = false
                    updateSaveButtonState()
                    Toast.makeText(this, "Failed to load periods: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchStudentOptions(url: String): List<StudentOption> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
            connectTimeout = 10000
            readTimeout = 10000
        }
        val response = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use {
            it.readText()
        }
        connection.disconnect()
        return parseStudentOptions(response)
    }

    private fun submitCandidate() {
        val selectedPresident = studentOptions.getOrNull(presidentSpinner.selectedItemPosition - 1)
        val selectedVice = studentOptions.getOrNull(viceSpinner.selectedItemPosition - 1)
        val selectedPeriod = periodOptions.firstOrNull { it.periodId == selectedPeriodId }
        val vision = visionInput.text?.toString()?.trim().orEmpty()
        val mission = missionInput.text?.toString()?.trim().orEmpty()
        val presidentPhotoUri = selectedPresidentPhotoUri
        val vicePhotoUri = selectedVicePhotoUri

        if (selectedPresident == null || selectedVice == null || selectedPeriod == null || vision.isBlank() || mission.isBlank() || presidentPhotoUri == null || vicePhotoUri == null) {
            Toast.makeText(this, "All fields are required, including period and 2 photos", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPresident.studentId == selectedVice.studentId) {
            Toast.makeText(this, "President and vice president must be different", Toast.LENGTH_SHORT).show()
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
                    writeFormField(output, boundary, "periodid", selectedPeriod.periodId.toString())
                    writeFormField(output, boundary, "vision", vision)
                    writeFormField(output, boundary, "mission", mission)
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
                choosePresidentPictureButton.isEnabled = true
                chooseVicePictureButton.isEnabled = true
                updateSaveButtonState()
                result.onSuccess { response ->
                    val success = response.optBoolean("success", false)
                    Toast.makeText(this, response.optString("message"), Toast.LENGTH_SHORT).show()
                    if (success) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }.onFailure {
                    Toast.makeText(this, "Failed to save candidate: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupEmptyStudentSpinners(message: String) {
        val placeholder = listOf(message)
        presidentSpinner.adapter = buildSpinnerAdapter(placeholder)
        viceSpinner.adapter = buildSpinnerAdapter(placeholder)
        presidentSpinner.setSelection(0, false)
        viceSpinner.setSelection(0, false)
    }

    private fun buildSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            items
        ) {
            override fun isEnabled(position: Int): Boolean = position != 0

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val colorRes = if (position == 0) android.R.color.darker_gray else android.R.color.black
                textView?.setTextColor(ContextCompat.getColor(this@AddCandidate, colorRes))
                return view
            }
        }
    }

    private fun setupStudentSelectionListeners() {
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateSaveButtonState()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateSaveButtonState()
            }
        }

        presidentSpinner.onItemSelectedListener = listener
        viceSpinner.onItemSelectedListener = listener
    }

    private fun hasValidStudentSelection(): Boolean {
        val presidentPosition = presidentSpinner.selectedItemPosition
        val vicePosition = viceSpinner.selectedItemPosition
        return presidentPosition > 0 && vicePosition > 0 && presidentPosition != vicePosition
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
        saveButton.isEnabled =
            studentsReady &&
                periodsReady &&
                selectedPeriodId > 0 &&
                hasValidStudentSelection()
    }
}
