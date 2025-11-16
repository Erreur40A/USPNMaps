package com.uspn.maps.ui.theme

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.uspn.maps.Salle

class SalleAdapter(
    private val context: Context,
    private var salles: List<Salle>
) : BaseAdapter(), Filterable {

    private var filteredSalles: List<Salle> = salles

    override fun getCount(): Int = filteredSalles.size

    override fun getItem(position: Int): Salle? {
        return if (position in filteredSalles.indices) filteredSalles[position] else null
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textView = convertView as? TextView ?: TextView(context)
        val salle = getItem(position)
        if (salle != null) {
            //TODO A AMELIORER
            textView.text = "${salle.nom} (${salle.batiment.code})"
            textView.setPadding(16, 16, 16, 16)
        }
        return textView
    }

    fun updateData(newSalles: List<Salle>) {
        filteredSalles = newSalles
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase()?.trim() ?: ""
                val results = FilterResults()

                val filteredList = if (query.isEmpty()) {
                    salles
                } else {
                    salles.filter { salle ->
                        salle.nom.lowercase().contains(query) ||
                                // TODO A REVOIR
                               // salle.batiment.lowercase().contains(query) ||
                                salle.description?.lowercase()?.contains(query) == true
                    }.sortedWith(compareBy(
                        { !it.nom.lowercase().startsWith(query) },
                        { !it.nom.lowercase().contains(query) },
                        // TODO A AMELIORER
                        //{ !it.batiment_id.lowercase().contains(query) },
                        { !(it.description?.lowercase()?.contains(query) == true) },
                        { it.nom }))
                }

                results.values = filteredList
                results.count = filteredList.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredSalles = results?.values as? List<Salle> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
