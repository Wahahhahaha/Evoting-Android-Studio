package com.example.e_voting

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        usernameInput = findViewById(R.id.usn)
        passwordInput = findViewById(R.id.pw)
        loginButton = findViewById(R.id.btnLogin)

        loginButton.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val username = usernameInput.text?.toString()?.trim().orEmpty()
        val password = passwordInput.text?.toString().orEmpty()

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Username dan password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        loginButton.isEnabled = false

        thread {
            val result = runCatching {
                val connection = (URL(ApiConfig.LOGIN_URL).openConnection() as HttpURLConnection).apply {
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

                val postData = buildString {
                    append("username=")
                    append(URLEncoder.encode(username, "UTF-8"))
                    append("&password=")
                    append(URLEncoder.encode(password, "UTF-8"))
                }

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseText = BufferedReader(
                    if (connection.responseCode in 200..299) {
                        connection.inputStream.reader(Charsets.UTF_8)
                    } else {
                        connection.errorStream?.reader(Charsets.UTF_8)
                            ?: connection.inputStream.reader(Charsets.UTF_8)
                    }
                ).use { reader ->
                    reader.readText()
                }

                connection.disconnect()
                JSONObject(responseText)
            }

            runOnUiThread {
                loginButton.isEnabled = true

                result.onSuccess { response ->
                    val success = response.optBoolean("success", false)
                    val message = response.optString("message", "Login gagal")

                    if (!success) {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        return@onSuccess
                    }

                    when (response.optInt("level", -1)) {
                        1 -> openDashboard(AdminDashboard::class.java, message, response)
                        2 -> openDashboard(StudentDashboard::class.java, message, response)
                        else -> {
                            Toast.makeText(
                                this,
                                "Level user tidak dikenali",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Tidak bisa terhubung ke server: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun openDashboard(target: Class<*>, message: String, response: JSONObject) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        val intent = Intent(this, target).apply {
            putExtra("userid", response.optInt("userid", -1))
            putExtra("studentid", response.optInt("studentid", -1))
            putExtra("name", response.optString("name"))
        }
        startActivity(intent)
        finish()
    }
}
