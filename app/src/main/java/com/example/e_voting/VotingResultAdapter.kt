package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class VotingResultAdapter(
    private val items: List<VotingResultItem>
) : RecyclerView.Adapter<VotingResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.result_item_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val periodTitle: TextView = itemView.findViewById(R.id.txtPeriodTitle)
        private val totalAllVotes: TextView = itemView.findViewById(R.id.txtTotalAllVotes)
        private val containerCandidates: LinearLayout = itemView.findViewById(R.id.containerCandidates)
        private val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

        fun bind(item: VotingResultItem) {
            periodTitle.text = item.periodTitle
            totalAllVotes.text = "Total Suara: ${numberFormat.format(item.totalVotes)}"
            containerCandidates.removeAllViews()

            item.candidates.forEachIndexed { index, candidate ->
                val rowView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.row_candidate_result, containerCandidates, false)

                val winnerBadge: TextView = rowView.findViewById(R.id.txtWinnerBadge)
                val candidateNumber: TextView = rowView.findViewById(R.id.txtCandidateNumber)
                val pairNames: TextView = rowView.findViewById(R.id.txtPairNames)
                val totalVotes: TextView = rowView.findViewById(R.id.txtTotalVotes)
                val percentage: TextView = rowView.findViewById(R.id.txtPercentage)
                val progressVotes: ProgressBar = rowView.findViewById(R.id.progressVotes)

                winnerBadge.visibility = if (candidate.isWinner) View.VISIBLE else View.GONE
                candidateNumber.text = String.format("%02d", index + 1)
                pairNames.text = "${candidate.presidentName} & ${candidate.viceName}"
                totalVotes.text = "Total: ${numberFormat.format(candidate.voteCount)} Votes"
                percentage.text = "${candidate.percentage}%"
                progressVotes.progress = candidate.percentage.coerceIn(0, 100)

                containerCandidates.addView(rowView)
            }
        }
    }
}
