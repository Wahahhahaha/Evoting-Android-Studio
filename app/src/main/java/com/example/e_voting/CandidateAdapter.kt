package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
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
        private val presidentNameText: TextView = itemView.findViewById(R.id.txtPresidentName)
        private val viceNameText: TextView = itemView.findViewById(R.id.txtViceName)
        private val periodText: TextView = itemView.findViewById(R.id.txtCandidatePeriod)
        private val visionText: TextView = itemView.findViewById(R.id.txtCandidateVision)
        private val missionText: TextView = itemView.findViewById(R.id.txtCandidateMission)
        private val presidentImage: ImageView = itemView.findViewById(R.id.imgPresident)
        private val viceImage: ImageView = itemView.findViewById(R.id.imgVice)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEditCandidate)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteCandidate)

        fun bind(item: CandidateItem) {
            presidentNameText.text = item.presidentName
            viceNameText.text = item.viceName
            periodText.text = "${item.periodTitle} • ${item.startDate} s/d ${item.endDate}"
            visionText.text = item.vision
            missionText.text = item.mission
            CandidateImageLoader.loadInto(presidentImage, item.presidentPicture)
            CandidateImageLoader.loadInto(viceImage, item.vicePicture)
            editButton.setOnClickListener { onEdit(item) }
            deleteButton.setOnClickListener { onDelete(item) }
        }
    }
}
