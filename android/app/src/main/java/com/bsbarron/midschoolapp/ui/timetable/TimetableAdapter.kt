package com.bsbarron.midschoolapp.ui.timetable

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bsbarron.midschoolapp.R
import com.bsbarron.midschoolapp.data.model.TimetableItem
import com.bsbarron.midschoolapp.databinding.ItemTimetableEntryBinding

class TimetableAdapter :
    ListAdapter<TimetableItem, TimetableAdapter.TimetableViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val binding = ItemTimetableEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimetableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TimetableViewHolder(
        private val binding: ItemTimetableEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TimetableItem) {
            binding.periodText.text = binding.root.context.getString(
                R.string.timetable_period_format,
                item.period
            )
            binding.subjectText.text = item.subject.ifBlank {
                binding.root.context.getString(R.string.timetable_no_subject)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TimetableItem>() {
        override fun areItemsTheSame(oldItem: TimetableItem, newItem: TimetableItem): Boolean {
            return oldItem.date == newItem.date &&
                oldItem.period == newItem.period &&
                oldItem.classroom == newItem.classroom
        }

        override fun areContentsTheSame(oldItem: TimetableItem, newItem: TimetableItem): Boolean {
            return oldItem == newItem
        }
    }
}
