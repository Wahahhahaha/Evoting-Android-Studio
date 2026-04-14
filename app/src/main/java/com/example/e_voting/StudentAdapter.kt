package com.example.e_voting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentAdapter(
    private val items: List<StudentItem>,
    private val onEdit: (StudentItem) -> Unit,
    private val onDelete: (StudentItem) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.student_item_data, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.txtName)
        private val classText: TextView = itemView.findViewById(R.id.txtClass)
        private val usernameText: TextView = itemView.findViewById(R.id.txtUsername)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(item: StudentItem) {
            nameText.text = item.name
            classText.text = item.className
            usernameText.text = "Username: ${item.username}"

            editButton.setOnClickListener { onEdit(item) }
            deleteButton.setOnClickListener { onDelete(item) }
        }
    }
}
