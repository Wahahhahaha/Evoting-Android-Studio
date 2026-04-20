package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VotingResultAdapter(
    private val items: List<VotingResultItem>
) : RecyclerView.Adapter<VotingResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.result_item_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val periodTag: TextView = itemView.findViewById(R.id.txtPeriodTag)
        private val winnerBadge: TextView = itemView.findViewById(R.id.txtWinnerBadge)
        private val candidateNumber: TextView = itemView.findViewById(R.id.txtCandidateNumber)
        private val pairNames: TextView = itemView.findViewById(R.id.txtPairNames)
        private val totalVotes: TextView = itemView.findViewById(R.id.txtTotalVotes)
        private val percentage: TextView = itemView.findViewById(R.id.txtPercentage)
        private val progressVotes: ProgressBar = itemView.findViewById(R.id.progressVotes)

        fun bind(item: VotingResultItem, number: Int) {
            periodTag.text = "Period: ${item.periodTitle}"
            winnerBadge.visibility = if (item.isWinner) View.VISIBLE else View.GONE
            candidateNumber.text = String.format("%02d", number)
            pairNames.text = "${item.presidentName} & ${item.viceName}"
            totalVotes.text = "Total: ${item.voteCount} Votes"
            percentage.text = "${item.percentage}%"
            progressVotes.progress = item.percentage.coerceIn(0, 100)
        }
    }
}
