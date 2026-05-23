package com.fuellogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter : ListAdapter<FuelEntry, HistoryAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FuelEntry>() {
            override fun areItemsTheSame(a: FuelEntry, b: FuelEntry) =
                a.date == b.date && a.time == b.time
            override fun areContentsTheSame(a: FuelEntry, b: FuelEntry) = a == b
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStation: TextView = view.findViewById(R.id.tvStation)
        val tvGallons: TextView = view.findViewById(R.id.tvGallons)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvOdometer: TextView = view.findViewById(R.id.tvOdometer)
        val tvPpg: TextView = view.findViewById(R.id.tvPpg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.tvDate.text = entry.date.ifEmpty { "Unknown date" }
        holder.tvStation.text = buildString {
            if (entry.station.isNotEmpty()) append(entry.station)
            if (entry.city.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(entry.city)
            }
        }.ifEmpty { "Unknown station" }
        holder.tvGallons.text = if (entry.gallons > 0) "%.3f gal".format(entry.gallons) else "— gal"
        holder.tvTotal.text = if (entry.totalAmount > 0) "$%.2f".format(entry.totalAmount) else "—"
        holder.tvPpg.text = if (entry.pricePerGallon > 0) "$%.3f/gal".format(entry.pricePerGallon) else "—/gal"
        holder.tvOdometer.text = if (entry.odometer > 0) "%,d mi".format(entry.odometer) else "— mi"
    }
}
