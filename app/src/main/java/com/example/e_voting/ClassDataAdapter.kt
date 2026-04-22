package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClassDataAdapter(
    private val items: List<ClassDataItem>,
    private val onEdit: (ClassDataItem) -> Unit,
    private val onDelete: (ClassDataItem) -> Unit
) : RecyclerView.Adapter<ClassDataAdapter.ClassViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.class_item_data, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val classNameText: TextView = itemView.findViewById(R.id.txtClassName)
        private val detailText: TextView = itemView.findViewById(R.id.txtStudentCount)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEditClass)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteClass)

        fun bind(item: ClassDataItem) {
            val batchLabel = if (item.batchName.isNotBlank()) item.batchName else "-"
            val classLabel = if (item.className.isNotBlank()) item.className else "-"
            val majorLabel = if (item.majorName.isNotBlank()) item.majorName else "-"

            classNameText.text = "$majorLabel $batchLabel $classLabel"

            detailText.text = when {
                item.studentCount >= 0 -> "${item.studentCount} students registered"
                else -> "Student data is not available"
            }

            editButton.setOnClickListener { onEdit(item) }
            deleteButton.setOnClickListener { onDelete(item) }
        }
    }
}
