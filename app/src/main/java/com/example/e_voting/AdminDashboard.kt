package com.example.e_voting

import android.content.Intent
import android.os.Bundle
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

        findViewById<MaterialCardView>(R.id.cardManageCandidate).setOnClickListener {
            startActivity(Intent(this, CandidateData::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardManageUser).setOnClickListener {
            startActivity(Intent(this, StudentData::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardManagePeriod).setOnClickListener {
            startActivity(Intent(this, PeriodData::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardVotingResult).setOnClickListener {
            startActivity(Intent(this, ResultData::class.java))
        }
    }
}
