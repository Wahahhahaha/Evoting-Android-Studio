package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PeriodAdapter(
    private val items: List<PeriodItem>,
    private val onEdit: (PeriodItem) -> Unit,
    private val onDelete: (PeriodItem) -> Unit
) : RecyclerView.Adapter<PeriodAdapter.PeriodViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.period_item_data, parent, false)
        return PeriodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeriodViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PeriodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.txtPeriodTitle)
        private val dateRangeText: TextView = itemView.findViewById(R.id.txtDateRange)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEditPeriod)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeletePeriod)

        fun bind(item: PeriodItem) {
            titleText.text = if (item.title.isBlank()) "Untitled Period" else item.title
            dateRangeText.text = when {
                item.startDate.isNotBlank() && item.endDate.isNotBlank() -> "${item.startDate} to ${item.endDate}"
                item.startDate.isNotBlank() -> "Starts ${item.startDate}"
                item.endDate.isNotBlank() -> "Ends ${item.endDate}"
                else -> "Date not set"
            }
            editButton.setOnClickListener { onEdit(item) }
            deleteButton.setOnClickListener { onDelete(item) }
        }
    }
}
