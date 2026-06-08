package com.lbs.schoolhelper.ui.meal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lbs.schoolhelper.R
import com.lbs.schoolhelper.databinding.ItemMealDayBinding
import com.google.android.material.card.MaterialCardView

class MealDayAdapter :
    ListAdapter<MealDayUiModel, MealDayAdapter.MealDayViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealDayViewHolder {
        val binding = ItemMealDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MealDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealDayViewHolder(
        private val binding: ItemMealDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MealDayUiModel) {
            val context = binding.root.context
            (binding.root as MaterialCardView).setCardBackgroundColor(
                context.getColor(
                    if (item.isToday) R.color.brand_green_soft else R.color.surface_card
                )
            )
            binding.dateText.text = item.dateLabel
            binding.detailText.text = item.detailText
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MealDayUiModel>() {
        override fun areItemsTheSame(oldItem: MealDayUiModel, newItem: MealDayUiModel): Boolean {
            return oldItem.dateLabel == newItem.dateLabel
        }

        override fun areContentsTheSame(oldItem: MealDayUiModel, newItem: MealDayUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
