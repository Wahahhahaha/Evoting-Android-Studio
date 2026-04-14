package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class StudentCandidateAdapter(
    private val items: List<CandidateItem>,
    private val onVote: (CandidateItem) -> Unit
) : RecyclerView.Adapter<StudentCandidateAdapter.StudentCandidateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentCandidateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.candidate_activity, parent, false)
        return StudentCandidateViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentCandidateViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class StudentCandidateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.first)
        private val visionText: TextView = itemView.findViewById(R.id.txtVisi)
        private val missionText: TextView = itemView.findViewById(R.id.txtMisi)
        private val voteButton: MaterialButton = itemView.findViewById(R.id.btnVote)

        fun bind(item: CandidateItem) {
            nameText.text = "${item.studentName} • ${item.periodTitle}"
            visionText.text = item.vision
            missionText.text = item.mission

            voteButton.isEnabled = !item.hasVoted
            voteButton.text = if (item.hasVoted) "SUDAH VOTE" else "VOTE"
            voteButton.setOnClickListener {
                if (!item.hasVoted) {
                    onVote(item)
                }
            }
        }
    }
}
