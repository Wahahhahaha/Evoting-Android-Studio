package com.example.e_voting

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton

class AdminDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_dashboard)

        findViewById<MaterialButton>(R.id.logout).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val manageCandidateButton = findViewById<Button>(R.id.managecandidate)
        val manageUserButton = findViewById<Button>(R.id.manageuser)

        findViewById<MaterialCardView>(R.id.cardManageCandidate).setOnClickListener {
            manageCandidateButton.performClick()
        }

        findViewById<MaterialCardView>(R.id.cardManageUser).setOnClickListener {
            manageUserButton.performClick()
        }

        manageCandidateButton.setOnClickListener {
            startActivity(Intent(this, CandidateData::class.java))
        }

        manageUserButton.setOnClickListener {
            startActivity(Intent(this, StudentData::class.java))
        }
    }
}
