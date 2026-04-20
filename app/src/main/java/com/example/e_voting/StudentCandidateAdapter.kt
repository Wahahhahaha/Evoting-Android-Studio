package com.example.e_voting

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
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
        private val card: CardView = itemView as CardView
        private val presidentNameText: TextView = itemView.findViewById(R.id.txtPresidentName)
        private val viceNameText: TextView = itemView.findViewById(R.id.txtViceName)
        private val periodText: TextView = itemView.findViewById(R.id.txtPeriodLabel)
        private val visionText: TextView = itemView.findViewById(R.id.txtVisi)
        private val missionText: TextView = itemView.findViewById(R.id.txtMisi)
        private val candidateImagePresident: ImageView = itemView.findViewById(R.id.imgPresident)
        private val candidateImageVice: ImageView = itemView.findViewById(R.id.imgVice)
        private val voteButton: MaterialButton = itemView.findViewById(R.id.btnVote)
        private val resultBadge: TextView = itemView.findViewById(R.id.txtResultBadge)
        private val voteCountText: TextView = itemView.findViewById(R.id.txtVoteCount)

        fun bind(item: CandidateItem) {
            presidentNameText.text = item.presidentName
            viceNameText.text = item.viceName
            periodText.text = item.periodTitle
            visionText.text = item.vision
            missionText.text = item.mission
            CandidateImageLoader.loadInto(candidateImagePresident, item.presidentPicture)
            CandidateImageLoader.loadInto(candidateImageVice, item.vicePicture)

            if (item.displayMode == "result") {
                voteButton.visibility = View.GONE
                voteCountText.visibility = View.VISIBLE
                voteCountText.text = "Total vote: ${item.voteCount}"

                if (item.isWinner) {
                    resultBadge.visibility = View.VISIBLE
                    resultBadge.text = "👑 WINNER"
                    card.cardElevation = 10f
                    card.radius = 28f
                    card.layoutParams = card.layoutParams.apply {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    itemView.alpha = 1f
                } else {
                    resultBadge.visibility = View.GONE
                    card.cardElevation = 2f
                    card.radius = 20f
                    card.layoutParams = card.layoutParams.apply {
                        width = (itemView.resources.displayMetrics.widthPixels * 0.9f).toInt()
                    }
                    itemView.alpha = 0.92f
                }
            } else {
                voteCountText.visibility = View.GONE
                resultBadge.visibility = View.GONE
                voteButton.visibility = View.VISIBLE
                card.cardElevation = 6f
                card.radius = 28f
                card.layoutParams = card.layoutParams.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
                itemView.alpha = 1f

                voteButton.isEnabled = !item.hasVoted
                voteButton.text = when {
                    item.isVotedCandidate -> "Your Choice"
                    item.hasVoted -> "Voted"
                    else -> "Vote"
                }

                voteButton.setBackgroundColor(
                    if (item.isVotedCandidate) Color.parseColor("#43A047")
                    else Color.parseColor("#FBC02D")
                )

                voteButton.setOnClickListener {
                    if (!item.hasVoted) {
                        onVote(item)
                    }
                }
            }
        }
    }
}
