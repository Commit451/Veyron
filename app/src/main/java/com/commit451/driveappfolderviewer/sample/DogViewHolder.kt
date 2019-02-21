package com.commit451.driveappfolderviewer.sample

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    companion object {

        fun inflate(parent: ViewGroup): DogViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_dog, parent, false)
            return DogViewHolder(view)
        }
    }

    private val text: TextView = view.findViewById(R.id.text)
    private val textCreated: TextView = view.findViewById(R.id.textCreated)

    fun bind(dog: Dog) {
        text.text = dog.name
        if (dog.created == null) {
            textCreated.text = "Unknown"
        } else {
            textCreated.text = "${DateFormat.getLongDateFormat(text.context).format(dog.created)} at ${DateFormat.getTimeFormat(text.context).format(dog.created)}"
        }
    }
}