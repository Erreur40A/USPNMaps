package com.uspn.maps.ui.theme

import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.uspn.maps.DatabaseHelper
import com.uspn.maps.Salle

class SearchManager(
    private val context: Context,
    private val dbHelper: DatabaseHelper
) {
    private var allSalles: List<Salle> = emptyList()
    private var selectedSalle: Salle? = null

    init {
        allSalles = dbHelper.getAllData()
    }

    fun createSearchLayout(): LinearLayout {
        val searchLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            elevation = 8f
        }

        ViewCompat.setOnApplyWindowInsetsListener(searchLayout) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        val backgroundSearchBar = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 64f
            setColor("#FFFFFF".toColorInt())
        }

        // Création de l'adapter
        val salleAdapter = SalleAdapter(context, allSalles)

        val searchBar = AutoCompleteTextView(context).apply {
            hint = "Rechercher une salle..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            threshold = 1
            setPadding(32, paddingTop, paddingLeft, paddingBottom)
            background = backgroundSearchBar

            setAdapter(salleAdapter)

            // ✅ Quand l'utilisateur sélectionne une salle
            setOnItemClickListener { parent, view, position, id ->
                val salle = salleAdapter.getItem(position)
                if (salle != null) {
                    selectedSalle = salle

                    // Afficher la salle dans la barre de recherche
                    setText("${salle.nomSalle} (${salle.batiment})", false)
                    clearFocus()

                    handleSalleSelection(salle)
                } else {
                    Log.e("SearchManager", "Salle sélectionnée null à la position $position")
                }
            }
        }

        val searchButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val query = searchBar.text.toString()
                if (query.isNotBlank()) {
                    val results = filterSalles(query)
                    showSearchResults(results)
                } else {
                    Toast.makeText(context, "Veuillez entrer un texte", Toast.LENGTH_SHORT).show()
                }
            }
        }

        searchLayout.addView(searchBar)
        searchLayout.addView(searchButton)

        val backgroundSearchLayout = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor("#293358".toColorInt())
        }
        searchLayout.background = backgroundSearchLayout

        return searchLayout
    }

    // Méthode de filtrage
    fun filterSalles(query: String): List<Salle> {
        val queryLower = query.lowercase()

        return allSalles.filter { salle ->
            salle.nomSalle.lowercase().contains(queryLower) ||
                    salle.batiment.lowercase().contains(queryLower) ||
                    salle.description?.lowercase()?.contains(queryLower) == true
        }.sortedWith(compareBy(
            { !it.nomSalle.lowercase().startsWith(queryLower) },
            { !it.nomSalle.lowercase().contains(queryLower) },
            { !it.batiment.lowercase().contains(queryLower) },
            { !(it.description?.lowercase()?.contains(queryLower) == true) },
            { it.nomSalle }
        ))
    }

    // Quand une salle est sélectionnée
    private fun handleSalleSelection(salle: Salle) {
        Log.d("SearchManager", "Salle sélectionnée: ${salle.nomSalle}")
        println("=== SALLE SÉLECTIONNÉE ===")
        println("ID: ${salle.id}")
        println("Nom: ${salle.nomSalle}")
        println("Code: ${salle.code}")
        println("Bâtiment: ${salle.batiment}")
        println("Étage: ${salle.etage}")
        println("Composante: ${salle.composante}")
        println("Coordonnées: ${salle.coord}")
        println("========================")

        Toast.makeText(
            context,
            "Salle: ${salle.nomSalle}\nBâtiment: ${salle.batiment}",
            Toast.LENGTH_SHORT
        ).show()
    }

    // 🪟 Afficher les résultats de recherche (dialogue)
    private fun showSearchResults(results: List<Salle>) {
        if (results.isEmpty()) {
            Toast.makeText(context, "Aucune salle trouvée", Toast.LENGTH_SHORT).show()
            return
        }

        val items = results.map { "${it.nomSalle} - ${it.batiment}" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Résultats (${results.size})")
            .setItems(items) { _, which ->
                handleSalleSelection(results[which])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    fun getSelectedSalle(): Salle? = selectedSalle

    // Récupérer toutes les salles (utile pour d'autres utilisations)
    fun getAllSalles(): List<Salle> = allSalles
}