package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CandidateAdapter(
    private val items: List<CandidateItem>,
    private val onEdit: (CandidateItem) -> Unit,
    private val onDelete: (CandidateItem) -> Unit
) : RecyclerView.Adapter<CandidateAdapter.CandidateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.admin_candidate_item, parent, false)
        return CandidateViewHolder(view)
    }

    override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CandidateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.txtCandidateName)
        private val periodText: TextView = itemView.findViewById(R.id.txtCandidatePeriod)
        private val visionText: TextView = itemView.findViewById(R.id.txtCandidateVision)
        private val missionText: TextView = itemView.findViewById(R.id.txtCandidateMission)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEditCandidate)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteCandidate)

        fun bind(item: CandidateItem) {
            nameText.text = item.studentName
            periodText.text = "${item.periodTitle} • ${item.startDate} s/d ${item.endDate}"
            visionText.text = item.vision
            missionText.text = item.mission
            editButton.setOnClickListener { onEdit(item) }
            deleteButton.setOnClickListener { onDelete(item) }
        }
    }
}
